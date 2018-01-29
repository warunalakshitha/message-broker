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
import org.wso2.broker.common.util.function.ThrowingFunction;

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
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.sql.DataSource;

/**
 * Class for manage authentication and authorization of message broker incoming connections.
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

    private static ThreadLocal<String> authContext = new ThreadLocal<>();

    /**
     * Constructor for initialize authentication manager and register sasl servers for auth provider mechanisms
     */
    public AuthManager(BrokerAuthConfiguration securityConfiguration, DataSource dataSource,
                       UserStoreManager userStoreManager) throws Exception {

        this.authenticationEnabled = securityConfiguration.getAuthentication().isEnabled();
        this.authorizationEnabled = securityConfiguration.getAuthorization().isEnabled();
        this.userStoreManager = userStoreManager;
        if (authenticationEnabled) {
            String jaasConfigPath = System.getProperty(BrokerAuthConstants.SYSTEM_PARAM_JAAS_CONFIG);
            BrokerAuthConfiguration.JaasConfiguration jaasConf = securityConfiguration.getAuthentication().getJaas();
            if (jaasConfigPath == null || jaasConfigPath.trim().isEmpty()) {
                Configuration jaasConfig = createJaasConfig(jaasConf.getLoginModule(), userStoreManager,
                                                            jaasConf.getOptions());
                Configuration.setConfiguration(jaasConfig);
            }
            registerSaslServers();
        }
        if (authorizationEnabled) {
            this.permissionStore = new PermissionStore(new ResourceDaoImpl(dataSource), new ResourceGroupDaoImpl
                    (dataSource));
            this.userCache = CacheBuilder.newBuilder().maximumSize(securityConfiguration.getAuthorization()
                                                                                        .getPermissionCache()
                                                                                        .getCacheSize())
                                         .expireAfterWrite(
                                                 securityConfiguration.getAuthorization().getPermissionCache()
                                                                      .getCacheTimeout(),
                                                 TimeUnit.MINUTES)
                                         .build(new UserCacheLoader());
        }
    }

    /**
     * Register security provider mechanisms
     */
    private void registerSaslServers() {
        // create PLAIN SaslServer builder
        PlainSaslServerBuilder plainSaslServerBuilder = new PlainSaslServerBuilder();
        saslMechanisms.put(plainSaslServerBuilder.getMechanismName(), plainSaslServerBuilder);
        // Register given Sasl Server factories
        if (Security
                .insertProviderAt(new BrokerSecurityProvider(BrokerAuthConstants.PROVIDER_NAME, saslMechanisms), 1)
                == -1) {
            LOGGER.info("AMQ security authentication providers are already installed.");
        } else {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("AMQ security authentication mechanisms providers are successfully registered.");
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
     * Create sasl server for given mechanism
     *
     * @param hostName  Hostname of the server
     * @param mechanism Sasl mechanism
     * @return Sasl server created for mechanism
     * @throws SaslException Throws if server does not support for given mechanism
     */
    public SaslServer createSaslServer(String hostName, String mechanism) throws SaslException {
        SaslServerBuilder saslServerBuilder = saslMechanisms.get(mechanism);
        if (saslServerBuilder != null) {
            return Sasl.createSaslServer(mechanism, BrokerAuthConstants.AMQP_PROTOCOL_IDENTIFIER, hostName,
                                         saslServerBuilder.getProperties(),
                                         saslServerBuilder.getCallbackHandler());
        } else {
            throw new SaslException("Server does not support for mechanism: " + mechanism);
        }
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
        if (userStoreManager.authenticate(userName, credentials)) {
            AuthManager.getAuthContext().set(userName);
            return true;
        }
        return false;
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
        return authorize(resourceGroup, resource, permission, (userGroups) ->
                permissionStore.authorizeByPattern(resourceGroup, resource, userGroups, permission));
    }

    private boolean authorize(String resourceGroup, String resource, int permission, ThrowingFunction<HashSet,
            Boolean, BrokerAuthException> authFunction) throws BrokerAuthException {
        if (isAuthorizationEnabled()) {
            try {
                String userName = AuthManager.getAuthContext().get();
                if (userName != null) {
                    Permission userPermission = userCache.get(userName);
                    AtomicInteger resourcePerm = userPermission.getPermissions().get(resourceGroup + Permission
                            .RESOURCE_GROUP_SEPARATOR + resource);
                    if (resourcePerm != null && (resourcePerm.intValue() & permission) == permission) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Permissions are loaded from cache for user name : {} resourceGroup: {} and "
                                                 + "" + "resource : {} ", userName, resourceGroup, resource);
                        }
                        return true;
                    } else {
                        HashSet<String> userGroups = userPermission.getUserGroups();
                        if (userGroups != null && authFunction.apply(userGroups)) {
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
                    throw new BrokerAuthException("Authorization id cannot be found in the context.");
                }
            } catch (ExecutionException e) {
                throw new BrokerAuthException(
                        "Error occurred while retrieving permissions from cache for resource: " + resource, e);
            } finally {
                authContext.remove();
            }
        } else {
            return true;
        }
    }

    public PermissionStore getPermissionStore() {
        return permissionStore;
    }
    /**
     * Provides broker authentication enabled.
     * @return broker authentication enabled or not
     */
    public boolean isAuthenticationEnabled() {
        return authenticationEnabled;
    }

    public boolean isAuthorizationEnabled() {
        return authorizationEnabled;
    }

    public static ThreadLocal<String> getAuthContext() {
        return authContext;
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

}
