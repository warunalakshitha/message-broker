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
package org.wso2.broker.auth.user;

import org.wso2.broker.auth.exception.BrokerAuthException;
import org.wso2.broker.common.StartupContext;

import java.util.Set;

/**
 * Interface provides user store operations required for broker.
 */
public interface UserStoreManager {

    /**
     * Initiate user store connector with startup context.
     *
     * @param startupContext the startup context provides registered services for user store connector functionality.
     */
    void initialize(StartupContext startupContext) throws Exception;

    /**
     * Authenticate given user with credentials.
     *
     * @param userName    userName
     * @param credentials credentials
     * @return authentication result
     * @throws BrokerAuthException throws when authentication failed.
     */
    boolean authenticate(String userName, char... credentials) throws BrokerAuthException;

    /**
     * Retrieve the list of users for given user identifier.
     *
     * @param userId user identifier
     * @return list of roles
     * @throws BrokerAuthException throws when getting role list from user manager
     */
    Set<String> getUserRoleList(String userId) throws BrokerAuthException;
}
