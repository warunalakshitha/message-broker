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
 *
 * Following use cases will be tested.
 *
 * 1. Role with exact resource permissions.
 * 2. Role with hierarchical permissions.
 * 3. Multiple roles with different permissions where user needs to have permission to one role.
 * 4. Role with resource permission but some actions are not permitted.
 */
public class ResourcePermissionTest {

    private PermissionStore permissionStore;

    private DataSource dataSource;

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

    @Test(dataProvider = "authorisedCaptainResources",
          description = "Test authorised Captain Resource adding")
    public void testAddResourcePermission(String resourceGroup, String resource) throws Exception {
        Map<String, Integer> bindingMap = new HashMap<>();
        bindingMap.put(CAPTAIN_ROLE, 1);
        permissionStore.getResourcePermission().grantPermission(resourceGroup, resource,
                                                                bindingMap);
    }

    @Test(dataProvider = "authorisedPlayerResources",
          description = "Test authorised Player Resources adding")
    public void testAddLimitedResourcePermission(String resourceGroup, String resource) throws Exception {
        Map<String, Integer> bindingMap = new HashMap<>();
        bindingMap.put(CRICKET_PLAYER_ROLE, 2);
        permissionStore.getResourcePermission().grantPermission(resourceGroup, resource,
                                                                bindingMap);
    }

    @Test(dataProvider = "requestCaptainResources",
          description = "Test  topics",
          priority = 1)
    public void testTopics(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        Assert.assertEquals(permissionStore.authorize(resourceGroup, resource, roles, 1), true);
    }

    @Test(dataProvider = "requestUnauthorizedCaptainResources",
          description = "Test topics with invalid permissions",
          priority = 1)
    public void testTopicsInvalidPermission(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        Assert.assertEquals(permissionStore.authorize(resourceGroup, resource, roles, 1), false);
    }

    @Test(dataProvider = "requestCaptainHierarchicalResources",
          description = "Test hierarchical topics",
          priority = 1)
    public void testHierarchicalTopics(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 1), true);
    }

    @Test(dataProvider = "requestCaptainHierarchicalResources",
          description = "Test hierarchical topics with invalid permissions",
          priority = 1)
    public void testHierarchicalTopicsInvalidPermission(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 2), false);
    }

    @Test(dataProvider = "requestUnauthorizedHierarchicalCaptainResources",
          description = "Test unauthorized hierarchical topics",
          priority = 1)
    public void testUnauthorizedHierarchicalTopics(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 1), false);
    }

    @Test(dataProvider = "requestPlayerResources",
          description = "Test hierarchical topics for player",
          priority = 1)
    public void testHierarchicalTopicsForPlayer(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CRICKET_PLAYER_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 2), true);
    }

    @Test(dataProvider = "requestPlayerResources",
          description = "Test hierarchical topics with invalid permissions topics for player",
          priority = 1)
    public void testHierarchicalTopicsInvalidPermissionForPlayer(String resourceGroup, String resource)
            throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CRICKET_PLAYER_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 1), false);
    }

    @Test(dataProvider = "requestUnauthorizedPlayerResources",
          description = "Test unauthorized hierarchical topics for player",
          priority = 1)
    public void testUnauthorizedHierarchicalTopicsForPlayer(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CRICKET_PLAYER_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 1), false);
    }

    @Test(dataProvider = "requestCaptainAndPlayerResources",
          description = "Test user with multiple roles",
          priority = 1)
    public void testUserWithMultipleRoles(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        roles.add(CRICKET_PLAYER_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 2), true);
    }

    @DataProvider(name = "authorisedPlayerResources")
    public Object[][] authorisedPlayerResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "world.#" },
                { RGroups.BINDING_KEY.toString(), "teams.plan.*" },
                { RGroups.BINDING_KEY.toString(), "*.cricket.batsmen" },
                { RGroups.BINDING_KEY.toString(), "cricket.bowlers.#" },
                };
    }

    @DataProvider(name = "authorisedCaptainResources")
    public Object[][] authorisedCaptainResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "sports.#" },
                { RGroups.BINDING_KEY.toString(), "*.cricket.bowlers" },
                { RGroups.BINDING_KEY.toString(), "teams.plan" }
        };
    }

    @DataProvider(name = "requestCaptainResources")
    public Object[][] requestResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "teams.plan" }
        };
    }

    @DataProvider(name = "requestUnauthorizedCaptainResources")
    public Object[][] requestUnauthorizedResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "sports.cricket" }
        };
    }

    @DataProvider(name = "requestCaptainHierarchicalResources")
    public Object[][] requestCaptainHierarchicalResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "sports.cricket" },
                { RGroups.BINDING_KEY.toString(), "sports.cricket.batsmen" },
                { RGroups.BINDING_KEY.toString(), "world.cricket.bowlers" },
                { RGroups.BINDING_KEY.toString(), "teams.plan" }
        };
    }

    @DataProvider(name = "requestPlayerResources")
    public Object[][] requestPlayerResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "world.sports" }
        };
    }

    @DataProvider(name = "requestUnauthorizedHierarchicalCaptainResources")
    public Object[][] requestUnauthorizedHierarchicalCaptainResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "world.sports" },
                { RGroups.BINDING_KEY.toString(), "world.cricket.cricket" },
                { RGroups.BINDING_KEY.toString(), "cricket.bowlers.fast" },
                { RGroups.BINDING_KEY.toString(), "teams.plan.private" },
                };
    }

    @DataProvider(name = "requestUnauthorizedPlayerResources")
    public Object[][] requestUnauthorizedPlayerResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "sports.cricket.batsmen" },
                { RGroups.BINDING_KEY.toString(), "world.cricket.bowlers" },
                { RGroups.BINDING_KEY.toString(), "teams.plan" }
        };
    }

    @DataProvider(name = "requestCaptainAndPlayerResources")
    public Object[][] requestCaptainAndPlayerResource() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "world.sports" },
                { RGroups.BINDING_KEY.toString(), "world.cricket.cricket" },
                { RGroups.BINDING_KEY.toString(), "cricket.bowlers.fast" },
                { RGroups.BINDING_KEY.toString(), "teams.plan.private" },
                };
    }
}
