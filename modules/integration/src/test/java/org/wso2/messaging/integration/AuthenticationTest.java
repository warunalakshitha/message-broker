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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.messaging.integration;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.wso2.messaging.integration.util.ClientHelper;

import javax.jms.JMSException;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.naming.InitialContext;

/**
 * Class for testing client connection authentication
 */
public class AuthenticationTest {

    @Parameters({ "broker-port" })
    @Test
    public void testValidClientConnection(String port) throws Exception {
        String topicName = "MyTopic1";
        InitialContext initialContext = ClientHelper.getInitialContextBuilder("admin", "admin", "localhost", port)
                .withTopic(topicName).build();
        TopicConnectionFactory connectionFactory = (TopicConnectionFactory) initialContext
                .lookup(ClientHelper.CONNECTION_FACTORY);
        TopicConnection connection = connectionFactory.createTopicConnection();
        connection.start();
        connection.close();
    }

    @Parameters({ "broker-port" })
    @Test
    public void testInvalidClientConnection(String port) throws Exception {
        String topicName = "MyTopic1";
        InitialContext initialContext = ClientHelper
                .getInitialContextBuilder("admin", "invalidPassword", "localhost", port).withTopic(topicName).build();
        TopicConnectionFactory connectionFactory = (TopicConnectionFactory) initialContext
                .lookup(ClientHelper.CONNECTION_FACTORY);
        Assert.assertThrows(JMSException.class, connectionFactory::createTopicConnection);
    }
}
