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
package org.wso2.broker.auth.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.auth.authorization.AuthScopeStore;
import org.wso2.broker.auth.authorization.authorizer.rdbms.scope.AuthScope;
import org.wso2.broker.auth.authorization.enums.ResourceAuthScopes;
import org.wso2.broker.auth.authorization.handler.AuthorizationHandler;
import org.wso2.broker.auth.exception.BrokerAuthException;
import org.wso2.broker.auth.exception.BrokerAuthServerException;
import org.wso2.broker.auth.rest.model.ScopeData;
import org.wso2.broker.auth.rest.model.ScopeUpdateRequest;
import org.wso2.broker.auth.rest.model.ScopeUpdateResponse;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

/**
 * Delegate class to handle auth scope related REST requests.
 */
public class ScopesApiDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScopesApiDelegate.class);

    private AuthScopeStore authScopeStore;

    private AuthorizationHandler authorizationHandler;

    public ScopesApiDelegate(AuthScopeStore authScopeStore, AuthorizationHandler authorizationHandler) {
        this.authScopeStore = authScopeStore;
        this.authorizationHandler = authorizationHandler;
    }

    public Response updateScope(String scopeName, ScopeUpdateRequest requestBody) {
        try {
            authorizationHandler.handle(ResourceAuthScopes.SCOPES_UPDATE);
            AuthScope authScope = authScopeStore.read(scopeName);
            if (Objects.isNull(authScope)) {
                throw new NotFoundException("Unknown scope name " + scopeName);
            }
            authScopeStore.update(scopeName, requestBody.getUserGroups());
            return Response.ok()
                           .entity(new ScopeUpdateResponse().message("Scope updated.")).build();
        } catch (BrokerAuthServerException e) {
            String message = "Error occurred while updating scope for scopeName: " + scopeName;
            LOGGER.error(message, e);
            throw new InternalServerErrorException(message, e);
        } catch (BrokerAuthException e) {
            throw new NotAuthorizedException(e.getMessage(), e);
        }
    }

    public Response getScope(String scopeName) {
        try {
            authorizationHandler.handle(ResourceAuthScopes.SCOPES_GET);
            AuthScope authScope = authScopeStore.read(scopeName);
            if (Objects.isNull(authScope)) {
                throw new NotFoundException("Unknown scope name " + scopeName);
            } else {
                return Response.ok().entity(toAuthScope(authScope)).build();
            }
        } catch (BrokerAuthServerException e) {
            String message = "Error occurred while getting scope for scopeName: " + scopeName;
            LOGGER.error(message, e);
            throw new InternalServerErrorException(message, e);
        } catch (BrokerAuthException e) {
            throw new NotAuthorizedException(e.getMessage(), e);
        }
    }

    public Response getAllScopes() {
        try {
            authorizationHandler.handle(ResourceAuthScopes.SCOPES_GET);
            List<AuthScope> authScopes = authScopeStore.readAll();
            return Response.ok().entity(toAuthScopes(authScopes)).build();
        } catch (BrokerAuthServerException e) {
            String message = "Error occurred while getting all scopes";
            LOGGER.error(message, e);
            throw new InternalServerErrorException(message, e);
        } catch (BrokerAuthException e) {
            throw new NotAuthorizedException(e.getMessage(), e);
        }
    }

    private ScopeData toAuthScope(AuthScope authScope) {
        return new ScopeData()
                .name(authScope.getScopeName())
                .authoriedUserGroups(new ArrayList<>(authScope.getAuthorizedUserGroups()));
    }

    private List<ScopeData> toAuthScopes(List<AuthScope> authScopes) {
        List<ScopeData> scopeDataList = new LinkedList<>();
        authScopes
                .forEach(authScope -> scopeDataList.add(new ScopeData()
                                                                .name(authScope.getScopeName())
                                                                .authoriedUserGroups(new ArrayList<>(
                                                                        authScope.getAuthorizedUserGroups()))));
        return scopeDataList;
    }
}
