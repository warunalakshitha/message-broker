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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.amqp.AckData;
import org.wso2.broker.amqp.AmqpDeliverMessage;
import org.wso2.broker.amqp.codec.AmqpChannel;
import org.wso2.broker.amqp.codec.BlockingTask;
import org.wso2.broker.amqp.codec.handlers.AmqpConnectionHandler;
import org.wso2.broker.core.Message;

import java.util.Collection;

/**
 * AMQP frame for basic.requeue.
 * Parameter Summary:
 *     1. requeue (bit) - requeue the message
 */
public class BasicRecover extends MethodFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicRecover.class);

    private final boolean requeue;

    public BasicRecover(int channel, boolean requeue) {
        super(channel, (short) 60, (short) 110);
        this.requeue = requeue;
    }

    @Override
    protected long getMethodBodySize() {
        return 1L;
    }

    @Override
    protected void writeMethod(ByteBuf buf) {
        buf.writeBoolean(requeue);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, AmqpConnectionHandler connectionHandler) {
        AmqpChannel channel = connectionHandler.getChannel(getChannel());

        if (isRequeue()) {
            ctx.fireChannelRead((BlockingTask) () -> {
                channel.rejectAll();
                ctx.writeAndFlush(new BasicRecoveryOk(getChannel()));
            });
        } else {
            Collection<AckData> unackedMessages = channel.recover();
            Channel nettyChannel = ctx.channel();
            nettyChannel.write(new BasicRecoveryOk(getChannel()));
            for (AckData ackData : unackedMessages) {
                Message message = ackData.getMessage();

                message.setRedeliver();
                nettyChannel.write(new AmqpDeliverMessage(message,
                                                          ackData.getConsumerTag(),
                                                          channel,
                                                          ackData.getQueueName()));
            }
            nettyChannel.flush();
        }
    }

    /**
     * Getter for requeue.
     */
    public boolean isRequeue() {
        return requeue;
    }

    public static AmqMethodBodyFactory getFactory() {
        return (buf, channel, size) -> {
            boolean noWait = buf.readBoolean();
            return new BasicRecover(channel, noWait);
        };
    }
}
