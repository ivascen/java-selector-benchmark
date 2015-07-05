package java.selector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PingClient extends ChannelStateMachine {
    private final ByteBuffer bbRead;
    private final ByteBuffer bbWrite;
    private final SelectorThread selectorThread;
    final long[] times;
    private int msgId = 0;

    final Runnable receive = new Runnable() {
        @Override
        public void run() {
            receive();
        }
    };
    final Runnable send = new Runnable() {
        @Override
        public void run() {
            send();
        }
    };
    private final ScheduledExecutorService scheduler;
    private final long delayMs;
    private CountDownLatch terminationLatch;

    PingClient(SocketChannel channel, String host, int port, int msgSize, int pings,
            SelectorThread selectorThread, ScheduledExecutorService scheduler, long delayMs,
            CountDownLatch latch) {
        super(channel);
        this.bbRead = ByteBuffer.allocateDirect(4096);
        this.bbWrite = ByteBuffer.allocateDirect(msgSize + 8);
        bbWrite.putInt(4, msgSize);
        this.times = new long[pings];
        this.selectorThread = selectorThread;
        this.scheduler = scheduler;
        this.delayMs = delayMs;
        this.terminationLatch = latch;
        try {
            channel.connect(new InetSocketAddress(host, port));
            channel.socket().setTcpNoDelay(true);
            channel.configureBlocking(false);
            selectorThread.enqueue(this);
        } catch (IOException e) {
            System.out.println("Unable to connect a client");
        }
    }

    @Override
    protected void register(Selector s) throws ClosedChannelException {
        super.register(s);
        // schedule for sending
        scheduler.schedule(send, delayMs, TimeUnit.MILLISECONDS);
    }

    public void reset(CountDownLatch latch) {
        msgId = 0;
        this.terminationLatch = latch;
        scheduler.schedule(send, delayMs, TimeUnit.MILLISECONDS);
    }

    void receive() {
        try {
            channel.read(bbRead);
        } catch (IOException e) {
            state = State.CLOSE;
            selectorThread.enqueue(this);
            System.err.println("Error on reading:" + e.getMessage());
            return;
        }
        bbRead.flip();
        int position = 0;
        int limit = bbRead.limit();
        int lastIdProcessed = 0;
        do {
            if (limit - position < 8)
                break;
            int id = bbRead.getInt(position);
            int size = bbRead.getInt(position + 4);
            if (limit - position < 8 + size)
                break;
            times[id] = System.nanoTime() - times[id];
            lastIdProcessed = id;
            position += 8 + size;
        } while (position < limit);
        // any leftovers?
        if (position > 0 && position != limit) {
            bbRead.position(position);
            bbRead.compact();
        }
        state = State.RESUME;
        selectorThread.enqueue(this);
        if (lastIdProcessed == times.length - 1) {
            terminationLatch.countDown();
        }
    }

    private void send() {
        try {
            times[msgId] = System.nanoTime();
            bbWrite.clear();
            bbWrite.putInt(0, msgId);
            do {
                channel.write(bbWrite);
            } while (bbWrite.hasRemaining());
            msgId++;
        } catch (Exception e) {
            System.err.println("Error on sending:" + e.getMessage());
            return;
        }
        // schedule for sending
        if (msgId < times.length) {
            scheduler.schedule(send, delayMs, TimeUnit.MILLISECONDS);
        }
    }

}
