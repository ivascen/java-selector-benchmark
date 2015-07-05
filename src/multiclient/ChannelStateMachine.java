package multiclient;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import multiclient.SelectorThread.AbstractTask;

public abstract class ChannelStateMachine extends AbstractTask {

    enum State {
        REGISTER, RESUME, CLOSE
    }

    protected final SocketChannel channel;
    protected State state = State.REGISTER;
    protected SelectionKey selectionKey;

    ChannelStateMachine(SocketChannel channel) {
        this.channel = channel;
    }

    @Override
    void handle(Selector s) {
        try {
            switch (state) {
            case REGISTER:
                register(s);
                break;
            case RESUME:
                resume(s);
                break;
            case CLOSE:
                close(s);
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void close(Selector s) throws IOException {
        channel.close();
    }

    protected void resume(Selector s) {
        if (selectionKey.isValid()) {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);
        }
    }

    protected void register(Selector s) throws ClosedChannelException {
        selectionKey = channel.register(s, SelectionKey.OP_READ, this);
    }
}
