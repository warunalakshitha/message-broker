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
import org.wso2.broker.auth.rest.ExchangesApiDelegate;
import org.wso2.broker.auth.rest.model.Error;
import org.wso2.broker.auth.rest.model.ExchangeAuthData;
import org.wso2.broker.auth.rest.model.ExchangeUpdateRequest;
import org.wso2.broker.auth.rest.model.ExchangeUpdateResponse;
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

@Path("/exchanges")
@Api(description = "the exchanges API")
@Produces({ "application/json" })
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaJAXRSSpecServerCodegen",
                            date = "2018-02-20T23:14:54.990+05:30")
public class ExchangesApi {

    private final ExchangesApiDelegate exchangesApiDelegate;

    public ExchangesApi(AuthManager authManager, AuthorizationHandler authorizationHandler) {
        this.exchangesApiDelegate = new ExchangesApiDelegate(authManager.getAuthorizer().getAuthResourceStore(),
                                                             authorizationHandler);
    }

    @GET
    @Produces({ "application/json" })
    @ApiOperation(value = "Get all auth resources related to exchanges",
                  notes = "Gets auth resources of exchanges in the broker. ",
                  response = ExchangeAuthData.class,
                  responseContainer = "List",
                  authorizations = {
                          @Authorization(value = "basicAuth")
                  },
                  tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "List of exchanges",
                         response = ExchangeAuthData.class,
                         responseContainer = "List"),
            @ApiResponse(code = 401,
                         message = "Authentication Data is missing or invalid",
                         response = Error.class)
    })
    public Response getAuthDataOfAllExchangesa(@Context Request request) {
        return AuthManager
                .doAuthContextAwareFunction(request.getSession()
                                                   .getAttribute(BrokerAuthConstants.AUTHENTICATION_ID).toString(),
                                            () -> {
                                                return exchangesApiDelegate.getAllExchanges();

                                            });
    }

    @GET
    @Path("/{name}")
    @Produces({ "application/json" })
    @ApiOperation(value = "Get auth Data for given exchange",
                  notes = "Retrieves auth Data for given exchange",
                  response = ExchangeAuthData.class,
                  authorizations = {
                          @Authorization(value = "basicAuth")
                  },
                  tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "Auth Data of exchange",
                         response = ExchangeAuthData.class),
            @ApiResponse(code = 401,
                         message = "Authentication Data is missing or invalid",
                         response = Error.class),
            @ApiResponse(code = 404,
                         message = "Resource not found",
                         response = Error.class)
    })
    public Response getExchangeAuthData(@Context Request request,
                                        @PathParam("name") @ApiParam("Name of the auth resource") String name) {
        return AuthManager
                .doAuthContextAwareFunction(request.getSession()
                                                   .getAttribute(BrokerAuthConstants.AUTHENTICATION_ID).toString(),
                                            () -> {
                                                return exchangesApiDelegate.getExchange(name);

                                            });
    }

    @PUT
    @Path("/{name}")
    @Consumes({ "application/json" })
    @Produces({ "application/json" })
    @ApiOperation(value = "Update exchange auth data",
                  notes = "Update exchange auth data",
                  response = ExchangeUpdateResponse.class,
                  authorizations = {
                          @Authorization(value = "basicAuth")
                  },
                  tags = {})
    @ApiResponses(value = {
            @ApiResponse(code = 201,
                         message = "Exchange auth data updated.",
                         response = ExchangeUpdateResponse.class),
            @ApiResponse(code = 400,
                         message = "Bad Request. Invalid request or validation error.",
                         response = Error.class),
            @ApiResponse(code = 401,
                         message = "Authentication Data is missing or invalid",
                         response = Error.class),
            @ApiResponse(code = 404,
                         message = "Exchange not found",
                         response = Error.class),
            @ApiResponse(code = 415,
                         message = "Unsupported media type. The entity of the request was in a not supported format.",
                         response = Error.class)
    })
    public Response updateExchangeAuthData(@Context Request request,
                                           @PathParam("name") @ApiParam("Name of the auth resource") String name,
                                           @Valid ExchangeUpdateRequest body) {
        return AuthManager
                .doAuthContextAwareFunction(request.getSession()
                                                   .getAttribute(BrokerAuthConstants.AUTHENTICATION_ID).toString(),
                                            () -> {
                                                return exchangesApiDelegate.updateExchangeAuthData(name, body);
                                            });
    }
}
