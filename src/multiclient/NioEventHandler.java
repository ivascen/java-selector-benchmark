package multiclient;

import java.nio.channels.SelectionKey;

public interface NioEventHandler {

    void onKeySelected(SelectionKey key);
    
}
