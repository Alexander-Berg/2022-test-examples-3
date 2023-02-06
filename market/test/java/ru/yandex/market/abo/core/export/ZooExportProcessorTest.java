package ru.yandex.market.abo.core.export;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

/**
 * @author imelnikov
 */
public class ZooExportProcessorTest extends EmptyTest {

    @Autowired
    ZooExportProcessor zooExportProcessor;

    @Test
    public void exportAssessors() {
        zooExportProcessor.export();
    }
}
