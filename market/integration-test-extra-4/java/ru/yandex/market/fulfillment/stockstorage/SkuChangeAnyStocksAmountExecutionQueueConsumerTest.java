package ru.yandex.market.fulfillment.stockstorage;

import java.io.File;
import java.nio.charset.StandardCharsets;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;

import ru.yandex.market.fulfillment.stockstorage.service.export.rty.stocks.SkuChangeAnyStocksAmountExecutionQueueConsumer;
import ru.yandex.market.fulfillment.stockstorage.service.logbroker.GenericLogbrokerEvent;
import ru.yandex.market.fulfillment.stockstorage.service.logbroker.LogBrokerInteractionException;
import ru.yandex.market.logbroker.LogbrokerService;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/system_property.xml")
@DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/stocks_state.xml")
@DatabaseSetup("classpath:database/states/change_stocks_amount_consumer/feed_id_mapping_state.xml")
public class SkuChangeAnyStocksAmountExecutionQueueConsumerTest extends AbstractContextualTest {

    @Autowired
    private SkuChangeAnyStocksAmountExecutionQueueConsumer changeStocksAmountConsumer;

    @Autowired
    private LogbrokerService fulfillmentLogbrokerService;

    /**
     * Тест №1: проверка корректности отсылки сообщений в LB
     * <p>
     * - в execution_queue - 3 записи для товаров
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будут отправлены данные с корректным количеством стоков для всех 3-х товаров (
     * sku0: PREORDER = 9
     * sku1: PREORDER = 900, FIT = 0
     * sku2: PREORDER = 90, FIT = 90
     * )
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/1/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_any_stocks_amount_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeSkusForPreorder() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/" +
                "change_any_stocks_amount_consumer/1/change_offer_request.json");
    }

    /**
     * Тест №2: проверка корректности отсылки сообщений в LB по заказам
     * <p>
     * - в execution_queue - 2 записи для товаров
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлены двнные с корректным количеством стоков:
     * sku4: FIT = 900
     * sku10: FIT = 900, PREORDER = 0
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/2/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_any_stocks_amount_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeSkusForOrder() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/" +
                "change_any_stocks_amount_consumer/2/change_offer_request.json");
    }

    /**
     * Тест №3: проверка корректности отсылки сообщений в LB по недоступным товараам
     * <p>
     * - в execution_queue - 3 записи для товаров, недоступных для заказа с 3-мя кейсами:
     * [is_available = false, is_preorder = true],
     * [is_available = false, is_preorder = false (via preorder stock amount = 0 and )]
     * [is_available = false, is_preorder = false (via sku disabled)]
     * Дооступность товара никак не влияет на выгрузку
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будут отправлены данные с корректными значениями
     * для товаров:
     * sku3: FIT = 0, PREORDER = 0
     * sku5: FIT = 0, PREORDER = 0
     * sku6: FIT = 1, PREORDER = 900
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/3/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_any_stocks_amount_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnUnavailable() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/" +
                "change_any_stocks_amount_consumer/3/change_offer_request.json");
    }

    /**
     * Тест №4: Проверка ретраев в случае, если в execution_queue оказалась запись с неизвестным SKU
     * <p>
     * - в execution_queue - 2 записи:
     * 1. Для товара недоступного для заказа
     * 2. Для товара с неизвестным SKU
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет одна запись для товара с неизвестным SKU
     * - в LB будет отправлены данные для одного товара:
     * sku3: FIT=0, PREORDER=0
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/4/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_any_stocks_amount_consumer/4/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnUnknownSku() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/" +
                "change_any_stocks_amount_consumer/4/change_offer_request.json");
    }

    /**
     * Тест №5: Проверка ретраев в случае, если при обработке сообщений из
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
    @DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/5/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_any_stocks_amount_consumer/5/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeWithExceptionThrownFromLogBroker() throws Exception {

        doThrow(new LogBrokerInteractionException("Error")).when(fulfillmentLogbrokerService).publishEvent(any());

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/" +
                "change_any_stocks_amount_consumer/5/change_offer_request.json");
    }

    /**
     * Тест №6:
     * - execution_queue - пустая
     *
     * <p>
     * Проверяем, что взаимодействия с LB не происходит
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/6/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_any_stocks_amount_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnEmptyQueue() {
        changeStocksAmountConsumer.consume();
        verify(fulfillmentLogbrokerService, times(0)).publishEvent(any());
    }

    /**
     * Тест №7: проверка отключения консьюмеров через флаг
     * ENABLE_RTY_NOTIFICATIONS_ABOUT_ANY_TYPES_OF_STOCKS_AMOUNT_CHANGING = false.
     * <p>
     * - в execution_queue - 2 записи для товаров
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue не изменилась
     * - в LB ничего не отправляли
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/7/system_property.xml")
    @DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/7/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_any_stocks_amount_consumer/7/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void tryToConsumeWithDisabledRtyNotificationFlag() {

        changeStocksAmountConsumer.consume();
        verifyNoMoreInteractions(fulfillmentLogbrokerService);
    }

    /**
     * Тест №8: проверка отправки количества DEFECT, EXPIRED, QUARANTINE стоков
     * <p>
     * - в execution_queue - 2 записи для товаров
     * - Для одного есть PREORDER стоки,
     * - Для второго DEFECT, EXPIRED, QUARANTINE
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлены данные с корректными значениями для товаров:
     * sku1: FIT = 0, PREORDER = 900
     * sku11: DEFECT = 6, EXPIRED = 15, QUARANTINE = 9
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_any_stocks_amount_consumer/8/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_any_stocks_amount_consumer/8/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void tryToConsumeWithNotCountedStocks() throws Exception {

        changeStocksAmountConsumer.consume();
        verifyLogBrokerRequest("" +
                "classpath:database/expected/change_any_stocks_amount_consumer/8/change_offer_request.json");
    }

    private void verifyLogBrokerRequest(String pathToChangeOfferRequest) throws Exception {
        ArgumentCaptor<GenericLogbrokerEvent<String>> argument = ArgumentCaptor.forClass(GenericLogbrokerEvent.class);
        Mockito.verify(fulfillmentLogbrokerService,
                Mockito.times(1)).publishEvent(argument.capture()
        );

        String expected = readRequestFromJson(pathToChangeOfferRequest);
        String actual = argument.getValue().getPayload();
        JSONAssert.assertEquals(expected, actual, false);
    }

    private String readRequestFromJson(String pathToJson) throws Exception {
        File file = ResourceUtils.getFile(pathToJson);
        return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
    }

}
