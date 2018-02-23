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
package org.wso2.broker.auth.authorization.provider;

import org.wso2.broker.auth.authorization.AuthProvider;
import org.wso2.broker.auth.exception.BrokerAuthException;
import org.wso2.broker.auth.user.UserStoreManager;
import org.wso2.broker.auth.user.impl.UserStoreManagerImpl;
import org.wso2.broker.common.StartupContext;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Class provides database based @{@link AuthProvider} implementation.
 */
public class UserStoreAuthProvider implements AuthProvider {

    private UserStoreManager userStoreManager;

    @Override
    public void initialize(StartupContext startupContext, Map<String, Object> properties) throws Exception {
        this.userStoreManager = startupContext.getService(UserStoreManager.class);
        if (Objects.isNull(userStoreManager)) {
            userStoreManager = new UserStoreManagerImpl();
            userStoreManager.initialize(startupContext);
        }
    }

    @Override
    public Set<String> getUserGroupsList(String userId) throws BrokerAuthException {
        return userStoreManager.getUserRoleList(userId);
    }
}
