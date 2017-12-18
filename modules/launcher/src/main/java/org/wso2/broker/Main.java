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
import org.wso2.broker.amqp.Server;
import org.wso2.broker.core.Broker;
import org.wso2.broker.core.configuration.BrokerConfiguration;
import org.wso2.broker.core.security.user.User;
import org.wso2.broker.core.security.user.UserStoreManager;
import org.wso2.broker.core.security.user.UsersFile;
import org.wso2.broker.core.security.util.BrokerSecurityConstants;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Starting point of the broker.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws InterruptedException {

        BrokerConfiguration configuration = loadConfiguration();
        loadAuthConfigurations();
        Broker broker = new Broker(configuration);
        broker.startMessageDelivery();
        Server amqpServer = new Server(broker, configuration);
        amqpServer.run();
    }

    /**
     * Loads configurations during the broker start up.
     * method will try to <br/>
     *  (1) Load the configuration file specified in 'broker.file' (e.g. -Dbroker.file=<FilePath>). <br/>
     *  (2) If -Dbroker.file is not specified, the broker.yaml file exists in current directory and load it. <br/>
     *  (3) If configuration files are not provided, starts with in-built broker.yaml file. <br/>
     *  
     *  <b>Note: </b> if provided configuration file cannot be read broker will not start.
     * @return a configuration object.
     */
    private static BrokerConfiguration loadConfiguration() {
        File brokerYamlFile;
        String brokerFilePath = System.getProperty(BrokerConfiguration.SYSTEM_PARAM_BROKER_CONFIG_FILE);
        if (brokerFilePath == null || brokerFilePath.trim().isEmpty()) {
            // use current path.
            brokerYamlFile = Paths.get("", BrokerConfiguration.BROKER_FILE_NAME).toAbsolutePath().toFile();

        } else {
            brokerYamlFile = Paths.get(brokerFilePath).toAbsolutePath().toFile();
        }
        return readConfig(BrokerConfiguration.BROKER_FILE_NAME, brokerYamlFile, BrokerConfiguration.class);
    }

    private static void loadAuthConfigurations() {
        loadJaaSConfiguration();
        loadUsers();
    }

    /**
     * Configure JaaS config to load login modules
     */
    private static void loadJaaSConfiguration() {
        InputStream resourceStream = null;
        String jaasConfigPath = System.getProperty(BrokerSecurityConstants.SYSTEM_PARAM_JAAS_CONFIG);
        if (jaasConfigPath == null || jaasConfigPath.trim().isEmpty()) {
            try {
                log.info("Using in-built configuration file -" + BrokerSecurityConstants.JAAS_FILE_NAME);
                resourceStream = Main.class.getResourceAsStream("/" + BrokerSecurityConstants.JAAS_FILE_NAME);
                Path path = Paths.get("", BrokerSecurityConstants.JAAS_FILE_NAME).toAbsolutePath();
                Files.copy(resourceStream, path, StandardCopyOption.REPLACE_EXISTING);
                System.setProperty(BrokerSecurityConstants.SYSTEM_PARAM_JAAS_CONFIG, path.toString());
            } catch (IOException e) {
                log.error("Unable to load the file - " + BrokerSecurityConstants.JAAS_FILE_NAME, e);
            } finally {
                try {
                    if (resourceStream != null) {
                        resourceStream.close();
                    }
                } catch (IOException e) {
                    log.error("Error while closing file - " + BrokerSecurityConstants.JAAS_FILE_NAME, e);
                }
            }
        }
    }

    /**
     * Loads the users from users.yaml during broker startup
     */
    private static void loadUsers() {
        File usersYamlFile;
        String usersFilePath = System.getProperty(BrokerSecurityConstants.SYSTEM_PARAM_USERS_CONFIG);
        if (usersFilePath == null || usersFilePath.trim().isEmpty()) {
            // use current path.
            usersYamlFile = Paths.get("", BrokerSecurityConstants.USERS_FILE_NAME).toAbsolutePath().toFile();
        } else {
            usersYamlFile = Paths.get(usersFilePath).toAbsolutePath().toFile();
        }
        UsersFile usersFile = readConfig(BrokerSecurityConstants.USERS_FILE_NAME, usersYamlFile, UsersFile.class);
        if (usersFile != null) {
            List<User> users = usersFile.getUsers();
            for (User user : users) {
                UserStoreManager.addUser(user);
            }
        }
    }

    /**
     * This method is used to read yaml configuration and load the content to given class object
     *
     * @param fileName  Yaml file name
     * @param yamlFile  Yaml file
     * @param classType Class which will map the yaml file contents
     * @param <T>       Type of the Class
     * @return Object mapping based on given class type
     */
    private static <T> T readConfig(String fileName, File yamlFile, Class<T> classType) {
        InputStream yamlStream = null;
        T configuration = null;
        try {
            if (yamlFile != null && yamlFile.canRead()) {
                yamlStream = new FileInputStream(yamlFile);
            } else {
                log.info("Using in-built configuration file -" + fileName);
                yamlStream = Main.class.getResourceAsStream("/" + fileName);

                if (yamlStream == null) {
                    throw new FileNotFoundException("Unable to find - " + fileName + " in class path");
                }
            }
            Yaml yaml = new Yaml();
            configuration = yaml.loadAs(yamlStream, classType);
        } catch (FileNotFoundException e) {
            String msg = "Unable to find - " + fileName + " broker will terminate now";
            log.warn(msg, e);
            throw new RuntimeException(msg, e);
        } finally {
            try {
                if (yamlStream != null) {
                    yamlStream.close();
                }
            } catch (IOException e) {
                log.error("Error while closing file - " + fileName, e);
            }
        }
        return configuration;
    }
}
