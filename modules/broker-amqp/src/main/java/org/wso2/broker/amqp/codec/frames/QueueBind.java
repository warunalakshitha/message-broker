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
import org.wso2.broker.amqp.codec.AmqpChannel;
import org.wso2.broker.amqp.codec.BlockingTask;
import org.wso2.broker.amqp.codec.ChannelException;
import org.wso2.broker.amqp.codec.handlers.AmqpConnectionHandler;
import org.wso2.broker.auth.AuthManager;
import org.wso2.broker.auth.BrokerAuthConstants;
import org.wso2.broker.common.data.types.FieldTable;
import org.wso2.broker.common.data.types.ShortString;
import org.wso2.broker.core.BrokerException;

/**
 * AMQP frame for queue.bind
 * Parameter Summary:
 *     1. reserved-1 (short) - deprecated
 *     2. queue (ShortString) - queue name
 *     3. exchange (ShortString) - name of the exchange to bind to
 *     4. routing­key (ShortString) - message routing key
 *     5. no-wait (bit) - No wait
 *     8. arguments (FieldTable) - arguments for declaration
 */
public class QueueBind extends MethodFrame {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueBind.class);
    private static final short CLASS_ID = 50;
    private static final short METHOD_ID = 20;

    private final ShortString queue;
    private final ShortString exchange;
    private final ShortString routingKey;
    private final boolean noWait;
    private final FieldTable arguments;

    public QueueBind(int channel, ShortString queue, ShortString exchange, ShortString routingKey, boolean noWait,
                     FieldTable arguments) {
        super(channel, CLASS_ID, METHOD_ID);
        this.queue = queue;
        this.exchange = exchange;
        this.routingKey = routingKey;
        this.noWait = noWait;
        this.arguments = arguments;
    }

    @Override
    protected long getMethodBodySize() {
        return 2L + queue.getSize() + exchange.getSize() + routingKey.getSize() + 1L + arguments.getSize();
    }

    @Override
    protected void writeMethod(ByteBuf buf) {
        buf.writeShort(0);
        queue.write(buf);
        exchange.write(buf);
        routingKey.write(buf);
        buf.writeBoolean(noWait);
        arguments.write(buf);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, AmqpConnectionHandler connectionHandler) {
        AmqpChannel channel = connectionHandler.getChannel(getChannel());

        ctx.fireChannelRead((BlockingTask) () -> {
            try {
                Attribute<String> authorizationId =
                        ctx.channel().attr(AttributeKey.valueOf(BrokerAuthConstants.AUTHORIZATION_ID));
                if (authorizationId != null) {
                    AuthManager.getAuthContext().set(authorizationId.get());
                }
                channel.bind(queue, exchange, routingKey, arguments);
                ctx.writeAndFlush(new QueueBindOk(getChannel()));

            } catch (BrokerException e) {
                LOGGER.error("Error while binding to {} with queue {} with routing key {} "
                        , exchange, queue, routingKey, e);
                ctx.writeAndFlush(new ChannelClose(getChannel(),
                                                   ChannelException.NOT_ALLOWED,
                                                   ShortString.parseString(e.getMessage()),
                                                   CLASS_ID,
                                                   METHOD_ID));
            }

        });

    }

    /**
     * Getter for queue.
     */
    public ShortString getQueue() {
        return queue;
    }

    /**
     * Getter for exchange.
     */
    public ShortString getExchange() {
        return exchange;
    }

    /**
     * Getter for routingKey.
     */
    public ShortString getRoutingKey() {
        return routingKey;
    }

    /**
     * Getter for noWait.
     */
    public boolean isNoWait() {
        return noWait;
    }

    /**
     * Getter for arguments.
     */
    public FieldTable getArguments() {
        return arguments;
    }

    public static AmqMethodBodyFactory getFactory() {
        return (buf, channel, size) -> {
            buf.skipBytes(2);
            ShortString queue = ShortString.parse(buf);
            ShortString exchange = ShortString.parse(buf);
            ShortString routingKey = ShortString.parse(buf);
            boolean noWait = buf.readBoolean();
            FieldTable arguments = FieldTable.parse(buf);

            return new QueueBind(channel, queue, exchange, routingKey, noWait, arguments);
        };
    }
}
