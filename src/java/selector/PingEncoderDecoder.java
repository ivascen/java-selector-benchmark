package java.selector;

import java.nio.ByteBuffer;

public class PingEncoderDecoder {
    
    //Meant to avoid garbage at every read() call.
    //Don't know if worth it, likely to be not.
    private static ThreadLocal<int[]> array = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() {
            int[] init = new int[4];
            for (int i = 0; i < init.length; i++) {
                init[i] = -1;
            }
            return init;
        } 
    };
    
    static int[] decode(ByteBuffer buffer) {
        int[] ids = array.get();
        int neededLength = buffer.remaining()/4;
        if (ids.length < neededLength) {
            ids = new int[ids.length*2];
            array.set(ids);
        }
        int index = 0;
        while (buffer.hasRemaining()) {
            ids[index++] = buffer.getInt();
        }
        //no need to flip as on next read buffer will be cleared.
        return ids;
    }
    
    static void encode(int msgId, ByteBuffer buffer) {
        buffer.putInt(msgId);
        buffer.flip();
    }

    static void release(int[] ids) {
        if (ids != null) {
            for (int i = 0; i < ids.length; i++) {
                ids[i] = -1;
            }
        }
    }
}
