package multiclient;

public interface Delayer {

    long apply(long delay, int clients, int index);

}
