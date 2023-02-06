package ru.yandex.market.abo.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.util.EnumDbConsistencyTest;

/**
 * @author imelnikov
 */
public class CoreMutexTest extends EnumDbConsistencyTest<Integer> {

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Test
    public void lockPg() {
        CoreMutex.lock(pgJdbcTemplate, CoreMutex.OUTLET_INBOX);
    }

    @Override
    protected Set<Integer> getDbIds() {
        return new HashSet<>(pgJdbcTemplate.queryForList("select id from mutex", Integer.class));
    }

    @Override
    protected Set<Integer> getEnumIds() {
        return Arrays.stream(CoreMutex.values()).map(CoreMutex::getId).collect(Collectors.toSet());
    }
}
