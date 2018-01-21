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
package org.wso2.broker.rest.auth;

import com.google.common.net.HttpHeaders;
import com.sun.security.auth.UserPrincipal;
import org.wso2.carbon.kernel.context.PrivilegedCarbonContext;
import org.wso2.msf4j.Request;
import org.wso2.msf4j.Response;
import org.wso2.msf4j.interceptor.RequestInterceptor;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Class implements @{@link RequestInterceptor} to authenticate requests based on authorization header
 */
public class BasicAuthInterceptor implements RequestInterceptor {

    private AuthenticateFunction authenticateFunction;

    private static final String BASIC_HEADER = "Basic";

    public BasicAuthInterceptor(AuthenticateFunction authenticateFunction) {
        this.authenticateFunction = authenticateFunction;
    }

    @Override
    public boolean interceptRequest(Request request, Response response) throws Exception {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null) {
            String header = authorizationHeader.substring(BASIC_HEADER.length()).trim();
            byte[] bytes = Base64.getDecoder().decode(header);
            char[] array = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)).array();

            int separatorIndex = Arrays.binarySearch(array, ':');
            String userName = new String(Arrays.copyOfRange(array, 0, separatorIndex));
            char[] credential = Arrays.copyOfRange(array, separatorIndex + 1, array.length);
            try {
                if (authenticateFunction.authenticate(userName, credential)) {
                    UserPrincipal userPrincipal = new UserPrincipal(userName);
                    PrivilegedCarbonContext privilegedCarbonContext = PrivilegedCarbonContext.getCurrentContext();
                    privilegedCarbonContext.setUserPrincipal(userPrincipal);
                    return true;
                }
            } finally {
                for (int i = 0; i < credential.length; i++) {
                    credential[i] = ' ';
                }
            }
        }
        return false;

    }

    /**
     * function to authenticate based on user credentials
     */
    @FunctionalInterface
    public interface AuthenticateFunction {
        boolean authenticate(String username, char... credentials) throws Exception;
    }
}



