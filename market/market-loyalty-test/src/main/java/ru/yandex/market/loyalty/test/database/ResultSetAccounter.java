package ru.yandex.market.loyalty.test.database;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.log4j.Log4j2;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Log4j2
public class ResultSetAccounter {
    private final AtomicInteger count = new AtomicInteger(0);
    private ConcurrentHashMap<Integer, String> queries;

    private boolean isStarted() {
        return queries != null;
    }

    public void start() {
        assertFalse("Counter already started", isStarted());
        count.set(0);
        queries = new ConcurrentHashMap<>();
        log.debug("Started.");
    }

    public void stopAndCheck() {
        assertTrue("Accounter was not started", isStarted());
        int unclosed = getCount();
        log.debug("Stopped. Unclosed result sets: {}", unclosed);
        assertThat("There are " + unclosed + " unclosed result sets after the end of test for queries: " +
                queries.values(), unclosed, equalTo(0));
        queries = null;
    }

    public void increase(int rsHashCode, String sql) {
        if (isStarted()) {
            count.incrementAndGet();
            queries.put(rsHashCode, sql);
        }
    }

    public void decrease(int rsHashCode) {
        if (isStarted()) {
            count.decrementAndGet();
            queries.remove(rsHashCode);
        }
    }

    public int getCount() {
        return count.get();
    }

}
