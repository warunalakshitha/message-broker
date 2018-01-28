/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.messaging.integration.util;

import org.wso2.broker.amqp.AmqpServerConfiguration;
import org.wso2.broker.amqp.Server;
import org.wso2.broker.auth.AuthManager;
import org.wso2.broker.auth.BrokerAuthConfiguration;
import org.wso2.broker.auth.BrokerAuthConstants;
import org.wso2.broker.auth.user.UserStoreManager;
import org.wso2.broker.auth.user.impl.UserStoreManagerImpl;
import org.wso2.broker.common.BrokerConfigProvider;
import org.wso2.broker.common.StartupContext;
import org.wso2.broker.coordination.BrokerHaConfiguration;
import org.wso2.broker.coordination.HaStrategy;
import org.wso2.broker.coordination.HaStrategyFactory;
import org.wso2.broker.core.Broker;
import org.wso2.broker.core.BrokerException;
import org.wso2.broker.core.configuration.BrokerConfiguration;
import org.wso2.broker.rest.BrokerRestServer;
import org.wso2.broker.rest.config.RestServerConfiguration;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.sql.DataSource;

/**
 * Representation of a single MB node.
 */
public class Node {

    private Broker broker;

    private Server server;

    private BrokerRestServer brokerRestServer;

    private HaStrategy haStrategy;

    public Node(String hostname, String port, String sslPort, String restPort, String adminUsername,
                String adminPassword, StartupContext startupContext, TestConfigProvider configProvider)
            throws Exception {

        BrokerConfiguration brokerConfiguration = new BrokerConfiguration();
        configProvider.registerConfigurationObject(BrokerConfiguration.NAMESPACE, brokerConfiguration);

        AmqpServerConfiguration serverConfiguration = new AmqpServerConfiguration();
        serverConfiguration.setHostName(hostname);
        serverConfiguration.getPlain().setPort(port);
        serverConfiguration.getSsl().setEnabled(true);
        serverConfiguration.getSsl().setPort(sslPort);
        serverConfiguration.getSsl().getKeyStore().setLocation(TestConstants.KEYSTORE_LOCATION);
        serverConfiguration.getSsl().getKeyStore().setPassword(TestConstants.KEYSTORE_PASSWORD);
        serverConfiguration.getSsl().getTrustStore().setLocation(TestConstants.TRUST_STORE_LOCATION);
        serverConfiguration.getSsl().getTrustStore().setPassword(TestConstants.TRUST_STORE_PASSWORD);
        configProvider.registerConfigurationObject(AmqpServerConfiguration.NAMESPACE, serverConfiguration);

        RestServerConfiguration restConfig = new RestServerConfiguration();
        restConfig.getPlain().setPort(restPort);
        configProvider.registerConfigurationObject(RestServerConfiguration.NAMESPACE, restConfig);

        BrokerHaConfiguration haConfiguration = configProvider.getConfigurationObject(
                BrokerHaConfiguration.NAMESPACE, BrokerHaConfiguration.class);

        startupContext.registerService(BrokerHaConfiguration.class, haConfiguration);
        startupContext.registerService(DataSource.class, DbUtils.getDataSource());

        startupContext.registerService(BrokerConfigProvider.class, configProvider);

        if (haConfiguration.isEnabled()) {
            //Initializing an HaStrategy implementation only if HA is enabled
            try {
                haStrategy = HaStrategyFactory.getHaStrategy(startupContext);
            } catch (Exception e) {
                throw new BrokerException("Error initializing HA Strategy: ", e);
            }
            startupContext.registerService(HaStrategy.class, haStrategy);
        }

        // auth configurations
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(BrokerAuthConstants.USERS_FILE_NAME);
        if (resource != null) {
            System.setProperty(BrokerAuthConstants.SYSTEM_PARAM_USERS_CONFIG, resource.getFile());
        }
        UserStoreManager userStoreManager = new UserStoreManagerImpl();
        BrokerAuthConfiguration securityConfiguration = new BrokerAuthConfiguration();
        BrokerAuthConfiguration.AuthorizationConfiguration authorizationConfiguration = new BrokerAuthConfiguration
                .AuthorizationConfiguration();
        authorizationConfiguration.setEnabled(false);
        securityConfiguration.setAuthorization(authorizationConfiguration);
        startupContext.registerService(AuthManager.class, new AuthManager(securityConfiguration, DbUtils
                .getDataSource(), userStoreManager));

        brokerRestServer = new BrokerRestServer(startupContext);
        broker = new Broker(startupContext);
        server = new Server(startupContext);
    }

    public void startUp() throws CertificateException, InterruptedException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        if (haStrategy != null) {
            haStrategy.start();
        }
        broker.startMessageDelivery();
        server.start();
        brokerRestServer.start();
    }

    public void shutdown() throws InterruptedException {
        if (haStrategy != null) {
            haStrategy.stop();
        }
        brokerRestServer.stop();
        server.stop();
        server.awaitServerClose();
        broker.stopMessageDelivery();
    }

    public boolean isActiveNode() {
        return haStrategy.isActiveNode();
    }

}
