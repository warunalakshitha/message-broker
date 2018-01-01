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
import java.util.regex.Pattern;
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

    public static final String DEFAULT_JAAS_LOGIN_MODULE = "CarbonSecurityConfig";

    public PlainSaslServer(UsernamePasswordCallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    @Override
    public String getMechanismName() {
        return PlainSaslServerBuilder.MECHANISM;
    }

    @Override
    public byte[] evaluateResponse(byte[] response) throws SaslException {
        try {
            String[] tokens = Pattern.compile("\u0000").splitAsStream(new String(response, StandardCharsets.UTF_8))
                    .toArray(String[]::new);
            if (tokens.length != 3) {
                throw new SaslException(
                        "Invalid SASL/PLAIN response. Tokens length should be 3 but received " + tokens.length);
            }
            String authcid = tokens[1];
            String password = tokens[2];
            if (authcid != null && password != null) {
                callbackHandler.setUsername(authcid);
                callbackHandler.setPassword(password.toCharArray());
                try {
                    LoginContext loginContext = new LoginContext(DEFAULT_JAAS_LOGIN_MODULE, callbackHandler);
                    loginContext.login();
                    isComplete = true;
                    authenticationId = authcid;
                    return new byte[0];
                } catch (LoginException e) {
                    throw new SaslException("Error while authenticate user with login module ", e);
                }
            } else {
                throw new SaslException("Invalid username: " + authcid + " and password: " + password + " received.");
            }
        } catch (IOException e) {
            throw new SaslException("Error processing data: " + e, e);
        }
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
