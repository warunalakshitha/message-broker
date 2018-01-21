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

import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.common.BaseDao;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Defines functionality required at persistence layer for managing resource group permissions.
 */
public abstract class ResourceGroupDao extends BaseDao {

    public ResourceGroupDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Persist resource group permissions for given user gropu
     *
     * @param resourceGroup          Resource group name
     * @param userGroupPermissionMap Map of user groups vs permissions
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract void persist(String resourceGroup, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException, BrokerAuthException;

    /**
     * Update resource group permissions for given user gropu
     *
     * @param resourceGroup          Resource group name
     * @param userGroupPermissionMap Map of user groups vs permissions
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract void update(String resourceGroup, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException;

    /**
     * Delete resource group permissions
     *
     * @param resourceGroup Resource group name
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract void delete(String resourceGroup) throws BrokerAuthException;

    /**
     * Persist resource group permissions for given user group
     *
     * @param resourceGroup Resource group name
     * @param userGroups    List of user groups need to remove the permissions
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract void delete(String resourceGroup, List<String> userGroups) throws BrokerAuthException;



    public abstract Map<String, Integer> retrieve(String resourceGroup) throws BrokerAuthException;
    /**
     * Retrieve all resource group permissions
     *
     * @return Map of resource group permissions vs user groups
     * @throws BrokerAuthException Throws when database operation failed.
     */
    public abstract Map<String, Map<String, Integer>> readAll() throws BrokerAuthException;

}
