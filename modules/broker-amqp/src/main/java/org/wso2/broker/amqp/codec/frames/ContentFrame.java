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

package org.wso2.broker.amqp.codec.frames;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.amqp.AmqpException;
import org.wso2.broker.amqp.codec.AmqpChannel;
import org.wso2.broker.amqp.codec.BlockingTask;
import org.wso2.broker.amqp.codec.InMemoryMessageAggregator;
import org.wso2.broker.amqp.codec.handlers.AmqpConnectionHandler;
import org.wso2.broker.auth.AuthManager;
import org.wso2.broker.auth.BrokerAuthConstants;
import org.wso2.broker.auth.exception.BrokerAuthException;
import org.wso2.broker.common.ValidationException;
import org.wso2.broker.core.BrokerException;
import org.wso2.broker.core.Message;

/**
 * AMQP content frame
 */
public class ContentFrame extends GeneralFrame {
    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentFrame.class);

    private final long length;
    private final ByteBuf payload;

    public ContentFrame(int channel, long length, ByteBuf payload) {
        super((byte) 3, channel);
        this.length = length;
        this.payload = payload;
    }

    @Override
    public long getPayloadSize() {
        return length;
    }

    @Override
    public void writePayload(ByteBuf buf) {
        try {
            buf.writeBytes(payload);
        } finally {
            payload.release();
        }
    }

    @Override
    public void handle(ChannelHandlerContext ctx, AmqpConnectionHandler connectionHandler) {
        AmqpChannel channel = connectionHandler.getChannel(getChannel());

        boolean allContentReceived;
        InMemoryMessageAggregator messageAggregator = channel.getMessageAggregator();

        try {
            allContentReceived = messageAggregator.contentBodyReceived(length, payload);
        } catch (AmqpException e) {
            LOGGER.warn("Content receiving failed", e);
            return;
        }

        if (allContentReceived) {
            Message message = messageAggregator.popMessage();

            ctx.fireChannelRead((BlockingTask) () -> {
                Attribute<String> authorizationId =
                        ctx.channel().attr(AttributeKey.valueOf(BrokerAuthConstants.AUTHENTICATION_ID));
                AuthManager.doAuthContextAwareFunction(authorizationId.get(), () -> {
                    try {
                        messageAggregator.publish(message);
                        // flow manager should always be executed through the event loop
                        ctx.executor().submit(() -> channel.getFlowManager()
                                                           .notifyMessageRemoval(
                                                                   ctx));
                    } catch (BrokerAuthException | ValidationException | BrokerException e) {
                        LOGGER.warn("Content receiving failed", e);
                    }
                });
            });
        }
    }

    public static ContentFrame parse(ByteBuf buf, int channel, long payloadSize) {
        ByteBuf payload = buf.retainedSlice(buf.readerIndex(), (int) payloadSize);
        buf.skipBytes((int) payloadSize);

        return new ContentFrame(channel, payloadSize, payload);
    }
}
