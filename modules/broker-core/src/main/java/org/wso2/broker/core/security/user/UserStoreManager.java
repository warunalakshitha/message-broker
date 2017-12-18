/*
 *   Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.broker.core.security.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.core.security.exception.BrokerAuthenticationException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages the users and authentication
 */
public class UserStoreManager {

    private static final Logger log = LoggerFactory.getLogger(UserStoreManager.class);

    /**
     * Store the list of users
     */
    private static Map<String, User> users = new ConcurrentHashMap<>();

    public Map<String, User> getUsers() {
        return users;
    }

    public static void addUser(User user) {
        if (user != null && user.getUsername() != null) {
            users.put(user.getUsername(), user);
        } else {
            log.error("User or username can not be null");
        }
    }

    /**
     * Method to authenticate user
     *
     * @param userName userName
     * @param password Password
     * @return Authentication result
     * @throws BrokerAuthenticationException
     */
    public static boolean authenticate(String userName, char[] password) throws BrokerAuthenticationException {
        User user = users.get(userName);
        if (user == null) {
            throw new BrokerAuthenticationException(
                    "User not found for userName: " + userName + ". Authentication failed.");
        } else {
            byte[] plainUserPassword = user.getPassword().getBytes(StandardCharsets.UTF_8);
            if (Arrays.equals(plainUserPassword, toBytes(password))) {
                return true;
            } else {
                throw new BrokerAuthenticationException(
                        "Password did not match with the configured user, userName: " + userName
                                + ". Authentication failed.");
            }
        }
    }

    /**
     * Convert given char array to bytes
     *
     * @param chars char array
     * @return bytes
     * @throws BrokerAuthenticationException
     */
    private static byte[] toBytes(char[] chars) throws BrokerAuthenticationException {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
        if (!byteBuffer.hasArray()) {
            throw new BrokerAuthenticationException(
                    "The password check failed due to inability to obtain byte[] from a ByteBuffer");
        }
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }
}
