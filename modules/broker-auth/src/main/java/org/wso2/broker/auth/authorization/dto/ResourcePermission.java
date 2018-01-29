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

import com.google.common.collect.Table;
import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.auth.authorization.dao.ResourceDao;
import org.wso2.broker.common.FastTopicMatcher;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Class represents resource permission.
 * This class will provide functionality to add,remove,update and read resource permissions.
 */
public class ResourcePermission {

    private ResourceDao resourceDao;

    private Table<String, String, Map<String, Integer>> resourcePermissions;

    private Map<String, FastTopicMatcher> fastTopicMatcherMap;

    public ResourcePermission(ResourceDao resourceDao) throws BrokerAuthException {
        this.resourceDao = resourceDao;
        resourcePermissions = readPermission();
        fastTopicMatcherMap = new HashMap<>();
        resourcePermissions.rowMap()
                           .entrySet()
                           .forEach(entry -> {
                               final FastTopicMatcher fastTopicMatcher = new FastTopicMatcher();
                               fastTopicMatcherMap.put(entry.getKey(), fastTopicMatcher);
                               entry.getValue()
                                    .keySet()
                                    .forEach(fastTopicMatcher::add);
                           });
    }

    /**
     * Add resource permission
     *
     * @param resourceGroup          Resource group
     * @param resource               Resource
     * @param userGroupPermissionMap User group permission map
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public void grantPermission(String resourceGroup, String resource, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException {
        Map<String, Integer> shallowCopy = new HashMap<>();
        shallowCopy.putAll(userGroupPermissionMap);
        resourceDao.persist(resourceGroup, resource, shallowCopy);
        Map<String, Integer> exPermissions = resourcePermissions.get(resourceGroup, resource);
        if (exPermissions != null) {
            shallowCopy.entrySet()
                       .forEach(entry -> exPermissions.put(entry.getKey(), entry.getValue()));
        } else {
            resourcePermissions.put(resourceGroup, resource, shallowCopy);
        }
        FastTopicMatcher fastTopicMatcher = fastTopicMatcherMap.get(resourceGroup);
        if (fastTopicMatcher == null) {
            fastTopicMatcher = new FastTopicMatcher();
            fastTopicMatcherMap.put(resourceGroup, fastTopicMatcher);
        }
        fastTopicMatcher.add(resource);
    }

    /**
     * Update resource permission
     *
     * @param resourceGroup          Resource group
     * @param resource               Resource
     * @param userGroupPermissionMap User group permission map
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public void updatePermission(String resourceGroup, String resource, Map<String, Integer> userGroupPermissionMap)
            throws BrokerAuthException {
        Map<String, Integer> shallowCopy = new HashMap<>();
        shallowCopy.putAll(userGroupPermissionMap);
        resourceDao.update(resourceGroup, resource, shallowCopy);
        Map<String, Integer> exPermissions = resourcePermissions.get(resourceGroup, resource);
        if (exPermissions != null) {
            shallowCopy.entrySet()
                       .forEach((entry) -> {
                           if (exPermissions.containsKey(entry.getKey())) {
                               exPermissions.put(entry.getKey(), entry.getValue());
                           }
                       });
        } else {
            resourcePermissions.put(resourceGroup, resource, shallowCopy);
        }
    }

    /**
     * Delete resource
     *
     * @param resourceGroup Resource group
     * @param resource      Resource
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public void deletePermission(String resourceGroup, String resource)
            throws BrokerAuthException {
        resourceDao.delete(resourceGroup, resource);
        resourcePermissions.remove(resourceGroup, resource);
        FastTopicMatcher fastTopicMatcher = fastTopicMatcherMap.get(resourceGroup);
        if (fastTopicMatcher != null) {
            fastTopicMatcher.remove(resource);
        }
    }

    /**
     * Revoke resource permission from user groups
     *
     * @param resourceGroup Resource group
     * @param resource      Resource
     * @param userGroups    User group array
     * @throws BrokerAuthException Exception if adding permission failed
     */

    public void revokePermission(String resourceGroup, String resource, List<String> userGroups)
            throws BrokerAuthException {
        List<String> shallowCopy = new LinkedList<>();
        shallowCopy.addAll(userGroups);
        resourceDao.delete(resourceGroup, resource, shallowCopy);
        Map<String, Integer> exPermissions = resourcePermissions.get(resourceGroup, resource);
        if (exPermissions != null) {
            shallowCopy.forEach(exPermissions::remove);
        }
    }

    /**
     * Get resource permission
     *
     * @param resourceGroup Resource group
     * @param resource      Resource
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public Map<String, Integer> readPermission(String resourceGroup, String resource)
            throws BrokerAuthException {

        Map<String, Integer> permissionsMap = resourcePermissions.get(resourceGroup, resource);
        if (permissionsMap == null || permissionsMap.size() == 0) {
            permissionsMap = resourceDao.read(resourceGroup, resource);
            resourcePermissions.put(resourceGroup, resource, permissionsMap);
            FastTopicMatcher fastTopicMatcher = fastTopicMatcherMap.get(resourceGroup);
            if (permissionsMap.size() > 0) {
                if (fastTopicMatcher == null) {
                    fastTopicMatcher = new FastTopicMatcher();
                    fastTopicMatcherMap.put(resourceGroup, fastTopicMatcher);
                }
                fastTopicMatcher.add(resource);
            }
        }
        return permissionsMap;
    }

    /**
     * Get resource Group permission
     *
     * @param resourceGroup Resource group
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public Map<String, Map<String, Integer>> readPermission(String resourceGroup)
            throws BrokerAuthException {
        Map<String, Map<String, Integer>> permissionsMap = resourcePermissions.row(resourceGroup);
        if (permissionsMap == null || permissionsMap.size() == 0) {
            permissionsMap = resourceDao.read(resourceGroup);
            final FastTopicMatcher fastTopicMatcher = new FastTopicMatcher();
            fastTopicMatcherMap.put(resourceGroup, fastTopicMatcher);
            permissionsMap.entrySet()
                          .forEach(e -> {
                              resourcePermissions.put(resourceGroup, e.getKey(), e.getValue());
                              fastTopicMatcher.add(e.getKey());
                          });
        }
        return permissionsMap;
    }

    /**
     * Get all resource permissions
     *
     * @throws BrokerAuthException Exception if adding permission failed
     */
    public Table<String, String, Map<String, Integer>> readPermission()
            throws BrokerAuthException {
        return resourceDao.readAll();
    }

    public Map<String, FastTopicMatcher> getFastTopicMatcherMap() {
        return fastTopicMatcherMap;
    }

    public Table<String, String, Map<String, Integer>> getResourcePermissions() {
        return resourcePermissions;
    }
}
