package ru.yandex.market.fintech.banksint.mybatis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.market.fintech.banksint.FunctionalTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommonPropertyMapperTest extends FunctionalTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CommonPropertyMapper commonPropertyMapper;

    @Test
    void selectBooleanPropertyValueByNameOrDefault() {

        jdbcTemplate.execute("delete from common_property where name = 'test_prop'");

        assertFalse(commonPropertyMapper.selectBooleanPropertyValueByName("test_prop"));
        assertTrue(commonPropertyMapper.selectBooleanPropertyValueByNameOrDefault("test_prop", true));
        assertFalse(commonPropertyMapper.selectBooleanPropertyValueByNameOrDefault("test_prop", false));

        jdbcTemplate.execute("insert into common_property values ('test_prop', 'true')");
        assertTrue(commonPropertyMapper.selectBooleanPropertyValueByNameOrDefault("test_prop", false));

    }
}
