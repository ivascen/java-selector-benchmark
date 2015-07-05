package multiclient;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.locks.LockSupport;

public enum SelectStrategy {
    NOW {
        @Override
        public int select(Selector selector) throws IOException {
            return selector.selectNow();
        }

        @Override
        public void wakeup(Selector selector) {
        }

        @Override
        public void chill() {
            LockSupport.parkNanos(1);
        }
    },
    BLOCK {
        @Override
        public int select(Selector selector) throws IOException {
            return selector.select(1);
        }

        @Override
        public void wakeup(Selector selector) {
            selector.wakeup();
        }

        @Override
        public void chill() {
        }
    };
    abstract int select(Selector selector) throws IOException;

    abstract void wakeup(Selector selector);

    abstract void chill();
}
