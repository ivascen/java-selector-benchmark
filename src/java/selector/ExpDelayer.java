package java.selector;


public class ExpDelayer implements Delayer {

    @Override
    public long
        apply(long delay, int clients, int index) {
        return 0;
    }

}
