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
package org.wso2.broker.auth.rest.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import org.wso2.broker.auth.AuthManager;
import org.wso2.broker.auth.BrokerAuthConstants;
import org.wso2.broker.auth.authorization.handler.AuthorizationHandler;
import org.wso2.broker.auth.rest.ScopesApiDelegate;
import org.wso2.broker.auth.rest.model.Error;
import org.wso2.broker.auth.rest.model.ScopeData;
import org.wso2.broker.auth.rest.model.ScopeUpdateRequest;
import org.wso2.broker.auth.rest.model.ScopeUpdateResponse;
import org.wso2.msf4j.Request;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/scopes")
@Api(description = "the scopes API")
@Produces({ "application/json" })
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSSpecServerCodegen",
                            date = "2018-02-20T18:23:36.272+05:30")
public class ScopesApi {

    private final ScopesApiDelegate scopesApiDelegate;

    public ScopesApi(AuthManager authManager, AuthorizationHandler authorizationHandler) {
        this.scopesApiDelegate = new ScopesApiDelegate(authManager.getAuthorizer().getAuthScopeStore(),
                                                       authorizationHandler);
    }

    @GET
    @Produces({ "application/json" })
    @ApiOperation(value = "Get all scopes",
                  notes = "Retrieves all the scopes",
                  response = ScopeData.class,
                  responseContainer = "List",
                  authorizations = {
                          @Authorization(value = "basicAuth")
                  },
                  tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "List of Scopes",
                         response = ScopeData.class,
                         responseContainer = "List"),
            @ApiResponse(code = 401,
                         message = "Authentication Data is missing or invalid",
                         response = Error.class)
    })
    public Response getAllScopes(@Context Request request) {
        return AuthManager
                .doAuthContextAwareFunction(request.getSession()
                                                   .getAttribute(BrokerAuthConstants.AUTHENTICATION_ID).toString(),
                                            () -> {
                                                return scopesApiDelegate.getAllScopes();

                                            });
    }

    @GET
    @Path("/{name}")
    @Produces({ "application/json" })
    @ApiOperation(value = "Get scope",
                  notes = "Retrieves scope for given scope name",
                  response = ScopeData.class,
                  authorizations = {
                          @Authorization(value = "basicAuth")
                  },
                  tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "Scope",
                         response = ScopeData.class),
            @ApiResponse(code = 401,
                         message = "Authentication Data is missing or invalid",
                         response = Error.class),
            @ApiResponse(code = 404,
                         message = "Scope not found",
                         response = Error.class)
    })
    public Response getScope(@Context Request request, @PathParam("name") @ApiParam("Name of the scope") String name) {
        return AuthManager
                .doAuthContextAwareFunction(request.getSession()
                                                   .getAttribute(BrokerAuthConstants.AUTHENTICATION_ID).toString(),
                                            () -> {
                                                return scopesApiDelegate.getScope(name);

                                            });
    }

    @PUT
    @Path("/{name}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update scope",
                  notes = "Update given scope",
                  response = ScopeUpdateResponse.class,
                  authorizations = {
                          @Authorization(value = "basicAuth")
                  },
                  tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "Scope updated",
                         response = ScopeUpdateResponse.class),
            @ApiResponse(code = 400,
                         message = "Bad Request. Invalid request or validation error.",
                         response = Error.class),
            @ApiResponse(code = 401,
                         message = "Authentication Data is missing or invalid",
                         response = Error.class),
            @ApiResponse(code = 404,
                         message = "Scope key not found",
                         response = Error.class),
            @ApiResponse(code = 415,
                         message = "Unsupported media type. The entity of the request was in a not supported "
                                 + "format.",
                         response = Error.class)
    })
    public Response updateScope(@Context Request request, @PathParam("name")
    @ApiParam("Name of the scope needs to update") String name, @Valid
                                        ScopeUpdateRequest body) {
        return AuthManager
                .doAuthContextAwareFunction(request.getSession()
                                                   .getAttribute(BrokerAuthConstants.AUTHENTICATION_ID).toString(),
                                            () -> {
                                                return scopesApiDelegate.updateScope(name, body);

                                            });
    }
}