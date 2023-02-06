package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.transportation;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.ZoneConfig;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.ReceivingConveyor;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

import static ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType.OTHER;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType.PICK;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType.PLACEMENT_BUF;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType.T_IN_BUF;
import static ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.datacreator.location.LocationType.T_OUT_BUF;

@DisplayName("Selenium: Работа с ТОТами конвейера")
@Epic("Selenium Tests")
public class RecreateTransportOrderTest extends AbstractUiTest {

    private String totId;
    private String conveyorExpensiveZone;
    private String conveyorFirstFloorZone;
    private String conveyorDefaultZone;
    private String conveyorSecondFloorZone;
    private String placementZone;
    private String placementBufForFirstFloor;
    private String placementBufForSecondFloor;
    private String mezFirstBuf;
    private String tInBuf;
    private String tInTransporter;
    private String tOutBuf;
    private String tOutTransporter;

    private final ReceivingConveyor receivingConveyor = new ReceivingConveyor();

    @BeforeEach
    @Step("Подготовка: Создаем участок, зоны, необходимые ячейки и тару")
    public void setUp() throws Exception {
        totId = DatacreatorSteps.Label().createContainer(ContainerIdType.TM);
        conveyorExpensiveZone = createZoneConfig(areaKey, List.of("EXPENSIVE"));
        conveyorFirstFloorZone = createZoneConfig(areaKey, List.of("FIRST_FLOOR"));
        conveyorDefaultZone = createZoneConfig(areaKey, List.of("DEFAULTZONE"));
        conveyorSecondFloorZone = createZoneConfig(areaKey, List.of("SECOND_FLOOR"));

        placementZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        tOutBuf = DatacreatorSteps.Location().createConveyorLocation(putawayZone, T_OUT_BUF, null);
        placementBufForFirstFloor = DatacreatorSteps.Location().createConveyorLocation(conveyorFirstFloorZone, PLACEMENT_BUF, tOutBuf);
        tInBuf = DatacreatorSteps.Location().createConveyorLocation(placementZone, T_IN_BUF, null);
        tInTransporter = DatacreatorSteps.Location().createConveyorLocation(putawayZone, PICK, tInBuf);
        tOutTransporter = DatacreatorSteps.Location().createConveyorLocation(putawayZone, PICK, tOutBuf);
        mezFirstBuf = DatacreatorSteps.Location().createConveyorLocation(conveyorFirstFloorZone, OTHER, tInBuf);
        placementBufForSecondFloor = DatacreatorSteps.Location().createConveyorLocation(conveyorSecondFloorZone, PLACEMENT_BUF, tOutBuf);

    }

    private String createZoneConfig(String area, List<String> types) {
        String putawayZone = DatacreatorSteps.Location().createPutawayZone(area);
        ZoneConfig config = new ZoneConfig(putawayZone, true, 100, types);
        receivingConveyor.createZoneConfig(config);
        return putawayZone;
    }

    @AfterEach
    @Step("Удаление созданных для теста данных")
    public void tearDown() {
        DatacreatorSteps.Location().deleteCell(placementBufForFirstFloor);
        DatacreatorSteps.Location().deleteCell(placementBufForSecondFloor);
        DatacreatorSteps.Location().deleteCell(mezFirstBuf);
        DatacreatorSteps.Location().deleteCell(tOutTransporter);
        DatacreatorSteps.Location().deleteCell(tInTransporter);
        DatacreatorSteps.Location().deleteCell(tOutBuf);
        DatacreatorSteps.Location().deleteCell(tInBuf);
        DatacreatorSteps.Location().deletePutawayZone(placementZone);
        receivingConveyor.deleteZoneConfig(conveyorExpensiveZone);
        receivingConveyor.deleteZoneConfig(conveyorFirstFloorZone);
        receivingConveyor.deleteZoneConfig(conveyorDefaultZone);
        receivingConveyor.deleteZoneConfig(conveyorSecondFloorZone);
        DatacreatorSteps.Location().deletePutawayZone(conveyorExpensiveZone);
        DatacreatorSteps.Location().deletePutawayZone(conveyorFirstFloorZone);
        DatacreatorSteps.Location().deletePutawayZone(conveyorDefaultZone);
        DatacreatorSteps.Location().deletePutawayZone(conveyorSecondFloorZone);
    }

    @Disabled("Выключено до починки MARKETWMS-11192")
    @RetryableTest
    @DisplayName("Создание транспортного ордера в НОК для тары без ранее созданного ТО")
    @ResourceLock("Создание транспортного ордера в НОК для тары без ранее созданного ТО")
    public void receivingNokWithoutTransportOrder() {
        uiSteps.Login().PerformLogin();
        uiSteps.Nok().recreateTransportOrderForReceiving(inboundTable.getStageCell(), totId);
    }

    @Disabled("Выключено до починки MARKETWMS-11192")
    @RetryableTest
    @DisplayName("Создание транспортного ордера из зоны мезонина")
    @ResourceLock("Создание транспортного ордера из зоны мезонина")
    public void receivingNokWithoutTransportOrderFromMezonin() {
        uiSteps.Login().PerformLogin();
        uiSteps.Nok().recreateTransportOrderFromMezonin(placementBufForFirstFloor, totId);
    }

}
