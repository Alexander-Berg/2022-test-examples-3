package ru.yandex.market.loyalty.core.test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;

import com.querydsl.sql.SQLQueryFactory;
import lombok.extern.log4j.Log4j2;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.test.database.ResultSetAccounter;

import static org.junit.Assert.assertEquals;

@Log4j2
public class ResultSetAccounterTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    SQLQueryFactory queryFactory;
    @Autowired
    ResultSetAccounter resultSetAccounter;

    @Test
    public void shouldIncreaseOnQueryAndDecreaseOnStatementClose() throws SQLException {
        Statement statement = createStatement();
        assertEquals(0, resultSetAccounter.getCount());

        statement.executeQuery("SELECT 'test'");
        assertEquals(1, resultSetAccounter.getCount());

        statement.close();
        assertEquals(0, resultSetAccounter.getCount());
    }

    @Test
    public void shouldCountConcurrentConnections() throws SQLException, InterruptedException {
        final int childCount = CPU_COUNT;
        CountDownLatch latch = new CountDownLatch(childCount);
        final var children = new ChildThread[childCount];

        // have no opened result sets before test
        assertEquals(0, resultSetAccounter.getCount());

        Statement statement = createStatement();
        // create one
        try (ResultSet rs = statement.executeQuery("SELECT 'main'")) {
            assertEquals(1, resultSetAccounter.getCount());

            // multithreading connections accounting
            for (int i = 0; i < childCount; i++) {
                children[i] = new ChildThread(latch);
                children[i].start();
            }

            latch.await();
            assertEquals(1 + childCount, resultSetAccounter.getCount());

            for (int i = 0; i < childCount; i++) {
                children[i].interrupt();
                children[i].join(1000);
            }
            // children connections should be closed
            assertEquals(1, resultSetAccounter.getCount());
        }
        assertEquals( 0, resultSetAccounter.getCount());
    }


    private Statement createStatement() {
        try {
            return queryFactory.getConnection().createStatement();
        } catch (SQLException e) {
            throw queryFactory.getConfiguration().translate(e);
        }
    }

    private class ChildThread extends Thread {
        private final CountDownLatch latch;

        private ChildThread(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void run() {
            try (Statement statement = createStatement()) {
                statement.executeQuery("SELECT 'child ready'");
                latch.countDown();
                Thread.sleep(1000);
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                log.debug("Child interrupted");
            }
        }
    }

}
