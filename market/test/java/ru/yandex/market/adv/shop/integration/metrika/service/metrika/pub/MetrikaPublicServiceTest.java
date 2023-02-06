package ru.yandex.market.adv.shop.integration.metrika.service.metrika.pub;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.junit.jupiter.MockServerSettings;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.service.random.RandomService;
import ru.yandex.market.adv.service.time.TimeService;
import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationMockServerTest;
import ru.yandex.market.adv.shop.integration.metrika.exception.BusinessMetrikaNotFoundException;
import ru.yandex.market.adv.shop.integration.metrika.model.order.MarketOrder;
import ru.yandex.market.adv.shop.integration.metrika.model.order.MarketOrderItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;

@DisplayName("Тесты на сервисе MetrikaPublicServiceImpl")
@MockServerSettings(ports = 12237)
class MetrikaPublicServiceTest extends AbstractShopIntegrationMockServerTest {

    private static final int BOUND = 1073741824;

    @Autowired
    private MetrikaPublicService metrikaPublicService;

    @Autowired
    private TimeService timeService;

    @Autowired
    private RandomService randomService;

    MetrikaPublicServiceTest(MockServerClient server) {
        super(server);
    }

    @DisplayName("Push в метрику прошел успешно")
    @DbUnitDataSet(before = "MetrikaPublicServiceTest/csv/pushToMetrika_correctData.before.csv")
    @Test
    void pushToMetrika_correctData_success() {

        mockPathMetrika(getBrowserInfo(), 1L, "pushToMetrika_correctData",
                "pushToMetrika_correctData", 200);
        mockPathMetrika(getBrowserInfo(), 1L, "pushToMetrika_correctData_2",
                "pushToMetrika_correctData_2", 200);

        metrikaPublicService.pushToMetrika(getOrder());
    }


    @DisplayName("Исключительная ситуация - push в метрику вернул ошибку 500")
    @DbUnitDataSet(before = "MetrikaPublicServiceTest/csv/pushToMetrika_correctData.before.csv")
    @Test
    void pushToMetrika_pushError_exception() {

        mockPathMetrika(getBrowserInfo(), 1L, "pushToMetrika_correctData",
                "pushToMetrika_error", 500);
        mockPathMetrika(getBrowserInfo(), 1L, "pushToMetrika_correctData_2",
                "pushToMetrika_correctData_2", 200);

        Assertions.assertThatThrownBy(
                () -> metrikaPublicService.pushToMetrika(getOrder())
        ).isInstanceOf(IllegalStateException.class);
    }

    @DisplayName("Исключительная ситуация - счетчик не найден")
    @DbUnitDataSet(before = "MetrikaPublicServiceTest/csv/pushToMetrika_counterNotFound.csv")
    @Test
    void pushToMetrika_counterNotFound_exception() {

        Assertions.assertThatThrownBy(
                () -> metrikaPublicService.pushToMetrika(getOrder())
        ).isInstanceOf(BusinessMetrikaNotFoundException.class);
    }


    private MarketOrder getOrder() {
        MarketOrder marketOrder = new MarketOrder();
        marketOrder.setIp("127.0.0.1");
        marketOrder.setUid("test_uid");
        marketOrder.setCreationTimestamp(timeService.get().getEpochSecond());
        marketOrder.setPartnerId(1L);
        marketOrder.setBusinessId(11L);
        marketOrder.setItems(List.of(
                getOffer("ecom_product_id", "test_name",
                        "test_category", new BigDecimal(150), 1L
                ),
                getOffer("ecom_product_id_2", "test_name_2",
                        "test_category_2", new BigDecimal(999), null
                )
        ));
        return marketOrder;
    }

    private MarketOrderItem getOffer(String id, String name, String category, BigDecimal price, Long supplierId) {
        MarketOrderItem marketOrderItem = new MarketOrderItem();
        marketOrderItem.setPp(51);
        marketOrderItem.setOfferId(id);
        marketOrderItem.setName(name);
        marketOrderItem.setCategory(category);
        marketOrderItem.setPrice(price);
        marketOrderItem.setCount(10);
        marketOrderItem.setPartnerId(supplierId);
        return marketOrderItem;
    }

    private String getBrowserInfo() {

        long time = timeService.get().getEpochSecond();
        return String.format("pv:1:ar:1:et:%s:st:%s:rn:%s:u:%s%s",
                time, time, randomService.get(BOUND), time, randomService.get(BOUND));
    }

    private void mockPathMetrika(String browserInfo, long counterId, String requestFile,
                                 String responseFile, int responseCode) {
        mockServerPath(
                "GET",
                "/watch/" + counterId,
                null,
                Map.of("browser-info", List.of(browserInfo),
                        "page-url", List.of("https://market.yandex.ru/my/cart"),
                        "site-info", List.of(loadFile("MetrikaPublicServiceTest/json/request/"
                                + requestFile + ".json")
                                .replaceAll("\\s+", ""))
                ),
                responseCode,
                "MetrikaPublicServiceTest/json/response/" + responseFile + ".json"
        );
    }
}
