package ru.yandex.market.rg.crm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.rg.config.FunctionalTest;

public class CRMContactRepositoryTest extends FunctionalTest {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void getStatInfo() {
        jdbcTemplate.execute(CRMContactRepository.CONTACTS_EXPORT_QUERY);
    }
}
