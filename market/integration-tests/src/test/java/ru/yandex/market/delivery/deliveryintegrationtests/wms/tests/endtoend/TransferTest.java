package ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.endtoend;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.Property;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;

import ru.yandex.market.delivery.deliveryintegrationtests.tool.UniqueId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Inbound;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Stock;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Transfer;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions.RetryableTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.datacreator.DatacreatorSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.AbstractUiTest;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;

@Resource.Classpath("wms/test.properties")
@DisplayName("EndToEnd: Transfer")
@Epic("EndToEnd Tests")
public class TransferTest extends AbstractUiTest {
    private static final Logger log = LoggerFactory.getLogger(TransferTest.class);
    private final String CART = DatacreatorSteps.Label().createContainer(ContainerIdType.L);

    private Inbound testInbound;

    private final String ITEM_ARTICLE = UniqueId.getString();
    private final Item ITEM = Item.builder().sku(ITEM_ARTICLE).vendorId(1559).article(ITEM_ARTICLE).build();

    @Property("test.storageCell")
    private String cell;

    @BeforeEach
    public void setUp() {
        PropertyLoader.newInstance().populate(this);
        prepareInboundForTransferTest();
    }

    @Step("Создаем поставку с 1 годным товаром для теста трансфера")
    private void prepareInboundForTransferTest() {
        testInbound = ApiSteps.Inbound().createInbound(
                "wms/tests/endtoend/TransferTest/createInbound.xml",
                ITEM.getArticle()
        );

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().initialReceiveItem(testInbound.getFulfillmentId(), 1);
        uiSteps.Login().PerformLogin();
        final String PALLETE =  uiSteps.Receiving().findPalletOfInbound(testInbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Receiving().receiveItem(ITEM, PALLETE, CART);

        processSteps.Incoming().closeAndApproveCloseInbound(testInbound.getFulfillmentId());

        uiSteps.Login().PerformLogin();
        uiSteps.Placement().placeContainer(CART, cell);

        openNewBrowser();
        wsSteps.Login().PerformLogin();
        wsSteps.Items().switchOffManualSetUpRequiredCheckbox(ITEM);
    }

    @Disabled("Отключен до починки https://st.yandex-team.ru/MARKETWMS-11357")
    @RetryableTest
    @DisplayName("Transfer flow")
    @ResourceLock("Transfer flow")
    @Description("Создание трансфера и обратного трансфера с проводкой по статусам")
    public void transferFlowTest() {
        log.info("Testing transfer flow");

        Stock stockBefore = ApiSteps
                .Stocks()
                .getStocks(ITEM);

        Transfer transfer = ApiSteps
                .Transfer()
                .createTransfer(testInbound, ITEM, Stock.Fit, Stock.Surplus, 1);

        ApiSteps
                .Transfer()
                .waitTransferStatusIs(transfer, Transfer.STATUS_COMPLETED);

       Stock stockAfter1 = ApiSteps
                   .Stocks()
                   .getUpdatedStocks(ITEM.getVendorId(),ITEM.getArticle(), stockBefore);

       ApiSteps
                   .Stocks()
                   .verifyItemTransferedFromFitToSurplus(stockBefore, stockAfter1);

        Transfer reverseTransfer = ApiSteps
                .Transfer()
                .createTransfer(testInbound, ITEM, Stock.Surplus, Stock.Fit, 1);

        ApiSteps
                .Transfer()
                .waitTransferStatusIs(reverseTransfer, Transfer.STATUS_COMPLETED);

        Stock stockAfter2 = ApiSteps
                .Stocks()
                .getUpdatedStocks(ITEM.getVendorId(),ITEM.getArticle(), stockAfter1);

        ApiSteps
                .Stocks()
                .verifyItemTransferedFromSurplusToFit(stockAfter1, stockAfter2);
    }
}
