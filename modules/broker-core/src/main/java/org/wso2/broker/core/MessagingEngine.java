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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.auth.authorization.enums.ResourceActions;
import org.wso2.broker.auth.authorization.enums.ResourceAuthScopes;
import org.wso2.broker.auth.authorization.enums.ResourceTypes;
import org.wso2.broker.auth.authorization.handler.AuthorizationHandler;
import org.wso2.broker.auth.exception.BrokerAuthException;
import org.wso2.broker.common.ResourceNotFoundException;
import org.wso2.broker.common.ValidationException;
import org.wso2.broker.common.data.types.FieldTable;
import org.wso2.broker.common.util.function.ThrowingConsumer;
import org.wso2.broker.common.util.function.ThrowingRunnable;
import org.wso2.broker.core.metrics.BrokerMetricManager;
import org.wso2.broker.core.store.SharedMessageStore;
import org.wso2.broker.core.store.StoreFactory;
import org.wso2.broker.core.task.TaskExecutorService;
import org.wso2.broker.core.util.MessageTracer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Broker's messaging core which handles message publishing, create and delete queue operations.
 */
final class MessagingEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingEngine.class);

    /**
     * Delay for waiting for an idle task.
     */
    private static final long IDLE_TASK_DELAY_MILLIS = 100;

    /**
     * Number of worker.
     */
    private static final int WORKER_COUNT = 5;

    /**
     * Internal queue used to put unprocessable messages.
     */
    public static final String DEFAULT_DEAD_LETTER_QUEUE = "amq.dlq";

    /**
     * Generated header names when putting a file to dead letter queue.
     */
    public static final String ORIGIN_QUEUE_HEADER = "x-origin-queue";
    public static final String ORIGIN_EXCHANGE_HEADER = "x-origin-exchange";
    public static final String ORIGIN_ROUTING_KEY_HEADER = "x-origin-routing-key";

    private final QueueRegistry queueRegistry;

    private final TaskExecutorService<MessageDeliveryTask> deliveryTaskService;

    private final ExchangeRegistry exchangeRegistry;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final SharedMessageStore sharedMessageStore;

    /**
     * In memory message id.
     */
    private final MessageIdGenerator messageIdGenerator;

    /**
     * Used to report messaging metrics
     */
    private final BrokerMetricManager metricManager;

    private final AuthorizationHandler authHandler;

    MessagingEngine(StoreFactory storeFactory, BrokerMetricManager metricManager, AuthorizationHandler authHandler)
            throws BrokerException, ValidationException {
        this.metricManager = metricManager;
        this.authHandler = authHandler;
        exchangeRegistry = storeFactory.getExchangeRegistry();
        // TODO: get the buffer sizes from configs
        sharedMessageStore = storeFactory.getSharedMessageStore(32768, 1024);
        queueRegistry = storeFactory.getQueueRegistry(sharedMessageStore);
        exchangeRegistry.retrieveFromStore(queueRegistry);

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("MessageDeliveryTaskThreadPool-%d").build();
        deliveryTaskService = new TaskExecutorService<>(WORKER_COUNT, IDLE_TASK_DELAY_MILLIS, threadFactory);
        messageIdGenerator = new MessageIdGenerator();

        initDefaultDeadLetterQueue();
    }

    private void initDefaultDeadLetterQueue() throws BrokerException, ValidationException {
        try {
            createQueue(DEFAULT_DEAD_LETTER_QUEUE, false, true, false, () -> {
            }, () -> {
            });
            bind(DEFAULT_DEAD_LETTER_QUEUE,
                 ExchangeRegistry.DEFAULT_DEAD_LETTER_EXCHANGE,
                 DEFAULT_DEAD_LETTER_QUEUE,
                 FieldTable.EMPTY_TABLE, exchange -> {
                    }, queueHandler -> {
                    });
        } catch (BrokerAuthException e) {
            throw new BrokerException("Auth error while initializing dead letter queue", e);
        }
    }

    void bind(String queueName, String exchangeName, String routingKey, FieldTable arguments)
            throws BrokerAuthException, BrokerException, ValidationException {
        bind(queueName, exchangeName, routingKey, arguments, exchange -> {
            authHandler.handle(ResourceTypes.EXCHANGE, exchange.getName(), ResourceActions.BIND);
        }, queueHandler -> {
            authHandler.handle(ResourceTypes.QUEUE, queueName, ResourceActions.BIND);
        });
    }

    private void bind(String queueName, String exchangeName, String routingKey, FieldTable arguments,
                      ThrowingConsumer<Exchange, BrokerAuthException> exchangeAuthorizer,
                      ThrowingConsumer<QueueHandler, BrokerAuthException> queueAuthorizer)
            throws BrokerException, ValidationException, BrokerAuthException {

        lock.writeLock().lock();
        try {
            Exchange exchange = exchangeRegistry.getExchange(exchangeName);
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            if (exchange == null) {
                throw new ValidationException("Unknown exchange name: " + exchangeName);
            }

            if (queueHandler == null) {
                throw new ValidationException("Unknown queue name: " + queueName);
            }

            if (!routingKey.isEmpty()) {
                exchangeAuthorizer.accept(exchange);
                queueAuthorizer.accept(queueHandler);
                exchange.bind(queueHandler, routingKey, arguments);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    void unbind(String queueName, String exchangeName, String routingKey)
            throws BrokerException, ValidationException, BrokerAuthException {
        lock.writeLock().lock();
        try {
            Exchange exchange = exchangeRegistry.getExchange(exchangeName);
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);

            if (exchange == null) {
                throw new ValidationException("Unknown exchange name: " + exchangeName);
            }

            if (queueHandler == null) {
                throw new ValidationException("Unknown queue name: " + queueName);
            }
            authHandler.handle(ResourceTypes.QUEUE, queueName,
                               ResourceActions.UNBIND);
            if (!exchange.getName().equals(exchangeRegistry.getDefaultExchange().getName())) {
                authHandler.handle(ResourceTypes.EXCHANGE, exchange.getName(),
                                   ResourceActions.UNBIND);
            }
            exchange.unbind(queueHandler.getQueue(), routingKey);
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean createQueue(String queueName, boolean passive, boolean durable, boolean autoDelete)
            throws BrokerException, ValidationException, BrokerAuthException {
        return createQueue(queueName, passive, durable, autoDelete,
                           () -> authHandler.handle(ResourceAuthScopes.QUEUES_CREATE),
                           () -> authHandler.createAuthResource(ResourceTypes.QUEUE, queueName, durable));
    }

    private boolean createQueue(String queueName, boolean passive, boolean durable, boolean autoDelete,
                                ThrowingRunnable<BrokerAuthException> queueAuthorizer,
                                ThrowingRunnable<BrokerAuthException> authResourceCreator)
            throws BrokerException, BrokerAuthException, ValidationException {

        lock.writeLock().lock();
        try {
            queueAuthorizer.run();
            boolean queueAdded = queueRegistry.addQueue(queueName, passive, durable, autoDelete);
            if (queueAdded) {
                authResourceCreator.run();
                QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
                // We need to bind every queue to the default exchange
                exchangeRegistry.getDefaultExchange().bind(queueHandler, queueName, FieldTable.EMPTY_TABLE);
            }
            return queueAdded;
        } finally {
            lock.writeLock().unlock();
        }
    }

    void publish(Message message) throws BrokerException, BrokerAuthException {
        publish(message, exchange -> {
            authHandler.handle(ResourceTypes.EXCHANGE, exchange.getName(),
                               ResourceActions.PUBLISH);
        }, queue -> {
            authHandler.handle(ResourceTypes.QUEUE, queue.getName(),
                               ResourceActions.PUBLISH);
        });
    }

    private void publish(Message message,
                         ThrowingConsumer<Exchange, BrokerAuthException> exchangeAuthorizer,
                         ThrowingConsumer<Queue, BrokerAuthException> queueAuthorizer)
            throws BrokerException, BrokerAuthException {
        lock.readLock().lock();
        try {
            Metadata metadata = message.getMetadata();
            Exchange exchange = exchangeRegistry.getExchange(metadata.getExchangeName());
            if (exchange != null) {
                exchangeAuthorizer.accept(exchange);
                String routingKey = metadata.getRoutingKey();
                BindingSet bindingSet = exchange.getBindingsForRoute(routingKey);

                if (bindingSet.isEmpty()) {
                    LOGGER.info("Dropping message since no queues found for routing key " + routingKey + " in "
                                        + exchange);
                    message.release();
                    MessageTracer.trace(message, MessageTracer.NO_ROUTES);
                } else {
                    try {
                        sharedMessageStore.add(message);
                        Set<String> uniqueQueues = new HashSet<>();
                        for (Binding binding : bindingSet.getUnfilteredBindings()) {
                            queueAuthorizer.accept(binding.getQueue());
                            uniqueQueues.add(binding.getQueue().getName());
                        }

                        for (Binding binding : bindingSet.getFilteredBindings()) {
                            if (binding.getFilterExpression().evaluate(metadata)) {
                                queueAuthorizer.accept(binding.getQueue());
                                uniqueQueues.add(binding.getQueue().getName());
                            }
                        }
                        publishToQueues(message, uniqueQueues);
                    } finally {
                        sharedMessageStore.flush(message.getInternalId());
                        // Release the original message. Shallow copies are distributed
                        message.release(); // TODO: avoid shallow copying when there is only one binding
                    }
                }
            } else {
                message.release();
                MessageTracer.trace(message, MessageTracer.UNKNOWN_EXCHANGE);
                throw new BrokerException("Message publish failed. Unknown exchange: " + metadata.getExchangeName());
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    private void publishToQueues(Message message, Set<String> uniqueQueues) throws BrokerException {
        // Unique queues can be empty due to un-matching selectors.
        if (uniqueQueues.isEmpty()) {
            LOGGER.info("Dropping message since message didn't have any routes to {}",
                        message.getMetadata().getRoutingKey());
            MessageTracer.trace(message, MessageTracer.NO_ROUTES);
            return;
        }

        for (String queueName : uniqueQueues) {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            Message copiedMessage = message.shallowCopy();
            queueHandler.enqueue(copiedMessage);
        }
    }

    /**
     * @param queueName name of the queue
     * @param message   synonymous for message id
     */
    void acknowledge(String queueName, Message message) throws BrokerException {
        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            queueHandler.acknowledge(message);
        } finally {
            lock.readLock().unlock();
        }
    }

    int deleteQueue(String queueName, boolean ifUnused, boolean ifEmpty) throws BrokerException,
            ValidationException,
            ResourceNotFoundException, BrokerAuthException {
        lock.writeLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            if (Objects.nonNull(queueHandler) && Objects.nonNull(queueHandler.getQueue())) {
                authHandler.handle(ResourceTypes.QUEUE, queueName,
                                   ResourceActions.DELETE);
            }
            int removeQueue = queueRegistry.removeQueue(queueName, ifUnused, ifEmpty);
            authHandler.deleteAuthResource(ResourceTypes.QUEUE, queueName);
            return removeQueue;
        } finally {
            lock.writeLock().unlock();
        }
    }

    void consume(Consumer consumer) throws BrokerException, BrokerAuthException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Consume request received for {}", consumer.getQueueName());
        }
        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(consumer.getQueueName());
            if (queueHandler != null) {
                authHandler.handle(ResourceTypes.QUEUE,
                                   consumer.getQueueName(),
                                   ResourceActions.CONSUME);
                synchronized (queueHandler) {
                    if (queueHandler.addConsumer(consumer) && queueHandler.consumerCount() == 1) {
                        deliveryTaskService.add(new MessageDeliveryTask(queueHandler));
                    }
                }
            } else {
                throw new BrokerException("Cannot add consumer. Queue [ " + consumer.getQueueName() + " ] "
                                                  + "not found. Create the queue before attempting to consume.");
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    void startMessageDelivery() {
        deliveryTaskService.start();
    }

    void stopMessageDelivery() {
        deliveryTaskService.stop();
    }

    void declareExchange(String exchangeName, String type,
                         boolean passive, boolean durable)
            throws BrokerException, ValidationException, BrokerAuthException {
        lock.writeLock().lock();
        try {
            authHandler.handle(ResourceAuthScopes.EXCHANGES_CREATE);
            exchangeRegistry.declareExchange(exchangeName, type, passive, durable);
            if (!passive) {
                authHandler.createAuthResource(ResourceTypes.EXCHANGE, exchangeName, durable);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    void createExchange(String exchangeName, String type, boolean durable) throws BrokerException,
            ValidationException, BrokerAuthException {
        lock.writeLock().lock();
        try {
            authHandler.handle(ResourceAuthScopes.EXCHANGES_CREATE);
            exchangeRegistry.createExchange(exchangeName, Exchange.Type.from(type), durable);
            authHandler.createAuthResource(ResourceTypes.EXCHANGE, exchangeName, durable);
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean deleteExchange(String exchangeName, boolean ifUnused)
            throws BrokerException, ValidationException, BrokerAuthException {
        lock.writeLock().lock();
        try {
            Exchange exchange = exchangeRegistry.getExchange(exchangeName);
            if (Objects.nonNull(exchange)) {
                authHandler.handle(ResourceAuthScopes.EXCHANGES_DELETE,
                                   ResourceTypes.EXCHANGE,
                                   exchangeName,
                                   ResourceActions.DELETE); ;
            }
            boolean deleteExchange = exchangeRegistry.deleteExchange(exchangeName, ifUnused);
            if (deleteExchange) {
                authHandler.deleteAuthResource(ResourceTypes.EXCHANGE, exchangeName);
            }
            return deleteExchange;
        } finally {
            lock.writeLock().unlock();
        }
    }

    void closeConsumer(Consumer consumer) {
        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(consumer.getQueueName());
            if (queueHandler != null) {
                synchronized (queueHandler) {
                    if (queueHandler.removeConsumer(consumer) && queueHandler.consumerCount() == 0) {
                        deliveryTaskService.remove(queueHandler.getQueue().getName());
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    long getNextMessageId() {
        return messageIdGenerator.getNextId();
    }

    public void requeue(String queueName, Message message) throws BrokerException {
        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            queueHandler.requeue(message);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void moveToDlc(String queueName, Message message) throws BrokerException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Moving message to DLC: {}", message);
        }
        try {
            Message dlcMessage = message.shallowCopyWith(getNextMessageId(),
                                                         DEFAULT_DEAD_LETTER_QUEUE,
                                                         ExchangeRegistry.DEFAULT_DEAD_LETTER_EXCHANGE);
            dlcMessage.getMetadata().addHeader(ORIGIN_QUEUE_HEADER, queueName);
            dlcMessage.getMetadata().addHeader(ORIGIN_EXCHANGE_HEADER, message.getMetadata().getExchangeName());
            dlcMessage.getMetadata().addHeader(ORIGIN_ROUTING_KEY_HEADER, message.getMetadata().getRoutingKey());

            publish(dlcMessage, exchange -> {
            }, queue -> {
            });
            acknowledge(queueName, message);
        } catch (BrokerAuthException e) {
            throw new BrokerException("Auth error while moving message to dlc.", e);
        } finally {
            message.release();
        }
    }

    public Collection<QueueHandler> getAllQueues() {
        lock.readLock().lock();
        try {
            return queueRegistry.getAllQueues();
        } finally {
            lock.readLock().unlock();
        }
    }

    public QueueHandler getQueue(String queueName) {
        lock.readLock().lock();
        try {
            return queueRegistry.getQueueHandler(queueName);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<Exchange> getAllExchanges() {
        lock.readLock().lock();
        try {
            return exchangeRegistry.getAllExchanges();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Map<String, BindingSet> getAllBindingsForExchange(String exchangeName) throws ValidationException {
        lock.readLock().lock();

        try {
            Exchange exchange = exchangeRegistry.getExchange(exchangeName);
            if (Objects.isNull(exchange)) {
                throw new ValidationException("Non existing exchange name " + exchangeName);
            }

            return exchange.getBindingsRegistry().getAllBindings();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Exchange getExchange(String exchangeName) {
        lock.readLock().lock();
        try {
            return exchangeRegistry.getExchange(exchangeName);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Method to reload data from the database on becoming the active node.
     *
     * @throws BrokerException if an error occurs retrieving queue and exchange data from the database
     */
    void reloadOnBecomingActive() throws BrokerException {
        sharedMessageStore.clearPendingMessages();
        queueRegistry.reloadQueuesOnBecomingActive();
        exchangeRegistry.reloadExchangesOnBecomingActive(queueRegistry);
    }

}
