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
package org.wso2.broker.auth.authorization;

import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.auth.authorization.dao.ResourceDao;
import org.wso2.broker.auth.authorization.dao.ResourceGroupDao;
import org.wso2.broker.auth.authorization.dto.ResourceGroupPermission;
import org.wso2.broker.auth.authorization.dto.ResourcePermission;
import org.wso2.broker.common.FastTopicMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class manages permissions to authorize actions on resources and resource groups
 */
public class PermissionStore {

    private ResourcePermission resourcePermission;

    private ResourceGroupPermission resourceGroupPermission;

    public PermissionStore(ResourceDao resourceDao, ResourceGroupDao resourceGroupDao) throws BrokerAuthException {
        resourcePermission = new ResourcePermission(resourceDao);
        resourceGroupPermission = new ResourceGroupPermission(resourceGroupDao);
    }

    /**
     * Authorise resource of resource group for given user groups based on permission value.
     * This will check whether user groups have permission to exactly matched resource
     *
     * @param resourceGroup Resource Group
     * @param resource      Resource
     * @param userGroups    user group set
     * @param permission    permission
     * @return isAuthorised or not
     */
    public boolean authorize(String resourceGroup, String resource, Set<String> userGroups,
                             int permission) throws BrokerAuthException {
        // First check Global permissions
        boolean isAuthorized = authorizeResourceGroup(resourceGroup, userGroups, permission);
        // if user does not have group permission then we need to check resource permission
        if (!isAuthorized) {
            Map<String, Integer> userPermissions = resourcePermission.readPermission(resourceGroup, resource);
            return userPermissions.entrySet()
                                  .stream()
                                  .filter((entry) -> (entry.getValue() & permission) == permission && userGroups
                                          .contains(entry.getKey()))
                                  .findFirst()
                                  .isPresent();
        }
        return true;
    }

    /**
     * Authorise resource pattern of resource group for given user groups based on permission value.
     * This will check whether user groups have permission to resource which will match any of the authorised resource
     * pattern.
     *
     * @param resourceGroup   Resource Group
     * @param resource Resource pattern
     * @param userGroups      user group set
     * @param permission      permission
     * @return isAuthorised or not
     */
    public boolean authorizeByPattern(String resourceGroup, String resource, Set<String> userGroups,
                                      int permission) throws BrokerAuthException {
        // First check Global permissions
        boolean isAuthorised = authorizeResourceGroup(resourceGroup, userGroups, permission);
        // if user does not have group permission then we need to check resource pattern permission
        if (!isAuthorised) {
            Map<String, Map<String, Integer>> resources = resourcePermission.readPermission(resourceGroup);
            FastTopicMatcher fastTopicMatcher = resourcePermission.getFastTopicMatcherMap().get(resourceGroup);
            List<String> matchingList = new ArrayList<>();
            fastTopicMatcher.matchingBindings(resource, matchingList::add);
            return matchingList.size() > 0 &&
                    matchingList
                            .stream()
                            .filter(s -> resources.get(s)
                                                  .entrySet()
                                                  .stream()
                                                  .filter(emp -> (emp.getValue() & permission) == permission
                                                          && userGroups.contains(emp.getKey()))
                                                  .findFirst()
                                                  .isPresent())
                            .findFirst()
                            .isPresent();
        }
        return true;
    }

    /**
     * Authorise resource resource group for given user groups based on permission value.
     *
     * @param resourceGroup Resource Group
     * @param userGroups    user group set
     * @param permission    permission
     * @return isAuthorised or not
     */
    private boolean authorizeResourceGroup(String resourceGroup, Set<String> userGroups,
                                           int permission) throws BrokerAuthException {
        Map<String, Integer> userGroupPermissions = resourceGroupPermission.readPermission(resourceGroup);
        return userGroupPermissions.entrySet()
                                   .stream()
                                   .filter((entry) -> (entry.getValue() & permission) == permission && userGroups
                                           .contains(entry.getKey()))
                                   .findFirst()
                                   .isPresent();
    }

    /**
     * Provides the resource data object for resource level operations
     *
     * @return Resource
     */
    public ResourcePermission getResourcePermission() {
        return resourcePermission;
    }

    /**
     * Provides the resource group data object for resource level operations
     *
     * @return Resource group
     */
    public ResourceGroupPermission getResourceGroupPermission() {
        return resourceGroupPermission;
    }

}



