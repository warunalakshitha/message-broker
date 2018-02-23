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
package org.wso2.broker.auth.authorization.authorizer.rdbms.scope;

import java.util.Set;

/**
 * Representation for auth scope.
 */
public class AuthScope {

    private final String scopeName;

    private Set<String> authorizedUserGroups;

    public AuthScope(String scopeName, Set<String> authorizedUserGroups) {
        this.scopeName = scopeName;
        this.authorizedUserGroups = authorizedUserGroups;
    }

    public String getScopeName() {
        return scopeName;
    }

    public Set<String> getAuthorizedUserGroups() {
        return authorizedUserGroups;
    }
}

