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

package org.wso2.broker.auth.authorization.authorizer.rdbms.scope.dao;

import org.wso2.broker.auth.authorization.authorizer.rdbms.scope.AuthScope;
import org.wso2.broker.auth.exception.BrokerAuthServerException;
import org.wso2.broker.common.BaseDao;

import java.util.List;
import javax.sql.DataSource;

/**
 * Defines functionality required at persistence layer for managing auth scopes..
 */
public abstract class AuthScopeDao extends BaseDao {

    public AuthScopeDao(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Persist scope key in the database
     *
     * @param scopeName  scope name
     * @param userGroups user groups
     * @throws BrokerAuthServerException when database operation failed.
     */
    public abstract void update(String scopeName, List<String> userGroups)
            throws BrokerAuthServerException;

    /**
     * Read scope for given scope name
     *
     * @param scopeName scope name
     * @return group of users
     * @throws BrokerAuthServerException when database operation failed.
     */
    public abstract AuthScope read(String scopeName) throws BrokerAuthServerException;

    /**
     * Read all scopes
     *
     * @return list of scopes
     * @throws BrokerAuthServerException when database operation failed.
     */
    public abstract List<AuthScope> readAll() throws BrokerAuthServerException;

}
