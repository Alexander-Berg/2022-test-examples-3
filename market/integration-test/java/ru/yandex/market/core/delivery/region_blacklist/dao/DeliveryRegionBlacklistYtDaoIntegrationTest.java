package ru.yandex.market.core.delivery.region_blacklist.dao;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.market.core.config.DevJdbcConfig;
import ru.yandex.market.core.delivery.region_blacklist.model.DeliveryRegionBlacklist;
import ru.yandex.market.core.yt.dynamic.YtDynamicTableConfig;

/**
 * Интеграционный тест для {@link DeliveryRegionBlacklistYtDao}
 *
 * не забудь указать [mbi.robot.yt.token] в
 *   src/integration-test/resources/ru/yandex/market/core/config/dev-datasource.properties
 */
@SpringJUnitConfig({
        DevJdbcConfig.class,
        YtDynamicTableConfig.class,
        DeliveryRegionBlacklistYtConfig.class
})
@Disabled
class DeliveryRegionBlacklistYtDaoIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(DeliveryRegionBlacklistYtDaoIntegrationTest.class);

    private static final long PARTNER_ID = 1209841L;
    private static final long WAREHOUSE_ID = 3400976L;

    @Value("${mbi.robot.yt.token}")
    String requiredToken;

    @Autowired
    DeliveryRegionBlacklistYtDao deliveryRegionBlacklistYtDao;

    @Test
    void setPartnerDeliveryRegionBlacklist() {
        deliveryRegionBlacklistYtDao.setPartnerDeliveryRegionBlacklist(
                DeliveryRegionBlacklist.builder()
                        .setPartnerId(PARTNER_ID)
                        .setWarehouseId(3400976L)
                        .setRegions(List.of(123L, 23456L, 3456789L))
                        .setUpdatedAt(Instant.parse("2021-11-19T19:23:12Z"))
                        .build()
        );
    }

    @Test
    void getPartnerDeliveryRegionBlacklists() {
        var records = deliveryRegionBlacklistYtDao.getPartnerDeliveryRegionBlacklists(PARTNER_ID);
        log.warn("records read: {}", records);
    }

    @Test
    void deleteDeliveryRegionBlacklist() {
        deliveryRegionBlacklistYtDao.deleteDeliveryRegionBlacklist(PARTNER_ID, WAREHOUSE_ID);
    }

}
