package ru.yandex.market.loyalty.back.usecase;

import liquibase.exception.LiquibaseException;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.service.RegionSettingsService;

import static org.junit.Assert.assertEquals;

/**
 * @author dinyat
 * 14/06/2017
 */
public abstract class MarketLoyaltyBackProdDataMockedDbTest extends MarketLoyaltyBackMockedDbTestBase {
    private static volatile boolean dataLoaded = false;
    private static final Object lock = new Object();

    @Before
    public void checkPackage() {
        assertEquals(
                "Все тесты по данным из liquibase должны находиться в одном пакете",
                MarketLoyaltyBackProdDataMockedDbTest.class.getPackage(),
                getClass().getPackage()
        );
    }

    @Qualifier("liquibasePopulateDb")
    @Autowired
    private SpringLiquibase liquibase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegionSettingsService regionSettingsService;


    @Before
    public void cleanNotificationsTable() {
        jdbcTemplate.update("TRUNCATE TABLE coupon_notification_sent CASCADE ");
    }

    @Before
    public void loadData() throws LiquibaseException {
        if (!dataLoaded) {
            synchronized (lock) {
                if (!dataLoaded) {
                    liquibase.setShouldRun(true);
                    liquibase.afterPropertiesSet();
                    dataLoaded = true;
                }
            }
        }
        regionSettingsService.reloadCache();
    }

    @Override
    protected boolean shouldClearDb() {
        return false;
    }

    @Override
    protected boolean shouldLoadTechnicalAccounts() {
        return false;
    }

    @Override
    protected boolean shouldCreateEmptyContext() {
        return false;
    }
}
