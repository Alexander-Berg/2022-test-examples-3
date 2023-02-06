package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.Epic;
import io.restassured.path.xml.XmlPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.AnomalyType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Cis;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Приёмка в аномалии товаров Честного Знака с обязательными КИЗами")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/infor.properties"})
@Slf4j
public class ReceivingAnomalyCisTest extends AnomalySeleniumTest {

    private static final String SKU_TEMPLATE = UniqueId.getString();
    private static final String ITEM_TYPE_INCORRECT_CIS = "INCORRECT_CIS";
    private static final String ITEM_TYPE_FIT = "FIT";
    private static final String NO_CIS_EXPECTED = "";
    private static final Set<AnomalyType> ANOMALY_TYPES = Collections.singleton(AnomalyType.INCORRECT_REQUIRED_CIS);
    private final long VENDOR = 1559;
    private static final int CIS_HANDLE_MODE_DISABLED = 0;

    // Приёмка КИЗов с карготипом required и cisHandleMode == 0
    @RetryableTest
    @DisplayName("Принимаем в аномалии товар с невалидным обязательным КИЗом")
    @ResourceLock("Принимаем в аномалии товар с невалидным обязательным КИЗом")
    public void createAndReceiveInboundWithRequiredInValidCis() {
        final String testNumber = "1";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final String cisActual = RandomUtil.generateCis(gtin).getLeft();

        XmlPath getInboundResponse = testReceivingAnomalyCis(
                testNumber,
                0,
                Map.of("CIS", cisActual),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_INCORRECT_CIS, cisActual);
    }

    // Приёмка КИЗов с карготипом required и cisHandleMode == 1
    @RetryableTest
    @DisplayName("Принимаем в аномалии товар с валидным обязательным КИЗом, " +
            "который должен быть заявлен в поставке, но на самом деле не заявлен")
    @ResourceLock("Принимаем в аномалии товар с валидным обязательным КИЗом, " +
            "который должен быть заявлен в поставке, но на самом деле не заявлен")
    public void createAndReceiveInboundWithRequiredMustBeButNotDeclaredCis() {
        final String testNumber = "2";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final Pair<String, String> cisParts = RandomUtil.generateCis(gtin);

        XmlPath getInboundResponse = testReceivingAnomalyCis(
                testNumber,
                1,
                Map.of("CIS", cisParts.getRight()),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_INCORRECT_CIS, cisParts.getLeft());
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар с КИЗом который не проходит проверку на криптохвост")
    @ResourceLock("Принимаем в аномалии товар с КИЗом который не проходит проверку на криптохвост")
    public void backendCisCryptotailCheckFails() {
        final String testNumber = "3";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final String cisActual = RandomUtil.generateCis(gtin).getLeft();

        XmlPath getInboundResponse = testReceivingAnomalyCis(
                testNumber,
                0,
                Map.of("CIS", cisActual + "#GS#i_am_bad_cryptotail"),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_INCORRECT_CIS, cisActual);
    }

    @RetryableTest
    @DisplayName("Принимаем две единицы товара с одинаковым КИЗом")
    @ResourceLock("Принимаем две единицы товара с одинаковым КИЗом")
    public void uniquenessCISCheckFails() {
        final String testNumber = "4";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final Pair<String, String> cis = RandomUtil.generateCis(gtin);

        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistryWithCargotypeAndCis(
                inbound,
                VENDOR,
                SKU_TEMPLATE + testNumber,
                2,
                gtin,
                Cis.CIS_CARGO_TYPE_REQUIRED,
                null,
                CIS_HANDLE_MODE_DISABLED,
                0,
                0
        );

        Item goodItem = Item.builder()
                .vendorId(1559)
                .article(gtin)
                .name("Item with CIS")
                .checkCis(1)
                .anomalyTypes(new HashSet<>())
                .instances(Map.of("CIS", cis.getRight()))
                .build();

        final String pallet = processSteps.Incoming().placeInboundToPallet(inbound.getFulfillmentId());
        final String goodCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        final String anomalyCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(goodItem, pallet, goodCartId, false);

        Item duplicatedCIS = Item.builder()
                .vendorId(1559)
                .article(gtin)
                .name("Item with CIS")
                .checkCis(1)
                .anomalyTypes(ANOMALY_TYPES)
                .instances(Map.of("CIS", cis.getRight()))
                .build();

        uiSteps.Receiving().continueReceiving(
                duplicatedCIS,
                anomalyCartId,
                true,
                true
        ).closePallet();
        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                anomalyCartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);

        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());
        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());

        //поставка состоит из 2х айтемов с одинаковым КИЗ
        ApiSteps.Inbound().checkGetInbound(getInboundResponse,
                List.of(ITEM_TYPE_FIT, ITEM_TYPE_INCORRECT_CIS),
                List.of(cis.getLeft(), cis.getLeft()));
    }

    @RetryableTest
    @DisplayName("Принимаем в аномалии товар для которого требуется КИЗ, но на самом деле отсутствует")
    @ResourceLock("Принимаем в аномалии товар для которого требуется КИЗ, но на самом деле отсутствует")
    public void requiredCisNotPresent() {
        final String testNumber = "5";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);

        Map<String, String> noCis = new HashMap<>();
        noCis.put("CIS", null);
        XmlPath getInboundResponse = testReceivingAnomalyCis(
                testNumber,
                CIS_HANDLE_MODE_DISABLED,
                noCis,
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_INCORRECT_CIS, NO_CIS_EXPECTED);
    }

    private XmlPath testReceivingAnomalyCis(String testNumber,
                                            int cisHandleMode,
                                            Map<String, String> instancesToReceive,
                                            String gtin) {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistryWithCargotypeAndCis(
                inbound,
                VENDOR,
                SKU_TEMPLATE + testNumber,
                1,
                gtin,
                Cis.CIS_CARGO_TYPE_REQUIRED,
                null,
                cisHandleMode,
                0,
                0
        );
        receiveAnomalyCisInbound(inbound, gtin, ANOMALY_TYPES, instancesToReceive);

        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());

        return ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
    }

    private void receiveAnomalyCisInbound(Inbound inbound,
                                          String gtin,
                                          Set<AnomalyType> anomalyTypes,
                                          Map<String, String> instancesToReceive) {
        final String pallet = processSteps.Incoming().placeInboundToPallet(inbound.getFulfillmentId());
        final String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(
                Item.builder()
                        .vendorId(1559)
                        .article(gtin)
                        .name("Item with CIS")
                        .checkCis(1)
                        .anomalyTypes(anomalyTypes)
                        .instances(instancesToReceive)
                        .build(),
                pallet,
                cartId,
                true
        ).closePallet();
        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                cartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);
    }
}
