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
import org.wso2.broker.auth.AuthManager;
import org.wso2.broker.auth.authorization.AuthResourceStore;
import org.wso2.broker.auth.authorization.authorizer.rdbms.resource.AuthResource;
import org.wso2.broker.auth.authorization.enums.ResourceActions;
import org.wso2.broker.auth.authorization.enums.ResourceTypes;
import org.wso2.broker.auth.authorization.handler.AuthorizationHandler;
import org.wso2.broker.auth.exception.BrokerAuthException;
import org.wso2.broker.auth.exception.BrokerAuthNotFoundException;
import org.wso2.broker.auth.exception.BrokerAuthServerException;
import org.wso2.broker.auth.rest.model.ActionUserGroupsMapping;
import org.wso2.broker.auth.rest.model.ExchangeAuthData;
import org.wso2.broker.auth.rest.model.ExchangeUpdateRequest;
import org.wso2.broker.auth.rest.model.ExchangeUpdateResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

/**
 * Delegate class to handle authorisations of exchanges related REST requests.
 */
public class ExchangesApiDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangesApiDelegate.class);

    private AuthResourceStore authResourceStore;

    private AuthorizationHandler authorizationHandler;

    public ExchangesApiDelegate(AuthResourceStore authResourceStore, AuthorizationHandler authorizationHandler) {
        this.authResourceStore = authResourceStore;
        this.authorizationHandler = authorizationHandler;
    }

    public Response updateExchangeAuthData(String exchangeName, ExchangeUpdateRequest requestBody) {
        try {
            authorizationHandler.handle(ResourceTypes.EXCHANGE, exchangeName, ResourceActions.UPDATE);
            AuthResource existResource = authResourceStore.read(ResourceTypes.EXCHANGE.toString(), exchangeName);
            if (Objects.isNull(existResource)) {
                throw new NotFoundException("Unknown exchange for name " + exchangeName);
            }
            AuthResource authResource = new AuthResource(ResourceTypes.EXCHANGE.toString(),
                                                         exchangeName,
                                                         requestBody.isDurable(),
                                                         requestBody.getOwner(),
                                                         toActionUserGroupMap(requestBody.getAuthorizedUserGroups()));
            authResourceStore.update(authResource);
            return Response.ok()
                           .entity(new ExchangeUpdateResponse().message("Exchange updated.")).build();
        } catch (BrokerAuthServerException e) {
            String message = "Error occurred while updating exchange for name " + exchangeName;
            LOGGER.error(message, e);
            throw new InternalServerErrorException(message, e);
        } catch (BrokerAuthNotFoundException e) {
            throw new NotFoundException(e.getMessage(), e);
        } catch (BrokerAuthException e) {
            throw new NotAuthorizedException(e.getMessage(), e);
        }
    }

    public Response getExchange(String exchangeName) {
        try {
            authorizationHandler.handle(ResourceTypes.EXCHANGE, exchangeName, ResourceActions.GET);
            AuthResource authResource = authResourceStore.read(ResourceTypes.EXCHANGE.toString(), exchangeName);
            if (Objects.isNull(authResource)) {
                throw new NotFoundException("Unknown exchange name " + exchangeName);
            } else {
                return Response.ok().entity(toAuthExchange(authResource)).build();
            }
        } catch (BrokerAuthServerException e) {
            String message = "Error occurred while getting exchange for exchangeName: " + exchangeName;
            LOGGER.error(message, e);
            throw new InternalServerErrorException(message, e);
        } catch (BrokerAuthNotFoundException e) {
            throw new NotFoundException(e.getMessage(), e);
        } catch (BrokerAuthException e) {
            throw new NotAuthorizedException(e.getMessage(), e);
        }
    }

    public Response getAllExchanges() {
        try {
            List<AuthResource> authResources =
                    authResourceStore.readAll(ResourceTypes.EXCHANGE.toString(),
                                              ResourceActions.GET.toString(),
                                              AuthManager.getAuthContext().get());
            return Response.ok().entity(toAuthExchanges(authResources)).build();
        } catch (BrokerAuthServerException e) {
            String message = "Error occurred while getting all exchanges";
            LOGGER.error(message, e);
            throw new InternalServerErrorException(message, e);
        } catch (BrokerAuthException e) {
            throw new NotAuthorizedException(e.getMessage(), e);
        }
    }

    private ExchangeAuthData toAuthExchange(AuthResource authResource) {
        return new ExchangeAuthData()
                .name(authResource.getResourceName())
                .owner(authResource.getOwner())
                .durable(authResource.isDurable())
                .mappings(toActionUserGroupsMapping(authResource.getActionsUserGroupsMap()));
    }

    private List<ExchangeAuthData> toAuthExchanges(List<AuthResource> authResources) {
        List<ExchangeAuthData> exchangeDataList = new LinkedList<>();
        authResources
                .forEach(authResource -> exchangeDataList
                        .add(new ExchangeAuthData()
                                     .name(authResource.getResourceName())
                                     .owner(authResource.getOwner())
                                     .durable(authResource.isDurable())
                                     .mappings(toActionUserGroupsMapping(authResource.getActionsUserGroupsMap()))));
        return exchangeDataList;
    }

    private Map<String, Set<String>> toActionUserGroupMap(List<ActionUserGroupsMapping> actionUserGroupsMappings) {

        Map<String, Set<String>> actionsUserGroupsMap = new HashMap<>();
        actionUserGroupsMappings.forEach(actionUserGroupsMapping -> actionsUserGroupsMap
                .put(actionUserGroupsMapping.getAction(),
                     new HashSet<>(actionUserGroupsMapping.getUserGroups())));
        return actionsUserGroupsMap;
    }

    private ArrayList<ActionUserGroupsMapping> toActionUserGroupsMapping(
            Map<String, Set<String>> actionsUserGroupsMap) {

        ArrayList<ActionUserGroupsMapping> actionUserGroupsMappings = new ArrayList<>(actionsUserGroupsMap.size());
        actionsUserGroupsMap.forEach((action, userGroups) -> {
            ActionUserGroupsMapping actionUserGroupsMapping = new ActionUserGroupsMapping();
            actionUserGroupsMapping.setAction(action);
            actionUserGroupsMapping.setUserGroups(new ArrayList<>(userGroups));
        });
        return actionUserGroupsMappings;
    }
}
