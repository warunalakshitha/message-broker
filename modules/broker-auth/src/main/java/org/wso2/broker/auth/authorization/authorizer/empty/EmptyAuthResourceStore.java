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
package org.wso2.broker.auth.authorization.authorizer.empty;

import org.wso2.broker.auth.authorization.AuthResourceStore;
import org.wso2.broker.auth.authorization.authorizer.rdbms.resource.AuthResource;
import org.wso2.broker.auth.exception.BrokerAuthDuplicateException;
import org.wso2.broker.auth.exception.BrokerAuthException;
import org.wso2.broker.auth.exception.BrokerAuthNotFoundException;
import org.wso2.broker.auth.exception.BrokerAuthServerException;

import java.util.List;
import java.util.Set;

/**
 * Defines empty @{@link AuthResourceStore} when authorization is disabled.
 */
class EmptyAuthResourceStore implements AuthResourceStore {

    @Override
    public boolean authorize(String resourceType, String resourceName, String action, String userId,
                             Set<String> userGroups) throws BrokerAuthServerException, BrokerAuthNotFoundException {
        return true;
    }

    @Override
    public void add(AuthResource authResource)
            throws BrokerAuthServerException, BrokerAuthDuplicateException, BrokerAuthNotFoundException {

    }

    @Override
    public void update(AuthResource authResource) throws BrokerAuthServerException, BrokerAuthNotFoundException {

    }

    @Override
    public void delete(String resourceType, String resourceName)
            throws BrokerAuthServerException, BrokerAuthNotFoundException {

    }

    @Override
    public AuthResource read(String resourceType, String resourceName)
            throws BrokerAuthServerException, BrokerAuthNotFoundException {
        return null;
    }

    @Override
    public List<AuthResource> readAll(String resourceType, String ownerId) throws BrokerAuthServerException {
        return null;
    }

    @Override
    public List<AuthResource> readAll(String resourceType, String action, String ownerId)
            throws BrokerAuthServerException, BrokerAuthException {
        return null;
    }
}
