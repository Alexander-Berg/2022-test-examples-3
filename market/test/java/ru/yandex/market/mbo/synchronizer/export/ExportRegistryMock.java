package ru.yandex.market.mbo.synchronizer.export;

import java.time.Instant;
import java.time.ZoneId;

public class ExportRegistryMock extends ExportRegistry {

    @Override
    public void processStart() {
        started = System.currentTimeMillis();
        folderName = dateFormat.withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(started));
    }
}
