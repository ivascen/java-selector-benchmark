package java.selector;

import java.nio.ByteBuffer;

public class KeyAttachment {

    private final ByteBuffer buffer;
    private final SelectorThread selectorThread;
    

    public KeyAttachment(ByteBuffer buffer, SelectorThread selectorThread) {
        super();
        this.buffer = buffer;
        this.selectorThread = selectorThread;
    }

    public SelectorThread getSelectorThread() {
        return selectorThread;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }
}
