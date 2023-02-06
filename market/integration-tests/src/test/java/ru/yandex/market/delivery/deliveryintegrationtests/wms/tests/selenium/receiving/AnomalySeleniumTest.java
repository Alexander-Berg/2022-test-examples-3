package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import io.qameta.allure.Step;
import io.restassured.path.xml.XmlPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

public class AnomalySeleniumTest extends AbstractUiTest {

    protected String anomalyPlacementLoc;
    protected String anomalyPlacementPermissionSK;
    protected String anomalyConsolidationPermissionSK;

    @BeforeEach
    @Step("Подготовка данных: Создание топологии")
    public void setUp() {
        anomalyPlacementLoc = DatacreatorSteps.Location().createAnomalyConsolidationCell(putawayZone);

        anomalyPlacementPermissionSK = DatacreatorSteps
                .Permission()
                .createAnomalyPlacementPermission(user.getLogin(), areaKey);
        anomalyConsolidationPermissionSK = DatacreatorSteps.Permission()
                .createAnomalyConsolidationPermission(user.getLogin(), areaKey);
    }

    @AfterEach
    @Step("Удаление данных: Очистка топологии")
    public void tearDown() {
        DatacreatorSteps.Permission().deletePermission(anomalyConsolidationPermissionSK);
        DatacreatorSteps.Permission().deletePermission(anomalyPlacementPermissionSK);
        DatacreatorSteps.Location().deleteCell(anomalyPlacementLoc);
    }

    @Step("Создаем поставку товаров")
    private Pair<String, Inbound> createInbound(String skuPrefix, String templatePath) {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistry(
                templatePath,
                inbound,
                skuPrefix,
                0,
                false
        );
        String palletId = processSteps.Incoming().placeInboundToPallet(inbound.getFulfillmentId());
        return Pair.of(palletId, inbound);
    }

    void receiveItemOnStock(Item item, String skuPrefix, String templatePath) {
        Pair<String, Inbound> inboundPair = createInbound(skuPrefix, templatePath);
        Inbound inbound = inboundPair.getSecond();
        String pallet = inboundPair.getFirst();
        final String cartId = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(
                item,
                pallet,
                cartId,
                false
        ).closePallet();
        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());
    }

    XmlPath receiveAnomalyItem(Item item, String skuPrefix, String anomalyPlacementLoc,
                               String templatePath, String areaKey) {
        Pair<String, Inbound> inboundPair = createInbound(skuPrefix, templatePath);
        Inbound inbound = inboundPair.getSecond();
        String pallet = inboundPair.getFirst();

        final String anomalyCartId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(item,
                pallet,
                anomalyCartId,
                true
        ).closePallet();
        String containerId = DatacreatorSteps.Label().createContainer(ContainerIdType.AN);
        processSteps.Incoming().placeAndConsolidateAnomaly(
                anomalyCartId,
                containerId,
                anomalyPlacementLoc,
                areaKey);
        processSteps.Incoming().approveCloseInbound(inbound.getFulfillmentId());

        return ApiSteps.Inbound().getInbound(inbound.getYandexId(), inbound.getPartnerId());
    }
}
