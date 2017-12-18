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

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.broker.common.data.types.FieldTable;

import java.util.Collection;

/**
 * Unit tests verifying topic exchange related functionality.
 */
public class TopicExchangeTest {

    private TopicExchange topicExchange;

    private static final String EXCHANGE_NAME = "amq.topic";

    @BeforeMethod
    public void beforeTestSetup() {
        topicExchange = new TopicExchange(EXCHANGE_NAME);
    }

    @Test
    public void testGetName() {
        Assert.assertEquals(topicExchange.getName(), EXCHANGE_NAME, "Invalid exchange name.");
    }

    @Test
    public void testGetType() {
        Assert.assertEquals(topicExchange.getType(), Exchange.Type.TOPIC, "Invalid exchange type");
    }

    @Test(dataProvider = "positiveTopicPairs", description = "Test positive topic matching")
    public void testPositiveSingleTopicMatching(String subscribedPattern,
                                                String publishedTopic) throws BrokerException {
        QueueHandler handler = QueueHandler.createNonDurableQueue(subscribedPattern, 10, false);
        Queue queue = handler.getQueue();
        topicExchange.bind(queue, subscribedPattern, FieldTable.EMPTY_TABLE);

        BindingSet bindingSet = topicExchange.getBindingsForRoute(publishedTopic);

        Collection<Binding> unfilteredBindings = bindingSet.getUnfilteredBindings();
        Assert.assertEquals(unfilteredBindings.iterator().hasNext(), true,
                "No matches found.");
        Assert.assertEquals(
                unfilteredBindings.iterator().next().getQueue().getName(), subscribedPattern,
                "No matches found.");
    }

    @Test(dataProvider = "negativeTopicPairs", description = "Test negative topic matching")
    public void testNegativeSingleTopicMatching(String subscribedPattern,
                                                String publishedTopic) throws BrokerException {
        QueueHandler handler = QueueHandler.createNonDurableQueue(subscribedPattern, 10, false);
        Queue queue = handler.getQueue();
        topicExchange.bind(queue, subscribedPattern, FieldTable.EMPTY_TABLE);

        BindingSet bindingSet = topicExchange.getBindingsForRoute(publishedTopic);
        Collection<Binding> unfilteredBindings = bindingSet.getUnfilteredBindings();
        Assert.assertEquals(unfilteredBindings.iterator().hasNext(), false, "No topic should match");
    }

    @Test(dataProvider = "positiveTopicPairs", description = "Test topic removal")
    public void testTopicRemoval(String subscribedPattern, String publishedTopic) throws BrokerException {

        QueueHandler handler = QueueHandler.createNonDurableQueue(subscribedPattern, 1000, false);
        Queue queue = handler.getQueue();
        topicExchange.bind(queue, subscribedPattern, FieldTable.EMPTY_TABLE);
        topicExchange.unbind(queue, subscribedPattern);

        BindingSet bindingSet = topicExchange.getBindingsForRoute(publishedTopic);
        Collection<Binding> unfilteredBindings = bindingSet.getUnfilteredBindings();
        Assert.assertEquals(unfilteredBindings.iterator().hasNext(), false, "No topic should match");
    }

    @Test
    public void testIsUnused() {
        Assert.assertEquals(topicExchange.isUnused(), true,
                "Fresh exchange should be in unused state.");
    }

    @AfterMethod
    public void tearDown() {
        topicExchange = null;
    }

    @DataProvider(name = "positiveTopicPairs")
    public Object[][] positiveTopicPatterns() {
        return new Object[][]{
                {"sports", "sports"},
                {"sports.cricket", "sports.cricket"},
                {"sports.*", "sports.cricket"},
                {"sports.#", "sports.cricket.batsmen"},
                {"*.cricket.bowlers", "srilanka.cricket.bowlers"}
        };
    }

    @DataProvider(name = "negativeTopicPairs")
    public Object[][] negativeTopicPatterns() {
        return new Object[][]{
                {"sports", "cricket"},
                {"sports.cricket", "sports"},
                {"sports.*", "sports.cricket.batsmen"},
                {"sports.#", "local.sports.cricket.batsmen"},
                {"srilanka.*", "srilanka.sports.cricket.batsmen"}
        };
    }

}
