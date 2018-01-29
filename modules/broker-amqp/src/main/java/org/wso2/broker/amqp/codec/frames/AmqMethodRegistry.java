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

import org.wso2.broker.amqp.codec.AmqConstant;
import org.wso2.broker.amqp.codec.AmqFrameDecodingException;
import org.wso2.broker.auth.AuthManager;

/**
 * Keep factory classes for different class IDs and method IDs.
 */
public class AmqMethodRegistry {
    public AmqMethodBodyFactory[][] factories = new AmqMethodBodyFactory[101][];

    public AmqMethodRegistry(AuthManager authManager) {
        factories[10] = new AmqMethodBodyFactory[52];
        factories[10][11] = ConnectionStartOk.getFactory(authManager);
        factories[10][20] = ConnectionSecure.getFactory();
        factories[10][21] = ConnectionSecureOk.getFactory(authManager);
        factories[10][31] = ConnectionTuneOk.getFactory();
        factories[10][40] = ConnectionOpen.getFactory();
        factories[10][41] = ConnectionOpenOk.getFactory();
        factories[10][50] = ConnectionClose.getFactory();
        factories[10][51] = ConnectionCloseOk.getFactory();

        factories[20] = new AmqMethodBodyFactory[42];
        factories[20][10] = ChannelOpen.getFactory();
        factories[20][11] = ChannelOpenOk.getFactory();
        factories[20][20] = ChannelFlow.getFactory();
        factories[20][21] = ChannelFlowOk.getFactory();
        factories[20][40] = ChannelClose.getFactory();
        factories[20][41] = ChannelCloseOk.getFactory();

        factories[40] = new AmqMethodBodyFactory[24];
        factories[40][10] = ExchangeDeclare.getFactory();
        factories[40][11] = ExchangeDeclareOk.getFactory();

        factories[50] = new AmqMethodBodyFactory[52];
        factories[50][10] = QueueDeclare.getFactory();
        factories[50][11] = QueueDeclareOk.getFactory();
        factories[50][20] = QueueBind.getFactory();
        factories[50][21] = QueueBindOk.getFactory();

        factories[60] = new AmqMethodBodyFactory[112];
        factories[60][10] = BasicQos.getFactory();
        factories[60][11] = BasicQosOk.getFactory();
        factories[60][20] = BasicConsume.getFactory();
        factories[60][21] = BasicConsumeOk.getFactory();
        factories[60][30] = BasicCancel.getFactory();
        factories[60][31] = BasicCancelOk.getFactory();
        factories[60][40] = BasicPublish.getFactory();
        factories[60][60] = BasicDeliver.getFactory();
        factories[60][80] = BasicAck.getFactory();
        factories[60][90] = BasicReject.getFactory();
        factories[60][110] = BasicRecover.getFactory();
        factories[60][111] = BasicRecoveryOk.getFactory();
    }

    public AmqMethodBodyFactory getFactory(short classId, short methodId) throws AmqFrameDecodingException {
        try {
            AmqMethodBodyFactory factory = factories[classId][methodId];
            if (factory == null) {
                throw new AmqFrameDecodingException(AmqConstant.COMMAND_INVALID,
                                              "Method " + methodId + " unknown in AMQP version 0-91"
                                                      + " (while trying to decode class " + classId + " method "
                                                      + methodId + ".");
            }

            return factory;
        } catch (NullPointerException e) {
            throw new AmqFrameDecodingException(AmqConstant.COMMAND_INVALID,
                                                "Class " + classId + " unknown in AMQP version 0-91"
                                                        + " (while trying to decode class " + classId + " method "
                                                        + methodId + ".");
        } catch (IndexOutOfBoundsException e) {
            if (classId >= factories.length) {
                throw new AmqFrameDecodingException(AmqConstant.COMMAND_INVALID,
                                                    "Class " + classId + " unknown in AMQP version 0-91"
                                                            + " (while trying to decode class " + classId + " method "
                                                            + methodId + ".");

            } else {
                throw new AmqFrameDecodingException(AmqConstant.COMMAND_INVALID,
                                                    "Method " + methodId + " unknown in AMQP version 0-91"
                                                            + " (while trying to decode class " + classId + " method "
                                                            + methodId + ".");

            }
        }
    }
}
