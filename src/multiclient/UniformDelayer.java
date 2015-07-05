package multiclient;


public class UniformDelayer implements Delayer {

    @Override
    public long apply(long delay, int clients, int index) {
        return delay/(long)clients * (long)index;
    }

}
