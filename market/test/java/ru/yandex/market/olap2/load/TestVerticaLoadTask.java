package ru.yandex.market.olap2.load;

import ru.yandex.market.olap2.load.tasks.VerticaLoadTask;

import java.io.File;

public class TestVerticaLoadTask extends VerticaLoadTask {

    public TestVerticaLoadTask(String stepEventId, String path) {
        this(stepEventId, path, null);
    }

    public TestVerticaLoadTask(String stepEventId, String path, Integer partition) {
        super(stepEventId, path, partition, null, null, null, null,
            null, null, null, null, null, 0, null,
                null);
        new File(this.getFile()).deleteOnExit();
    }

    @Override
    public void success() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rejected(Exception e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void failed(Exception e) {
        throw new UnsupportedOperationException();
    }
}
