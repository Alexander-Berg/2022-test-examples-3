package ru.yandex.market.abo.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.clch.model.CheckerType;

/**
 * @author artemmz
 * @date 14/03/19.
 */
public class ClchCheckerTypeConsistencyTest extends EnumDbConsistencyTest<Integer> {
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Override
    protected Set<Integer> getDbIds() {
        return new HashSet<>(pgJdbcTemplate.queryForList("SELECT id FROM clch_checker", Integer.class));
    }

    @Override
    protected Set<Integer> getEnumIds() {
        return Arrays.stream(CheckerType.values()).map(CheckerType::getId).collect(Collectors.toSet());
    }
}
