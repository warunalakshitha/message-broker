package org.wso2.broker.amqp.codec.frames;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.wso2.broker.amqp.codec.AmqpChannel;
import org.wso2.broker.amqp.codec.handlers.AmqpConnectionHandler;

/**
 * AMQP frame for tx.rollback
 */
public class TxRollback extends MethodFrame {

    private static final short CLASS_ID = 90;
    private static final short METHOD_ID = 30;

    public TxRollback(int channel) {
        super(channel, CLASS_ID, METHOD_ID);
    }

    @Override
    protected long getMethodBodySize() {
        return 0L;
    }

    @Override
    protected void writeMethod(ByteBuf buf) {
    }

    @Override
    public void handle(ChannelHandlerContext ctx, AmqpConnectionHandler connectionHandler) {
        int channelId = getChannel();
        AmqpChannel channel = connectionHandler.getChannel(channelId);
        channel.rollback();
        ctx.writeAndFlush(new TxRollbackOk(channelId));
    }

    public static AmqMethodBodyFactory getFactory() {
        return (buf, channel, size) -> new TxRollback(channel);
    }
}
