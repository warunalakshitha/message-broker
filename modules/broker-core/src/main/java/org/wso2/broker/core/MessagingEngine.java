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
import org.wso2.broker.common.data.types.FieldTable;
import org.wso2.broker.core.metrics.BrokerMetricManager;
import org.wso2.broker.core.store.StoreFactory;
import org.wso2.broker.core.store.dao.SharedMessageStore;
import org.wso2.broker.core.task.TaskExecutorService;
import org.wso2.broker.core.util.MessageTracer;

import java.util.Collection;
import java.util.HashSet;
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

    MessagingEngine(StoreFactory storeFactory, BrokerMetricManager metricManager) throws BrokerException {
        this.metricManager = metricManager;
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

    private void initDefaultDeadLetterQueue() throws BrokerException {
        createQueue(DEFAULT_DEAD_LETTER_QUEUE, false, true, false);
        bind(DEFAULT_DEAD_LETTER_QUEUE,
             ExchangeRegistry.DEFAULT_DEAD_LETTER_EXCHANGE,
             DEFAULT_DEAD_LETTER_QUEUE,
             FieldTable.EMPTY_TABLE);
    }

    void bind(String queueName, String exchangeName, String routingKey, FieldTable arguments) throws BrokerException {
        lock.writeLock().lock();
        try {
            Exchange exchange = exchangeRegistry.getExchange(exchangeName);
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
            if (exchange == null) {
                throw new BrokerException("Unknown exchange name: " + exchangeName);
            }

            if (queueHandler == null) {
                throw new BrokerException("Unknown queue name: " + queueName);
            }

            if (!routingKey.isEmpty()) {
                exchange.bind(queueHandler.getQueue(), routingKey, arguments);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    void unbind(String queueName, String exchangeName, String routingKey) throws BrokerException {
        lock.writeLock().lock();
        try {
            Exchange exchange = exchangeRegistry.getExchange(exchangeName);
            QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);

            if (exchange == null) {
                throw new BrokerException("Unknown exchange name: " + exchangeName);
            }

            if (queueHandler == null) {
                throw new BrokerException("Unknown queue name: " + queueName);
            }

            exchange.unbind(queueHandler.getQueue(), routingKey);
        } finally {
            lock.writeLock().unlock();
        }
    }

    boolean createQueue(String queueName, boolean passive, boolean durable, boolean autoDelete) throws BrokerException {
        lock.writeLock().lock();
        try {
            boolean queueAdded = queueRegistry.addQueue(queueName, passive, durable, autoDelete);
            if (queueAdded) {
                QueueHandler queueHandler = queueRegistry.getQueueHandler(queueName);
                // We need to bind every queue to the default exchange
                exchangeRegistry.getDefaultExchange().bind(queueHandler.getQueue(), queueName, FieldTable.EMPTY_TABLE);
            }
            return queueAdded;
        } finally {
            lock.writeLock().unlock();
        }
    }

    void publish(Message message) throws BrokerException {
        lock.readLock().lock();
        try {
            Metadata metadata = message.getMetadata();
            Exchange exchange = exchangeRegistry.getExchange(metadata.getExchangeName());
            if (exchange != null) {
                String routingKey = metadata.getRoutingKey();
                BindingSet bindingSet = exchange.getBindingsForRoute(routingKey);

                if (bindingSet.isEmpty()) {
                    LOGGER.info("Dropping message since no queues found for routing key " + routingKey + " in "
                                        + exchange);
                    message.release();
                    MessageTracer.trace(metadata, MessageTracer.NO_ROUTES);
                } else {
                    try {
                        sharedMessageStore.add(message);
                        Set<String> uniqueQueues = new HashSet<>();
                        for (Binding binding : bindingSet.getUnfilteredBindings()) {
                            uniqueQueues.add(binding.getQueue().getName());
                        }

                        for (Binding binding : bindingSet.getFilteredBindings()) {
                            if (binding.getFilterExpression().evaluate(metadata)) {
                                uniqueQueues.add(binding.getQueue().getName());
                            }
                        }
                        publishToQueues(message, uniqueQueues);
                    } finally {
                        sharedMessageStore.flush(metadata.getInternalId());
                        // Release the original message. Shallow copies are distributed
                        message.release(); // TODO: avoid shallow copying when there is only one binding
                    }
                }
            } else {
                message.release();
                MessageTracer.trace(metadata, MessageTracer.UNKNOWN_EXCHANGE);
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
            MessageTracer.trace(message.getMetadata(), MessageTracer.NO_ROUTES);
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

    boolean deleteQueue(String queueName, boolean ifUnused, boolean ifEmpty) throws BrokerException {
        lock.writeLock().lock();
        try {
            return queueRegistry.removeQueue(queueName, ifUnused, ifEmpty);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void consume(Consumer consumer) throws BrokerException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Consume request received for {}", consumer.getQueueName());
        }
        lock.readLock().lock();
        try {
            QueueHandler queueHandler = queueRegistry.getQueueHandler(consumer.getQueueName());
            if (queueHandler != null) {
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

    void createExchange(String exchangeName, String type,
                        boolean passive, boolean durable) throws BrokerException {
        lock.writeLock().lock();
        try {
            exchangeRegistry.declareExchange(exchangeName, Exchange.Type.from(type), passive, durable);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void deleteExchange(String exchangeName, boolean ifUnused) throws BrokerException {
        lock.writeLock().lock();
        try {
            exchangeRegistry.deleteExchange(exchangeName, ifUnused);
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

            publish(dlcMessage);
            acknowledge(queueName, message);
        } finally {
            message.release();
        }
    }

    public Collection<QueueHandler> getAllQueues() {
        return queueRegistry.getAllQueues();
    }

    public QueueHandler getQueue(String queueName) {
        return queueRegistry.getQueueHandler(queueName);
    }

    public ExchangeRegistry getExchangeRegistry() {
        return exchangeRegistry;
    }
}
