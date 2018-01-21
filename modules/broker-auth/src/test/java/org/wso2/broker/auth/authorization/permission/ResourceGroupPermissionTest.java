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
package org.wso2.broker.auth.authorization.permission;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

/**
 * Class provides test cases for permission scenarios.
 */
public class ResourceGroupPermissionTest {

    private PermissionStore permissionStore;

    private DataSource dataSource;

    private static final String MANAGER_ROLE = "manager";

    private static final String CAPTAIN_ROLE = "captain";

    private static final String CRICKET_PLAYER_ROLE = "cricketPlayer";

    @BeforeClass
    public void beforeTest() throws BrokerAuthException {
        dataSource = DbUtil.getDataSource();
        permissionStore = new PermissionStore(new ResourceDaoImpl(dataSource), new ResourceGroupDaoImpl(dataSource));
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

    @Test(dataProvider = "authorisedGroups",
          description = "Test Resource authorise Group adding")
    public void testResourceGroupPermission(String resourceGroup, Map<String, Integer> rolePermissions) throws
            Exception {
        permissionStore.getResourceGroupPermission().grantPermission(resourceGroup, rolePermissions);
    }

    @Test(priority = 1,
          description = "Test authorised group permissions")
    public void testAuthorisedGroupPermission() throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(MANAGER_ROLE);
        Assert.assertEquals(permissionStore.authorize(RGroups.QUEUE.toString(), "queue", roles, 1), true);
        Assert.assertEquals(permissionStore.authorize(RGroups.QUEUE.toString(), "queue", roles, 2), true);
        Assert.assertEquals(permissionStore.authorize(RGroups.QUEUE.toString(), "queue", roles, 7), true);
    }

    @Test(priority = 1,
          description = "Test unauthorised group permissions")
    public void testUnAuthorisedGroupPermission() throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        Assert.assertEquals(permissionStore.authorize(RGroups.QUEUE.toString(), "queue", roles, 4), false);
        Assert.assertEquals(permissionStore.authorize(RGroups.BINDING_KEY.toString(), "queue", roles, 2), false);
        Assert.assertEquals(permissionStore.authorize(RGroups.BINDING_KEY.toString(), "queue", roles, 3), false);
    }

    @Test(priority = 1,
          description = "Test multiple authorised roles")
    public void testMultipleAuthorisedRoles() throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        roles.add(CRICKET_PLAYER_ROLE);
        Assert.assertEquals(permissionStore.authorize(RGroups.QUEUE.toString(), "queue", roles, 4), true);
        Assert.assertEquals(permissionStore.authorize(RGroups.QUEUE.toString(), "queue2", roles, 3), true);
        Assert.assertEquals(permissionStore.authorize(RGroups.EXCHANGE.toString(), "amq.topic", roles, 3), true);
    }

    @DataProvider(name = "authorisedGroups")
    public Object[][] authorisedGroups() {
        // Add Exchange group Permissions
        Map<String, Integer> exchangeMap = new HashMap<>();
        exchangeMap.put(MANAGER_ROLE, 3);
        exchangeMap.put(CAPTAIN_ROLE, 3);

        // Add Queue Permissions
        Map<String, Integer> queueMap = new HashMap<>();
        queueMap.put(MANAGER_ROLE, 7);
        queueMap.put(CAPTAIN_ROLE, 3);
        queueMap.put(CRICKET_PLAYER_ROLE, 4);

        // Add Binding Permissions
        Map<String, Integer> bindingMap = new HashMap<>();
        bindingMap.put(MANAGER_ROLE, 7);

        return new Object[][] {
                { RGroups.EXCHANGE.toString(), exchangeMap },
                { RGroups.QUEUE.toString(), queueMap },
                { RGroups.BINDING_KEY.toString(), bindingMap }
        };
    }
}
