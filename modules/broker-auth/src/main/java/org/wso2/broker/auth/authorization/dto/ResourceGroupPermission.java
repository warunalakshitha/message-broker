/*
 *   Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.wso2.broker.auth.authorization.dto;

import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.auth.authorization.dao.ResourceGroupDao;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class represents resource group permission.
 * This class will provide functionality to add,remove,update and retrieve resource group permissions.
 */
public class ResourceGroupPermission {

    private ResourceGroupDao resourceGroupDao;

    private Map<String, Map<String, Integer>> resourceGroupPermissions = new HashMap<>();

    public ResourceGroupPermission(ResourceGroupDao resourceGroupDao)
            throws BrokerAuthException {
        this.resourceGroupDao = resourceGroupDao;
    }

    /**
     * Add resource group permission
     *
     * @param resourceGroup          Resource group
     * @param userGroupPermissionMap User group permission map
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public void grantPermission(String resourceGroup, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException {
        Map<String, Integer> shallowCopy = new HashMap<>();
        shallowCopy.putAll(userGroupPermissionMap);
        resourceGroupDao.persist(resourceGroup, shallowCopy);
        Map<String, Integer> exPermissions = resourceGroupPermissions.get(resourceGroup);
        if (exPermissions != null) {
            shallowCopy.entrySet().forEach(entry -> exPermissions.put(entry.getKey(), entry.getValue()));
        } else {
            resourceGroupPermissions.put(resourceGroup, shallowCopy);
        }
    }

    /**
     * Update resource group permission
     *
     * @param resourceGroup          Resource group
     * @param userGroupPermissionMap User group permission map
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public void updatePermission(String resourceGroup, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException {
        Map<String, Integer> shallowCopy = new HashMap<>();
        shallowCopy.putAll(userGroupPermissionMap);
        resourceGroupDao.update(resourceGroup, shallowCopy);
        Map<String, Integer> exPermissions = resourceGroupPermissions.get(resourceGroup);
        if (exPermissions != null) {
            shallowCopy.entrySet()
                       .forEach((entry) -> {
                           if (exPermissions.containsKey(entry.getKey())) {
                               exPermissions.put(entry.getKey(), entry.getValue());
                                 }
                       });
        } else {
            resourceGroupPermissions.put(resourceGroup, shallowCopy);
        }
    }

    /**
     * Delete resource group
     *
     * @param resourceGroup Resource group
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public void deletePermission(String resourceGroup)
            throws BrokerAuthException {
        resourceGroupDao.delete(resourceGroup);
        resourceGroupPermissions.remove(resourceGroup);
    }

    /**
     * Revoke resource group permission
     *
     * @param resourceGroup Resource group
     * @param userGroups    User group array
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public void revokePermission(String resourceGroup, List<String> userGroups)
            throws BrokerAuthException {
        List<String> shallowCopy = new LinkedList<>();
        shallowCopy.addAll(userGroups);
        resourceGroupDao.delete(resourceGroup, shallowCopy);
        Map<String, Integer> exPermissions = resourceGroupPermissions.get(resourceGroup);
        if (exPermissions != null) {
            shallowCopy.forEach(exPermissions::remove);
        }
    }

    /**
     * Get resource group permission
     *
     * @param resourceGroup Resource group
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public Map<String, Integer> readPermission(String resourceGroup)
            throws BrokerAuthException {
        Map<String, Integer> permissionsMap = resourceGroupPermissions.get(resourceGroup);
        if (permissionsMap == null || permissionsMap.size() == 0) {
            permissionsMap = resourceGroupDao.retrieve(resourceGroup);
            resourceGroupPermissions.put(resourceGroup, permissionsMap);
        }
        return permissionsMap;

    }

    /**
     * Get all resource group permission
     *
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public Map<String, Map<String, Integer>> readPermission() throws BrokerAuthException {
        return resourceGroupDao.readAll();

    }

    public Map<String, Map<String, Integer>> getResourceGroupPermissions() {
        return resourceGroupPermissions;
    }
}
