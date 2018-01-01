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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.amqp.codec.AmqpChannel;
import org.wso2.broker.amqp.codec.BlockingTask;
import org.wso2.broker.amqp.codec.handlers.AmqpConnectionHandler;

/**
 * AMQP frame for basic.reject
 * Parameter Summary:
 *      1. delivery-tag (longlong) - delivery tag
 *      2. requeue (bit) - requeue message
 */
public class BasicReject extends MethodFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicReject.class);

    private final long deliveryTag;
    private final boolean requeue;

    public BasicReject(int channel, long deliveryTag, boolean requeue) {
        super(channel, (short) 60, (short) 80);
        this.deliveryTag = deliveryTag;
        this.requeue = requeue;
    }

    @Override
    protected long getMethodBodySize() {
        return 8L + 1L;
    }

    @Override
    protected void writeMethod(ByteBuf buf) {
        buf.writeLong(deliveryTag);
        buf.writeBoolean(requeue);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, AmqpConnectionHandler connectionHandler) {
        AmqpChannel channel = connectionHandler.getChannel(getChannel());
        ctx.fireChannelRead((BlockingTask) () -> channel.reject(deliveryTag, requeue));
    }

    public static AmqMethodBodyFactory getFactory() {
        return (buf, channel, size) -> {
            long deliveryTag = buf.readLong();
            boolean requeue = buf.readBoolean();
            return new BasicReject(channel, deliveryTag, requeue);
        };
    }
}
