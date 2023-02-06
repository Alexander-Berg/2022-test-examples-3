package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.servicebus;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import lombok.extern.slf4j.Slf4j;
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
import ru.yandex.market.delivery.deliveryintegrationtests.tool.FileUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.RadiatorClient;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.client.ServiceBus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;

import static org.hamcrest.Matchers.is;

@Resource.Classpath("wms/test.properties")
@DisplayName("Service-Bus API: Inbound")
@Epic("API Tests")
@Slf4j
public class InboundTest {

    @Property("test.vendorId")
    private long vendorId;

    private final ServiceBus serviceBus = new ServiceBus();
    private final RadiatorClient radiatorClient = new RadiatorClient();

    private final Inbound TEST_INBOUND = new Inbound(13383409L, "0000001658");

    private final Inbound INB_JUST_FIRST_BOM_CREATED_AND_ACCEPTED = new Inbound(13383412L, "0000001756");
    private final Inbound INB_TWO_BOMS_CREATED_FIRST_ACCEPTED = new Inbound(13383413L, "0000001758");
    private final Inbound INB_TWO_BOMS_CREATED_FIRST_LESS_THAN_SECOND = new Inbound(13383414L, "0000001759");
    private final Inbound INB_TWO_BOMS_CREATED_ONE_DEFECT = new Inbound(13383415L, "0000001761");

    @BeforeEach
    public void setUp() throws Exception {
        PropertyLoader.newInstance().populate(this);
    }

    @Test
    @DisplayName("getInboundStatus")
    public void getInboundStatusTest() {
        log.info("Testing getInboundStatus");

        serviceBus.getInboundStatus(TEST_INBOUND)
                .body("root.response.inboundsStatus.inboundStatus.statusCode",
                        Matchers.is("40"));
    }

    /**
     *
     * Ожидается поведение:
     *
     * Actual = Годный + Излишек
     * Surplus = (Годный + Излишек + Дефект) - Заявленое
     * Defect = Брак + Просрочка
     *
     * тестовые данные: https://wiki.yandex-team.ru/users/ivalekseev/Avtotesty-WMS/getrefItems/
     *
     * Просрочка сейчас учитывается не по алгоритму: https://st.yandex-team.ru/DELIVERY-11528
     *
     */
    @Test
    @DisplayName("getInboundDetails")
    public void getInboundDetailsTest() {
        log.info("Testing getInboundDetails");

        serviceBus.getInboundDetails(TEST_INBOUND)
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.article",
                        Matchers.is("AUTO_GET_EXP_ITEMS_TEST"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.vendorId",
                        Matchers.is(String.valueOf(vendorId)))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.declared",
                        Matchers.is("5"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.actual",
                        Matchers.is("7"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.defect",
                        Matchers.is("3"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.surplus",
                        Matchers.is("5"));
    }

    @Test
    @DisplayName("Поставка из которой удалили строки")
    @Description("Было заявлено 3 товара, приняли 5. Затем удалили 2 первых строки приемки (fit). " +
            "Должно получиться 1 fit и 2 surplus")
    public void inboundWithDeletedStringsTest() {
        log.info("Testing inbound with deleted strings");

        Inbound inbound = new Inbound(1561992606305L, "0000002256");

        serviceBus.getInboundDetails(inbound)
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.article",
                        Matchers.is("AUTOSURPDEL"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.vendorId",
                        Matchers.is(String.valueOf(vendorId)))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.declared",
                        Matchers.is("3"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.actual",
                        Matchers.is("3"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.defect",
                        Matchers.is("0"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.surplus",
                        Matchers.is("2"));
    }

    @Test
    @DisplayName("2-местный товар с одним заведенным BOM")
    @Description("В поставке 2х местного товара принят и заведен только 1 BOM. " +
            "Все принятые единицы должны считаться дефектом.")
    public void justFirstBomCreatedAndAcceptedCountsAsDefectInbDetailsTest() {

        serviceBus.getInboundDetails(INB_JUST_FIRST_BOM_CREATED_AND_ACCEPTED)
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.article",
                        Matchers.is("2BOM1"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.vendorId",
                        Matchers.is(String.valueOf(vendorId)))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.declared",
                        Matchers.is("10"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.actual",
                        Matchers.is("0"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.defect",
                        Matchers.is("2"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.surplus",
                        Matchers.is("0"));
    }

    @Test
    @DisplayName("2-местный товар, принят только один BOM")
    @Description("У 2х местного товара заведено 2 BOM, но принят один. " +
            "Все принятые единицы должны считаться дефектом.")
    public void twoBomsCreatedOnlyFirstAcceptedInbDetailsTest() {

        serviceBus.getInboundDetails(INB_TWO_BOMS_CREATED_FIRST_ACCEPTED)
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.article",
                        Matchers.is("2BOM2"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.vendorId",
                        Matchers.is(String.valueOf(vendorId)))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.declared",
                        Matchers.is("10"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.actual",
                        Matchers.is("0"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.defect",
                        Matchers.is("2"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.surplus",
                        Matchers.is("0"));
    }

    @Test
    @DisplayName("2-местный товар, одного из BOM недосдача")
    @Description("В поставке 2х местного товара один BOM недопоставлен.")
    public void twoBomsFirstInsufficientInbDetailsTest() {

        serviceBus.getInboundDetails(INB_TWO_BOMS_CREATED_FIRST_LESS_THAN_SECOND)
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.article",
                        Matchers.is("2BOM3"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.vendorId",
                        Matchers.is(String.valueOf(vendorId)))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.declared",
                        Matchers.is("10"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.actual",
                        Matchers.is("2"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.defect",
                        Matchers.is("3"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.surplus",
                        Matchers.is("0"));
    }

    @Test
    @DisplayName("2-местный товар, один BOM дефект")
    @Description("В поставке 2х местного товара один БОМ дефектный")
    public void twoBomsOneDefectInbDetailsTest() {

        serviceBus.getInboundDetails(INB_TWO_BOMS_CREATED_ONE_DEFECT)
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.article",
                        Matchers.is("2BOM4"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.unitId.vendorId",
                        Matchers.is(String.valueOf(vendorId)))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.declared",
                        Matchers.is("10"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.actual",
                        Matchers.is("1"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.defect",
                        Matchers.is("1"))
                .body("root.response.inboundDetails.inboundUnitDetailsList.inboundUnitDetails.surplus",
                        Matchers.is("0"));
    }


    @Test
    @DisplayName("createInbound")
    public void createInboundTest() {
        String body = FileUtil.bodyStringFromFile("wms/wrapRequests/createInbound.xml",
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                "InboundTestItem",
                serviceBus.getToken(),
                false
        );
        serviceBus.createInbound(body);
    }

    @Test
    @DisplayName("updateInbound")
    public void updateInboundTest() {
        log.info("Testing updateInbound");

        Inbound inboundToUpdate = serviceBus.createInbound(
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                "InboundTestItem"
        );

        serviceBus.updateInbound(inboundToUpdate, DateUtil.currentDateTime())
                .statusCode(HttpStatus.SC_OK);
    }

    @Test
    @DisplayName("createInbound With new Item Servicebus")
    @Tag("notForMultitesting")
    public void createNewItemInboundTest() {
        final String article = UniqueId.getStringUUID();
        String body = FileUtil.bodyStringFromFile("wms/wrapRequests/createInbound.xml",
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                article,
                serviceBus.getToken(),
                false
        );
        serviceBus.createInbound(body);

        String prefix = "root.response.itemReferences.itemReference.item.";
        radiatorClient.getReferenceItems(1559, article)
                .body(prefix + "unitId.vendorId", is("1559"))
                .body(prefix + "unitId.article", is(article))
                .body(prefix + "count", is("0"))
                .body(prefix + "price", is("0"))
                .body(prefix + "hasLifeTime", is("false"))
                .body(prefix + "boxCount", is("1"))
                .body(prefix + "korobyte.width", is("12"))
                .body(prefix + "korobyte.height", is("11"))
                .body(prefix + "korobyte.length", is("10"))
                .body(prefix + "korobyte.weightGross", is("1.1"))
                .body(prefix + "korobyte.weightNet", is("1.1"))
                .body(prefix + "korobyte.weightTare", is("0.0"))
                .body(prefix + "remainingLifetimes.inbound.days.value", is("60"))
                .body(prefix + "remainingLifetimes.outbound.days.value", is("30"));
    }

    @Test
    @DisplayName("createInbound with three Items")
    void createThreeItemInboundTest() {
        String body = FileUtil.bodyStringFromFile("wms/tests/api/InboundTest/createThreeItemInbound.xml",
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                "InboundTestItem",
                serviceBus.getToken()
        );
        serviceBus.createInbound(body);
    }

    @Test
    @DisplayName("getInboundHistory")
    public void getInboundHistoryTest() {
        log.info("Testing getInboundHistory");

        serviceBus.getInboundHistory(TEST_INBOUND)
                .body("root.response.inboundStatusHistory.history.inboundStatus.find {it.statusCode == '40'}.statusCode",
                        Matchers.is("40"))
                .body("root.response.inboundStatusHistory.history.inboundStatus.find {it.statusCode == '30'}.statusCode",
                        Matchers.is("30"))
                .body("root.response.inboundStatusHistory.history.inboundStatus.find {it.statusCode == '1'}.statusCode",
                        Matchers.is("1"));
    }


    @Test
    @DisplayName("cancelInbound: Отмена поставки")
    public void cancelInboundTest() {
        log.info("Testing cancelInbound");

        String body = FileUtil.bodyStringFromFile("wms/wrapRequests/createInbound.xml",
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                "InboundTestItem",
                serviceBus.getToken(),
                false
        );
        Inbound inboundToCancel = serviceBus.createInbound(body);

        serviceBus.cancelInbound(inboundToCancel);

        serviceBus.getInboundStatus(inboundToCancel)
                .body("root.response.inboundsStatus.inboundStatus.statusCode",
                        is("0"));
    }

    @Test
    @DisplayName("putInboundRegistryTest: Создание реестра поставки")
    public void putInboundRegistryTest() {
        log.info("Testing putInboundRegistry");

        final String sku = UniqueId.getStringUUID();
        final Item item = Item.builder().article(sku).sku(sku).shelfLife(false).vendorId(1559)
                .name("ReturnInboundApiTest").build();
        final String palletId = UniqueId.getString();
        final String uit = "99" + RandomUtil.randomStringNumbersOnly(10);
        final String lot = RandomUtil.randomStringNumbersOnly(10);

        String body = FileUtil.bodyStringFromFile("wms/wrapRequests/createInbound.xml",
                UniqueId.getStringUUID(),
                UniqueId.get(),
                DateUtil.tomorrowDateTime(),
                "InboundTestItem",
                serviceBus.getToken(),
                false
        );
        Inbound inbound = serviceBus.createInbound(body);

        ApiSteps.Inbound().putInboundRegistry(
                "wms/servicebus/putInboundRegistry/putInboundRegistryWH2WH.xml",
                inbound,
                uit,
                sku,
                1559,
                lot,
                palletId,
                false
        );
    }
}
