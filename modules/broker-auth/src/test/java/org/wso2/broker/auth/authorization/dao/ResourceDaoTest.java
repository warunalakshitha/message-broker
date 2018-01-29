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

import com.google.common.collect.Table;
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
 * Class for testing  for {@link ResourceDao} operations.
 */
public class ResourceDaoTest {

    private DataSource dataSource;

    private PermissionStore permissionStore;

    private static final String MANAGER_ROLE = "manager";

    private static final String CAPTAIN_ROLE = "captain";

    private static final String PLAYER_ROLE = "cricketPlayer";

    private Map<String, Integer> ePermissions, qPermissions;

    @BeforeClass
    public void beforeTest() throws BrokerAuthException {
        dataSource = DbUtil.getDataSource();
        permissionStore = new PermissionStore(new ResourceDaoImpl(dataSource), new ResourceGroupDaoImpl(dataSource));
        ePermissions = new HashMap<>();
        qPermissions = new HashMap<>();
        ePermissions.put(MANAGER_ROLE, 3);
        ePermissions.put(CAPTAIN_ROLE, 2);
        qPermissions.put(MANAGER_ROLE, 3);
    }

    @AfterClass
    public void cleanup() throws Exception {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("DELETE FROM MB_RESOURCE_PERMISSION");
        connection.commit();
        statement.close();
        connection.close();
    }

    @Test(priority = 1,
          description = "Test persist resource permission")
    public void testPersist() throws Exception {
        permissionStore.getResourcePermission()
                       .grantPermission(RGroups.EXCHANGE.toString(), "amq.topic", ePermissions);
        permissionStore.getResourcePermission().grantPermission(RGroups.QUEUE.toString(), "queue1", qPermissions);

        Map<String, Integer> exchanges = permissionStore.getResourcePermission()
                                                        .readPermission(RGroups.EXCHANGE.toString(), "amq.topic");
        Map<String, Integer> queues = permissionStore.getResourcePermission()
                                                     .readPermission(RGroups.QUEUE.toString(), "queue1");

        Assert.assertEquals(exchanges.get(MANAGER_ROLE).intValue(), 3, "Manager role permissions are incorrect");
        Assert.assertEquals(exchanges.get(CAPTAIN_ROLE).intValue(), 2, "Captain role permissions are incorrect");
        Assert.assertEquals(exchanges.get(PLAYER_ROLE), null, "Player role permissions should not be exist");
        Assert.assertEquals(queues.get(MANAGER_ROLE).intValue(), 3, "Manager role permissions are incorrect");
        Assert.assertEquals(queues.get(CAPTAIN_ROLE), null, "Captain role permissions should not be exist");
    }

    @Test(priority = 2,
          description = "Test persist duplicate resource permission",
          expectedExceptions = BrokerAuthException.class,
          expectedExceptionsMessageRegExp = ".*Error occurred while resource persisting permissions.*")
    public void testPersistDuplicate() throws Exception {
        permissionStore.getResourcePermission()
                       .grantPermission(RGroups.EXCHANGE.toString(), "amq.topic", ePermissions);
    }

    @Test(priority = 3,
          description = "Test persist resource permission again")
    public void testNewPersist() throws Exception {
        ePermissions.clear();
        ePermissions.put(PLAYER_ROLE, 1);
        permissionStore.getResourcePermission().grantPermission(RGroups.EXCHANGE.toString(), "amq.topic", ePermissions);
        Map<String, Integer> permissions = permissionStore.getResourcePermission()
                                                          .readPermission(RGroups.EXCHANGE.toString(), "amq.topic");

        Assert.assertEquals(permissions.get(MANAGER_ROLE).intValue(), 3, "Manager role permissions are incorrect");
        Assert.assertEquals(permissions.get(CAPTAIN_ROLE).intValue(), 2, "Captain role permissions are incorrect");
        Assert.assertEquals(permissions.get(PLAYER_ROLE).intValue(), 1, "Player role permissions are incorrect");
    }

    @Test(priority = 4,
          description = "Test update resource permission")
    public void testUpdate() throws Exception {
        ePermissions.clear();
        qPermissions.clear();
        ePermissions.put(MANAGER_ROLE, 7);
        ePermissions.put(CAPTAIN_ROLE, 4);
        qPermissions.put(MANAGER_ROLE, 7);
        qPermissions.put(CAPTAIN_ROLE, 7);
        permissionStore.getResourcePermission().updatePermission(RGroups.EXCHANGE.toString(), "amq.topic",
                                                                 ePermissions);
        permissionStore.getResourcePermission().updatePermission(RGroups.QUEUE.toString(), "queue1", qPermissions);

        Map<String, Integer> exchanges = permissionStore.getResourcePermission()
                                                        .readPermission(RGroups.EXCHANGE.toString(), "amq.topic");
        Map<String, Integer> queues = permissionStore.getResourcePermission()
                                                     .readPermission(RGroups.QUEUE.toString(), "queue1");

        Assert.assertEquals(exchanges.get(MANAGER_ROLE).intValue(), 7, "Manager role permissions are incorrect");
        Assert.assertEquals(exchanges.get(CAPTAIN_ROLE).intValue(), 4, "Captain role permissions are incorrect");
        Assert.assertEquals(exchanges.get(PLAYER_ROLE).intValue(), 1, "Player role permissions are incorrect");
        Assert.assertEquals(queues.get(MANAGER_ROLE).intValue(), 7, "Manager role permissions are incorrect");
        Assert.assertEquals(queues.get(CAPTAIN_ROLE), null, "Captain role permissions should not be exist");
    }

    @Test(priority = 5,
          description = "Test retrieve resource permission")
    public void testRetrieve() throws Exception {

        ePermissions.clear();
        ePermissions.put(MANAGER_ROLE, 1);
        ePermissions.put(PLAYER_ROLE, 3);
        permissionStore.getResourcePermission().grantPermission(RGroups.EXCHANGE.toString(), "amq.direct",
                                                                ePermissions);
        Map<String, Integer> topicExchanges = permissionStore.getResourcePermission()
                                                             .readPermission(RGroups.EXCHANGE.toString(), "amq.topic");
        Map<String, Integer> directExchanges = permissionStore.getResourcePermission()
                                                              .readPermission(RGroups.EXCHANGE.toString(),
                                                                              "amq.direct");
        Map<String, Map<String, Integer>> exchanges = permissionStore.getResourcePermission()
                                                                     .readPermission(RGroups.EXCHANGE.toString());
        Map<String, Map<String, Integer>> queues = permissionStore.getResourcePermission()
                                                                  .readPermission(RGroups.QUEUE.toString());
        Map<String, Map<String, Integer>> bindings = permissionStore.getResourcePermission()
                                                                    .readPermission(RGroups.BINDING_KEY.toString());

        Assert.assertEquals(topicExchanges.get(MANAGER_ROLE).intValue(), 7, "Manager role permissions are incorrect");
        Assert.assertEquals(topicExchanges.get(CAPTAIN_ROLE).intValue(), 4, "Captain role permissions are incorrect");
        Assert.assertEquals(topicExchanges.get(PLAYER_ROLE).intValue(), 1, "Player role permissions are incorrect");
        Assert.assertEquals(directExchanges.get(MANAGER_ROLE).intValue(), 1, "Captain role permissions are incorrect");
        Assert.assertEquals(directExchanges.get(PLAYER_ROLE).intValue(), 3, "Player role permissions are incorrect");
        Assert.assertEquals(directExchanges.get(CAPTAIN_ROLE), null, "Captain role permissions should not be exist");
        Assert.assertEquals(exchanges.size(), 2, "Invalid number of permissions");
        Assert.assertEquals(queues.size(), 1, "Invalid number of permissions");
        Assert.assertEquals(bindings.size(), 0, "Invalid number of permissions");
    }

    @Test(priority = 6,
          description = "Test retrieve all resource permissions")
    public void testRetrieveAll() throws Exception {
        Table<String, String, Map<String, Integer>> allPermissions = permissionStore.getResourcePermission()
                                                                                    .readPermission();
        Assert.assertEquals(allPermissions.size(), 3, "Invalid number of permissions");
    }

    @Test(priority = 7,
          description = "Test delete resource permissions")
    public void testDelete() throws Exception {

        LinkedList<String> exRevokeList = new LinkedList<>();
        exRevokeList.add(CAPTAIN_ROLE);
        exRevokeList.add(PLAYER_ROLE);
        permissionStore.getResourcePermission().revokePermission(RGroups.EXCHANGE.toString(), "amq.topic",
                                                                 exRevokeList);
        Map<String, Integer> exchanges = permissionStore.getResourcePermission()
                                                        .readPermission(RGroups.EXCHANGE.toString(), "amq.topic");
        Map<String, Map<String, Integer>> queues = permissionStore.getResourcePermission()
                                                                  .readPermission(RGroups.QUEUE.toString());

        Assert.assertEquals(exchanges.get(MANAGER_ROLE).intValue(), 7, "Manager role permissions are incorrect");
        Assert.assertEquals(exchanges.get(CAPTAIN_ROLE), null, "Captain role permissions should not be exist");
        Assert.assertEquals(exchanges.get(PLAYER_ROLE), null, "Player role permissions should not be exist");
        Assert.assertEquals(exchanges.size(), 1, "Invalid number of permissions");
        Assert.assertEquals(queues.size(), 1, "Invalid number of permissions");
    }

    @Test(priority = 8,
          description = "Test delete resource")
    public void testDeleteResource() throws Exception {

        permissionStore.getResourcePermission().deletePermission(RGroups.EXCHANGE.toString(), "amq.topic");
        Map<String, Integer> exchanges = permissionStore.getResourcePermission()
                                                        .readPermission(RGroups.EXCHANGE.toString(), "amq.topic");
        Assert.assertEquals(exchanges.size(), 0, "Resource should not be exist");
    }
}
