package ru.yandex.market.abo.core.shopdata.clch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.core.shopdata.clch.model.ShopDataCheckerType;
import ru.yandex.market.abo.util.EnumDbConsistencyTest;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 27.07.2020
 */
public class ShopDataCheckerTypeConsistencyTest extends EnumDbConsistencyTest<Integer> {

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Override
    protected Set<Integer> getDbIds() {
        return new HashSet<>(pgJdbcTemplate.queryForList("SELECT id FROM sd_checker_query", Integer.class));
    }

    @Override
    protected Set<Integer> getEnumIds() {
        return Arrays.stream(ShopDataCheckerType.values()).map(ShopDataCheckerType::getId).collect(Collectors.toSet());
    }
}
