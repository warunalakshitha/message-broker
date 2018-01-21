/*
 *   Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.wso2.messaging.integration.standalone.auth;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.wso2.messaging.integration.util.ClientHelper;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;

/**
 * class for testing authorization of broker resources.
 */
public class AuthorizationTest {

    @Parameters({ "broker-port", "admin-username", "admin-password" })
    @Test(description = "Test authorised user")
    public void testAuthorisedCreateQueue(String port, String adminUsername, String adminPassword) throws Exception {
        String queueName = "authorisedQueue";
        InitialContext initialContextForQueue = ClientHelper
                .getInitialContextBuilder(adminUsername, adminPassword, "localhost", port)
                .withQueue(queueName)
                .build();

        ConnectionFactory connectionFactory
                = (ConnectionFactory) initialContextForQueue.lookup(ClientHelper.CONNECTION_FACTORY);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session producerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = producerSession.createQueue(queueName);
        Assert.assertEquals(queue.getQueueName(), queueName, "Queue name does not match");

    }

    @Parameters({ "broker-port", "test-username", "test-password" })
    @Test(description = "Test unauthorised user", expectedExceptions = JMSException.class)
    public void testUnAuthorisedCreateQueue(String port, String testUsername, String testPassword) throws Exception {
        String queueName = "unAuthorisedQueue";
        InitialContext initialContextForQueue = ClientHelper
                .getInitialContextBuilder(testUsername, testPassword, "localhost", port)
                .withQueue(queueName)
                .build();
        ConnectionFactory connectionFactory
                = (ConnectionFactory) initialContextForQueue.lookup(ClientHelper.CONNECTION_FACTORY);
        Connection connection = connectionFactory.createConnection();
        connection.start();
        Session producerSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        producerSession.createQueue(queueName);
    }
}
