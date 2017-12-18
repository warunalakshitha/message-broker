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

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.amqp.codec.AmqConstant;
import org.wso2.broker.amqp.codec.AmqpConnectionHandler;
import org.wso2.broker.common.data.types.FieldTable;
import org.wso2.broker.common.data.types.LongString;
import org.wso2.broker.common.data.types.ShortString;
import org.wso2.broker.core.Broker;
import org.wso2.broker.core.security.sasl.SaslServerBuilder;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

/**
 * AMQP connection.start frame.
 */
public class ConnectionStartOk extends MethodFrame {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionStartOk.class);
    private final FieldTable clientProperties;
    private final ShortString mechanisms;
    private final ShortString locales;
    private final LongString response;
    private static final short CLASS_ID = 10;
    private static final short METHOD_ID = 11;

    public ConnectionStartOk(int channel, FieldTable clientProperties, ShortString mechanisms,
            ShortString locales, LongString response) {
        super(channel, (short) 10, (short) 11);
        this.clientProperties = clientProperties;
        this.mechanisms = mechanisms;
        this.locales = locales;
        this.response = response;
    }

    @Override
    protected long getMethodBodySize() {
        return clientProperties.getSize() + mechanisms.getSize() + locales.getSize() + response.getSize();
    }

    @Override
    protected void writeMethod(ByteBuf buf) {
        clientProperties.write(buf);
        mechanisms.write(buf);
        locales.write(buf);
        response.write(buf);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, AmqpConnectionHandler connectionHandler) {
        if (mechanisms == null || authenticate(connectionHandler)) {
            ctx.writeAndFlush(new ConnectionTune(256, 65535, 0));
        } else {
            String replyText = "Authentication Failed";
            ctx.writeAndFlush(
                    new ConnectionClose(403, new ShortString(replyText.length(), replyText.getBytes(Charsets.UTF_8)),
                            CLASS_ID, METHOD_ID));
        }
    }

    public static AmqMethodBodyFactory getFactory() {
        return (buf, channel, size) -> {
            FieldTable clientProperties = FieldTable.parse(buf);
            ShortString mechanisms = ShortString.parse(buf);
            LongString response = LongString.parse(buf);
            ShortString locale = ShortString.parse(buf);
            return new ConnectionStartOk(channel, clientProperties, mechanisms, locale, response);
        };
    }

    private boolean authenticate(AmqpConnectionHandler connectionHandler) {
        Broker broker = connectionHandler.getBroker();
        SaslServer saslServer;
        SaslServerBuilder saslServerBuilder = broker.getAuthenticationManager().getSaslMechanisms()
                .get(mechanisms.toString());
        try {
            if (saslServerBuilder != null) {
                saslServer = Sasl.createSaslServer(mechanisms.toString(), AmqConstant.AMQP_PROTOCOL_IDENTIFIER,
                        connectionHandler.getHostName(), saslServerBuilder.getProperties(),
                        saslServerBuilder.getCallbackHandler());
            } else {
                throw new SaslException("Server does not support for mechanism: " + mechanisms);
            }
            if (saslServer != null) {
                saslServer.evaluateResponse(response.getBytes());
                if (saslServer.isComplete() && saslServer.getAuthorizationID() != null) {
                    return true;
                } else {
                    throw new SaslException("Authentication Failed");
                }
            } else {
                throw new SaslException("Sasl server cannot be found for mechanism: " + mechanisms);
            }
        } catch (SaslException e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception occurred while authenticating incoming connection ", e);
            }
        }
        return false;
    }
}
