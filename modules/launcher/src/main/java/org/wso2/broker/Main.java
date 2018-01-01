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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.amqp.AmqpServerConfiguration;
import org.wso2.broker.amqp.Server;
import org.wso2.broker.core.Broker;
import org.wso2.broker.core.configuration.BrokerConfiguration;
import org.wso2.carbon.config.ConfigProviderFactory;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Starting point of the broker.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        try {
            ConfigProvider configProvider = initConfigProvider();
            BrokerConfiguration configuration = configProvider
                    .getConfigurationObject("broker", BrokerConfiguration.class);
            AmqpServerConfiguration serverConfiguration = configProvider
                    .getConfigurationObject("transport.amqp", AmqpServerConfiguration.class);
            Broker broker = new Broker(configuration);
            broker.startMessageDelivery();
            Server amqpServer = new Server(broker, serverConfiguration);
            amqpServer.run();
        } catch (Throwable e) {
            log.error("Error while starting broker", e);
            throw e;
        }
    }

    /**
     * Loads configurations during the broker start up.
     * method will try to <br/>
     *  (1) Load the configuration file specified in 'broker.file' (e.g. -Dbroker.file=<FilePath>). <br/>
     *  (2) If -Dbroker.file is not specified, the broker.yaml file exists in current directory and load it. <br/>
     *
     *  <b>Note: </b> if provided configuration file cannot be read broker will not start.
     * @return a configuration object.
     */
    private static ConfigProvider initConfigProvider() throws ConfigurationException {
        Path brokerYamlFile;
        String brokerFilePath = System.getProperty(BrokerConfiguration.SYSTEM_PARAM_BROKER_CONFIG_FILE);
        if (brokerFilePath == null || brokerFilePath.trim().isEmpty()) {
            // use current path.
            brokerYamlFile = Paths.get("", BrokerConfiguration.BROKER_FILE_NAME).toAbsolutePath();
        } else {
            brokerYamlFile = Paths.get(brokerFilePath).toAbsolutePath();
        }

        return ConfigProviderFactory.getConfigProvider(brokerYamlFile, null);
    }
}
