package multiclient;

import java.util.Arrays;

public class Reporter {

    private long[] measuraments;
    private int index = 0;
    
    public void init(int iterations) {
        measuraments  = new long[iterations];
    }
    
    public void observe(long time) {
        measuraments[index++] = time;
    }

    public void report() {
        Arrays.sort(measuraments);
        long min = 0;
        int i = 0;
        while (min == 0) {
            min = measuraments[i++];
        }
        int iterations = measuraments.length;
        System.out.printf("@%d,%d,%d,%d,%d,%d,%d\n", measuraments[i], measuraments[iterations / 2],
                measuraments[(int) (iterations / 100 * 90)], measuraments[(int) (iterations / 100 * 99)],
                measuraments[(int) (iterations / 1000 * 999)], measuraments[(int) (iterations / 10000 * 9999)],
                measuraments[iterations - 1]);
    }
    
    public void reset() {
        Arrays.fill(measuraments, 0L);
        index = 0;
    }

    public void observe(long[] times) {
        System.arraycopy(times, 0, measuraments, index, times.length);
        index+=times.length;
    }
    
}
