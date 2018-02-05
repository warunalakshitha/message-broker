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
import org.wso2.broker.auth.rest.QueuesApiDelegate;
import org.wso2.broker.auth.rest.model.Error;
import org.wso2.broker.auth.rest.model.QueueAuthData;
import org.wso2.broker.auth.rest.model.QueueUpdateRequest;
import org.wso2.broker.auth.rest.model.QueueUpdateResponse;
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

@Path("/queues")
@Api(description = "the queues API")
@Produces({ "application/json" })
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSSpecServerCodegen",
                            date = "2018-02-20T23:14:54.990+05:30")
public class QueuesApi {

    private final QueuesApiDelegate queuesApiDelegate;

    public QueuesApi(AuthManager authManager, AuthorizationHandler authorizationHandler) {
        this.queuesApiDelegate = new QueuesApiDelegate(authManager.getAuthorizer().getAuthResourceStore(),
                                                       authorizationHandler);
    }

    @GET
    @Produces({ "application/json" })
    @ApiOperation(value = "Get all auth resources related to queues",
                  notes = "Gets auth resources of queues in the broker. ",
                  response = QueueAuthData.class,
                  responseContainer = "List",
                  authorizations = {
                          @Authorization(value = "basicAuth")
                  },
                  tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "List of queues",
                         response = QueueAuthData.class,
                         responseContainer = "List"),
            @ApiResponse(code = 401,
                         message = "Authentication Data is missing or invalid",
                         response = Error.class)
    })
    public Response getAuthDataOfAllQueuesa(@Context Request request) {
        return AuthManager
                .doAuthContextAwareFunction(request.getSession()
                                                   .getAttribute(BrokerAuthConstants.AUTHENTICATION_ID).toString(),
                                            () -> {
                                                return queuesApiDelegate.getAllQueues();

                                            });
    }

    @GET
    @Path("/{name}")
    @Produces({ "application/json" })
    @ApiOperation(value = "Get auth Data for given queue",
                  notes = "Retrieves auth Data for given queue",
                  response = QueueAuthData.class,
                  authorizations = {
                          @Authorization(value = "basicAuth")
                  },
                  tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "Auth Data of queue",
                         response = QueueAuthData.class),
            @ApiResponse(code = 401,
                         message = "Authentication Data is missing or invalid",
                         response = Error.class),
            @ApiResponse(code = 404,
                         message = "Queue not found",
                         response = Error.class)
    })
    public Response getQueueAuthData(@Context Request request,
                                     @PathParam("name") @ApiParam("Name of the auth resource") String name) {
        return AuthManager
                .doAuthContextAwareFunction(request.getSession()
                                                   .getAttribute(BrokerAuthConstants.AUTHENTICATION_ID).toString(),
                                            () -> {
                                                return queuesApiDelegate.getQueue(name);

                                            });
    }

    @PUT
    @Path("/{name}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update queue auth data",
                  notes = "Update queue auth data",
                  response = QueueUpdateResponse.class,
                  authorizations = {
                          @Authorization(value = "basicAuth")
                  },
                  tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201,
                         message = "Queue auth data updated.",
                         response = QueueUpdateResponse.class),
            @ApiResponse(code = 400,
                         message = "Bad Request. Invalid request or validation error.",
                         response = Error.class),
            @ApiResponse(code = 401,
                         message = "Authentication Data is missing or invalid",
                         response = Error.class),
            @ApiResponse(code = 409,
                         message = "Duplicate resource",
                         response = Error.class),
            @ApiResponse(code = 415,
                         message = "Unsupported media type. The entity of the request was in a not supported format.",
                         response = Error.class)
    })
    public Response updateQueueAuthData(@Context Request request,
                                        @PathParam("name") @ApiParam("Name of the auth resource") String name,
                                        @Valid QueueUpdateRequest body) {
        return AuthManager
                .doAuthContextAwareFunction(request.getSession()
                                                   .getAttribute(BrokerAuthConstants.AUTHENTICATION_ID).toString(),
                                            () -> {
                                                return queuesApiDelegate.updateQueueAuthData(name, body);

                                            });
    }
}
