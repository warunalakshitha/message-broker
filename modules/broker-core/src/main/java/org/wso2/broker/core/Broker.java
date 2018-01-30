/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.broker.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.auth.AuthManager;
import org.wso2.broker.auth.BrokerAuthException;
import org.wso2.broker.auth.authorization.enums.RBindingKey;
import org.wso2.broker.auth.authorization.enums.RExchange;
import org.wso2.broker.auth.authorization.enums.RGroups;
import org.wso2.broker.auth.authorization.enums.RQueue;
import org.wso2.broker.common.StartupContext;
import org.wso2.broker.common.data.types.FieldTable;
import org.wso2.broker.common.util.function.ThrowingRunnable;
import org.wso2.broker.common.util.function.ThrowingSupplier;
import org.wso2.broker.coordination.BasicHaListener;
import org.wso2.broker.coordination.HaListener;
import org.wso2.broker.coordination.HaStrategy;
import org.wso2.broker.core.metrics.BrokerMetricManager;
import org.wso2.broker.core.metrics.DefaultBrokerMetricManager;
import org.wso2.broker.core.metrics.NullBrokerMetricManager;
import org.wso2.broker.core.rest.api.QueuesApi;
import org.wso2.broker.core.store.StoreFactory;
import org.wso2.broker.rest.BrokerServiceRunner;
import org.wso2.carbon.metrics.core.MetricService;

import java.util.Collection;
import java.util.Objects;
import javax.sql.DataSource;

/**
 * Broker API class.
 */
public final class Broker {

    private static final Logger LOGGER = LoggerFactory.getLogger(Broker.class);

    private final MessagingEngine messagingEngine;

    private final AuthManager authManager;

    /**
     * Used to manage metrics related to broker
     */
    private final BrokerMetricManager metricManager;

    /**
     * The {@link HaStrategy} for which the HA listener is registered.
     */
    private HaStrategy haStrategy;

    private BrokerHelper brokerHelper;

    public Broker(StartupContext startupContext) throws Exception {
        MetricService metrics = startupContext.getService(MetricService.class);
        if (Objects.nonNull(metrics)) {
            metricManager = new DefaultBrokerMetricManager(metrics);
        } else {
            metricManager = new NullBrokerMetricManager();
        }

        DataSource dataSource = startupContext.getService(DataSource.class);
        StoreFactory storeFactory = new StoreFactory(dataSource, metricManager);
        this.messagingEngine = new MessagingEngine(storeFactory, metricManager);
        BrokerServiceRunner serviceRunner = startupContext.getService(BrokerServiceRunner.class);
        serviceRunner.deploy(new QueuesApi(this));
        startupContext.registerService(Broker.class, this);
        haStrategy = startupContext.getService(HaStrategy.class);
        if (haStrategy == null) {
            brokerHelper = new BrokerHelper();
        } else {
            LOGGER.info("Broker is in PASSIVE mode"); //starts up in passive mode
            brokerHelper = new HaEnabledBrokerHelper();
        }
        this.authManager = startupContext.getService(AuthManager.class);
    }

    /**
     * Provides {@link AuthManager} for broker
     *
     * @return Broker authentication manager
     */
    public AuthManager getAuthManager() {
        return authManager;
    }

    public void publish(Message message) throws BrokerException, BrokerAuthException {
        authorize(RGroups.BINDING_KEY.toString(), message.getMetadata().getRoutingKey(), RBindingKey.PUBLISH.getAsInt(),
                  () -> {
                      messagingEngine.publish(message); metricManager.markPublish();
                  });
    }

    /**
     * Acknowledge single or a given set of messages. Removes the message from underlying queue
     * @param queueName   name of the queue the relevant messages belongs to
     * @param message delivery tag of the message sent by the broker
     */
    public void acknowledge(String queueName, Message message) throws BrokerException {
        messagingEngine.acknowledge(queueName, message);
        metricManager.markAcknowledge();
    }

    /**
     * Adds a consumer for a queue. Queue will be the queue returned from {@link Consumer#getQueueName()}
     *
     * @param consumer {@link Consumer} implementation
     * @throws BrokerException throws {@link BrokerException} if unable to add the consumer
     */
    public void addConsumer(Consumer consumer) throws BrokerException {
        messagingEngine.consume(consumer);
    }

    public void removeConsumer(Consumer consumer) {
        messagingEngine.closeConsumer(consumer);
    }

    public void createExchange(String exchangeName, String type,
                               boolean passive, boolean durable) throws BrokerException, BrokerAuthException {
        authorize(RGroups.EXCHANGE.toString(), exchangeName, RExchange.CREATE.getAsInt(), () -> {
            messagingEngine.createExchange(exchangeName, type, passive, durable);
        });
    }

    public void deleteExchange(String exchangeName, boolean ifUnused) throws BrokerException, BrokerAuthException {
        authorize(RGroups.EXCHANGE.toString(), exchangeName, RExchange.DELETE.getAsInt(), () -> {
            messagingEngine.deleteExchange(exchangeName, ifUnused);
        });
    }

    public boolean createQueue(String queueName, boolean passive,
                            boolean durable, boolean autoDelete) throws BrokerException, BrokerAuthException {
        return authorize(RGroups.QUEUE.toString(), queueName, RQueue.CREATE.getAsInt(), () ->
                messagingEngine.createQueue(queueName, passive, durable, autoDelete));
    }

    public boolean deleteQueue(String queueName, boolean ifUnused, boolean ifEmpty) throws BrokerException {
        return messagingEngine.deleteQueue(queueName, ifUnused, ifEmpty);
    }

    public void bind(String queueName, String exchangeName,
                     String routingKey, FieldTable arguments) throws BrokerException, BrokerAuthException {
        Exchange exchange = messagingEngine.getExchangeRegistry().getExchange(exchangeName);
        if (exchange.getType().equals(Exchange.Type.TOPIC)) {
            authorizeByPattern(RGroups.BINDING_KEY.toString(), routingKey, RBindingKey.BIND.getAsInt(), () -> {
                messagingEngine.bind(queueName, exchangeName, routingKey, arguments);
            });
        } else {
            authorize(RGroups.BINDING_KEY.toString(), routingKey, RBindingKey.BIND.getAsInt(), () -> {
                messagingEngine.bind(queueName, exchangeName, routingKey, arguments);
            });
        }
    }

    public void unbind(String queueName, String exchangeName, String routingKey)
            throws BrokerException, BrokerAuthException {
        Exchange exchange = messagingEngine.getExchangeRegistry().getExchange(exchangeName);
        if (exchange.getType().equals(Exchange.Type.TOPIC)) {
            authorizeByPattern(RGroups.BINDING_KEY.toString(), exchangeName, RBindingKey.UNBIND.getAsInt(), () -> {
                messagingEngine.unbind(queueName, exchangeName, routingKey);
            });
        } else {
            authorize(RGroups.BINDING_KEY.toString(), exchangeName, RBindingKey.UNBIND.getAsInt(), () -> {
                messagingEngine.unbind(queueName, exchangeName, routingKey);
            });
        }
    }

    public void startMessageDelivery() {
        brokerHelper.startMessageDelivery();
    }

    public void stopMessageDelivery() {
        LOGGER.info("Stopping message delivery threads.");
        messagingEngine.stopMessageDelivery();
    }

    public void shutdown() {
        brokerHelper.shutdown();
    }

    public long getNextMessageId() {
        return messagingEngine.getNextMessageId();
    }

    public void requeue(String queueName, Message message) throws BrokerException {
        messagingEngine.requeue(queueName, message);
    }

    public Collection<QueueHandler> getAllQueues() {
        return messagingEngine.getAllQueues();
    }

    public QueueHandler getQueue(String queueName) {
        return messagingEngine.getQueue(queueName);
    }

    public void moveToDlc(String queueName, Message message) throws BrokerException {
        messagingEngine.moveToDlc(queueName, message);
    }

    private boolean authorize(String resourceGroup, String resource, int permission,
                              ThrowingSupplier<Boolean, BrokerException> supplier)
            throws BrokerException, BrokerAuthException {
        if (authManager.authorize(resourceGroup, resource, permission)) {
            return supplier.get();
        } else {
            throw new BrokerAuthException("Unauthorized action on resource : " + resource);
        }
    }

    private void authorize(String resourceGroup, String resource, int permission, ThrowingRunnable<BrokerException>
            runnable) throws BrokerException, BrokerAuthException {
        if (authManager.authorize(resourceGroup, resource, permission)) {
            runnable.run();
        } else {
            throw new BrokerAuthException("Unauthorized action on resource : " + resource);
        }
    }

    private void authorizeByPattern(String resourceGroup, String resource, int permission,
                                    ThrowingRunnable<BrokerException> runnable)
            throws BrokerAuthException, BrokerException {
        if (authManager.authorizeByPattern(resourceGroup, resource, permission)) {
            runnable.run();
        } else {
            throw new BrokerAuthException("Unauthorized action on resource : " + resource);
        }
    }

    private class BrokerHelper {

        public void startMessageDelivery() {
            LOGGER.info("Starting message delivery threads.");
            messagingEngine.startMessageDelivery();
        }

        public void shutdown() {
            stopMessageDelivery();
        }

    }

    private class HaEnabledBrokerHelper extends BrokerHelper implements HaListener {

        private BasicHaListener basicHaListener;

        HaEnabledBrokerHelper() {
            basicHaListener = new BasicHaListener(this);
            haStrategy.registerListener(basicHaListener, 1);
        }

        @Override
        public synchronized void startMessageDelivery() {
            basicHaListener.setStartCalled(); //to allow starting when the node becomes active when HA is enabled
            if (!basicHaListener.isActive()) {
                return;
            }
            super.startMessageDelivery();
        }

        @Override
        public void shutdown() {
            haStrategy.unregisterListener(basicHaListener);
            super.shutdown();
        }

        /**
         * {@inheritDoc}
         */
        public void activate() {
            try {
                messagingEngine.reloadOnBecomingActive();
                authManager.getPermissionStore().clearAllPermissions();
            } catch (BrokerException e) {
                LOGGER.error("Error on loading data from the database on becoming active ", e);
            }
            startMessageDeliveryOnBecomingActive();
            LOGGER.info("Broker mode changed from PASSIVE to ACTIVE");
        }

        /**
         * {@inheritDoc}
         */
        public void deactivate() {
            stopMessageDelivery();
            LOGGER.info("Broker mode changed from ACTIVE to PASSIVE");
        }

        /**
         * Method to start message delivery by the broker, only if startMessageDelivery()} has been called, prior to
         * becoming the active node.
         */
        private synchronized void startMessageDeliveryOnBecomingActive() {
            if (basicHaListener.isStartCalled()) {
                startMessageDelivery();
            }
        }

    }

}
