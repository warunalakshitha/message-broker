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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.broker.core;

import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.broker.auth.authorization.handler.AuthorizationHandler;
import org.wso2.broker.auth.exception.BrokerAuthException;
import org.wso2.broker.common.ResourceNotFoundException;
import org.wso2.broker.common.ValidationException;
import org.wso2.broker.common.data.types.FieldTable;
import org.wso2.broker.core.configuration.BrokerConfiguration;
import org.wso2.broker.core.metrics.NullBrokerMetricManager;
import org.wso2.broker.core.store.StoreFactory;

import javax.sql.DataSource;

public class MessagingEngineTest {

    private MessagingEngine messagingEngine;

    private static final String DEFAULT_QUEUE_NAME = "TestQueue";

    private static final String DEFAULT_EXCHANGE_NAME = "amq.direct";

    private static final String DEFAULT_ROUTING_KEY = "TestQueue";

    @BeforeClass
    public void beforeTest() throws BrokerException, ValidationException {
        DataSource dataSource = DbUtil.getDataSource();
        NullBrokerMetricManager metricManager = new NullBrokerMetricManager();
        StoreFactory storeFactory = new StoreFactory(dataSource, metricManager, new BrokerConfiguration());
        AuthorizationHandler authorizationHandler = Mockito.mock(AuthorizationHandler.class);

        messagingEngine = new MessagingEngine(storeFactory, metricManager, authorizationHandler);
    }

    @BeforeMethod
    public void setup() throws BrokerException, ValidationException, BrokerAuthException {
        messagingEngine.createQueue(DEFAULT_QUEUE_NAME, false, false, false);
        messagingEngine.bind(DEFAULT_QUEUE_NAME, DEFAULT_EXCHANGE_NAME, DEFAULT_ROUTING_KEY, FieldTable.EMPTY_TABLE);
    }

    @AfterMethod
    public void tearDown() throws BrokerException, ValidationException, BrokerAuthException {
        messagingEngine.unbind(DEFAULT_QUEUE_NAME, DEFAULT_EXCHANGE_NAME, DEFAULT_ROUTING_KEY);

    }

    @Test (description = "Test multiple identical binding calls for the same queue. This shouldn't throw any errors")
    public void testMultipleIdenticalBindingsForTheSameQueue()
            throws BrokerException, ValidationException, BrokerAuthException {
        messagingEngine.bind(DEFAULT_QUEUE_NAME, DEFAULT_EXCHANGE_NAME, DEFAULT_ROUTING_KEY, FieldTable.EMPTY_TABLE);
        messagingEngine.bind(DEFAULT_QUEUE_NAME, DEFAULT_EXCHANGE_NAME, DEFAULT_ROUTING_KEY, FieldTable.EMPTY_TABLE);
    }

    @Test (dataProvider = "nonExistingQueues", description = "Test bind operation with non existing queues"
            , expectedExceptions = ValidationException.class)
    public void testNegativeBindWithNonExistingQueue(String queueName) throws Exception {
        messagingEngine.bind(queueName, DEFAULT_EXCHANGE_NAME, DEFAULT_ROUTING_KEY, FieldTable.EMPTY_TABLE);
    }

    @Test (dataProvider = "nonExistingExchanges", description = "Test bind operation with non existing queues"
            , expectedExceptions = ValidationException.class)
    public void testNegativeBindWithNonExistingExchange(String exchangeName) throws Exception {
        messagingEngine.bind(DEFAULT_QUEUE_NAME, exchangeName, DEFAULT_ROUTING_KEY, FieldTable.EMPTY_TABLE);
    }

    @Test (dataProvider = "nonExistingExchanges"
            , description = "Test multiple bind operation for the same queue with identical bindings"
            , expectedExceptions = ValidationException.class)
    public void testNegativeUnbindWithNonExistingExchangeTest(String exchangeName)
            throws BrokerException, ValidationException, BrokerAuthException {
        messagingEngine.unbind(DEFAULT_QUEUE_NAME, exchangeName, DEFAULT_ROUTING_KEY);
    }

    @Test (dataProvider = "nonExistingQueues", description = "Test unbind operation with non existing queues"
            , expectedExceptions = ValidationException.class)
    public void testNegativeUnbindWithNonExistingQueueTest(String queueName) throws BrokerException,
            ValidationException, BrokerAuthException {
        messagingEngine.unbind(queueName, DEFAULT_EXCHANGE_NAME, DEFAULT_ROUTING_KEY);
    }

    @Test (dataProvider = "nonExistingQueues",
           description = "Test non existing queue delete.",
           expectedExceptions = {ResourceNotFoundException.class})
    public void testNonExistingQueueDelete(String queueName) throws BrokerException,
            ResourceNotFoundException, ValidationException, BrokerAuthException {
        messagingEngine.deleteQueue(queueName, false, false);
    }

    @Test (dataProvider = "nonExistingExchanges",
            description = "Test non existing exchange delete. This shouldn't throw an exception")
    public void testNonExistingExchangeDelete(String exchangeName)
            throws BrokerException, ValidationException, BrokerAuthException {
        messagingEngine.deleteExchange(exchangeName, false);
    }

    @DataProvider(name = "nonExistingExchanges")
    public Object[] nonExistingExchanges() {
        return new Object[]{ "myExchange", "testExchange" };
    }

    @DataProvider(name = "nonExistingQueues")
    public Object[] nonExistingQueues() {
        return new Object[]{ "myQueue", "invalidQueue" };
    }

}
