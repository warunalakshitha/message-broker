/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.broker.coordination.rdbms;

import org.wso2.broker.coordination.CoordinationException;

/**
 * Enum that defines allowed types of membership events.
 */
public enum MembershipEventType {

    MEMBER_ADDED(1),
    MEMBER_REMOVED(2),
    COORDINATOR_CHANGED(3);

    /**
     * Integer identifying the event type.
     */
    private int code;

    MembershipEventType(int code) {
        this.code = code;
    }

    /**
     * Retrieve the integer code of the event type.
     *
     * @return the integer code
     */
    public int getCode() {
        return code;
    }

    /**
     * Retrieve the relevant type of the membership event by passing the integer code.
     *
     * @param typeInInt the integer code
     * @return the membership type
     * @throws CoordinationException if the membership event type is invalid
     */
    public static MembershipEventType getTypeFromInt(int typeInInt) throws CoordinationException {
        switch (typeInInt) {
            case 1:
                return MEMBER_ADDED;
            case 2:
                return MEMBER_REMOVED;
            case 3:
                return COORDINATOR_CHANGED;
            default:
                throw new CoordinationException("Invalid membership event type");
        }
    }

}
