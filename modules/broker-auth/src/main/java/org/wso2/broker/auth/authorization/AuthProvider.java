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

import org.wso2.broker.auth.exception.BrokerAuthException;
import org.wso2.broker.common.StartupContext;

import java.util.Map;
import java.util.Set;

/**
 * Interface represents authorizer for broker users which provide authorization groups..
 */
public interface AuthProvider {

    /**
     * Initialize authorization strategy based on given auth configuration, user store manager and data source.
     *
     * @param startupContext the startup context provides registered services for authorizer
     * @param properties     set of properties
     */
    void initialize(StartupContext startupContext, Map<String, Object> properties) throws Exception;

    /**
     * Get set of user Groups for the user
     *
     * @param userId user id
     * @return set of user groups
     * @throws BrokerAuthException throws if error occurs while authorizing user
     */
    Set<String> getUserGroupsList(String userId) throws BrokerAuthException;
}
