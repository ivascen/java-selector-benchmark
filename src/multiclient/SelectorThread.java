package multiclient;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

public class SelectorThread {
    private static class SelectorEventLoop extends Thread {
        private volatile boolean run = true;
        private final BlockingQueue<AbstractTask> queue;
        private final SelectStrategy ss;
        private final CountDownLatch started = new CountDownLatch(1);
        private final Selector selector;
        private final NioEventHandler ev;
        public SelectorEventLoop(BlockingQueue<AbstractTask> queue, SelectStrategy ss, Selector selector, NioEventHandler ev) {
            super();
            this.queue = queue;
            this.selector = selector;
            this.ev = ev;
            this.ss =ss;
        }
        @Override
        public void run() {
            started.countDown();
            final SelectStrategy ss = this.ss;
            final Selector selector = this.selector;
            final Queue<AbstractTask> q = queue;
            final NioEventHandler eventHandler = ev;
            while (run) {
                if (selectAndDrain(selector, ss, q, eventHandler) == 0) {
                    ss.chill();
                }
            }
        }
        private int selectAndDrain(final Selector selector, final SelectStrategy ss,
                final Queue<AbstractTask> q, final NioEventHandler eventHandler) {
            int i = 0;
            try {
                ss.select(selector);
                Set<SelectionKey> set = selector.selectedKeys();
                for (SelectionKey key : set) {
                    if (key.isValid()) {
                        eventHandler.onKeySelected(key);
                        i++;
                    }
                }
                selector.selectedKeys().clear();
                AbstractTask r;
                while ((r = q.poll()) != null) {
                    r.handle(selector);
                    i++;
                }
            }
            catch (IOException e) {
                throw new RuntimeException("Selector thread error", e);
            }
            return i;
        }
        void shutdown() {
            run = false;
            selector.wakeup();
        }
        void wakeup(){
            ss.wakeup(selector);
        }
        void joinStart() {
            try {
                started.await();
            } catch (InterruptedException e) {
                Thread.interrupted();
                throw new RuntimeException("WTF");
            }
        }
    }
    static abstract class AbstractTask {
        abstract void handle(Selector s);
    }
    interface NioEventHandler {
        void onKeySelected(SelectionKey key);
    }
    private final BlockingQueue<AbstractTask> queue;
    private final SelectorEventLoop loop;

    SelectorThread(Selector selector, NioEventHandler ev, SelectStrategy ss, int queueSize) {
        queue = new ArrayBlockingQueue<AbstractTask>(queueSize);
        loop = new SelectorEventLoop(queue, ss, selector, ev);
        loop.start();
        loop.joinStart();
    }


    void shutdown() throws InterruptedException {
        loop.shutdown();
        loop.join();
    }

    void enqueue(AbstractTask t) {
        queue.add(t);
        loop.wakeup();
    }
}
