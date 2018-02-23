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
package org.wso2.broker.amqp.codec.auth;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.wso2.broker.amqp.codec.frames.ConnectionSecure;
import org.wso2.broker.amqp.codec.frames.ConnectionTune;
import org.wso2.broker.amqp.codec.handlers.AmqpConnectionHandler;
import org.wso2.broker.auth.AuthManager;
import org.wso2.broker.auth.BrokerAuthConstants;
import org.wso2.broker.common.data.types.LongString;
import org.wso2.broker.common.data.types.ShortString;
import org.wso2.broker.core.BrokerException;

import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

/**
 * Sasl based Authentication strategy which implements @{@link AuthenticationStrategy} to support sasl mechanisms based
 * authentication to broker client connections
 */
public class SaslAuthenticationStrategy implements AuthenticationStrategy {

    private AuthManager authManager;

    public static final String SASL_SERVER_ATTRIBUTE = "broker.sasl.server";

    SaslAuthenticationStrategy(AuthManager authManager) {
        this.authManager = authManager;
    }

    @Override
    public void handle(int channel, ChannelHandlerContext ctx, AmqpConnectionHandler connectionHandler,
                       ShortString mechanism, LongString response) throws BrokerException {
        try {
            SaslServer saslServer = authManager
                    .createSaslServer(connectionHandler.getConfiguration().getHostName(), mechanism.toString());
            byte[] challenge = saslServer.evaluateResponse(response.getBytes());
            if (saslServer.isComplete()) {
                ctx.channel().attr(AttributeKey.valueOf(BrokerAuthConstants.AUTHENTICATION_ID))
                   .set(saslServer.getAuthorizationID());
                ctx.writeAndFlush(new ConnectionTune(256, 65535, 0));
            } else {
                ctx.channel().attr(AttributeKey.valueOf(SASL_SERVER_ATTRIBUTE)).set(saslServer);
                ctx.writeAndFlush(new ConnectionSecure(channel, LongString.parse(challenge)));
            }
        } catch (SaslException e) {
            throw new BrokerException("Exception occurred while handling authentication with Sasl", e);
        }
    }
}
