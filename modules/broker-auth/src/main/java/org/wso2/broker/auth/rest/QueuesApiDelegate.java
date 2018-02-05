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
import org.wso2.broker.auth.rest.model.QueueAuthData;
import org.wso2.broker.auth.rest.model.QueueUpdateRequest;
import org.wso2.broker.auth.rest.model.QueueUpdateResponse;

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
 * Delegate class to handle authorisations of queues related REST requests.
 */
public class QueuesApiDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueuesApiDelegate.class);

    private AuthResourceStore authResourceStore;

    private AuthorizationHandler authorizationHandler;

    public QueuesApiDelegate(AuthResourceStore authResourceStore, AuthorizationHandler authorizationHandler) {
        this.authResourceStore = authResourceStore;
        this.authorizationHandler = authorizationHandler;
    }

    public Response updateQueueAuthData(String queueName, QueueUpdateRequest requestBody) {
        try {
            authorizationHandler.handle(ResourceTypes.QUEUE, queueName, ResourceActions.UPDATE);
            AuthResource existResource = authResourceStore.read(ResourceTypes.QUEUE.toString(), queueName);
            if (Objects.isNull(existResource)) {
                throw new NotFoundException("Unknown queue for name " + queueName);
            }
            AuthResource authResource = new AuthResource(ResourceTypes.QUEUE.toString(),
                                                         queueName,
                                                         requestBody.isDurable(),
                                                         requestBody.getOwner(),
                                                         toActionUserGroupMap(requestBody.getAuthorizedUserGroups()));
            authResourceStore.update(authResource);
            return Response.ok()
                           .entity(new QueueUpdateResponse().message("Queue updated.")).build();
        } catch (BrokerAuthServerException e) {
            String message = "Error occurred while updating queue for name " + queueName;
            LOGGER.error(message, e);
            throw new InternalServerErrorException(message, e);
        } catch (BrokerAuthNotFoundException e) {
            throw new NotFoundException(e.getMessage(), e);
        } catch (BrokerAuthException e) {
            throw new NotAuthorizedException(e.getMessage(), e);
        }
    }

    public Response getQueue(String queueName) {
        try {
            authorizationHandler.handle(ResourceTypes.QUEUE, queueName, ResourceActions.GET);
            AuthResource authResource = authResourceStore.read(ResourceTypes.QUEUE.toString(), queueName);
            if (Objects.isNull(authResource)) {
                throw new NotFoundException("Unknown queue name " + queueName);
            } else {
                return Response.ok().entity(toAuthQueue(authResource)).build();
            }
        } catch (BrokerAuthServerException e) {
            String message = "Error occurred while getting queue for queueName: " + queueName;
            LOGGER.error(message, e);
            throw new InternalServerErrorException(message, e);
        } catch (BrokerAuthNotFoundException e) {
            throw new NotFoundException(e.getMessage(), e);
        } catch (BrokerAuthException e) {
            throw new NotAuthorizedException(e.getMessage(), e);
        }
    }

    public Response getAllQueues() {
        try {
            List<AuthResource> authResources =
                    authResourceStore.readAll(ResourceTypes.QUEUE.toString(),
                                              ResourceActions.GET.toString(),
                                              AuthManager.getAuthContext().get());
            return Response.ok().entity(toAuthQueues(authResources)).build();
        } catch (BrokerAuthServerException e) {
            String message = "Error occurred while getting all queues";
            LOGGER.error(message, e);
            throw new InternalServerErrorException(message, e);
        } catch (BrokerAuthException e) {
            throw new NotAuthorizedException(e.getMessage(), e);
        }
    }

    private QueueAuthData toAuthQueue(AuthResource authResource) {
        return new QueueAuthData()
                .name(authResource.getResourceName())
                .owner(authResource.getOwner())
                .durable(authResource.isDurable())
                .mappings(toActionUserGroupsMapping(authResource.getActionsUserGroupsMap()));
    }

    private List<QueueAuthData> toAuthQueues(List<AuthResource> authResources) {
        List<QueueAuthData> queueDataList = new LinkedList<>();
        authResources
                .forEach(authResource -> queueDataList
                        .add(new QueueAuthData()
                                     .name(authResource.getResourceName())
                                     .owner(authResource.getOwner())
                                     .durable(authResource.isDurable())
                                     .mappings(toActionUserGroupsMapping(authResource.getActionsUserGroupsMap()))));
        return queueDataList;
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
