package ru.yandex.market.olap2.load;

import java.io.File;
import java.time.ZonedDateTime;

import ru.yandex.market.olap2.load.tasks.ClickhouseLoadTask;

public class TestClickhouseLoadTask extends ClickhouseLoadTask {

    public TestClickhouseLoadTask(String stepEventId, String path) {
        this(stepEventId, path, null);
    }

    public TestClickhouseLoadTask(String stepEventId, String path, Integer partition) {
        super(stepEventId, path, partition, null, null, null, null,
            null, null, null, null, null, null,
                ZonedDateTime.now(), null, null);
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
