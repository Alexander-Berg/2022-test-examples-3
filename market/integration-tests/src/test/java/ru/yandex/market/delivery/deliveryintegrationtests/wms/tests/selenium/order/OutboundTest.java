package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.PropertyLoader;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.DropId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Outbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.OutboundStatus;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;

@DisplayName("Selenium: Outbound")
@Epic("Selenium Tests")
@Slf4j
public class OutboundTest extends AbstractUiTest {

    private final String ITEM_ARTICLE = UniqueId.getString();
    private final Item ITEM = Item.builder()
            .sku(ITEM_ARTICLE)
            .vendorId(1559)
            .article(ITEM_ARTICLE)
            .quantity(1)
            .build();

    private Outbound outbound;

    private String areaKey;
    private String pickingZone;
    private String shippingZone;
    private String storageCell;
    private String shippingCell;
    private String shippingDoor;
    private String droppingCell;

    @BeforeEach
    @Step("Подготовка: Создаем участок, выдаем разрешения," +
            "проверяем, что нет запущеных отборов и принимаем товар для заказа")
    public void setUp() {
        PropertyLoader.newInstance().populate(this);

        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зона отбора
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        shippingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);

        // ячейки отбора, консолидации и стол упаковки
        storageCell = DatacreatorSteps.Location().createPickingCell(pickingZone);

        //ячейка дропинга
        droppingCell = DatacreatorSteps.Location().createDroppingCell(RECEIVING_AND_SHIPPING_DOCK);

        // ячейки для отгрузки
        shippingDoor = DatacreatorSteps.Location().createShippingDoor(shippingZone);
        shippingCell = DatacreatorSteps.Location().createShippingWithdrawalCell(shippingZone);

        processSteps.Incoming().acceptItemsAndPlaceThemToPickingCell(ITEM, storageCell);
    }

    @AfterEach
    @Step("Очистка данных после теста")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(storageCell);
        DatacreatorSteps.Location().deleteCell(droppingCell);
        DatacreatorSteps.Location().deleteCell(shippingCell);
        DatacreatorSteps.Location().deleteCell(shippingDoor);

        // удаление зон
        DatacreatorSteps.Location().deletePutawayZone(pickingZone);
        DatacreatorSteps.Location().deletePutawayZone(shippingZone);

        // удаление участка
        DatacreatorSteps.Location().deleteArea(areaKey);

        if (outbound != null) {
            wsSteps.Login().PerformLogin();
            wsSteps.Outbound().cancelReservationforOutbound(outbound);
        }
    }

    @Disabled("Функционал выключен на складах")
    @RetryableTest
    @Tag("DroppingReleaseSuite")
    @Tag("ShippingReleaseSuite")
    @DisplayName("Тест изъятия: годный товар")
    @ResourceLock("Тест изъятия: годный товар")
    public void outboundTest() {
        outbound = ApiSteps.Outbound().createOutbound(ITEM);

        wsSteps.Login().PerformLogin();
        wsSteps.Outbound().startOutboundComplectation(outbound.getFulfillmentId());

        ApiSteps.Outbound().verifyOutboundStatus(outbound, OutboundStatus.ASSEMBLING);

        ParcelId parcelId = DatacreatorSteps.Label().createParcel();

        processSteps.Outgoing().pickWithdrawalAssignment(areaKey, parcelId.getId());

        uiSteps.Login().PerformLogin();

        DropId dropId = uiSteps.Order().deliverySorting(parcelId, droppingCell);

        ApiSteps.Outbound().verifyOutboundStatus(outbound, OutboundStatus.ASSEMBLED);

        processSteps.Outgoing().shipWithdrawals(outbound, dropId, shippingDoor, shippingCell);
    }
}
