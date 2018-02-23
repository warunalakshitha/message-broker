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
import org.wso2.broker.auth.authorization.provider.DefaultAuthProvider;

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

    public void setAuthentication(AuthenticationConfiguration authentication) {
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

        private AuthProviderConfiguration authProvider = new AuthProviderConfiguration();

        private CacheConfiguration cache = new CacheConfiguration();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public AuthProviderConfiguration getAuthProvider() {
            return authProvider;
        }

        public void setAuthProvider(AuthProviderConfiguration authProvider) {
            this.authProvider = authProvider;
        }

        public CacheConfiguration getCache() {
            return cache;
        }

        public void setCache(CacheConfiguration cache) {
            this.cache = cache;
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
     * Represents authProvider configuration for broker
     */
    public static class AuthProviderConfiguration {

        private String className = DefaultAuthProvider.class.getCanonicalName();

        private Map<String, Object> properties = new HashMap<>();

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }
    }

    /**
     * Represents permission cache configuration required for authorization
     */
    public static class CacheConfiguration {

        /**
         * Cache timeout in minutes
         */
        private int timeout = 15;
        /**
         * Maximum cache size
         */
        private int size = 5000;

        public int getTimeout() {
            return timeout;
        }

        public void setTimeout(int timeout) {
            this.timeout = timeout;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }
    }
}
