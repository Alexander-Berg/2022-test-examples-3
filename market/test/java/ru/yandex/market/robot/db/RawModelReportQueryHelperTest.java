package ru.yandex.market.robot.db;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.robot.db.liquibase.LiquibaseTestConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = LiquibaseTestConfig.class)
public class RawModelReportQueryHelperTest {
    private static final int TEST_SOURCE_ID = 1;
    private static final String TEST_CATEGORY = "TEST_CATEGORY";
    private static final int TEST_MARKET_CATEGORY_ID = 1;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void whenCallParamsReportQueryByCategoryIdShouldNotFail() {
        RawModelReportQueryHelper.paramsReportQuery(jdbcTemplate,
            vendorParam -> {
            },
            TEST_SOURCE_ID,
            TEST_MARKET_CATEGORY_ID
        );
    }

    @Test
    public void whenCallParamsReportQueryByCategoryShouldNotFail() {
        RawModelReportQueryHelper.paramsReportQuery(jdbcTemplate,
            vendorParam -> {
            },
            TEST_SOURCE_ID,
            TEST_CATEGORY
        );
    }

}
