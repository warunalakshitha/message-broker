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
package org.wso2.broker.auth;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.auth.authentication.sasl.BrokerSecurityProvider;
import org.wso2.broker.auth.authentication.sasl.SaslServerBuilder;
import org.wso2.broker.auth.authentication.sasl.plain.PlainSaslServerBuilder;
import org.wso2.broker.auth.authorization.Permission;
import org.wso2.broker.auth.authorization.PermissionStore;
import org.wso2.broker.auth.authorization.dao.impl.ResourceDaoImpl;
import org.wso2.broker.auth.authorization.dao.impl.ResourceGroupDaoImpl;
import org.wso2.broker.auth.user.UserStoreManager;
import org.wso2.carbon.kernel.context.PrivilegedCarbonContext;

import java.security.Principal;
import java.security.Security;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.sql.DataSource;

/**
 * Class for manage authentication of message broker incoming connections.
 * This has list of sasl servers registered by the message broker which will be used during authentication of incoming
 * connections.
 */
public class AuthManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthManager.class);
    /**
     * Map of SASL Server mechanisms
     */
    private Map<String, SaslServerBuilder> saslMechanisms = new HashMap<>();

    private PermissionStore permissionStore;

    private UserStoreManager userStoreManager;

    private LoadingCache<String, Permission> userCache;

    private boolean authenticationEnabled;

    private boolean authorizationEnabled;

    /**
     * Constructor for initialize authentication manager and register sasl servers for auth provider mechanisms
     */
    public AuthManager(BrokerAuthConfiguration securityConfiguration, DataSource dataSource,
                       UserStoreManager userStoreManager
    ) throws Exception {

        this.authenticationEnabled = securityConfiguration.getAuthentication().isEnabled();
        this.authorizationEnabled = securityConfiguration.getAuthorization().isEnabled();
        this.userStoreManager = userStoreManager;
        if (authenticationEnabled) {
            String jaasConfigPath = System.getProperty(BrokerAuthConstants.SYSTEM_PARAM_JAAS_CONFIG);
            if (jaasConfigPath == null || jaasConfigPath.trim().isEmpty()) {
                Configuration jaasConfig = createJaasConfig(
                        securityConfiguration.getAuthentication().getJaas
                                ().getLoginModule(),
                        userStoreManager, securityConfiguration.getAuthentication().getJaas().getOptions());
                Configuration.setConfiguration(jaasConfig);
            }
            registerSaslServers();
        }
        if (authorizationEnabled) {
            this.permissionStore = new PermissionStore(new ResourceDaoImpl(dataSource), new ResourceGroupDaoImpl
                    (dataSource));
            this.userCache = CacheBuilder.newBuilder().maximumSize(securityConfiguration.getAuthorization()
                                                                           .getPermissionCache().getCacheSize())
                    .expireAfterWrite(
                            securityConfiguration.getAuthorization().getPermissionCache().getCacheTimeout(),
                            TimeUnit.MINUTES)
                    .build(new UserCacheLoader());
        }
    }

    /**
     * Register auth provider mechanisms.
     */
    private void registerSaslServers() {
        // create PLAIN SaslServer builder
        PlainSaslServerBuilder plainSaslServerBuilder = new PlainSaslServerBuilder();
        saslMechanisms.put(plainSaslServerBuilder.getMechanismName(), plainSaslServerBuilder);
        // Register given Sasl Server factories
        if (Security
                .insertProviderAt(new BrokerSecurityProvider(BrokerAuthConstants.PROVIDER_NAME, saslMechanisms), 1)
                == -1) {
            LOGGER.info("AMQ auth authentication providers is already installed");
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("AMQ auth authentication mechanisms providers successfully registered.");
            }
        }
    }

    /**
     * Creates Jaas config
     *
     * @param loginModuleClassName Jaas login module class name
     * @return Configuration
     */
    private static Configuration createJaasConfig(String loginModuleClassName, UserStoreManager userStoreManager,
                                                  Map<String, Object> options
    ) {
        options.put(BrokerAuthConstants.USER_STORE_MANAGER_PROPERTY, userStoreManager);
        AppConfigurationEntry[] entries = {
                new AppConfigurationEntry(loginModuleClassName, AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                                          options)
        };
        return new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return entries;
            }
        };
    }

    /**
     * Authenticate given user with credential based on broker @{@link UserStoreManager}
     *
     * @param userName    Username
     * @param credentials User Credentials
     * @return Authenticated or not
     * @throws BrokerAuthException Throws if error occur during authentication
     */
    public boolean authenticate(String userName, char... credentials) throws BrokerAuthException {
        return userStoreManager.authenticate(userName, credentials);
    }

    /**
     * Authorize given resource for given resource group and permission. User should have permission with
     * a resource which has same resource name.
     * user's authorised
     *
     * @param resourceGroup Resource Group
     * @param resource      Resource
     * @param permission    Permission
     * @return If authorised or not
     * @throws BrokerAuthException Throws if error occur during authorization
     */
    public boolean authorize(String resourceGroup, String resource, int permission) throws BrokerAuthException {
        return authorize(resourceGroup, resource, permission, (userGroups) ->
                permissionStore.authorize(resourceGroup, resource, userGroups, permission));
    }

    /**
     * Authorize given resource for given resource group and permission. User should have permission with
     * resource pattern which matches the given resource
     * user's authorised
     *
     * @param resourceGroup Resource Group
     * @param resource      Resource
     * @param permission    Permission
     * @return If authorised or not
     * @throws BrokerAuthException Throws if error occur during authorization
     */
    public boolean authorizeByPattern(String resourceGroup, String resource, int permission)
            throws BrokerAuthException {
        return authorize(resourceGroup, resource, permission, (userGroups) -> permissionStore
                .authorizeByPattern(resourceGroup, resource, userGroups, permission));
    }

    private boolean authorize(String resourceGroup, String resource,
                              int permission, AuthFunction authFunction) throws BrokerAuthException {
        if (isAuthorizationEnabled()) {
            try {
                Principal userPrincipal = PrivilegedCarbonContext.getCurrentContext().getUserPrincipal();
                if (userPrincipal != null) {
                    String userName = userPrincipal.getName();
                    Permission userPermission = userCache.get(userName);
                    AtomicInteger resourcePerm = userPermission.getPermissions().get(
                            resourceGroup + Permission.RESOURCE_GROUP_SEPARATOR + resource);
                    if (resourcePerm != null && (resourcePerm.intValue() & permission) == permission) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Permissions are loaded from cache for user name : {} resourceGroup: {} and "
                                                 + "resource : {} ", userName, resourceGroup, resource);
                        }
                        return true;
                    } else {
                        HashSet<String> userGroups = userPermission.getUserGroups();
                        if (userGroups != null && authFunction.authorize(userGroups)) {
                            if (resourcePerm != null) {
                                resourcePerm.set(resourcePerm.get() | permission);
                            } else {
                                userPermission.getPermissions().put(
                                        resourceGroup + Permission.RESOURCE_GROUP_SEPARATOR + resource,
                                        new AtomicInteger(permission));
                            }
                            return true;
                        } else {
                            return false;
                        }
                    }
                } else {
                    throw new BrokerAuthException("User principal cannot be found in the context.");
                }
            } catch (ExecutionException e) {
                throw new BrokerAuthException(
                        "Error occurred while retrieving permissions from cache for resource: " + resource, e);
            }
        } else {
            return true;
        }
    }

    /**
     * Provides map of auth mechanisms registered for broker
     *
     * @return Registered auth Mechanisms
     */
    public Map<String, SaslServerBuilder> getSaslMechanisms() {
        return saslMechanisms;
    }

    public PermissionStore getPermissionStore() {
        return permissionStore;
    }

    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public boolean isAuthorizationEnabled() {
        return authorizationEnabled;
    }

    private class UserCacheLoader extends CacheLoader<String, Permission> {
        @Override
        public Permission load(@Nonnull String userName) throws Exception {
            Permission permission = new Permission();
            permission.setPermissions(new HashMap<>());
            permission.setUserGroups(userStoreManager.getUserRoleList(userName));
            return permission;
        }
    }

    /**
     * Function to authorize based on resource and group.
     */
    @FunctionalInterface
    interface AuthFunction {
        boolean authorize(HashSet<String> userGroups) throws BrokerAuthException, ExecutionException;
    }
}
