package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.inventorization;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;


@DisplayName("Selenium: Inventorization - Палетная инвентаризация")
@Epic("Selenium Tests")
@Slf4j
public class PalletInventorizationTest extends AbstractUiTest {

    private Inbound inbound;
    private final String ITEM_ARTICLE = UniqueId.getString();
    private String palleteStorageCell;
    private final Integer qty = 20;
    private final Integer countToMove = 1;
    private final Item ITEM = Item.builder()
            .sku(ITEM_ARTICLE)
            .vendorId(1559)
            .article(ITEM_ARTICLE)
            .quantity(qty)
            .build();
    private String pickingCart;
    private String cart2move;
    private String palleteZone = "PLI";
    private String palleteStorageCell2Move;

    @BeforeEach
    @Step("Подготовка: инициализация состояния окружения для теста")
    public void setUp(){
        // ячейки
        palleteStorageCell = DatacreatorSteps.Location().createPalletStorageCell(palleteZone);
        palleteStorageCell2Move = DatacreatorSteps.Location().createPalletStorageCell(palleteZone);

        pickingCart = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
        cart2move = DatacreatorSteps.Label().createContainer(ContainerIdType.CART);
    }

    @AfterEach
    @Step("Очистка данных после теста")
    public void tearDown() {
        // удаление ячеек
        DatacreatorSteps.Location().deleteCell(palleteStorageCell);
        DatacreatorSteps.Location().deleteCell(palleteStorageCell2Move);
    }

    @Step("Создаем поставку товаров: {sku} СГ: {isShelfLife}, и принимаем её на паллету")
    private String createInboundAndPlaceToPallet(String sku, boolean isShelfLife) {
        inbound = ApiSteps.Inbound().createInbound(sku, isShelfLife);
        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().initialReceiveItem(inbound.getFulfillmentId(), 1);
        uiSteps.Login().PerformLogin();
        return uiSteps.Receiving().findPalletOfInbound(inbound.getFulfillmentId());
    }

    @Step("Приёмка товара в дефолтную тару с последующим размещением товара")
    private void receiveItemIntoDefaultCart(Item item) {

        String pallet = createInboundAndPlaceToPallet(item.getArticle(), item.isShelfLife());

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().enterDefaultCart(item, pallet, pickingCart)
                .enterBarcode(item)
                .checkInfo(item)
                .enterQuantity(item.getQuantity());
        uiSteps.Receiving().isBarcodeInputPageDisplayed();

        processSteps.Incoming().closeAndApproveCloseInbound(inbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(pickingCart,palleteStorageCell);
    }

    @Step("Перемещаем товар чтобы палета стала троганой")
    private void makePalletTouched(Item item, String cartId, String cellId, int countToMove){
        uiSteps.Login().PerformLogin();
        List<String> item2move = uiSteps.Balances().findIdCountByNznAndSupSku(item.getSku(), cartId, countToMove);

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().goToMovementMenu().moveUIT(cart2move, cellId, item2move);
    }

    @Disabled("Выключен до починки https://st.yandex-team.ru/MARKETWMS-18083")
    @RetryableTest
    @DisplayName("Палетная инвентаризация нетронутой палеты")
    @ResourceLock("Палетная инвентаризация нетронутой палеты")
    public void palletInventorizationTest() {
        receiveItemIntoDefaultCart(ITEM);
        DatacreatorSteps.Inventorization().createOrGetExistingInventorizationTask(palleteStorageCell);
        ApiSteps.Inventorization().refreshTouchedPallets();
        uiSteps.Login().PerformLogin();
        uiSteps.Inventorization().palletInventorization(palleteStorageCell, pickingCart);
    }

    @Disabled("Выключен до починки https://st.yandex-team.ru/MARKETWMS-18083")
    @RetryableTest
    @DisplayName("Палетная инвентаризация тронутой палеты")
    @ResourceLock("Палетная инвентаризация тронутой палеты")
    public void palletTouchedInventorizationTest() {
        receiveItemIntoDefaultCart(ITEM);
        makePalletTouched(ITEM, pickingCart,palleteStorageCell2Move, countToMove);
        DatacreatorSteps.Inventorization().createOrGetExistingInventorizationTask(palleteStorageCell);
        ApiSteps.Inventorization().refreshTouchedPallets();
        uiSteps.Login().PerformLogin();
        uiSteps.Inventorization().touchedPalletInventorization(palleteStorageCell, pickingCart, qty, countToMove);
    }
}

