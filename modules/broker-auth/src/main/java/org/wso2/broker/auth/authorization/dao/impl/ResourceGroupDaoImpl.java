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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.auth.authorization.dao.ResourceGroupDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Class implements {@link ResourceGroupDao} to provide database functionality to manage permission storage for resource
 * groups.
 */
public class ResourceGroupDaoImpl extends ResourceGroupDao {

    public ResourceGroupDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void persist(String resourceGroup, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException {
        Connection connection = null;
        PreparedStatement insertPermissionsStmt = null;
        try {
            connection = getConnection();
            insertPermissionsStmt = connection.prepareStatement(RDBMSConstants.PS_INSERT_RESOURCE_GROUP_PERMISSION);
            for (Map.Entry<String, Integer> permissionEntry : userGroupPermissionMap.entrySet()) {
                insertPermissionsStmt.setString(1, resourceGroup);
                insertPermissionsStmt.setString(2, permissionEntry.getKey());
                insertPermissionsStmt.setInt(3, permissionEntry.getValue());
                insertPermissionsStmt.addBatch();
            }
            insertPermissionsStmt.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            throw new BrokerAuthException("Error occurred while persisting resource group permissions.", e);
        } finally {
            close(connection, insertPermissionsStmt);
        }
    }

    @Override
    @SuppressFBWarnings("SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING")
    public void delete(String resourceGroup, List<String> userGroups) throws BrokerAuthException {
        Connection connection = null;
        PreparedStatement deletePermissionsStmt = null;
        try {
            String idList = getSQLFormattedIdList(userGroups.size());
            connection = getConnection();
            deletePermissionsStmt = connection.prepareStatement(
                    "DELETE FROM MB_RESOURCE_PERMISSION WHERE  RESOURCE_GROUP_NAME = ? AND USER_GROUP_ID IN (" + idList
                            + ") ");
            deletePermissionsStmt.setString(1, resourceGroup);
            for (int i = 0; i < userGroups.size(); i++) {
                deletePermissionsStmt.setString(i + 2, userGroups.get(i));
            }
            deletePermissionsStmt.execute();
            connection.commit();

        } catch (SQLException e) {
            throw new BrokerAuthException("Error occurred while deleting resource group permissions.", e);
        } finally {
            close(connection, deletePermissionsStmt);
        }
    }

    @Override
    public void update(String resourceGroup, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException {
        Connection connection = null;
        PreparedStatement updatePermissionsStmt = null;
        try {
            connection = getConnection();
            updatePermissionsStmt = connection.prepareStatement(RDBMSConstants.PS_UPDATE_RESOURCE_GROUP_PERMISSION);
            for (Map.Entry<String, Integer> permissionEntry : userGroupPermissionMap.entrySet()) {
                updatePermissionsStmt.setInt(1, permissionEntry.getValue());
                updatePermissionsStmt.setString(2, resourceGroup);
                updatePermissionsStmt.setString(3, permissionEntry.getKey());
                updatePermissionsStmt.addBatch();
            }
            updatePermissionsStmt.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            throw new BrokerAuthException("Error occurred while updating resource group permissions.", e);
        } finally {
            close(connection, updatePermissionsStmt);
        }
    }

    @Override
    public void delete(String resourceGroup) throws BrokerAuthException {
        Connection connection = null;
        PreparedStatement deletePermissionsStmt = null;
        try {
            connection = getConnection();
            deletePermissionsStmt = connection.prepareStatement(RDBMSConstants.PS_DELETE_RESOURCE_GROUP_PERMISSION);
            deletePermissionsStmt.setString(1, resourceGroup);
            deletePermissionsStmt.execute();
            connection.commit();
        } catch (SQLException e) {
            throw new BrokerAuthException("Error deleting resource group.", e);
        } finally {
            close(connection, deletePermissionsStmt);
        }
    }

    @Override
    public Map<String, Integer> retrieve(String resourceGroup) throws BrokerAuthException {
        Map<String, Integer> userGroupPermissions = new HashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(RDBMSConstants.PS_SELECT_RESOURCE_GROUP_PERMISSION);
            statement.setString(1, resourceGroup);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                userGroupPermissions.put(resultSet.getString(1), resultSet.getInt(2));
            }
        } catch (SQLException e) {
            throw new BrokerAuthException("Error occurred while retrieving permissions for give resource group: "
                                                  + resourceGroup, e);
        } finally {
            close(connection, statement, resultSet);
        }
        return userGroupPermissions;
    }

    @Override
    public Map<String, Map<String, Integer>> readAll() throws BrokerAuthException {
        HashMap<String, Map<String, Integer>> resourceGroupPermissions = new HashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            connection = getConnection();
            statement = connection.prepareStatement(RDBMSConstants.PS_SELECT_RESOURCE_GROUP_PERMISSION_ALL);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String resourceGroup = resultSet.getString(1);
                Map<String, Integer> userGroupPermissions = resourceGroupPermissions.get(resourceGroup);
                if (userGroupPermissions == null) {
                    userGroupPermissions = new HashMap<>();
                    resourceGroupPermissions.put(resourceGroup, userGroupPermissions);
                }
                userGroupPermissions.put(resultSet.getString(2), resultSet.getInt(3));
            }
        } catch (SQLException e) {
            throw new BrokerAuthException("Error occurred while retrieving resource groups permissions.", e);
        } finally {
            close(connection, statement, resultSet);
        }
        return resourceGroupPermissions;
    }
}

