package ru.yandex.market.fulfillment.stockstorage;

import java.io.File;
import java.nio.charset.StandardCharsets;

import Market.DataCamp.SyncAPI.SyncChangeOffer;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.googlecode.protobuf.format.JsonFormat;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeStocksAmountExecutionQueueConsumer;
import ru.yandex.market.fulfillment.stockstorage.service.logbroker.GenericLogbrokerEvent;
import ru.yandex.market.fulfillment.stockstorage.service.logbroker.LogBrokerInteractionException;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.group.StocksWarehouseGroupCache;
import ru.yandex.market.logbroker.LogbrokerService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/system_property.xml")
@DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/stocks_state.xml")
@DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/feed_id_mapping_state.xml")
public class SkuChangeStocksAmountExecutionQueueConsumerTest extends AbstractContextualTest {

    @Autowired
    private SkuChangeStocksAmountExecutionQueueConsumer changeStocksAmountConsumer;

    @Autowired
    private LogbrokerService marketIndexerLogbrokerService;
    @Autowired
    private StocksWarehouseGroupCache stocksWarehouseGroupCache;

    @BeforeEach
    void loadCache() {
        stocksWarehouseGroupCache.reload();
    }

    /**
     * ???????? ???1: ???????????????? ???????????????????????? ?????????????? ?????????????????? ?? LB ???? ??????????????????????
     * <p>
     * - ?? execution_queue - 3 ???????????? ?????? ??????????????, ?????????????????? ?????? ????????????????????:
     * 1. fit.amount - fit.freezed = 0, preorder.amount - preorder.freezed > 0
     * 2. fit.amount - fit.freezed > 0, preorder.amount - preorder.freezed > 0
     * 3. fit stocks = null, preorder.amount - preorder.freezed > 0
     * - ?????? ???????? ?????????????? ???????????????????????? feedId ??????????????
     *
     * <p>
     * ??????????????????, ?????? ?????????? ????????????????????:
     * - execution_queue ?????????? ??????????
     * - ?? LB ?????????? ???????????????????? ?????????????????????? ?? ???????????????????? ?????????????????????? ?????????????????????????? ????????????
     * ?????? ???????? 3-?? ?????????????? (
     * sku0 - 9
     * sku1 - 900
     * sku2 - 90
     * )
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/1/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_stocks_amount_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeSkusForPreorder() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_stocks_amount_consumer/1/change_offer_request.json");
    }

    /**
     * ???????? ???2: ???????????????? ???????????????????????? ?????????????? ?????????????????? ?? LB ???? ??????????????
     * <p>
     * - ?? execution_queue - 2 ???????????? ?????? ??????????????, ?????????????????? ?????? ????????????:
     * 1. fit.amount - fit.freezed > 0, preorder stocks = null
     * 2. fit.amount - fit.freezed > 0, preorder.amount=0
     * - ?????? ?????????? ?????????????? ???????????????????????? feedId ??????????????
     *
     * <p>
     * ??????????????????, ?????? ?????????? ????????????????????:
     * - execution_queue ?????????? ??????????
     * - ?? LB ?????????? ???????????????????? ?????????????????????? ?? ???????????????????? ?????????????????????? ????????????:
     * sku4 - 900
     * sku10 - 900
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/2/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_stocks_amount_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeSkusForOrder() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_stocks_amount_consumer/2/change_offer_request.json");
    }

    /**
     * ???????? ???3: ???????????????? ???????????????????????? ?????????????? ?????????????????? ?? LB ???? ?????????????????????? ????????????????
     * <p>
     * - ?? execution_queue - 3 ???????????? ?????? ??????????????, ?????????????????????? ?????? ???????????? ?? 3-???? ??????????????:
     * [is_available = false, is_preorder = true],
     * [is_available = false, is_preorder = false (via preorder stock amount = 0 and )]
     * [is_available = false, is_preorder = false (via sku disabled)]
     * - ?????? ???????? ?????????????? ???????????????????????? feedId ??????????????
     * ???????????????????????? ???????????? ?????????? ???? ???????????? ???? ????????????????
     * <p>
     * ??????????????????, ?????? ?????????? ????????????????????:
     * - execution_queue ?????????? ??????????
     * - ?? LB ?????????? ???????????????????? ?????????????????????? ?? ?????????????????????? ????????????????????
     * ?????? ??????????????:
     * sku3 - 0
     * sku5 - 0
     * sku6 - 1
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/3/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_stocks_amount_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnUnavailable() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_stocks_amount_consumer/3/change_offer_request.json");
    }

    /**
     * ???????? ???4: ???????????????? ?????????????? ?? ????????????, ???????? ?? execution_queue ?????????????????? ???????????? ?? ?????????????????????? SKU
     * <p>
     * - ?? execution_queue - 2 ????????????:
     * 1. ?????? ???????????? ???????????????????????? ?????? ????????????
     * 2. ?????? ???????????? ?? ?????????????????????? SKU
     * - ?????? ???????? ?????????????? ???????????????????????? feedId ??????????????
     *
     * <p>
     * ??????????????????, ?????? ?????????? ????????????????????:
     * - execution_queue ?????????? ???????? ???????????? ?????? ???????????? ?? ?????????????????????? SKU
     * - ?? LB ?????????? ???????????????????? ?????????????????????? ?? ?????????????????????? ????????????????????
     * ?????? ???????????? ????????????: sku3 - 0
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/4/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_stocks_amount_consumer/4/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnUnknownSku() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_stocks_amount_consumer/4/change_offer_request.json");
    }

    /**
     * ???????? ???7: ???????????????? ?????????????? ?? ????????????, ???????? ?????? ?????????????????? ?????????????????? ????
     * execution_queue ?????????? ?????????????? ????????????????????
     * <p>
     * - ?? execution_queue - 2 ????????????:
     * 1. ?????? ???????????? ???????????????????????? ?????? ????????????
     * 2. ?????? ???????????? ?? ?????????????????????? ???? feedId
     *
     * <p>
     * ??????????????????, ?????? ?????????? ???????????????????? ?????? ???????????? ?????????? ???????????????????????????? ?? execution_queue
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/7/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_stocks_amount_consumer/7/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeWithExceptionThrownFromLogBroker() throws Exception {

        doThrow(new LogBrokerInteractionException("Error")).when(marketIndexerLogbrokerService).publishEvent(any());

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_stocks_amount_consumer/7/change_offer_request.json");
    }

    /**
     * ???????? ???8:
     * - execution_queue - ????????????
     *
     * <p>
     * ??????????????????, ?????? ???????????????????????????? ?? LB ???? ????????????????????
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/8/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_stocks_amount_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnEmptyQueue() {
        changeStocksAmountConsumer.consume();
        verify(marketIndexerLogbrokerService, times(0)).publishEvent(any());
    }

    /**
     * ???????? ???11: ???????????????? ???????????????????? ?????????????????????? ?????????? ????????
     * ENABLE_RTY_NOTIFICATIONS_ABOUT_STOCKS_AMOUNT_CHANGING = false.
     * <p>
     * - ?? execution_queue - 2 ???????????? ?????? ??????????????, ?????????????????? ?????? ????????????????????
     * - ?????? ???????? ?????????????? ???????????????????????? feedId ??????????????. ?????? ?????????????? ???????????? - ???????? ??????, ?????? ?????????????? - 2.
     *
     * <p>
     * ??????????????????, ?????? ?????????? ????????????????????:
     * - execution_queue ???? ????????????????????
     * - ?? LB ???????????? ???? ????????????????????
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/11/system_property.xml")
    @DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/11/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_stocks_amount_consumer/11/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void tryToConsumeWithDisabledRtyNotificationFlag() {

        changeStocksAmountConsumer.consume();
        verifyNoMoreInteractions(marketIndexerLogbrokerService);
    }

    /**
     * ???????? ???12: ???????????????? ???????????????? ???????????????????? DEFECT, EXPIRED, QUARANTINE ????????????
     * <p>
     * - ?? execution_queue - 2 ???????????? ?????? ??????????????
     * - ?????? ???????????? ???????? PREORDER ??????????, ?????? ?????????????? ???????????? DEFECT, EXPIRED, QUARANTINE
     * - ?????? ???????? ?????????????? ???????????????????????? feedId ??????????????.
     *
     * <p>
     * ??????????????????, ?????? ?????????? ????????????????????:
     * - execution_queue ?????????? ??????????
     * - ?? LB ?????????? ???????????????????? ?????????????????????? ?? ?????????????????????? ????????????????????
     * ?????? ??????????????:
     * sku1 - 900
     * sku11 - 0 - ?????????? ?? DEFECT, EXPIRED, QUARANTINE ???? ??????????????????????
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/12/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_stocks_amount_consumer/12/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void tryToConsumeWithNotCountedStocks() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_stocks_amount_consumer/12/change_offer_request" +
                ".json");
    }

    /**
     * ???????? ???13: ???????????????? ???????????????????????? ?????????????? ?????????????????? ???? ?????????????????? ????????????
     * <p>
     * - ?? execution_queue - 2 ???????????? ?????? ??????????????, ?????????????????? ?????? ????????????:
     * 1. sku4 ???? ?????????????? ???????????? ????????????
     * 2. sku5 ???? ???? ?????????????? ???????????? ????????????
     * - ?????? ?????????? ?????????????? ???????????????????????? feedId ??????????????
     *
     * <p>
     * ??????????????????, ?????? ?????????? ????????????????????:
     * - execution_queue ?????????? ??????????
     * - ?? LB ?????????? ???????????????????? ?????????????????????? ?? ???????????????????? ?????????????????????? ????????????:
     * sku4: ???? ???????????? ?????????????? 145, 147 - 900
     * sku5: ???? 145
     * </p>
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/change_stocks_amount_consumer/13/queue_state.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
            value = "classpath:database/expected/change_stocks_amount_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testAmountForGroup() throws Exception {
        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest(
                "classpath:database/expected/change_stocks_amount_consumer/13/change_offer_request.json");
    }


    private void verifyLogBrokerRequest(String pathToChangeOfferRequest) throws Exception {
        ArgumentCaptor<GenericLogbrokerEvent<SyncChangeOffer.ChangeOfferRequest>> argument =
                ArgumentCaptor.forClass(GenericLogbrokerEvent.class);
        Mockito.verify(marketIndexerLogbrokerService,
                Mockito.times(1)).publishEvent(argument.capture()
        );

        String expected = readRequestFromJson(pathToChangeOfferRequest);
        String actual =
                JsonFormat.printToString(SyncChangeOffer.ChangeOfferRequest.parseFrom(argument.getValue().getBytes()));
        JSONAssert.assertEquals(expected, actual, false);
    }

    private String readRequestFromJson(String pathToJson) throws Exception {
        File file = ResourceUtils.getFile(pathToJson);
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }
}
