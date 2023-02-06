package ru.yandex.market.olap2.load;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.yandex.market.olap2.config.IntegrationTestConfig;
import ru.yandex.market.olap2.ytreflect.YtTestTable;

import java.sql.ResultSetMetaData;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.base.Preconditions.checkArgument;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(SpringRunner.class)
@ActiveProfiles("integration-test")
@SpringBootTest(classes = {IntegrationTestConfig.class})
public class LoadTaskExecutorITest {

    @Autowired
    private LoadTaskExecutor loadTaskExecutor;

    @Autowired
    private NamedParameterJdbcOperations jdbcOperations;

    @Test(timeout = 120_000)
    public void fullLoadTest() throws InterruptedException {
        AtomicBoolean taskFinishedSuccessfully = new AtomicBoolean(false);
        CountDownLatch taskFinished = new CountDownLatch(1);

        LoadTask task = new LoadTask("fulltstsid1", YtTestTable.TBL, null) {
            @Override
            public void success() {
                checkArgument(taskFinishedSuccessfully.compareAndSet(false, true));
                taskFinished.countDown();
            }

            @Override
            public void rejected(Exception e) {
                taskFinished.countDown();
            }

            @Override
            public void failed(Exception e) {
                taskFinished.countDown();
            }
        };

        jdbcOperations.getJdbcOperations().execute("drop table if exists " + task.getTable() + " cascade");

        try {
            loadTaskExecutor.putTask(task);
            assertTrue(taskFinished.await(10, TimeUnit.MINUTES));
            assertTrue(taskFinishedSuccessfully.get());
            log.info(selectTable(task));
        } finally {
            jdbcOperations.getJdbcOperations().execute("drop table if exists " + task.getTmpTable() + " cascade");
        }
    }

    private String selectTable(LoadTask task) {
        StringBuilder sb = new StringBuilder();
        jdbcOperations.query("select * from " + task.getTable(), Collections.emptyMap(),
            (rs) -> {
                ResultSetMetaData metadata = rs.getMetaData();
                int columnCount = metadata.getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    sb.append(metadata.getColumnName(i));
                    sb.append('=');
                    sb.append(rs.getString(i));
                    sb.append(';');
                }
                sb.append('\n');
            });
        return sb.toString();
    }
}
