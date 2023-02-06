package ru.yandex.market.loyalty.core.service;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.core.dao.ConfigDao;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

public class ConfigurationServiceTest extends MarketLoyaltyCoreMockedDbTestBase {

    @Autowired
    private ConfigDao configDao;

    @Ignore("Ignored due to org.postgresql.util.PSQLException: FATAL: the database system is shutting down  in " +
            "Teamcity")
    @Test
    public void shouldGetFromPropertyIfSet() {
        configDao.put("property.that.was.set", "valueInDb");
        configurationService.reloadCache();
        assertEquals("valueInProperties", configurationService.get("market.loyalty.config.property.that.was.set"));
    }

    @Test
    public void shouldGetFromDbOnUnknownProperty() {
        configDao.put("property.to.fetch.from.db", "valueInDb");
        configurationService.reloadCache();
        assertEquals("valueInDb", configurationService.get("market.loyalty.config.property.to.fetch.from.db"));
    }

    @Test
    public void shouldFailOnUnknownPropertyThatNotFoundInDb() {
        MarketLoyaltyException exception = assertThrows(MarketLoyaltyException.class, () ->
                configurationService.get("market.loyalty.config.unknown.property")
        );

        assertEquals(MarketLoyaltyErrorCode.OTHER_ERROR, exception.getMarketLoyaltyErrorCode());
    }
}
