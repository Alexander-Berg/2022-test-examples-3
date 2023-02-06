package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.wrap;

import io.qameta.allure.Epic;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.WrapInfor;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;

import java.util.List;

@Resource.Classpath("wms/test.properties")
@DisplayName("API: Outbound")
@Epic("API Tests")
public class OutboundTest {
    private static final Logger log = LoggerFactory.getLogger(OutboundTest.class);

    @Property("test.vendorId")
    private long vendorId;
    private final ServiceBus serviceBus = new ServiceBus();

    private final WrapInfor wrapInfor = new WrapInfor();

    private final Outbound TEST_OUTBOUND = new Outbound(1556274949855L, "0000001047");

    private final Item ITEM1 = Item.builder()
            .sku("INBITSTITEM1")
            .vendorId(1559)
            .article("INBITSTITEM1")
            .quantity(1)
            .build();
    private final Item ITEM2 = Item.builder()
            .sku("INBITSTITEM2")
            .vendorId(1559)
            .article("INBITSTITEM2")
            .quantity(1)
            .build();
    private final Item ITEM3 = Item.builder()
            .sku("INBITSTITEM3")
            .vendorId(1559)
            .article("INBITSTITEM3")
            .quantity(1)
            .build();
    private List<Item> itemList = List.of(ITEM1, ITEM2, ITEM3);

    @BeforeEach
    public void setUp() throws Exception {
        PropertyLoader.newInstance().populate(this);
    }

    //TODO надо ли выпилить тоже? в сервисбасе такого нет пока
    @Deprecated
    @Test
    @DisplayName("reCreateExistingOutbound")
    public void reCreateOutboundTest() {
        log.info("Тест проверяет, что у повторно созданного изъятия в ответе присутствуют " +
                "все поля и соответствуют первоначальным");
        long yandexId = UniqueId.get();
        Outbound initialOutbound =  serviceBus.createOutbound(
                yandexId,
                itemList,
                DateUtil.tomorrowDateTime()
        );

        Assertions.assertEquals(initialOutbound.getYandexId(), yandexId);
        Assertions.assertNotNull(initialOutbound.getFulfillmentId());
        Assertions.assertNotNull(initialOutbound.getPartnerId());

        Outbound recreatedOutbound = serviceBus.createOutbound(
                initialOutbound.getYandexId(),
                itemList,
                DateUtil.tomorrowDateTime()
        );

        Assertions.assertEquals(initialOutbound.getYandexId(), recreatedOutbound.getYandexId());
        Assertions.assertEquals(initialOutbound.getPartnerId(), recreatedOutbound.getPartnerId());
        Assertions.assertEquals(initialOutbound.getFulfillmentId(), recreatedOutbound.getFulfillmentId());
    }

    @Test
    @DisplayName("getOutboundStatus")
    public void getOutboundStatusTest() {
        log.info("Testing outboundStatus");

        wrapInfor.getOutboundStatus(TEST_OUTBOUND)
                .body("root.response.outboundsStatus.outboundStatus.status.statusCode",
                        Matchers.is("330"));
    }

    @Test
    @DisplayName("getOutboundHistory")
    public void getOutboundHistoryTest() {
        log.info("Testing outboundHistory");

        wrapInfor.getOutboundHistory(TEST_OUTBOUND)
                .body("root.response.outboundStatusHistory.history.outboundStatus.find {it.statusCode == '330'}.statusCode",
                        Matchers.is("330"))
                .body("root.response.outboundStatusHistory.history.outboundStatus.find {it.statusCode == '320'}.statusCode",
                        Matchers.is("320"))
                .body("root.response.outboundStatusHistory.history.outboundStatus.find {it.statusCode == '310'}.statusCode",
                        Matchers.is("310"))
                .body("root.response.outboundStatusHistory.history.outboundStatus.find {it.statusCode == '1'}.statusCode",
                        Matchers.is("1"));
    }
}
