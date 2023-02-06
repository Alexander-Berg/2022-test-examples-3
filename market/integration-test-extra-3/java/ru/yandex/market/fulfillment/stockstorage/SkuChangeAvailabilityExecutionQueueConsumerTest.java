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

import ru.yandex.market.fulfillment.stockstorage.service.export.rty.availability.SkuChangeAvailabilityExecutionQueueConsumer;
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

@DatabaseSetup("classpath:database/states/change_availability_consumer/system_property.xml")
@DatabaseSetup("classpath:database/states/change_availability_consumer/stocks_state.xml")
@DatabaseSetup("classpath:database/states/change_availability_consumer/feed_id_mapping_state.xml")
public class SkuChangeAvailabilityExecutionQueueConsumerTest extends AbstractContextualTest {
    @Autowired
    private SkuChangeAvailabilityExecutionQueueConsumer changeAvailabilityConsumer;

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
     * - в LB будет отправлена протобафина с корректными значениями
     * для всех 3-х товаров (disabled = True && "order_method": "PRE_ORDERED")
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_availability_consumer/1/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeSkusForPreorder() throws Exception {

        changeAvailabilityConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_availability_consumer/1/change_offer_request.json");
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
     * - в LB будет отправлена протобафина с корректными значениями
     * для обоих товаров (disabled = True && "order_method": "AVAILABLE_FOR_ORDER")
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_availability_consumer/2/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeSkusForOrder() throws Exception {

        changeAvailabilityConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_availability_consumer/2/change_offer_request.json");
    }

    /**
     * Тест №3: проверка корректности отсылки сообщений в LB по недоступным товараам
     * <p>
     * - в execution_queue - 3 записи для товаров, недоступных для заказа с 3-мя кейсами:
     * [is_available = false, is_preorder = true],
     * [is_available = false, is_preorder = false (via preorder stock amount = 0 and )]
     * [is_available = false, is_preorder = false (via sku disabled)]
     * - для всех товаров присутствует feedId маппинг
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлена протобафина с корректными значениями
     * для товаров (disabled = True && "order_method": none)
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_availability_consumer/3/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnUnavailable() throws Exception {

        changeAvailabilityConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_availability_consumer/3/change_offer_request.json");
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
     * для одного товара (disabled = True && "order_method": "PRE_ORDERED")
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_availability_consumer/4/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_consumer/4/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnUnknownSku() throws Exception {

        changeAvailabilityConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_availability_consumer/4/change_offer_request.json");
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
    @DatabaseSetup("classpath:database/states/change_availability_consumer/7/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_consumer/7/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeWithExceptionThrownFromLogBroker() throws Exception {

        doThrow(new LogBrokerInteractionException("Error")).when(marketIndexerLogbrokerService).publishEvent(any());

        changeAvailabilityConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_availability_consumer/7/change_offer_request.json");
    }

    /**
     * Тест №8:
     * - execution_queue - пустая
     *
     * <p>
     * Проверяем, что взаимодействия с LB не происходит
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_availability_consumer/8/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeOnEmptyQueue() {
        changeAvailabilityConsumer.consume();
        verify(marketIndexerLogbrokerService, times(0)).publishEvent(any());
    }

    /**
     * Тест №10: проверка корректности отсылки сообщений в LB
     * с несколькими фидами для одной пары (vendor_id, warehouse_id).
     * <p>
     * - в execution_queue - 2 записи для товаров, доступных для предзаказа
     * - для всех товаров присутствует feedId маппинг. Для первого товара - один фид, для второго - 2.
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлена протобафина с 3-мя сообщениями.
     * </p>
     */
    @Test
    @DatabaseSetup("classpath:database/states/change_availability_consumer/10/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void consumeSkusWithMultiFeedIdsMapping() throws Exception {

        changeAvailabilityConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_availability_consumer/10/change_offer_request.json");
    }

    /**
     * Тест №11: проверка отключения консьюмеров через флаг
     * ENABLE_RTY_NOTIFICATIONS_ABOUT_SKU_AVAILABILITY_CHANGING = false.
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
    @DatabaseSetup("classpath:database/states/change_availability_consumer/11/system_property.xml")
    @DatabaseSetup("classpath:database/states/change_availability_consumer/11/queue_state.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_consumer/11/queue_state.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void tryToConsumeWithDisabledRtyNotificationFlag() {

        changeAvailabilityConsumer.consume();
        verifyNoMoreInteractions(marketIndexerLogbrokerService);
    }

    /**
     * Тест №12: проверка корректной отсылки сообщений в LB для групп складов
     * <p>
     * - в execution_queue - 2 записи для товаров, доступных для заказа:
     * 1. sku4 на главном складе группы
     * 2. sku5 на не главном складе группы
     * - для обоих товаров присутствует feedId маппинг
     *
     * <p>
     * Проверяем, что после выполнения:
     * - execution_queue будет пуста
     * - в LB будет отправлено два события для каждого склада из группы для sku4 и событие для неглавного склада
     * для sku5
     * </p>
     */
    @Test
    @DatabaseSetup(value = "classpath:database/states/change_availability_consumer/12/queue_state.xml",
            type = DatabaseOperation.INSERT)
    @ExpectedDatabase(
            value = "classpath:database/expected/change_availability_consumer/empty_queue.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void testGroup() throws Exception {
        changeAvailabilityConsumer.consume();
        verifyLogBrokerRequest("classpath:database/expected/change_availability_consumer/12/change_offer_request.json");
    }

    private void verifyLogBrokerRequest(String pathToChangeOfferRequest) throws Exception {
        ArgumentCaptor<GenericLogbrokerEvent<SyncChangeOffer.ChangeOfferRequest>> argument =
                ArgumentCaptor.forClass(GenericLogbrokerEvent.class);
        Mockito.verify(marketIndexerLogbrokerService, Mockito.times(1)).publishEvent(argument.capture());

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
