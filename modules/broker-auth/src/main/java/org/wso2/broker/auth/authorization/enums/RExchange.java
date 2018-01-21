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
package org.wso2.broker.auth.authorization.enums;

import java.util.function.IntSupplier;

/**
 * Enum class which implements {@link IntSupplier} used to represent Exchange resource actions
 */
public enum RExchange implements IntSupplier {

    CREATE(1), DELETE(2), GRANT(3);
    private int permission;

    RExchange(int permission) {
        this.permission = permission;
    }

    @Override
    public int getAsInt() {
        return permission;
    }
}
