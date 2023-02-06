package ru.yandex.market.supercontroller.mbologs.dao;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SqlldrControllerTest {

    private static final String SESSION_TO_FAIL = "sessionToFail";

    private static class SqlldrControllerMock extends SqlldrController {

        @Override
        protected void loadFromFileReal(String destination, Path dumpFile) {
            if (destination.contains(SESSION_TO_FAIL)) {
                throw new RuntimeException("Throwing exception on \"sessionToFail\"");
            }
        }
    }

    @Test
    public void testLoadSubPartitions() throws InterruptedException {
        SqlldrController sqlldrController = new SqlldrControllerMock();

        sqlldrController.setSqlldrTimeout(1);
        sqlldrController.setSqlldrThreads(2);

        Assertions.assertThatCode(() ->
            sqlldrController.loadSubPartitions(ImmutableMap.of(
                "session", Paths.get("/tmp1"),
                SESSION_TO_FAIL, Paths.get("/tmp2")
            ))
        ).hasMessageContaining("SQLLoader execution finished with errors");
    }
}
