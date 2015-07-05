package multiclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import multiclient.SelectorThread.NioEventHandler;

public class MultiConnectionPingServer implements NioEventHandler{

    private static class EchoAcceptedChannel extends ChannelStateMachine implements Runnable {
        private final ByteBuffer buffy = ByteBuffer.allocateDirect(PAGE_SIZE).order(ByteOrder.nativeOrder());
        private final SelectorThread t;
        EchoAcceptedChannel(SocketChannel channel, SelectorThread t) {
            super(channel);
            this.t = t;
        }
        @Override
        public void run() {
            try {
                if (pong()) {
                    state = State.CLOSE;
                    t.enqueue(this);
                } else {
                    state = State.RESUME;
                    t.enqueue(this);
                }
            } catch (IOException e) {
                try {
                    channel.close();
                } catch (IOException e1) {
                }
                System.out.println(e.getMessage());
            }
        }
        private boolean pong() throws IOException {
            int read;
            buffy.clear();
            while ((read = channel.read(buffy)) == 0) {
                Thread.yield();
            }
            if (read == -1)
                return true;
            buffy.flip();
            do {
                channel.write(buffy);
            } while (buffy.hasRemaining());
            return false;
        }
        
    }
    private static final String nic = System.getProperty("nic", "0.0.0.0");
    private static final int port = Integer.getInteger("port", 12345);
    private static final int threads = Integer.getInteger("threads", Runtime.getRuntime()
            .availableProcessors() / 2);

    private static final int PAGE_SIZE = 4096;
    private final ExecutorService executor;
    private final ServerSocketChannel serverSocket;
    private final Selector selector;
    private final SelectorThread selectorThreadt;

    MultiConnectionPingServer(SelectStrategy ss) throws IOException, InterruptedException {
        System.out.println("Listening on interface : " + nic + ":" + port);
        selector = Selector.open();
        serverSocket = ServerSocketChannel.open();
        serverSocket.socket().bind(new InetSocketAddress(nic, port));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        executor = Executors.newFixedThreadPool(threads);
        selectorThreadt = new SelectorThread(selector, this, ss, 64000);
    }
    
    @Override
    public void onKeySelected(SelectionKey key) {
        try {
            if (key.isReadable()) {
                EchoAcceptedChannel echo = (EchoAcceptedChannel) key.attachment();
                key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
                executor.execute(echo);
            } else if (key.isAcceptable()) {
                SocketChannel accepted = serverSocket.accept();
                accepted.configureBlocking(false);
                accepted.socket().setTcpNoDelay(true);
                final EchoAcceptedChannel echo = new EchoAcceptedChannel(accepted, selectorThreadt);
                echo.selectionKey = accepted.register(selector, SelectionKey.OP_READ, echo);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to handle selected key", e);
        }
    }
    
    public static void main(String[] args) throws IOException, InterruptedException {
        new MultiConnectionPingServer(getSelectStrategy(args));
    }

    static SelectStrategy getSelectStrategy(String[] args) {
        SelectStrategy ss;
        String select;
        if (args.length < 1) {
            select = "select";
        } else {
            select = args[0];
        }
        if (select.equalsIgnoreCase("selectNow")) {
            ss = SelectStrategy.NOW;
        } else if (select.equalsIgnoreCase("select")) {
            ss = SelectStrategy.BLOCK;
        } else {
            throw new IllegalArgumentException("The first argument is invalid");
        }
        System.out.println("Server type: " + select);
        return ss;
    }
}
