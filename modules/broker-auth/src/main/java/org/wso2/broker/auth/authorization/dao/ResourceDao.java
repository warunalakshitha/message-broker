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

package org.wso2.broker.auth.authorization.dao;

import com.google.common.collect.Table;
import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.common.BaseDao;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Defines functionality required at persistence layer for managing resource permissions.
 */
public abstract class ResourceDao extends BaseDao {

    public ResourceDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Persist resource permission in the database
     *
     * @param resourceGroup          Resource Group
     * @param resource               Resource
     * @param userGroupPermissionMap User group permission map
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract void persist(String resourceGroup, String resource, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException;

    /**
     * Update resource permission in the database
     *
     * @param resourceGroup          Resource Group
     * @param resource               Resource
     * @param userGroupPermissionMap User group permission map
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract void update(String resourceGroup, String resource, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException;

    /**
     * Delete resource from database on given user group
     *
     * @param resourceGroup Resource Group
     * @param resource      Resource
     * @param userGroups    List of user groups need to remove the permissions
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract void delete(String resourceGroup, String resource, List<String> userGroups)
            throws BrokerAuthException;

    /**
     * Delete resource from database
     *
     * @param resourceGroup Resource Group
     * @param resource      Resource
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract void delete(String resourceGroup, String resource)
            throws BrokerAuthException;

    public abstract Map<String, Integer> read(String resourceGroup, String resource) throws BrokerAuthException;

    public abstract Map<String, Map<String, Integer>> read(String resourceGroup) throws BrokerAuthException;

    /**
     * Retrieve all resource user permissions
     *
     * @return Table of resource group,resource and user group permissions
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract Table<String, String, Map<String, Integer>> readAll() throws BrokerAuthException;

}
