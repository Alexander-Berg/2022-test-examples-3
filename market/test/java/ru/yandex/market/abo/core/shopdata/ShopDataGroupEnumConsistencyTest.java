package ru.yandex.market.abo.core.shopdata;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.core.shopdata.model.ShopDataGroupEnum;
import ru.yandex.market.abo.util.EnumDbConsistencyTest;

public class ShopDataGroupEnumConsistencyTest extends EnumDbConsistencyTest<Integer> {
    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @Override
    protected Set<Integer> getDbIds() {
        return new HashSet<>(pgJdbcTemplate.queryForList("SELECT id FROM sd_group", Integer.class));
    }

    @Override
    protected Set<Integer> getEnumIds() {
        return Arrays.stream(ShopDataGroupEnum.values()).map(ShopDataGroupEnum::getId).collect(Collectors.toSet());
    }
}
