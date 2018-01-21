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
public class ResourcePermissionTest {

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
          description = "Test hierarchical topics",
          priority = 1)
    public void testHierarchicalTopics(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 1), true);
    }

    @Test(dataProvider = "requestCaptainResources",
          description = "Test hierarchical topics with invalid permissions",
          priority = 1)
    public void testHierarchicalTopicsInvalidPermission(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 2), false);
    }

    @Test(dataProvider = "requestUnauthorizedCaptainResources",
          description = "test unauthorized hierarchical topics",
          priority = 1)
    public void testUnauthorizedHierarchicalTopics(String resourceGroup, String resource) throws Exception {
        Set<String> roles = new HashSet<>();
        roles.add(CAPTAIN_ROLE);
        Assert.assertEquals(permissionStore.authorizeByPattern(resourceGroup, resource, roles, 1), false);
    }

    @DataProvider(name = "authorisedPlayerResources")
    public Object[][] authorisedPlayerResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "sports.*" },
                };
    }

    @DataProvider(name = "authorisedCaptainResources")
    public Object[][] authorisedCaptainResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "sports.#" },
                { RGroups.BINDING_KEY.toString(), "*.cricket.bowlers" },
                { RGroups.BINDING_KEY.toString(), "teams.#" }
        };
    }

    @DataProvider(name = "requestCaptainResources")
    public Object[][] requestResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "sports.cricket" },
                { RGroups.BINDING_KEY.toString(), "sports.cricket.batsmen" },
                { RGroups.BINDING_KEY.toString(), "world.cricket.bowlers" },
                { RGroups.BINDING_KEY.toString(), "teams.plan" }
        };
    }

    @DataProvider(name = "requestUnauthorizedCaptainResources")
    public Object[][] requestUnauthorizedCaptainResources() {
        return new Object[][] {
                { RGroups.BINDING_KEY.toString(), "world.sports" },
                { RGroups.BINDING_KEY.toString(), "world.cricket.cricket" },
                { RGroups.BINDING_KEY.toString(), "cricket.bowlers.fast" },
                { RGroups.BINDING_KEY.toString(), "team.plan.private" },
                };
    }
}
