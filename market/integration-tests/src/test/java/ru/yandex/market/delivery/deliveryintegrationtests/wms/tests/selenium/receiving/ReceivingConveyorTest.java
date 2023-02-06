package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.receiving;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ReceivingStation;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ZoneConfig;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving.BarcodeInputPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.ReceivingConveyor;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.logistic.api.model.common.InboundType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType.PICK;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType.PLACEMENT_BUF;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType.T_IN_BUF;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType.T_OUT_BUF;

@DisplayName("Selenium: Receiving Conveyor")
@Epic("Selenium Tests")
public final class ReceivingConveyorTest extends AbstractUiTest {
    public static final String CREATE_INBOUND_XML = "wms/servicebus/putInboundRegistry/putInboundRegistry.xml";
    private String fromPalletId;
    private String toPalletId;
    private String totId;
    private String flipboxId;
    private String conveyorExpensiveZone;
    private String conveyorFirstFloorZone;
    private String conveyorDefaultZone;
    private String placementZone;
    private String placementBuf;
    private String tInBuf;
    private String tInTransporter;
    private String tOutBuf;
    private String tOutTransporter;
    private final String ITEM_ARTICLE = UniqueId.getString();
    private final Item ITEM = Item.builder()
            .vendorId(1559)
            .article(ITEM_ARTICLE)
            .name("Item Name")
            .build();

    private final ReceivingConveyor receivingConveyor = new ReceivingConveyor();

    @BeforeEach
    @Step("Подготовка данных")
    public void setUp() {
        var inbound = ApiSteps.Inbound().putInbound(InboundType.DEFAULT);
        ApiSteps.Inbound().putInboundRegistry(CREATE_INBOUND_XML,
                inbound,
                ITEM_ARTICLE,
                1559,
                false
        );
        fromPalletId = processSteps.Incoming().placeInboundToPallet(inbound.getFulfillmentId());
        toPalletId = DatacreatorSteps.Label().createContainer(ContainerIdType.PLT);
        totId = DatacreatorSteps.Label().createContainer(ContainerIdType.TM);
        flipboxId = DatacreatorSteps.Label().createContainer(ContainerIdType.RCP);

        conveyorExpensiveZone = createZoneConfig(areaKey, List.of("EXPENSIVE"));
        conveyorFirstFloorZone = createZoneConfig(areaKey, List.of("FIRST_FLOOR"));
        conveyorDefaultZone = createZoneConfig(areaKey, List.of("DEFAULTZONE"));

        placementZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        tOutBuf = DatacreatorSteps.Location().createConveyorLocation(putawayZone, T_OUT_BUF, null);
        placementBuf = DatacreatorSteps.Location().createConveyorLocation(conveyorFirstFloorZone, PLACEMENT_BUF,
                tOutBuf);
        tInBuf = DatacreatorSteps.Location().createConveyorLocation(placementZone, T_IN_BUF, null);
        tInTransporter = DatacreatorSteps.Location().createConveyorLocation(putawayZone, PICK, tInBuf);
        tOutTransporter = DatacreatorSteps.Location().createConveyorLocation(putawayZone, PICK, tOutBuf);
    }

    private String createZoneConfig(String area, List<String> types) {
        String putawayZone = DatacreatorSteps.Location().createPutawayZone(area);
        ZoneConfig config = new ZoneConfig(putawayZone, true, 100, types);
        receivingConveyor.createZoneConfig(config);
        return putawayZone;
    }

    @AfterEach
    @Step("Очистка данных")
    public void tearDown() {
        disableConveyorTable(inboundTable.getStageCell());
        DatacreatorSteps.Location().deleteCell(placementBuf);
        DatacreatorSteps.Location().deleteCell(tOutTransporter);
        DatacreatorSteps.Location().deleteCell(tInTransporter);
        DatacreatorSteps.Location().deleteCell(tOutBuf);
        DatacreatorSteps.Location().deleteCell(tInBuf);
        DatacreatorSteps.Location().deletePutawayZone(placementZone);
        receivingConveyor.deleteZoneConfig(conveyorExpensiveZone);
        receivingConveyor.deleteZoneConfig(conveyorFirstFloorZone);
        receivingConveyor.deleteZoneConfig(conveyorDefaultZone);
        DatacreatorSteps.Location().deletePutawayZone(conveyorExpensiveZone);
        DatacreatorSteps.Location().deletePutawayZone(conveyorFirstFloorZone);
        DatacreatorSteps.Location().deletePutawayZone(conveyorDefaultZone);
    }

    @RetryableTest
    @DisplayName("Приемка без вложенности")
    @ResourceLock("Приемка без вложенности")
    public void receiveWithoutFlipbox() {
        enableConveyorTable(inboundTable.getStageCell(), false);
        uiSteps.Login().PerformLogin();
        BarcodeInputPage barcodeInputPage = uiSteps.Receiving()
                .receivingOnConveyor(ITEM, fromPalletId, totId);
        uiSteps.Receiving().putItemOnConveyor(barcodeInputPage, totId);
    }

    @Disabled("Выключено до починки MARKETWMS-11192")
    @RetryableTest
    @DisplayName("Приемка c вложенностью")
    @ResourceLock("Приемка c вложенностью")
    public void receiveWithFlipbox() {
        enableConveyorTable(inboundTable.getStageCell(), true);
        uiSteps.Login().PerformLogin();
        BarcodeInputPage barcodeInputPage = uiSteps.Receiving()
                .receivingOnConveyorWithNesting(ITEM, fromPalletId, flipboxId, totId);
        uiSteps.Receiving().putItemOnConveyor(barcodeInputPage, totId);
    }

    private void enableConveyorTable(String table, boolean nestingEnabled) {
        ReceivingStation stationWithoutNesting = new ReceivingStation(
                table, true, "CHOOSE", nestingEnabled
        );
        receivingConveyor.createReceivingStation(stationWithoutNesting);
    }

    private void disableConveyorTable(String table) {
        receivingConveyor.deleteReceivingStation(table);
    }

}
