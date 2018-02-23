/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.wso2.broker.auth.authorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.auth.BrokerAuthConfiguration;
import org.wso2.broker.auth.authorization.provider.DefaultAuthProvider;
import org.wso2.broker.common.StartupContext;

/**
 * Factory class for create new instance of @{@link AuthProvider}.
 */
public class AuthProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthProviderFactory.class);

    /**
     * Provides an instance of @{@link AuthProvider}
     *
     * @param startupContext          the startup context provides registered services for authenticator functionality.
     * @param brokerAuthConfiguration the auth configuration
     * @return authorizer for given configuration
     * @throws Exception throws if error occurred while providing new instance of authorizer
     */
    public AuthProvider getAuthorizer(BrokerAuthConfiguration brokerAuthConfiguration,
                                      StartupContext startupContext) throws Exception {

        if (brokerAuthConfiguration.getAuthentication().isEnabled() &&
                brokerAuthConfiguration.getAuthorization().isEnabled()) {
            String authenticatorClass = brokerAuthConfiguration.getAuthorization()
                                                               .getAuthProvider()
                                                               .getClassName();
            LOGGER.info("Initializing authProvider: {}", authenticatorClass);
            AuthProvider authProvider = (AuthProvider) ClassLoader.getSystemClassLoader()
                                                                  .loadClass(authenticatorClass).newInstance();
            authProvider.initialize(startupContext, brokerAuthConfiguration.getAuthorization()
                                                                           .getAuthProvider()
                                                                           .getProperties());
            return authProvider;
        } else {
            return new DefaultAuthProvider();
        }
    }
}
