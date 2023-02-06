package ru.yandex.market.jmf.logic.def.test;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.jmf.metadata.Fqns;

public class DbVersionServiceDeletedAttributeTest extends DbVersionServiceGetTest {
    @Inject
    JdbcTemplate jdbcTemplate;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        // Добавляем колонку, которой нет в метаинформации,
        // эмулируя удаленный атрибут из метаинформации

        String table = "tbl_" + DUMMY_FQN.getId();
        jdbcTemplate.update("ALTER TABLE " + table + " ADD COLUMN deletedcolumn varchar(255) DEFAULT 'test'");

        String versionTable = "tbl_" + Fqns.versionOf(DUMMY_FQN).getId();
        jdbcTemplate.update("ALTER TABLE " + versionTable + " ADD COLUMN deletedcolumn varchar(255) DEFAULT 'test'");
        jdbcTemplate.update("ALTER TABLE " + versionTable + " ADD COLUMN deletedcolumn_changed boolean DEFAULT false");
    }

    @AfterEach
    public void tearDown() {
        String table = "tbl_" + DUMMY_FQN.getId();
        jdbcTemplate.update("ALTER TABLE " + table + " DROP COLUMN deletedcolumn");

        String versionTable = "tbl_" + Fqns.versionOf(DUMMY_FQN).getId();
        jdbcTemplate.update("ALTER TABLE " + versionTable + " DROP COLUMN deletedcolumn");
        jdbcTemplate.update("ALTER TABLE " + versionTable + " DROP COLUMN deletedcolumn_changed");
    }
}
