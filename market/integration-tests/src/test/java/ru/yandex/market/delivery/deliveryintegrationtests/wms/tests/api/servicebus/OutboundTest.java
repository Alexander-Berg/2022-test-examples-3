package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.servicebus;

import io.qameta.allure.Epic;
import io.restassured.response.ValidatableResponse;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.DateUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;

@Resource.Classpath("wms/test.properties")
@DisplayName("Service-Bus API: Outbound")
@Epic("API Tests")
public class OutboundTest {
    private static final Logger log = LoggerFactory.getLogger(OutboundTest.class);

    @Property("test.vendorId")
    private long vendorId;

    private final Outbound TEST_OUTBOUND = new Outbound(1556274949855L, "0000001047");

    private final ServiceBus serviceBus = new ServiceBus();

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

    @Test
    @Tag("notForMultitesting")
    @DisplayName("getOutbound: Получение реестров автоматического изъятия")
    public void getOutboundTest() {
        log.info("Testing getOutbound");

        final String prefix = "root.response.registries.registry.";
        // для престейбла используем уже отгруженное автоматическое изъятие
        serviceBus.getOutbound(1611938832, "0000086461")
                .body("root.response.outbound.outboundType",
                        Matchers.is("0"))
                .body(prefix + "pallets.pallet.unitInfo.compositeId.partialIds[0].partialId.value",
                        Matchers.is("DRP07448"))
                .body(prefix + "boxes.box.unitInfo.compositeId.partialIds[0].partialId[0].value",
                        Matchers.is("P000007448"))
                .body(prefix + "boxes.box.unitInfo.compositeId.partialIds[0].partialId[1].value",
                        Matchers.is("outbound-1611938832"));
    }

    @Test
    @DisplayName("putOutbound: Создание нового изъятия")
    public void putOutboundTest() {
        log.info("Testing putOutbound");

        long yandexId = UniqueId.get();
        Outbound outbound = new Outbound(yandexId, UniqueId.getString());

        serviceBus.putOutbound(outbound);

        serviceBus.getOutbound(outbound)
                .body("root.response.outbound.outboundId.yandexId",
                        Matchers.is(Long.toString(yandexId)));
    }

    @Test
    @DisplayName("putOutbound: Обновление автоматического изъятия")
    public void refreshExistingOutboundTest() {
        log.info("Testing refreshing existing outbound with putOutbound");

        long yandexId = UniqueId.get();
        String partnerId = serviceBus.putOutbound(yandexId)
                .extract()
                .xmlPath()
                .getString("root.response.outboundId.partnerId");

        long currentTS = System.currentTimeMillis();
        Date date = new Date(currentTS);
        Date dateMinusThreeHours = new Date(currentTS - 3600 * 1000 * 3);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String currentDate = formatter.format(date);
        String currentDateMinusThreeHours = formatter.format(dateMinusThreeHours);
        String interval = String.format("%1$s+03:00/%1$s+03:00", currentDate);
        String expectedInterval = String.format("%1$s+00:00/%1$s+00:00", currentDateMinusThreeHours);

        serviceBus.putOutbound(yandexId, interval).statusCode(HttpStatus.SC_OK);

        serviceBus.getOutbound(yandexId, partnerId)
                .body("root.response.outbound.outboundId.yandexId",
                        Matchers.is(Long.toString(yandexId)))
                .body("root.response.outbound.outboundId.partnerId",
                        Matchers.is(partnerId))
                .body("root.response.outbound.interval",
                        Matchers.is(expectedInterval));
    }

    @Test
    @DisplayName("putOutbound: Создание изъятия для межскладского перемещения")
    public void putOutboundInterWarehouseTest() {
        log.info("Testing putOutbound");

        long yandexId = UniqueId.get();
        String interval = String.format("%1$s/%1$s", DateUtil.currentDateTime());

        serviceBus.putOutbound(yandexId, interval, "wms/servicebus/putOutboundWithType3.xml");
    }

    @Test
    @DisplayName("putOutboundRegistry: Загрузка реестра изъятия для межскладского перемещения")
    public void putOutboundRegistryTest() {
        log.info("Testing putOutboundRegistry");

        long yandexId = UniqueId.get();
        String interval = String.format("%1$s/%1$s", DateUtil.currentDateTime());

        Outbound outbound = serviceBus.putOutbound(yandexId,
                interval,
                "wms/servicebus/putOutboundWithType3.xml");

        serviceBus.putOutboundRegistry(outbound.getYandexId(),
                outbound.getFulfillmentId(),
                DateUtil.currentDateTime(),
                "INBITSTITEM1",
                1559);
    }

    @Test
    @Tag("notForMultitesting")
    @DisplayName("getOutboundDetails")
    public void getOutboundDetailsTest() {
        log.info("Testing getOutboundDetails");

        serviceBus.getOutboundDetails(TEST_OUTBOUND)
                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM1'}.unitId.article",
                        Matchers.is("INBITSTITEM1"))
                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM1'}.unitId.vendorId",
                        Matchers.is(String.valueOf(vendorId)))

                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM1'}.declared",
                        Matchers.is("1"))
                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM1'}.actual",
                        Matchers.is("1"))

                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM2'}.unitId.article",
                        Matchers.is("INBITSTITEM2"))
                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM2'}.unitId.vendorId",
                        Matchers.is(String.valueOf(vendorId)))

                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM2'}.declared",
                        Matchers.is("1"))
                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM2'}.actual",
                        Matchers.is("1"))

                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM3'}.unitId.article",
                        Matchers.is("INBITSTITEM3"))
                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM3'}.unitId.vendorId",
                        Matchers.is(String.valueOf(vendorId)))

                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM3'}.declared",
                        Matchers.is("1"))
                .body("root.response.outboundDetails.outboundUnitDetailsList.outboundUnitDetails.find " +
                                "{it.unitId.article == 'INBITSTITEM3'}.actual",
                        Matchers.is("1"));
    }

    @Test
    @DisplayName("createOutbound")
    public void createOutboundTest() {
        log.info("Testing createOutbound");

        serviceBus.createOutbound(
                UniqueId.get(),
                itemList,
                DateUtil.tomorrowDateTime()
        );
    }

    @Test
    @DisplayName("putOutboundWithRegistryTest")
    public void putOutboundWithRegistryTest() {
        log.info("Testing putRegistryWithCustomItems");

        long yandexId = UniqueId.get();
        ValidatableResponse resp = serviceBus.putOutbound(
                yandexId);

        String fulfillmentId = resp.extract().xmlPath().getString("root.response.outboundId.partnerId");
        log.info("Outbound created. yandexId = {}, fulfillmentId = {}", yandexId, fulfillmentId);

        Outbound outbound = new Outbound(yandexId, fulfillmentId, fulfillmentId);
        serviceBus.putRegistryWithCustomItems(outbound, itemList, DateUtil.tomorrowDateTime());
    }

    @Test
    @DisplayName("cancelOutbound")
    public void cancelOutboundTest() {
        log.info("Testing cancelOutbound");

        Outbound outbound = serviceBus.createOutbound(
                UniqueId.get(),
                itemList,
                DateUtil.tomorrowDateTime()
        );

        serviceBus.cancelOutbound(outbound);
    }

    @Test
    @Tag("notForMultitesting")
    @DisplayName("getOutboundsStatus")
    public void getOutboundsStatusTest() {
        log.info("Testing outboundsStatus");

        serviceBus.getOutboundsStatus(TEST_OUTBOUND)
                .body("root.response.outboundsStatus.outboundStatus.status.statusCode",
                        Matchers.is("330"));
    }

}
