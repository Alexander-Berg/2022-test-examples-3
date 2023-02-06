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
     * Тест №1: проверка корректности отсылки сообщений в LB по предзаказам
     * <p>
     * - в execution_queue - 3 записи для товаров, доступных для предзаказа:
     * 1. fit.amount - fit.freezed = 0, preorder.amount - preorder.freezed > 0
     * 2. fit.amount - fit.freezed > 0, preorder.amount - preorder.freezed > 0
     * 3. fit stocks = null, preorder.amount - preorder.freezed > 0
     * - для всех товаров присутствует feedId маппинг
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлена протобафина с корректным количеством предхзаказных стоков
     * для всех 3-х товаров (
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
     * Тест №2: проверка корректности отсылки сообщений в LB по заказам
     * <p>
     * - в execution_queue - 2 записи для товаров, доступных для заказа:
     * 1. fit.amount - fit.freezed > 0, preorder stocks = null
     * 2. fit.amount - fit.freezed > 0, preorder.amount=0
     * - для обоих товаров присутствует feedId маппинг
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлена протобафина с корректным количеством стоков:
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
     * Тест №3: проверка корректности отсылки сообщений в LB по недоступным товараам
     * <p>
     * - в execution_queue - 3 записи для товаров, недоступных для заказа с 3-мя кейсами:
     * [is_available = false, is_preorder = true],
     * [is_available = false, is_preorder = false (via preorder stock amount = 0 and )]
     * [is_available = false, is_preorder = false (via sku disabled)]
     * - для всех товаров присутствует feedId маппинг
     * Дооступность товара никак не влияет на выгрузку
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлена протобафина с корректными значениями
     * для товаров:
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
     * Тест №4: Проверка ретраев в случае, если в execution_queue оказалась запись с неизвестным SKU
     * <p>
     * - в execution_queue - 2 записи:
     * 1. Для товара недоступного для заказа
     * 2. Для товара с неизвестным SKU
     * - для всех товаров присутствует feedId маппинг
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет одна запись для товара с неизвестным SKU
     * - в LB будет отправлена протобафина с корректными значениями
     * для одного товара: sku3 - 0
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
     * Тест №7: Проверка ретраев в случае, если при обработке сообщений из
     * execution_queue будет брошено исключение
     * <p>
     * - в execution_queue - 2 записи:
     * 1. Для товара недоступного для заказа
     * 2. Для товара с неизвестным по feedId
     *
     * <p>
     * Проверяем, что после выполнения обе записи будут перевыставлены в execution_queue
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
     * Тест №8:
     * - execution_queue - пустая
     *
     * <p>
     * Проверяем, что взаимодействия с LB не происходит
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
     * Тест №11: проверка отключения консьюмеров через флаг
     * ENABLE_RTY_NOTIFICATIONS_ABOUT_STOCKS_AMOUNT_CHANGING = false.
     * <p>
     * - в execution_queue - 2 записи для товаров, доступных для предзаказа
     * - для всех товаров присутствует feedId маппинг. Для первого товара - один фид, для второго - 2.
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue не изменилась
     * - в LB ничего не отправляли
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
     * Тест №12: проверка отправки количества DEFECT, EXPIRED, QUARANTINE стоков
     * <p>
     * - в execution_queue - 2 записи для товаров
     * - Для одного есть PREORDER стоки, для второго только DEFECT, EXPIRED, QUARANTINE
     * - для всех товаров присутствует feedId маппинг.
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлена протобафина с корректными значениями
     * для товаров:
     * sku1 - 900
     * sku11 - 0 - стоки в DEFECT, EXPIRED, QUARANTINE не учитываются
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
     * Тест №13: проверка корректности отсылки сообщений об изменении стоков
     * <p>
     * - в execution_queue - 2 записи для товаров, доступных для заказа:
     * 1. sku4 на главном складе группы
     * 2. sku5 на не главном складе группы
     * - для обоих товаров присутствует feedId маппинг
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлена протобафина с корректным количеством стоков:
     * sku4: на группу складов 145, 147 - 900
     * sku5: на 145
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
