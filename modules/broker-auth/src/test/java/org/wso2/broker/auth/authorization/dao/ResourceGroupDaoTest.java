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
package org.wso2.broker.auth.authorization.dao;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.auth.DbUtil;
import org.wso2.broker.auth.authorization.PermissionStore;
import org.wso2.broker.auth.authorization.dao.impl.ResourceDaoImpl;
import org.wso2.broker.auth.authorization.dao.impl.ResourceGroupDaoImpl;
import org.wso2.broker.auth.authorization.enums.RGroups;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Class for testing  for {@link ResourceGroupDao} operations.
 */
public class ResourceGroupDaoTest {

    private DataSource dataSource;

    private PermissionStore permissionStore;

    private static final String MANAGER_ROLE = "manager";

    private static final String CAPTAIN_ROLE = "captain";

    private static final String PLAYER_ROLE = "cricketPlayer";

    private Map<String, Integer> ePermissions, qPermissions, bindPermissions;

    @BeforeClass
    public void beforeTest() throws BrokerAuthException {
        dataSource = DbUtil.getDataSource();
        permissionStore = new PermissionStore(new ResourceDaoImpl(dataSource), new ResourceGroupDaoImpl(dataSource));
        ePermissions = new HashMap<>();
        qPermissions = new HashMap<>();
        bindPermissions = new HashMap<>();
        ePermissions.put(MANAGER_ROLE, 3);
        ePermissions.put(CAPTAIN_ROLE, 2);
        qPermissions.put(MANAGER_ROLE, 3);
        bindPermissions.put(CAPTAIN_ROLE, 2);
    }

    @AfterClass
    public void cleanup() throws Exception {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("DELETE FROM MB_RESOURCE_GROUP_PERMISSION");
        connection.commit();
        statement.close();
        connection.close();
    }

    @Test(priority = 1,
          description = "Test persist resource group permissions")
    public void testPersist() throws Exception {

        permissionStore.getResourceGroupPermission().grantPermission(RGroups.EXCHANGE.toString(), ePermissions);
        permissionStore.getResourceGroupPermission().grantPermission(RGroups.QUEUE.toString(), qPermissions);
        permissionStore.getResourceGroupPermission().grantPermission(RGroups.BINDING_KEY.toString(), bindPermissions);

        Map<String, Integer> exchanges = permissionStore.getResourceGroupPermission()
                                                        .readPermission(RGroups.EXCHANGE.toString());
        Map<String, Integer> bindings = permissionStore.getResourceGroupPermission()
                                                       .readPermission(RGroups.BINDING_KEY.toString());
        Map<String, Integer> queues = permissionStore.getResourceGroupPermission()
                                                     .readPermission(RGroups.QUEUE.toString());

        Assert.assertEquals(exchanges.get(MANAGER_ROLE).intValue(), 3, "Manager role permissions are incorrect");
        Assert.assertEquals(exchanges.get(CAPTAIN_ROLE).intValue(), 2, "Captain role permissions are incorrect");
        Assert.assertEquals(exchanges.get(PLAYER_ROLE), null, "Player role permissions should not be exist");
        Assert.assertEquals(queues.get(MANAGER_ROLE).intValue(), 3, "Manager role permissions are incorrect");
        Assert.assertEquals(bindings.get(CAPTAIN_ROLE).intValue(), 2, "Captain role permissions are incorrect");
        Assert.assertEquals(bindings.get(MANAGER_ROLE), null, "Manager role permissions should not be exist");
        Assert.assertEquals(queues.get(CAPTAIN_ROLE), null, "Captain role permissions should not be exist");

    }

    @Test(priority = 2,
          description = "Test persist duplicate resource group permissions",
          expectedExceptions = BrokerAuthException.class,
          expectedExceptionsMessageRegExp = ".*Error occurred while persisting resource group permissions.*")
    public void testPersistDuplicate() throws Exception {
        permissionStore.getResourceGroupPermission().grantPermission(RGroups.EXCHANGE.toString(), ePermissions);
    }

    @Test(priority = 3,
          description = "Test persist resource group permissions again")
    public void testNewPersist() throws Exception {
        ePermissions.clear();
        ePermissions.put(PLAYER_ROLE, 1);
        permissionStore.getResourceGroupPermission().grantPermission(RGroups.EXCHANGE.toString(), ePermissions);
        Map<String, Integer> permissions = permissionStore.getResourceGroupPermission()
                                                          .readPermission(RGroups.EXCHANGE.toString());
        Assert.assertEquals(permissions.get(MANAGER_ROLE).intValue(), 3, "Manager role permissions are incorrect");
        Assert.assertEquals(permissions.get(CAPTAIN_ROLE).intValue(), 2, "Captain role permissions are incorrect");
        Assert.assertEquals(permissions.get(PLAYER_ROLE).intValue(), 1, "Player role permissions are incorrect");
    }

    @Test(priority = 4,
          description = "Test update resource group permissions")
    public void testUpdate() throws Exception {
        ePermissions.clear();
        qPermissions.clear();
        bindPermissions.clear();
        ePermissions.put(MANAGER_ROLE, 8);
        ePermissions.put(CAPTAIN_ROLE, 5);
        qPermissions.put(MANAGER_ROLE, 8);
        qPermissions.put(CAPTAIN_ROLE, 6);
        bindPermissions.put(CAPTAIN_ROLE, 5);
        bindPermissions.put(MANAGER_ROLE, 11);
        permissionStore.getResourceGroupPermission().updatePermission(RGroups.EXCHANGE.toString(), ePermissions);
        permissionStore.getResourceGroupPermission().updatePermission(RGroups.QUEUE.toString(), qPermissions);
        permissionStore.getResourceGroupPermission().updatePermission(RGroups.BINDING_KEY.toString(), bindPermissions);

        Map<String, Integer> exchanges = permissionStore.getResourceGroupPermission()
                                                        .readPermission(RGroups.EXCHANGE.toString());
        Map<String, Integer> bindings = permissionStore.getResourceGroupPermission()
                                                       .readPermission(RGroups.BINDING_KEY.toString());
        Map<String, Integer> queues = permissionStore.getResourceGroupPermission()
                                                     .readPermission(RGroups.QUEUE.toString());

        Assert.assertEquals(exchanges.get(MANAGER_ROLE).intValue(), 8, "Manager role permissions are incorrect");
        Assert.assertEquals(exchanges.get(CAPTAIN_ROLE).intValue(), 5, "Captain role permissions are incorrect");
        Assert.assertEquals(exchanges.get(PLAYER_ROLE).intValue(), 1, "Player role permissions are incorrect");
        Assert.assertEquals(queues.get(MANAGER_ROLE).intValue(), 8, "Manager role permissions are incorrect");
        Assert.assertEquals(bindings.get(CAPTAIN_ROLE).intValue(), 5, "Captain role permissions are incorrect");
        Assert.assertEquals(bindings.get(MANAGER_ROLE), null, "Manager role permissions should not be exist");
        Assert.assertEquals(queues.get(CAPTAIN_ROLE), null, "Captain role permissions should not be exist");
    }

    @Test(priority = 5,
          description = "Test retrieve resource group permissions")
    public void testRetrieve() throws Exception {
        Map<String, Integer> exchanges = permissionStore.getResourceGroupPermission()
                                                        .readPermission(RGroups.EXCHANGE.toString());
        Map<String, Integer> queues = permissionStore.getResourceGroupPermission()
                                                     .readPermission(RGroups.QUEUE.toString());
        Map<String, Integer> bindings = permissionStore.getResourceGroupPermission()
                                                       .readPermission(RGroups.BINDING_KEY.toString());

        Assert.assertEquals(exchanges.size(), 3, "Invalid number of permissions");
        Assert.assertEquals(queues.size(), 1, "Invalid number of permissions");
        Assert.assertEquals(bindings.size(), 1, "Invalid number of permissions");
    }

    @Test(priority = 5,
          description = "Test retrieve all resource group permissions")
    public void testRetrieveAll() throws Exception {
        Map<String, Map<String, Integer>> allPermissions = permissionStore.getResourceGroupPermission()
                                                                          .readPermission();
        Assert.assertEquals(allPermissions.size(), 3, "Invalid number of permissions");
    }

    @Test(priority = 6,
          description = "Test delete resource group permissions")
    public void testDelete() throws Exception {
        LinkedList<String> exRevokeList = new LinkedList<>();
        exRevokeList.add(CAPTAIN_ROLE);
        exRevokeList.add(PLAYER_ROLE);
        permissionStore.getResourceGroupPermission().revokePermission(RGroups.EXCHANGE.toString(), exRevokeList);
        Map<String, Integer> exchanges = permissionStore.getResourceGroupPermission()
                                                        .readPermission(RGroups.EXCHANGE.toString());
        Assert.assertEquals(exchanges.get(MANAGER_ROLE).intValue(), 8, "Manager role permissions are incorrect");
        Assert.assertEquals(exchanges.get(CAPTAIN_ROLE), null, "Captain role permissions should not be exist");
        Assert.assertEquals(exchanges.get(PLAYER_ROLE), null, "Player role permissions should not be exist");
    }

    @Test(priority = 7,
          description = "Test delete resource group")
    public void testDeleteResourceGroup() throws Exception {
        permissionStore.getResourceGroupPermission().deletePermission(RGroups.EXCHANGE.toString());
        Map<String, Integer> exchanges = permissionStore.getResourceGroupPermission()
                                                        .readPermission(RGroups.EXCHANGE.toString());
        Assert.assertEquals(exchanges.size(), 0, "Resource group should not be exist");
    }
}
