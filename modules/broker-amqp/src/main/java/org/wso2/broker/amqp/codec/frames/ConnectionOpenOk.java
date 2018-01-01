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
import org.wso2.broker.amqp.codec.handlers.AmqpConnectionHandler;

/**
 * AMQP frame for connection.open-ok
 * Parameter Summary:
 *     1. reserved-1 (ShortString) - deprecated param
 */
public class ConnectionOpenOk extends MethodFrame {
    public ConnectionOpenOk() {
        super(0, (short) 10, (short) 41);
    }

    @Override
    protected long getMethodBodySize() {
        return 1L;
    }

    @Override
    protected void writeMethod(ByteBuf buf) {
        buf.writeByte(0);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, AmqpConnectionHandler connectionHandler) {
        // Server does not handle this
    }

    public static AmqMethodBodyFactory getFactory() {
        return (buf, channel, size) -> {
            // read the size of deprecated short string value
            short stringSize = buf.readUnsignedByte();
            // skip the other deprecated byte as well
            buf.skipBytes(stringSize);
            return new ConnectionOpenOk();
        };
    }
}
