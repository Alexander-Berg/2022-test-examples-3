package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.Epic;
import io.restassured.path.xml.XmlPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.AnomalyType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Cis;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@DisplayName("Selenium: Приёмка прямого потока товаров с КИЗами Честного Знака")
@Epic("Selenium Tests")
@Slf4j
public class ReceivingCisTest extends AbstractUiTest {

    private final String SKU_TEMPLATE = UniqueId.getString();
    private final int CIS_HANDLE_MODE_DISABLED = 0;
    private final int CIS_HANDLE_MODE_ENABLED = 1;
    private final String ITEM_TYPE_FIT = "FIT";
    private final String NO_CIS_EXPECTED = "";
    private final long VENDOR = 1559;
    private final int IMEI_COUNT = 2;
    private final int SN_COUNT = 1;

    // Приёмка КИЗов с карготипом optional и cisHandleMode == 0

    @RetryableTest
    @DisplayName("Принимаем на годный сток товар с валидным опциональным КИЗом, заявленным в поставке")
    @ResourceLock("Принимаем на годный сток товар с валидным опциональным КИЗом, заявленным в поставке")
    public void createAndReceiveInboundWithOptionalValidDeclaredCis() {
        final String testNumber = "1";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final Pair<String, String> generatedCis = RandomUtil.generateCis(gtin);
        final String cisDeclared = generatedCis.getLeft();
        final String cisActual = generatedCis.getRight();

        XmlPath getInboundResponse = testReceivingWithCis(
                testNumber,
                cisDeclared,
                Cis.CIS_CARGO_TYPE_OPTIONAL,
                CIS_HANDLE_MODE_DISABLED,
                Map.of("CIS", cisActual),
                Collections.emptySet(),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_FIT, cisDeclared);
    }

    @RetryableTest
    @DisplayName("Принимаем на годный сток товар с отсутствующим опциональным КИЗом, заявленным в поставке")
    @ResourceLock("Принимаем на годный сток товар с отсутствующим опциональным КИЗом, заявленным в поставке")
    public void createAndReceiveInboundWithDeclaredOptionalCisAndNoActualCis() {
        final String testNumber = "2";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final String cisDeclared = RandomUtil.generateCis(gtin).getLeft();

        XmlPath getInboundResponse = testReceivingWithCis(
                testNumber,
                cisDeclared,
                Cis.CIS_CARGO_TYPE_OPTIONAL,
                CIS_HANDLE_MODE_DISABLED,
                Collections.emptyMap(),
                Collections.emptySet(),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_FIT, NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Принимаем на годный сток товар с невалидным по формату опциональным КИЗом, заявленным в поставке")
    @ResourceLock("Принимаем на годный сток товар с невалидным по формату опциональным КИЗом, заявленным в поставке")
    public void createAndReceiveInboundWithOptionalDeclaredInvalidCis() {
        final String testNumber = "3";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final String cisDeclared = RandomUtil.generateCis(gtin).getLeft();

        XmlPath getInboundResponse = testReceivingWithCis(
                testNumber,
                cisDeclared,
                Cis.CIS_CARGO_TYPE_OPTIONAL,
                CIS_HANDLE_MODE_DISABLED,
                Map.of("CIS", cisDeclared),
                Collections.singleton(AnomalyType.INCORRECT_OPTIONAL_CIS),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_FIT, NO_CIS_EXPECTED);
    }

    @RetryableTest
    @DisplayName("Принимаем на годный сток товар с валидным опциональным КИЗом, не заявленным в поставке")
    @ResourceLock("Принимаем на годный сток товар с валидным опциональным КИЗом, не заявленным в поставке")
    public void createAndReceiveInboundWithOptionalNotDeclaredValidCis() {
        final String testNumber = "4";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final Pair<String, String> generatedCis = RandomUtil.generateCis(gtin);
        final String cisBeforeGs = generatedCis.getLeft();
        final String cisActual = generatedCis.getRight();

        XmlPath getInboundResponse = testReceivingWithCis(
                testNumber,
                null,
                Cis.CIS_CARGO_TYPE_OPTIONAL,
                CIS_HANDLE_MODE_DISABLED,
                Map.of("CIS", cisActual),
                Collections.emptySet(),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_FIT, cisBeforeGs);
    }

    // Приёмка КИЗов с карготипом optional и cisHandleMode == 1

    @RetryableTest
    @DisplayName("Принимаем на годный сток товар с валидным опциональным КИЗом, " +
            "который должен быть заявлен в поставке, но на самом деле не заявлен")
    @ResourceLock("Принимаем на годный сток товар с валидным опциональным КИЗом, " +
            "который должен быть заявлен в поставке, но на самом деле не заявлен")
    public void createAndReceiveInboundWithOptionalMustBeButNotDeclaredValidCis() {
        final String testNumber = "5";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final String cisActual = RandomUtil.generateCis(gtin).getRight();

        XmlPath getInboundResponse = testReceivingWithCis(
                testNumber,
                null,
                Cis.CIS_CARGO_TYPE_OPTIONAL,
                CIS_HANDLE_MODE_ENABLED,
                Map.of("CIS", cisActual),
                Collections.singleton(AnomalyType.INCORRECT_OPTIONAL_CIS),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_FIT, NO_CIS_EXPECTED);
    }

    // Приёмка КИЗов с карготипом required и cisHandleMode == 0

    @RetryableTest
    @DisplayName("Принимаем на годный сток товар с валидным обязательным КИЗом, заявленным в поставке")
    @ResourceLock("Принимаем на годный сток товар с валидным обязательным КИЗом, заявленным в поставке")
    public void createAndReceiveInboundWithRequiredValidDeclaredCis() {
        final String testNumber = "6";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final Pair<String, String> generatedCis = RandomUtil.generateCis(gtin);
        final String cisDeclared = generatedCis.getLeft();
        final String cisActual = generatedCis.getRight();

        XmlPath getInboundResponse = testReceivingWithCis(
                testNumber,
                cisDeclared,
                Cis.CIS_CARGO_TYPE_REQUIRED,
                CIS_HANDLE_MODE_DISABLED,
                Map.of("CIS", cisActual),
                Collections.emptySet(),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_FIT, cisDeclared);
    }

    @RetryableTest
    @DisplayName("Принимаем на годный сток товар с валидным обязательным КИЗом, не заявленным в поставке")
    @ResourceLock("Принимаем на годный сток товар с валидным обязательным КИЗом, не заявленным в поставке")
    public void createAndReceiveInboundWithRequiredNotDeclaredValidCis() {
        final String testNumber = "7";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final Pair<String, String> generatedCis = RandomUtil.generateCis(gtin);
        final String cisBeforeGtin = generatedCis.getLeft();
        final String cisActual = generatedCis.getRight();

        XmlPath getInboundResponse = testReceivingWithCis(
                testNumber,
                null,
                Cis.CIS_CARGO_TYPE_REQUIRED,
                CIS_HANDLE_MODE_DISABLED,
                Map.of("CIS", cisActual),
                Collections.emptySet(),
                gtin
        );

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_FIT, cisBeforeGtin);
    }

    private XmlPath testReceivingWithCis(String testNumber,
                                         String cisDeclared,
                                         int cisCargoType,
                                         int cisHandleMode,
                                         Map<String, String> instancesToReceive,
                                         Set<AnomalyType> anomalyTypes,
                                         String gtin) {

        final Inbound inbound =
                createCisInbound(testNumber, cisDeclared, gtin, cisCargoType, cisHandleMode);
        receiveCisInbound(inbound, gtin, anomalyTypes, instancesToReceive);

        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());

        return ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
    }

    private Inbound createCisInbound(String testNumber,
                                     String cisDeclared,
                                     String gtin,
                                     int cisCargoType,
                                     int cisHandleMode) {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        return ApiSteps.Inbound().putInboundRegistryWithCargotypeAndCis(
                inbound,
                VENDOR,
                SKU_TEMPLATE + testNumber,
                1,
                gtin,
                cisCargoType,
                cisDeclared,
                cisHandleMode,
                IMEI_COUNT,
                SN_COUNT
        );
    }

    private void receiveCisInbound(Inbound inbound,
                                   String gtin,
                                   Set<AnomalyType> anomalyTypes,
                                   Map<String, String> instancesToReceive) {
        final String pallet = processSteps.Incoming().placeInboundToPallet(inbound.getFulfillmentId());
        final String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(
                Item.builder()
                        .vendorId(VENDOR)
                        .article(gtin)
                        .name("Item with CIS")
                        .checkCis(1)
                        .checkImei(IMEI_COUNT)
                        .checkSn(SN_COUNT)
                        .anomalyTypes(anomalyTypes)
                        .instances(instancesToReceive)
                        .build(),
                pallet,
                cartId,
                false
        ).closePallet();
    }
}
