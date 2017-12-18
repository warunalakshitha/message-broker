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
package org.wso2.broker.core.security.jaas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.core.security.exception.BrokerAuthenticationException;
import org.wso2.broker.core.security.user.UserStoreManager;

import java.io.IOException;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * Default JaaS login module {@link LoginModule} for Message broker.
 * This will be configured in jaas.conf file.
 * AuthConfig {
 * org.wso2.broker.core.security.jaas.UserNamePasswordLoginModule required;
 * };
 */
public class UserNamePasswordLoginModule implements LoginModule {

    private static final Logger log = LoggerFactory.getLogger(UserNamePasswordLoginModule.class);
    private String userName;
    private char[] password;
    private boolean success = false;
    private CallbackHandler callbackHandler;

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState,
            Map<String, ?> options) {
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback userNameCallback = new NameCallback("userName");
        PasswordCallback passwordCallback = new PasswordCallback("password", false);
        Callback[] callbacks = { userNameCallback, passwordCallback };
        try {
            callbackHandler.handle(callbacks);
        } catch (UnsupportedCallbackException e) {
            throw new BrokerAuthenticationException("Callback type does not support. ", e);
        } catch (IOException e) {
            throw new BrokerAuthenticationException("Exception occurred while handling authentication data. ", e);
        }
        userName = userNameCallback.getName();
        password = passwordCallback.getPassword();
        success = validateUserPassword(userName, password);
        return success;
    }

    /**
     * Authenticate user credentials using userstore manager
     *
     * @param userName Username
     * @param password Password
     * @return Denotes the whether user authentication success ot not
     */
    private boolean validateUserPassword(String userName, char[] password) throws BrokerAuthenticationException {
        return userName != null && password != null && UserStoreManager.authenticate(userName, password);
    }

    @Override
    public boolean commit() throws LoginException {
        userName = null;
        for (int i = 0; i < password.length; i++) {
            password[i] = ' ';
        }
        password = null;
        return success;
    }

    @Override
    public boolean abort() throws LoginException {
        logout();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        success = false;
        userName = null;
        if (password != null) {
            for (int i = 0; i < password.length; i++) {
                password[i] = ' ';
            }
            password = null;
        }
        return true;
    }
}
