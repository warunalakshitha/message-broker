/*
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.broker.core.security.authentication.sasl.plain;

import org.wso2.broker.core.security.authentication.Authenticator;
import org.wso2.broker.core.security.authentication.util.BrokerSecurityConstants;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;
import javax.security.sasl.SaslServerFactory;

/**
 * SASL server factory for {@link PlainSaslServer}  which will be registered using
 * {@link org.wso2.broker.core.security.authentication.sasl.SaslServerBuilder}
 */
public class PlainSaslServerFactory implements SaslServerFactory {

    @Override
    public SaslServer createSaslServer(String mechanism, String protocol, String serverName, Map<String, ?> props,
            CallbackHandler cbh) throws SaslException {
        return (PlainSaslServerBuilder.MECHANISM.equals(mechanism)) ?
                new PlainSaslServer(cbh, (Authenticator) props.get(BrokerSecurityConstants.AUTHENTICATOR_PROPERTY)) :
                null;
    }

    @Override
    public String[] getMechanismNames(Map<String, ?> props) {
        return new String[0];
    }
}
