package ru.yandex.market.protobuf;

import ru.yandex.market.protobuf.readers.MessageReader;

/**
 * @author s-ermakov
 */
public class ObjectMessageReader implements MessageReader<Object> {

    private int count;
    private int index;

    private boolean isClosed;

    public ObjectMessageReader(int count) {
        this.count = count;
    }

    @Override
    public Object read() {
        if (isClosed) {
            throw new IllegalStateException("Trying to get object from closed message reader");
        }

        if (index < count) {
            index++;
            return new Object();
        }
        return null;
    }

    @Override
    public void close() {
        this.isClosed = true;
    }

    public boolean isClosed() {
        return this.isClosed;
    }
}
