package ru.yandex.market.api.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;

class DbApiResourceServiceTest extends FunctionalTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DbApiResourceService service;

    @BeforeEach
    void setUp() {
        service = new DbApiResourceService() {
            @Override
            public CalculatedApiLimit getResourceLimit(ApiLimitType apiLimitType, int groupId, long subjectId) {
                return null; // not used in test
            }
        };
        service.setJdbcTemplate(jdbcTemplate);
    }

    @Test
    @DbUnitDataSet(
            before = "DbApiResourceServiceTest.callProcedureUpdateResourceLimits.before.csv",
            after = "DbApiResourceServiceTest.callProcedureUpdateResourceLimits.after.csv"
    )
    void updateResourceLimits() {
        // simple smoke test
        service.rebuildResourceLimits();
    }
}
