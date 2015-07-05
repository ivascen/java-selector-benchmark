package multiclient;

public class ZeroDelayer implements Delayer {

    @Override
    public long apply(long delay, int clients, int index) {
        return 0;
    }

}
