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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package io.ballerina.messaging.broker.amqp.codec.frames;

import io.ballerina.messaging.broker.amqp.codec.AmqpChannel;
import io.ballerina.messaging.broker.amqp.codec.BlockingTask;
import io.ballerina.messaging.broker.amqp.codec.handlers.AmqpConnectionHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * AMQP frame for tx.select
 */
public class TxSelect extends MethodFrame {

    private static final short CLASS_ID = 90;
    private static final short METHOD_ID = 10;

    public TxSelect(int channel) {
        super(channel, CLASS_ID, METHOD_ID);
    }

    @Override
    protected long getMethodBodySize() {
        return 0L;
    }

    @Override
    protected void writeMethod(ByteBuf buf) {
        // Nothing to write
    }

    @Override
    public void handle(ChannelHandlerContext ctx, AmqpConnectionHandler connectionHandler) {
        int channelId = getChannel();
        AmqpChannel channel = connectionHandler.getChannel(channelId);
        ctx.fireChannelRead((BlockingTask) () -> {
            channel.setLocalTransactional();
            ctx.writeAndFlush(new TxSelectOk(channelId));
        });
    }

    public static AmqMethodBodyFactory getFactory() {
        return (buf, channel, size) -> new TxSelect(channel);
    }
}
