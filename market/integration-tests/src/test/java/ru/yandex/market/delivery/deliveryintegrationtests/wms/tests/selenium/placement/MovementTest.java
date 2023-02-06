package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.placement;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import ru.qatools.properties.Property;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;


@DisplayName("Selenium: Placement - свободное перемещение")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class MovementTest extends AbstractUiTest {

    @Property("wms.ui.receiving.table")
    private String table;

    @Property("wms.ui.receiving.printer")
    private String printer;

    private Inbound inbound;
    private final Long VENDOR_ID = 1559L;
    private final String ITEM_ARTICLE = UniqueId.getString();
    private Item ITEM;
    private String areaKey;
    private String pickingZone;
    private String cart;
    private String storageCell;
    private String cell2Move;
    private String emptyContainer;

    @BeforeEach
    @Step("Подготовка: Создаем участок и ячейку хранения ")
    public void setUp() throws Exception {
        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зоны отбора и упаковки
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        storageCell = DatacreatorSteps.Location().createPickingCell(pickingZone);
        cell2Move = DatacreatorSteps.Location().createPickingCell(pickingZone);

        emptyContainer = DatacreatorSteps.Label().createContainer(ContainerIdType.L);
    }

    @AfterEach
    @Step("Проверяем что после перемещения тара пустая")
    public void tearDown(){
        uiSteps.Login().PerformLogin();
        uiSteps.Balances().checkNznIsEmpty(cart);

        uiSteps.Login().PerformLogin();
        uiSteps.Balances().checkNznIsEmpty(emptyContainer);
    }

    @RetryableTest
    @DisplayName("Приёмка товара в дефолтную тару с последующим перемещением товара тарой")
    @ResourceLock("Приёмка товара в дефолтную тару с последующим перемещением товара тарой")
    void placeItemCartToCell() {
        ITEM = Item.builder()
                .sku(ITEM_ARTICLE)
                .vendorId(1559)
                .article(ITEM_ARTICLE)
                .quantity(1)
                .build();

        cart = processSteps.Incoming().acceptItemsToCart(ITEM);
        uiSteps.Login().PerformLogin();
        uiSteps.Placement().moveContainer(cart, storageCell);
    }

    @RetryableTest
    @DisplayName("Поуитный набор в задание и перемещение тарой")
    @ResourceLock("Поуитный набор в задание и перемещение тарой")
    void placeItemUITToCell() {
        ITEM = Item.builder()
                .sku(ITEM_ARTICLE)
                .vendorId(1559)
                .article(ITEM_ARTICLE)
                .quantity(1)
                .build();

        cart = processSteps.Incoming().acceptItemsToCart(ITEM);

        uiSteps.Login().PerformLogin();
        List<String> snList = uiSteps.Balances().findByNznAndSupSku(ITEM.getArticle(), cart);

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeUIT(cart, storageCell, snList.get(0));
        uiSteps.Login().PerformLogin();
        uiSteps.Placement().goToMovementMenu().moveUIT(emptyContainer, cell2Move, snList);
    }

    @RetryableTest
    @DisplayName("Поуитный набор и перемещение")
    @ResourceLock("Поуитный набор и перемещение")
    void placeItemUITToCellByUIT() {
        ITEM = Item.builder()
                .sku(ITEM_ARTICLE)
                .vendorId(1559)
                .article(ITEM_ARTICLE)
                .quantity(1)
                .build();

        cart = processSteps.Incoming().acceptItemsToCart(ITEM);

        uiSteps.Login().PerformLogin();
        List<String> snList = uiSteps.Balances().findByNznAndSupSku(ITEM.getArticle(), cart);

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeUIT(cart, storageCell, snList.get(0));
        uiSteps.Login().PerformLogin();
        uiSteps.Placement().moveUITByPieces(emptyContainer, cell2Move, snList);
    }

}
