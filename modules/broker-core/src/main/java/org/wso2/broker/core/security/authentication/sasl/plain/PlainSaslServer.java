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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

/**
 * This class implements SASL server for Plain text security mechanism.
 * Response message will be in following format.
 * message   = [authzid] UTF8NUL authcid UTF8NUL passwd
 * <p>
 * authzid = authorization identity
 * authcid = authentication identity
 * passwd = password
 */
public class PlainSaslServer implements SaslServer {

    private UsernamePasswordCallbackHandler callbackHandler;

    private boolean isComplete = false;

    private String authenticationId;

    public static final String BROKER_SECURITY_CONFIG = "BrokerSecurityConfig";

    public static final String PLAIN_MECHANISM = "PLAIN";

    public PlainSaslServer(UsernamePasswordCallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    @Override
    public String getMechanismName() {
        return PLAIN_MECHANISM;
    }

    @Override
    public byte[] evaluateResponse(byte[] response) throws SaslException {
        try {
            int authzidNullPosition = getUTF8NULPosition(response, 0);
            if (authzidNullPosition < 0) {
                throw new SaslException("Invalid SASL/PLAIN response due to authzid null terminator not found");
            }
            int authcidNullPosition = getUTF8NULPosition(response, authzidNullPosition + 1);
            if (authcidNullPosition < 0) {
                throw new SaslException("Invalid SASL/PLAIN response due to authcid null terminator not found");
            }
            String authcid = new String(response, authzidNullPosition + 1,
                    authcidNullPosition - authzidNullPosition - 1, StandardCharsets.UTF_8);
            int passwordLen = response.length - authcidNullPosition - 1;
            char[] password = new char[passwordLen];
            for (int i = 0; i < passwordLen; i++) {
                password[i] = (char) response[++authcidNullPosition];
            }
            callbackHandler.setUsername(authcid);
            callbackHandler.setPassword(password);
            try {
                LoginContext loginContext = new LoginContext(BROKER_SECURITY_CONFIG, callbackHandler);
                loginContext.login();
                isComplete = true;
                authenticationId = authcid;
                return new byte[0];
            } catch (LoginException e) {
                throw new SaslException("Error while authenticate user with login module ", e);
            }
        } catch (IOException e) {
            throw new SaslException("Error processing data: " + e, e);
        }
    }

    private int getUTF8NULPosition(byte[] response, int startPosition) {
        int position = startPosition;
        while (position < response.length) {
            if (response[position] == (byte) 0) {
                return position;
            }
            position++;
        }
        return -1;
    }

    @Override
    public boolean isComplete() {
        return isComplete;
    }

    @Override
    public String getAuthorizationID() {
        return authenticationId;
    }

    @Override
    public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException {
        return new byte[0];
    }

    @Override
    public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException {
        return new byte[0];
    }

    @Override
    public Object getNegotiatedProperty(String propName) {
        return null;
    }

    @Override
    public void dispose() throws SaslException {
        callbackHandler = null;
    }
}
