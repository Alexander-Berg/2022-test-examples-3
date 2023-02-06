package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.restassured.path.xml.XmlPath;
import lombok.Builder;
import lombok.Data;
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
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving.ReceivingAdditionalTest.Constants.ANY_ITEM_COMPOSITE_IDS_PATH;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving.ReceivingAdditionalTest.Constants.ANY_ITEM_COMPOSITE_ID_VALUE_PATH;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving.ReceivingAdditionalTest.Constants.REGISTRY_TYPES_PATH;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving.ReceivingAdditionalTest.Constants.TYPE_ID_REL_PATH;

@DisplayName("Selenium: Допоставка товаров из аномалий")
@Epic("Selenium Tests")
@Slf4j
public class ReceivingAdditionalTest extends AnomalySeleniumTest {

    private static final String SKU_TEMPLATE = UniqueId.getString();
    private static final String ITEM_TYPE_INCORRECT_CIS = "INCORRECT_CIS";
    private static final Set<AnomalyType> ANOMALY_TYPES = Collections.singleton(AnomalyType.INCORRECT_REQUIRED_CIS);
    private final long VENDOR = 1559;
    private final int CIS_HANDLE_MODE_ENABLED = 1;

    // Приёмка КИЗов с карготипом required и cisHandleMode == 1
    @RetryableTest
    @DisplayName("Допоставляем ранее не заявленный обязательный КИЗ")
    @ResourceLock("Допоставляем ранее не заявленный обязательный КИЗ")
    public void createAndReceiveInboundWithRequiredMustBeButNotDeclaredCis() {
        final String testNumber = "2";
        final String gtin = testNumber.repeat(RandomUtil.CIS_GTIN_LENGTH);
        final Pair<String, String> cisParts = RandomUtil.generateCis(gtin);
        final String article = SKU_TEMPLATE + testNumber;

        AnomalyReceivingResult anomalyReceivingResult = fullReceiveInboundWithAnomalies(
                article,
                gtin,
                cisParts
        );
        String consignmentId = anomalyReceivingResult.getInboundResultXPath()
                .registry("5")
                .singleAnomalyConsignmentId();
        fullReceiveInboundWithParent(
                article,
                gtin,
                cisParts,
                anomalyReceivingResult.getReceivedInboundExternalId(),
                anomalyReceivingResult.getReceivedInboundInternalId(),
                consignmentId,
                anomalyReceivingResult.getContainerWithAnomalies()
        );
    }

    @Step("Производим поставку товара")
    private AnomalyReceivingResult fullReceiveInboundWithAnomalies(String article,
                                                                   String ean,
                                                                   Pair<String, String> cisParts) {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistryWithCargotypeAndCis(
                inbound,
                VENDOR,
                article,
                1,
                ean,
                Cis.CIS_CARGO_TYPE_REQUIRED,
                null,
                CIS_HANDLE_MODE_ENABLED,
                0,
                0
        );

        final String pallet = processSteps.Incoming().placeInboundToPallet(inbound.getFulfillmentId());
        final String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveItem(Item.builder()
                                .vendorId(VENDOR)
                                .article(ean)
                                .name("Item with CIS")
                                .checkCis(1)
                                .anomalyTypes(ANOMALY_TYPES)
                                .instances(Map.of("CIS", cisParts.getRight()))
                                .build(),
                        pallet,
                        cartId,
                        true)
                .closePallet();
        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        processSteps.Incoming()
                .placeAndConsolidateAnomaly(
                        cartId,
                        containerId,
                        anomalyPlacementLoc,
                        areaKey);

        processSteps.Incoming()
                .approveCloseInbound(inbound.getFulfillmentId());

        XmlPath getInboundResponse = ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());

        ApiSteps.Inbound().checkGetInbound(getInboundResponse, ITEM_TYPE_INCORRECT_CIS, cisParts.getLeft());

        return AnomalyReceivingResult.builder()
                .receivedInboundExternalId(inbound.getYandexId())
                .receivedInboundInternalId(inbound.getPartnerId())
                .containerWithAnomalies(containerId)
                .inboundResultXPath(new InboundResultXPathProcessor(getInboundResponse))
                .build();
    }

    @Step("Производим допоставку товара")
    private void fullReceiveInboundWithParent(String article,
                                              String ean,
                                              Pair<String, String> cisParts,
                                              long parentInboundExternalId,
                                              String parentInboundInternalId,
                                              String consignmentIdToReceive,
                                              String sourceContainerId) {
        final Inbound additionalInbound = ApiSteps.Inbound()
                .putAdditionalInbound(parentInboundExternalId,
                        parentInboundInternalId,
                        InboundType.ADDITIONAL);

        ApiSteps.Inbound().putAdditionalInboundRegistry(
                "wms/servicebus/putInboundRegistry/putInboundRegistryAdditional.xml",
                String.valueOf(additionalInbound.getYandexId()),
                additionalInbound.getPartnerId(),
                "1559",
                article,
                consignmentIdToReceive,
                cisParts.getLeft(),
                ean);

        final String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.RCP);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving()
                .receiveItem(Item.builder()
                                .vendorId(VENDOR)
                                .article(ean)
                                .name("Item with CIS")
                                .checkCis(1)
                                .instances(Map.of("CIS", cisParts.getRight()))
                                .build(),
                        sourceContainerId,
                        cartId,
                        false)
                .closePallet();
        processSteps.Incoming()
                .approveCloseInbound(additionalInbound.getFulfillmentId());
    }

    @Data
    @Builder
    private static class AnomalyReceivingResult {
        String containerWithAnomalies;
        long receivedInboundExternalId;
        String receivedInboundInternalId;
        InboundResultXPathProcessor inboundResultXPath;
    }

    static class Constants {
        static final String REGISTRY_COLLECTION_PATH =
                "root.response.registries.registry";

        static final String REGISTRY_PATH =
                REGISTRY_COLLECTION_PATH + "[registryIndex]";

        static final String REGISTRY_TYPES_PATH =
                REGISTRY_COLLECTION_PATH + ".registryType";

        static final String ITEMS_COLLECTION_PATH =
                REGISTRY_PATH + ".items.item";

        static final String ANY_ITEM_COMPOSITE_IDS_PATH =
                ITEMS_COLLECTION_PATH + ".unitInfo.compositeId.partialIds.partialId";

        static final String ANY_ITEM_COMPOSITE_ID_PATH =
                ANY_ITEM_COMPOSITE_IDS_PATH + "[partialIdIndex]";

        static final String ANY_ITEM_COMPOSITE_ID_VALUE_PATH =
                ANY_ITEM_COMPOSITE_ID_PATH + ".value";

        static final String TYPE_ID_REL_PATH = ".idType";
    }

    private static class InboundResultXPathProcessor {


        private final XmlPath inboundXml;

        private InboundResultXPathProcessor(XmlPath inboundXml) {
            this.inboundXml = inboundXml;
        }

        public RegistryXPathProcessor registry(String registryType) {
            int registryIndexByType = findKeyIndexByType(inboundXml, registryType, REGISTRY_TYPES_PATH);
            XmlPath registryXPath = inboundXml.param("registryIndex", registryIndexByType);
            return new RegistryXPathProcessor(registryXPath);
        }
    }

    private static class RegistryXPathProcessor {

        private final XmlPath registryXPath;

        RegistryXPathProcessor(XmlPath registryXPath) {
            this.registryXPath = registryXPath;
        }

        public String singleAnomalyConsignmentId() {
            int consignmentIdIndex = findKeyIndexByType(
                    registryXPath,
                    "CONSIGNMENT_ID",
                    ANY_ITEM_COMPOSITE_IDS_PATH + TYPE_ID_REL_PATH);
            return registryXPath
                    .param("partialIdIndex", consignmentIdIndex)
                    .get(ANY_ITEM_COMPOSITE_ID_VALUE_PATH);
        }
    }

    private static int findKeyIndexByType(XmlPath xmlPath, String key, String collectionSubpath) {
        List<String> list = xmlPath.getList(collectionSubpath);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(key)) {
                return i;
            }
        }
        throw new RuntimeException();
    }
}
