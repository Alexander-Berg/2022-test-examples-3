package ru.yandex.market.adv.shop.integration.checkouter.mapper.click;

import java.util.List;

import javax.annotation.Nonnull;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketVendorClicksLog;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.click.MarketVendorClicksLogSource;

@DisplayName("Тесты на маппер MarketVendorClicksLogMapper")
class MarketVendorClicksLogMapperTest extends AbstractShopIntegrationTest {

    @Autowired
    private TimeService timeService;
    @Autowired
    private MarketVendorClicksLogMapper mapper;

    @DisplayName("Успешный маппинг MarketVendorClicksLogSource на MarketVendorClicksLog")
    @Test
    void map_marketVendorClicksLog_success() {
        Assertions.assertThat(
                        mapper.map(
                                getSource("1.offer1", 1L, 2L, "3", "puid", null)
                        )
                )
                .isEqualTo(
                        getTarget("offer1", 1L, "puid", false)
                );
    }

    @DisplayName("Успешный маппинг List<MarketVendorClicksLogSource> на List<MarketVendorClicksLog>")
    @Test
    void map_listMarketVendorClicksLog_success() {
        Assertions.assertThat(
                        mapper.map(
                                List.of(
                                        getSource("1.1.offer1", 1L, 2L, "1", "puid", null),
                                        getSource("1.2.offer2", 0L, 1L, "", "", "uuid"),
                                        getSource("1.2.offer3", 0L, 1L, null, "", "uuid"),
                                        getSource("1.2.offer4", 0L, 1L, "2", "", "uuid"),
                                        getSource("1.2.offer5", 0L, 1L, "3", "", "uuid"),
                                        getSource("offer6", 2L, 1L, "3", "", "uuid"),
                                        getSource("1.2.D.offer7", 2L, 1L, "3", "", "uuid"),
                                        getSource("2.offer8", 2L, 1L, "3", "", "uuid"),
                                        getSource("1-2-.offer9", 0L, 3L, "3", "", "uuid"),
                                        getSource("1-2-offer10", 0L, 3L, "3", "", "uuid"),
                                        getSource("1-2-offer11.", 0L, 3L, "3", "", "uuid"),
                                        getSource("", 0L, 4L, "3", "", "uuid"),
                                        getSource("1.1", 0L, 4L, "3", "", "uuid")
                                )
                        )
                )
                .containsExactlyInAnyOrder(
                        getTarget("1.1.offer1", 1L, "puid", false),
                        getTarget("1.2.offer2", 1L, "uuid", true),
                        getTarget("1.2.offer3", 1L, "uuid", true),
                        getTarget("1.2.offer4", 1L, "uuid", true),
                        getTarget("2.offer5", 1L, "uuid", true),
                        getTarget("offer6", 2L, "uuid", true),
                        getTarget("2.D.offer7", 2L, "uuid", true),
                        getTarget("offer8", 2L, "uuid", true),
                        getTarget("offer9", 3L, "uuid", true),
                        getTarget("1-2-offer10", 3L, "uuid", true),
                        getTarget("", 3L, "uuid", true),
                        getTarget("", 4L, "uuid", true),
                        getTarget("1", 4L, "uuid", true)
                );
    }

    @Nonnull
    private MarketVendorClicksLogSource getSource(String offerId, long supplierId, long shopId,
                                                  String supplierType, String puid, String uuid) {
        return new MarketVendorClicksLogSource(1634812973, supplierId, shopId, supplierType, offerId, puid,
                uuid, 1L, 2L, 1L, 0L);
    }

    @Nonnull
    private MarketVendorClicksLog getTarget(String offerId, long partnerId, String uid, boolean noAuth) {
        return new MarketVendorClicksLog(partnerId, offerId, uid, timeService.get(), noAuth, 1L, 2L);
    }
}
