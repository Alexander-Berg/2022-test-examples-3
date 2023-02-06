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


@DisplayName("Selenium: Placement - новое размещение")
@Epic("Selenium Tests")
@Resource.Classpath({"wms/wms.properties"})
@Slf4j
public class PlacementTest extends AbstractUiTest {

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

    @BeforeEach
    @Step("Подготовка: Создаем участок и ячейку хранения ")
    public void setUp() throws Exception {
        // участок
        areaKey = DatacreatorSteps.Location().createArea();

        // зоны отбора и упаковки
        pickingZone = DatacreatorSteps.Location().createPutawayZone(areaKey);
        storageCell = DatacreatorSteps.Location().createPickingCell(pickingZone);
    }

    @AfterEach
    @Step("Проверяем что после размещения тара пустая")
    public void tearDown(){
        uiSteps.Login().PerformLogin();
        uiSteps.Balances().checkNznIsEmpty(cart);
    }

    @RetryableTest
    @DisplayName("Приёмка товара в дефолтную тару с последующим размещением товара тарой")
    @ResourceLock("Приёмка товара в дефолтную тару с последующим размещением товара тарой")
    void placeItemCartToCell() {
        ITEM = Item.builder()
                .sku(ITEM_ARTICLE)
                .vendorId(1559)
                .article(ITEM_ARTICLE)
                .quantity(1)
                .build();

        cart = processSteps.Incoming().acceptItemsToCart(ITEM);
        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(cart, storageCell);

    }

    @RetryableTest
    @DisplayName("Приёмка товара в дефолтную тару с последующим размещением товара УИТно")
    @ResourceLock("Приёмка товара в дефолтную тару с последующим размещением товара УИТно")
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
    }

}
