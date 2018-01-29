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

import org.wso2.broker.auth.authentication.jaas.BrokerLoginModule;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents authentication configuration for broker.
 */
public class BrokerAuthConfiguration {

    /**
     * Namespace used in the config file.
     */
    public static final String NAMESPACE = "wso2.broker.auth";

    private AuthenticationConfiguration authentication = new AuthenticationConfiguration();

    private AuthorizationConfiguration authorization = new AuthorizationConfiguration();

    public AuthenticationConfiguration getAuthentication() {
        return authentication;
    }

    public void setAuthentication(
            AuthenticationConfiguration authentication
                                 ) {
        this.authentication = authentication;
    }

    public AuthorizationConfiguration getAuthorization() {
        return authorization;
    }

    public void setAuthorization(AuthorizationConfiguration authorization) {
        this.authorization = authorization;
    }

    /**
     * Represents authentication configuration for broker
     */
    public static class AuthenticationConfiguration {

        private boolean enabled = true;

        private JaasConfiguration jaas = new JaasConfiguration();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public JaasConfiguration getJaas() {
            return jaas;
        }

        public void setJaas(JaasConfiguration jaas) {
            this.jaas = jaas;
        }
    }

    /**
     * Represents authorization configuration for broker
     */
    public static class AuthorizationConfiguration {

        private boolean enabled = true;

        private PermissionCacheConfiguration permissionCache = new PermissionCacheConfiguration();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public PermissionCacheConfiguration getPermissionCache() {
            return permissionCache;
        }

        public void setPermissionCache(
                PermissionCacheConfiguration permissionCache
                                      ) {
            this.permissionCache = permissionCache;
        }
    }

    /**
     * Represents jaas configuration for authentication
     */
    public static class JaasConfiguration {

        /**
         * Jaas login module class name
         */
        private String loginModule = BrokerLoginModule.class.getCanonicalName();

        /**
         * Jaas login module options
         */
        private Map<String, Object> options = new HashMap<>();

        public String getLoginModule() {
            return loginModule;
        }

        public void setLoginModule(String loginModule) {
            this.loginModule = loginModule;
        }

        public Map<String, Object> getOptions() {
            return options;
        }

        public void setOptions(Map<String, Object> options) {
            this.options = options;
        }
    }

    /**
     * Represents permission cache configuration required for authorization
     */
    public static class PermissionCacheConfiguration {

        /**
         * Cache timeout in minutes
         */
        private int cacheTimeout = 15;
        /**
         * Maximum cache size
         */
        private int cacheSize = 5000;

        public int getCacheTimeout() {
            return cacheTimeout;
        }

        public void setCacheTimeout(int cacheTimeout) {
            this.cacheTimeout = cacheTimeout;
        }

        public int getCacheSize() {
            return cacheSize;
        }

        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }
    }
}
