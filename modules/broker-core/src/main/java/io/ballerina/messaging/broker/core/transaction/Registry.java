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

package io.ballerina.messaging.broker.core.transaction;

import io.ballerina.messaging.broker.common.ValidationException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.transaction.xa.Xid;

/**
 * Manages {@link Branch} objects related to transactions.
 */
public class Registry {

    private final Map<Xid, Branch> branchMap;

    public Registry() {
        branchMap = new ConcurrentHashMap<>();
    }

    public void register(Branch branch) throws ValidationException {
        if (Objects.nonNull(branchMap.putIfAbsent(branch.getXid(), branch))) {
            throw new ValidationException("Branch with already exist with Xid " + branch.getXid());
        }
    }

    public void unregister(Xid xid) {
        branchMap.remove(xid);
    }
}
