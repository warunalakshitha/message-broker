/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.broker.auth.authorization.dao.impl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.auth.authorization.dao.ResourceDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Class implements {@link ResourceDao} to provide database functionality to manage permission storage for resources.
 */
public class ResourceDaoImpl extends ResourceDao {

    public ResourceDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void persist(String resourceGroup, String resource, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException {
        Connection connection = null;
        PreparedStatement insertPermissionsStmt = null;
        try {
            connection = getConnection();
            insertPermissionsStmt = connection.prepareStatement(RDBMSConstants.PS_INSERT_RESOURCE_PERMISSION);
            for (Map.Entry<String, Integer> permissionEntry : userGroupPermissionMap.entrySet()) {
                insertPermissionsStmt.setString(1, resourceGroup);
                insertPermissionsStmt.setString(2, resource);
                insertPermissionsStmt.setString(3, permissionEntry.getKey());
                insertPermissionsStmt.setInt(4, permissionEntry.getValue());
                insertPermissionsStmt.addBatch();
            }
            insertPermissionsStmt.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            throw new BrokerAuthException("Error persisting permissions.", e);
        } finally {
            close(connection, insertPermissionsStmt);
        }
    }

    @Override
    @SuppressFBWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
    public void delete(String resourceGroup, String resource, List<String> userGroups) throws BrokerAuthException {
        Connection connection = null;
        PreparedStatement deletePermissionsStmt = null;
        try {
            String idList = getSQLFormattedIdList(userGroups.size());
            connection = getConnection();
            deletePermissionsStmt = connection.prepareStatement(
                    "DELETE FROM MB_RESOURCE_PERMISSION WHERE RESOURCE_GROUP_NAME = ? AND RESOURCE_NAME= ? AND "
                            + "USER_GROUP_ID IN (" + idList + ") ");
            deletePermissionsStmt.setString(1, resourceGroup);
            deletePermissionsStmt.setString(2, resource);
            for (int i = 0; i < userGroups.size(); i++) {
                deletePermissionsStmt.setString(i + 3, userGroups.get(i));
            }
            deletePermissionsStmt.execute();
            connection.commit();

        } catch (SQLException e) {
            throw new BrokerAuthException("Error deleting permissions.", e);
        } finally {
            close(connection, deletePermissionsStmt);
        }
    }

    @Override
    public void delete(String resourceGroup, String resource) throws BrokerAuthException {
        Connection connection = null;
        PreparedStatement deletePermissionsStmt = null;
        try {
            connection = getConnection();
            deletePermissionsStmt = connection.prepareStatement(RDBMSConstants.PS_DELETE_RESOURCE_PERMISSION);
            deletePermissionsStmt.setString(1, resourceGroup);
            deletePermissionsStmt.setString(2, resource);
            deletePermissionsStmt.execute();
            connection.commit();
        } catch (SQLException e) {
            throw new BrokerAuthException("Error deleting permissions.", e);
        } finally {
            close(connection, deletePermissionsStmt);
        }
    }
    @Override
    public Map<String, Integer> read(String resourceGroup, String resource) throws BrokerAuthException {
        Map<String, Integer> userGroupPermissions = new HashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(RDBMSConstants.PS_SELECT_RESOURCE_PERMISSION);
            statement.setString(1, resourceGroup);
            statement.setString(2, resource);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                userGroupPermissions.put(resultSet.getString(1), resultSet.getInt(2));
            }
        } catch (SQLException e) {
            throw new BrokerAuthException("Error occurred while retrieving exchanges", e);
        } finally {
            close(connection, statement, resultSet);
        }
        return userGroupPermissions;
    }

    @Override
    public Map<String, Map<String, Integer>> read(String resourceGroup) throws BrokerAuthException {
        Map<String, Map<String, Integer>> userGroupPermissions = new HashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(RDBMSConstants.PS_SELECT_RESOURCE_PERMISSION_BY_GROUP);
            statement.setString(1, resourceGroup);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String resourceName = resultSet.getString(1);
                Map<String, Integer> permissionMap = userGroupPermissions.get(resourceName);
                if (permissionMap == null) {
                    permissionMap = new HashMap<>();
                    userGroupPermissions.put(resourceName, permissionMap);
                }
                permissionMap.put(resultSet.getString(2), resultSet.getInt(3));
            }
        } catch (SQLException e) {
            throw new BrokerAuthException("Error occurred while retrieving exchanges", e);
        } finally {
            close(connection, statement, resultSet);
        }
        return userGroupPermissions;
    }

    @Override
    public void update(String resourceGroup, String resource, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException {
        Connection connection = null;
        PreparedStatement updatePermissionsStmt = null;
        try {
            connection = getConnection();
            updatePermissionsStmt = connection.prepareStatement(RDBMSConstants.PS_UPDATE_RESOURCE_PERMISSION);
            for (Map.Entry<String, Integer> permissionEntry : userGroupPermissionMap.entrySet()) {
                updatePermissionsStmt.setInt(1, permissionEntry.getValue());
                updatePermissionsStmt.setString(2, resourceGroup);
                updatePermissionsStmt.setString(3, resource);
                updatePermissionsStmt.setString(4, permissionEntry.getKey());
                updatePermissionsStmt.addBatch();
            }
            updatePermissionsStmt.executeBatch();
            connection.commit();
        } catch (SQLException e) {
            throw new BrokerAuthException("Error updating permissions.", e);
        } finally {
            close(connection, updatePermissionsStmt);
        }
    }

    @Override
    public Table<String, String, Map<String, Integer>> readAll() throws BrokerAuthException {
        Table<String, String, Map<String, Integer>> resourcePermissions = HashBasedTable.create();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(RDBMSConstants.PS_SELECT_RESOURCE_PERMISSION_ALL);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String resourceGroup = resultSet.getString(1);
                String resource = resultSet.getString(2);
                Map<String, Integer> permissionMap;
                Map<String, Map<String, Integer>> resources = resourcePermissions.row(resourceGroup);
                if (resources == null || (permissionMap = resources.get(resource)) == null) {
                    permissionMap = new HashMap<>();
                    resourcePermissions.put(resourceGroup, resource, permissionMap);
                }
                permissionMap.put(resultSet.getString(3), resultSet.getInt(4));
            }
        } catch (SQLException e) {
            throw new BrokerAuthException("Error occurred while retrieving exchanges", e);
        } finally {
            close(connection, statement, resultSet);
        }
        return resourcePermissions;
    }
}

