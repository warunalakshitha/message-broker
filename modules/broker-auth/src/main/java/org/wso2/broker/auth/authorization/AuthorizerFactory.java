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

import org.wso2.broker.auth.BrokerAuthConfiguration;
import org.wso2.broker.auth.authorization.authorizer.empty.DefaultAuthorizer;
import org.wso2.broker.auth.authorization.authorizer.rdbms.RdbmsAuthorizer;
import org.wso2.broker.common.StartupContext;

/**
 * Factory class for create new instance of @{@link Authorizer}.
 */
public class AuthorizerFactory {

    /**
     * Provides an instance of @{@link Authorizer}
     *
     * @param authProvider              an authProvider to retrieve authorize information for user.
     * @param startupContext          the startup context provides registered services for authenticator functionality.
     * @param brokerAuthConfiguration the auth configuration
     * @return authProvider for given configuration
     * @throws Exception throws if error occurred while providing new instance of authProvider
     */
    public Authorizer getAutStore(AuthProvider authProvider,
                                  BrokerAuthConfiguration brokerAuthConfiguration,
                                  StartupContext startupContext) throws Exception {

        if (brokerAuthConfiguration.getAuthentication().isEnabled() &&
                brokerAuthConfiguration.getAuthorization().isEnabled()) {
            Authorizer authorizer = new RdbmsAuthorizer();
            authorizer.initialize(authProvider,
                                  startupContext,
                                  brokerAuthConfiguration.getAuthorization()
                                                        .getAuthProvider()
                                                        .getProperties());
            return authorizer;
        } else {
            return new DefaultAuthorizer();
        }
    }
}
