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
package org.wso2.broker.common.util.function;

/**
 * Represents a supplier of results.
 * <p>
 * <p>There is no requirement that a new or distinct result be returned each
 * time the supplier is invoked.
 * <p>
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #get()}.
 *
 * @param <T> the type of results supplied by this supplier
 * @param <E> the type of the exception thrown
 * @since 1.8
 */
@FunctionalInterface
public interface ThrowingSupplier<T, E extends Exception> {
    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws E;
}
