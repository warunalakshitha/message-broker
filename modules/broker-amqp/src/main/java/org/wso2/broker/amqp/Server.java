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

package org.wso2.broker.amqp;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.broker.amqp.codec.AmqpConnectionHandler;
import org.wso2.broker.amqp.codec.AmqpDecoder;
import org.wso2.broker.amqp.codec.AmqpEncoder;
import org.wso2.broker.amqp.codec.AmqpMessageWriter;
import org.wso2.broker.amqp.codec.BlockingTaskHandler;
import org.wso2.broker.core.Broker;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

/**
 * AMQP Server implementation.
 */
public class Server {

    private static final Logger LOGGER = LoggerFactory.getLogger(Server.class);

    /**
     * Number of threads used for blocking tasks like I/O operations. Putting a higher number of threads here is OK
     * since these are I/O bound threads.
     * TODO This should be read from
     */
    private static final int BLOCKING_TASK_EXECUTOR_THREADS = 32;

    private final Broker broker;
    private final AmqpServerConfiguration configuration;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private EventExecutorGroup ioExecutors;
    private Channel plainServerChannel;
    private Channel sslServerChannel;

    public Server(Broker broker, AmqpServerConfiguration configuration) {
        this.configuration = configuration;
        this.broker = broker;
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ioExecutors = new DefaultEventExecutorGroup(BLOCKING_TASK_EXECUTOR_THREADS);
    }

    /**
     * Start the AMQP server.
     *
     * @throws InterruptedException throws Exception when binding to port
     */
    public void run()
            throws InterruptedException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException, IOException {
        try {
            ChannelFuture plainSocketFuture = bindToPlainSocket();

            ChannelFuture sslSocketFuture = null;
            if (configuration.getSsl().isEnabled()) {
                sslSocketFuture = bindToSslSocket();
            }

            // Wait until the server socket is closed.
            plainSocketFuture.channel().closeFuture().sync();

            if (sslSocketFuture != null) {
                sslSocketFuture.channel().closeFuture().sync();
            }
        } finally {
            shutdownExecutors();
        }
    }

    private void shutdownExecutors() {
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
        ioExecutors.shutdownGracefully();
    }

    private ChannelFuture bindToPlainSocket() throws InterruptedException {
        String hostname = configuration.getPlain().getHostName();
        int port = Integer.parseInt(configuration.getPlain().getPort());

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new SocketChannelInitializer(ioExecutors))
                .option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        ChannelFuture future = b.bind(hostname, port).sync();
        LOGGER.info("Listening AMQP on " + hostname + ":" + port);
        return future;
    }

    private ChannelFuture bindToSslSocket()
            throws InterruptedException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException, IOException {
        String hostname = configuration.getSsl().getHostName();
        int port = Integer.parseInt(configuration.getSsl().getPort());

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
         .channel(NioServerSocketChannel.class)
         .childHandler(new SslSocketChannelInitializer(ioExecutors, new SslHandlerFactory(configuration)))
         .option(ChannelOption.SO_BACKLOG, 128)
         .childOption(ChannelOption.SO_KEEPALIVE, true);

        // Bind and start to accept incoming connections.
        ChannelFuture future = b.bind(hostname, port).sync();
        LOGGER.info("Listening AMQP/" + configuration.getSsl().getProtocol() + " on " + hostname + ":" + port);
        return future;
    }

    public void start()
            throws InterruptedException, CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException, IOException {
        ChannelFuture channelFuture = bindToPlainSocket();
        plainServerChannel = channelFuture.channel();

        if (configuration.getSsl().isEnabled()) {
            ChannelFuture sslSocketFuture = bindToSslSocket();
            sslServerChannel = sslSocketFuture.channel();
        }
    }

    public void stop() throws InterruptedException {
        try {
            plainServerChannel.close().sync();

            if (sslServerChannel != null) {
                sslServerChannel.close().sync();
            }
        } finally {
            shutdownExecutors();
        }
    }

    private class SocketChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final EventExecutorGroup ioExecutors;

        public SocketChannelInitializer(EventExecutorGroup ioExecutors) {
            this.ioExecutors = ioExecutors;
        }

        protected void initChannel(SocketChannel socketChannel) {
            socketChannel.pipeline()
                         .addLast(new AmqpDecoder())
                         .addLast(new AmqpEncoder())
                         .addLast(new AmqpConnectionHandler(broker, configuration))
                         .addLast(new AmqpMessageWriter())
                         .addLast(ioExecutors, new BlockingTaskHandler());
        }
    }

    private class SslSocketChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final EventExecutorGroup ioExecutors;
        private final SslHandlerFactory sslHandlerFactory;

        public SslSocketChannelInitializer(EventExecutorGroup ioExecutors, SslHandlerFactory sslHandlerFactory) {
            this.ioExecutors = ioExecutors;
            this.sslHandlerFactory = sslHandlerFactory;
        }

        protected void initChannel(SocketChannel socketChannel) {
            socketChannel.pipeline()
                         .addLast(sslHandlerFactory.create())
                         .addLast(new AmqpDecoder())
                         .addLast(new AmqpEncoder())
                         .addLast(new AmqpConnectionHandler(broker, configuration))
                         .addLast(new AmqpMessageWriter())
                         .addLast(ioExecutors, new BlockingTaskHandler());
        }
    }
}
