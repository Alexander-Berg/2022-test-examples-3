package ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.tests.transfer;


import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.qatools.properties.PropertyLoader;
import ru.qatools.properties.Resource;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.dto.FFApiStockType;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.step.FFWfApiSteps;
import ru.yandex.market.delivery.deliveryintegrationtests.ffwfapi.tests.AbstractWMSTest;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Stock;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.step.api.ApiSteps;

@Resource.Classpath("wms/test.properties")

@DisplayName("FF Workflow API Transfer Test")
public class TransferSurplusToFitTest extends AbstractWMSTest {
    private final Logger log = LoggerFactory.getLogger(TransferSurplusToFitTest.class);

    private final FFWfApiSteps ffWfApiSteps = new FFWfApiSteps();

    private final Item wmsItem = Item.builder().sku("TRANSFER-AUTOTEST2").vendorId(10345562)
            .article("TRANSFER-AUTOTEST2").quantity(2).build();
    private final Long RND_WH_ID = 147L;

    private Long inboundId;
    private String ffInboundId;

    @BeforeEach
    @Step("Создание поставки для трансфера")
    public void setUp() {
        PropertyLoader.newInstance().populate(this);

        inboundId = ffWfApiSteps.createInbound("ffwfapi/tests/transfer/TransferSurplusToFitTest/inbound.json");
        ffInboundId = ffWfApiSteps.waitRequestCreated(inboundId, false);

        wmsSteps.acceptItemAndMoveToPickingCell(ffInboundId, wmsItem);
        wmsSteps.closeInbound(ffInboundId);

        ffWfApiSteps.waitRequestComplete(inboundId);
    }

    @Disabled("Падает приемка поставки в WMS - MARKETFF-4469")
    @Test
    @DisplayName("Проверка флоу трансфера с излишка на годный")
    public void transferSurplusToFitFlowTest() {
        log.info("Starting transferFlowTest...");

        Stock stockBefore = ApiSteps
                .Stocks()
                .getStocks(wmsItem);

        Long transferId = ffWfApiSteps.createTransfer(
                inboundId,
                wmsItem.getArticle(),
                1,
                RND_WH_ID,
                FFApiStockType.SURPLUS,
                FFApiStockType.FIT,
                wmsItem.getVendorId());

        ffWfApiSteps.waitRequestCreated(transferId, false);

        ffWfApiSteps.waitRequestComplete(transferId);

        ffWfApiSteps.verifyTransferDetails(transferId, 1);

        Stock stockAfter = ApiSteps
                .Stocks()
                .getStocks(wmsItem);

        ApiSteps
                .Stocks()
                .verifyItemTransferedFromSurplusToFit(stockBefore, stockAfter);
    }
}
