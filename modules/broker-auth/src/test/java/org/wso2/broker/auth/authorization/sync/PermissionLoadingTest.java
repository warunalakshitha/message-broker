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
package org.wso2.broker.auth.authorization.sync;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.auth.DbUtil;
import org.wso2.broker.auth.authorization.PermissionStore;
import org.wso2.broker.auth.authorization.dao.ResourceDao;
import org.wso2.broker.auth.authorization.dao.ResourceGroupDao;
import org.wso2.broker.auth.authorization.dao.impl.ResourceDaoImpl;
import org.wso2.broker.auth.authorization.dao.impl.ResourceGroupDaoImpl;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

/**
 * Class for testing whether resources or resource group load on demand.
 * When server restart we need to make sure old data will be reloaded on demand.
 */
public class PermissionLoadingTest {

    private DataSource dataSource;

    private PermissionStore permissionStore;

    private static final String MANAGER_ROLE = "manager";

    private static final String CAPTAIN_ROLE = "captain";

    private ResourceDao resourceDao;

    private ResourceGroupDao resourceGroupDao;

    @BeforeClass
    public void beforeTest() throws BrokerAuthException {
        dataSource = DbUtil.getDataSource();
        permissionStore = new PermissionStore(new ResourceDaoImpl(dataSource), new ResourceGroupDaoImpl(dataSource));
        resourceDao = new ResourceDaoImpl(dataSource);
        resourceGroupDao = new ResourceGroupDaoImpl(dataSource);
    }

    @AfterClass
    public void cleanup() throws Exception {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        statement.execute("DELETE FROM MB_RESOURCE_PERMISSION");
        statement.execute("DELETE FROM MB_RESOURCE_GROUP_PERMISSION");
        connection.commit();
        statement.close();
        connection.close();
    }

    @Test(priority = 1,
          description = "Test load resource permission")
    public void testLoadPermissionsByResource() throws Exception {
        Map<String, Integer> permissionMap = new HashMap<>();
        permissionMap.put(MANAGER_ROLE, 15);
        resourceDao.persist("ResourceGroup1", "resource1", permissionMap);
        Map<String, Integer> permissions = permissionStore.getResourcePermission()
                                                          .readPermission("ResourceGroup1", "resource1");
        Assert.assertEquals(permissions.get(MANAGER_ROLE).intValue(), 15, "Manager role permissions are incorrect");
        Assert.assertEquals(permissions.get(CAPTAIN_ROLE), null, "Captain role permissions should not be exist");
    }

    @Test(priority = 2,
          description = "Test load resource permission ")
    public void testLoadPermissionsByResourceGroup() throws Exception {
        Map<String, Integer> permissionMap = new HashMap<>();
        permissionMap.put(CAPTAIN_ROLE, 18);
        resourceDao.persist("ResourceGroup2", "resource1", permissionMap);
        Map<String, Map<String, Integer>> permissions = permissionStore.getResourcePermission()
                                                                       .readPermission("ResourceGroup2");
        Assert.assertEquals(permissions.get("resource1").get(CAPTAIN_ROLE).intValue(), 18,
                            "Captain role permissions are incorrect");
        Assert.assertEquals(permissions.get("resource1").get(MANAGER_ROLE), null,
                            "Manager role permissions should not be exist");
    }

    @Test(priority = 3,
          description = "Test load resource group permission ")
    public void testLoadPermissionsResourceGroup() throws Exception {
        Map<String, Integer> permissionMap = new HashMap<>();
        permissionMap.put(MANAGER_ROLE, 100);
        resourceGroupDao.persist("Resource Group3", permissionMap);
        Map<String, Integer> permissions = permissionStore.getResourceGroupPermission()
                                                          .readPermission(("Resource Group3"));
        Assert.assertEquals(permissions.get(MANAGER_ROLE).intValue(), 100, "Manager role permissions are incorrect");
        Assert.assertEquals(permissions.get(CAPTAIN_ROLE), null, "Captain role permissions should not be exist");

    }
}
