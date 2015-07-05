package java.selector;

import static java.selector.MultiConnectionPingServer.getSelectStrategy;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.selector.SelectorThread.NioEventHandler;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MultiConnectionPingClient implements NioEventHandler {
    private static final String host = System.getProperty("host", "localhost");
    private static final int port = Integer.getInteger("port", 12345);
    private static final int msgSize = Integer.getInteger("msg_size", 32);
    private static final int pings = Integer.getInteger("pings", 200);
    private static final int clients = Integer.getInteger("clients", 1);
    private static final int iterations = Integer.getInteger("iterations", 20);
    private static final long delayMs = Long.getLong("delay", 5L);
    private static final int threads = Integer.getInteger("threads", Runtime.getRuntime()
            .availableProcessors() / 2);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(threads);

    private static final ThreadPoolExecutor exec = new ThreadPoolExecutor(threads, threads * 2, 10,
            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(pings * clients * 2),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private final PingClient[] pingClients = new PingClient[clients];
    private final Reporter reporter = new Reporter();

    MultiConnectionPingClient(final SelectStrategy ss) throws Exception {
        final Selector selector = Selector.open();
        SelectorThread thread = new SelectorThread(selector, this, ss, clients);
        CountDownLatch latch = new CountDownLatch(clients);
        for (int i = 0; i < clients; i++) {
            PingClient p = new PingClient(SocketChannel.open(), host, port, msgSize, pings, thread,
                    scheduler, delayMs, latch);
            pingClients[i] = p;
        }
        latch.await();
        reporter.init(clients * pings);
        for (int i = 0; i < iterations; i++) {
            reporter.reset();
            for (PingClient c : pingClients) {
                reporter.observe(c.times);
            }
            reporter.report();
            restart();
        }
        scheduler.shutdown();
        exec.shutdown();
        thread.shutdown();
    }

    void restart() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(clients);
        for (PingClient c : pingClients) {
            c.reset(latch);
        }
        latch.await();
    }

    @Override
    public void onKeySelected(SelectionKey key) {
        if (key.isReadable()) {
            PingClient client = (PingClient) key.attachment();
            if (client != null) {
                key.interestOps(0);
                exec.submit(client.receive);
            }
        } else {
            System.out.println("WTF??!!");
        }
    }

    public static void main(String[] args) throws Exception {
        new MultiConnectionPingClient(getSelectStrategy(args));
    }
}
