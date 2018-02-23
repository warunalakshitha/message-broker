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

import org.wso2.broker.auth.authorization.authorizer.rdbms.scope.AuthScope;
import org.wso2.broker.auth.exception.BrokerAuthNotFoundException;
import org.wso2.broker.auth.exception.BrokerAuthServerException;

import java.util.List;
import java.util.Set;

/**
 * Representation for auth scope store which provides Mandatory access control (MAC) based
 * authorization model for broker resources.
 */
public interface AuthScopeStore {

    /**
     * Authorize user for given resource and action
     *
     * @param authScopeName an authScopeName
     * @param userGroups    set of user groups of user
     * @return is authorized or not
     * @throws BrokerAuthServerException throws if error occurs while authorization
     */
    boolean authorize(String authScopeName, Set<String> userGroups)
            throws BrokerAuthServerException, BrokerAuthNotFoundException;

    /**
     * Update auth
     *
     * @param authScopeName an authScopeName
     * @param userGroups    set of user groups of user
     * @throws BrokerAuthServerException throws if error occurs while granting scope.
     */
    void update(String authScopeName, List<String> userGroups) throws BrokerAuthServerException;

    /**
     * Read authScope for given scope key.
     *
     * @param authScopeName an authScopeName
     * @return auth scope
     * @throws BrokerAuthServerException throws if error occurs while reading scope.
     */
    AuthScope read(String authScopeName) throws BrokerAuthServerException;

    /**
     * Read all authScopes.
     *
     * @return all auth scopes
     * @throws BrokerAuthServerException throws if error occurs while reading scope.
     */
    List<AuthScope> readAll() throws BrokerAuthServerException;

}
