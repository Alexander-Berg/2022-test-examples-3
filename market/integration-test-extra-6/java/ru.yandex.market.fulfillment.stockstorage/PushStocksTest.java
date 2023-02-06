package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.service.helper.IdGenerator;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.queue.PushStocksEventExecutionQueueConsumer;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.DefaultStockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.validator.StockUpdateValidator;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("classpath:database/states/system_property.xml")
public class PushStocksTest extends AbstractContextualTest {

    public static final String STOCKS_URL = "/stocks";

    @SpyBean
    private DefaultStockUpdatingStrategy defaultStockUpdatingStrategy;

    @SpyBean
    private IdGenerator idGenerator;

    @Autowired
    private PushStocksEventExecutionQueueConsumer pushStocksEventExecutionQueueConsumer;

    @Test
    public void pushStocksHasUnitDuplicates() throws Exception {
        String contentAsString = mockMvc.perform(post(STOCKS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/push/with_unit_duplicates.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains(StockUpdateValidator.DUPLICATE_UNIT_IDENTIFIER_ERROR);

        verify(skuEventAuditService, never()).logSkuCreated(any(UnitId.class), any(Long.class));
    }

    @Test
    public void failOnPushWithWrongArticles() throws Exception {
        String contentAsString = mockMvc.perform(post(STOCKS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/push/with_wrong_articles.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains(StockUpdateValidator.BLANK_ARTICLE_ERROR.substring(2))
                .contains(StockUpdateValidator.EXTRA_WHITESPACES_ERROR.substring(2));

        verify(skuEventAuditService, never()).logSkuCreated(any(UnitId.class), any(Long.class));
    }

    @Test
    public void pushStocksWithUndefinedSource() throws Exception {
        String contentAsString = mockMvc.perform(post(STOCKS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/push/with_undefined_source.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("\"source\":\"must not be null\"");

        verify(skuEventAuditService, never()).logSkuCreated(any(UnitId.class), any(Long.class));
    }

    @Test
    public void pushStocksHasStockTypeDuplicates() throws Exception {
        String contentAsString = mockMvc.perform(post(STOCKS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/push/with_stock_type_duplicates.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("has duplicated stock types", "sku0")
                .doesNotContain("sku1");

        verify(skuEventAuditService, never()).logSkuCreated(any(UnitId.class), any(Long.class));
    }

    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/pushed_fit_one_warehouse.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushStocksFitSuccessfulOnEmptyDatabase() {
        pushStocks("requests/push/normal_push_fit_stocks_one_warehouse.json", status().is2xxSuccessful());


        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(skuEventAuditService, times(2)).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku0", 12L, 1)), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku1", 12L, 1)), any(Long.class));
        verify(stockEventsHandler, times(3)).handle(anyList());
    }

    @Test
    // никаких записей в бд и вызовов логгирования не должно произойти
    public void pushStocksAllFailed() {
        doThrow(new RuntimeException("weee"))
                .when(defaultStockUpdatingStrategy)
                .doUpdateStocks(anyList(), anyMap());

        pushStocks("requests/push/normal_push_fit_stocks_one_warehouse.json", status().is2xxSuccessful());

        verify(skuEventAuditService, never()).logNewStockAmountPushed(anyList());
        verify(skuEventAuditService, never()).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(stockEventsHandler, never()).handle(anyList());
    }


    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/pushed_fit_first_failed_none_rollbacked.xml",
            assertionMode = NON_STRICT_UNORDERED)
    /*
    * так как работа выполняется батчами, ожидается, что если для одного warehouseId валится хотя бы один сток, все
    * остальные стоки также откатываются и апдейт не выполняется.
    * */
    public void pushStocksFirstFailed() {
        doThrow(new RuntimeException("weee"))
                .doCallRealMethod()
                .when(defaultStockUpdatingStrategy)
                .doUpdateStocks(anyList(), anyMap());

        doReturn("d720999c-19f7-495f-915e-067f910ffdb5")
                .doReturn("d720999c-19f7-495f-915e-067f910ffdb6")
                .when(idGenerator).get();

        pushStocks("requests/push/normal_push_fit_stocks_one_warehouse.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(skuEventAuditService, never())
                .logSkuCreated(eq(new UnitId("sku0", 12L, 1)), any(Long.class));
        verify(skuEventAuditService, times(1))
                .logSkuCreated(eq(new UnitId("sku1", 12L, 1)), any(Long.class));
        verify(stockEventsHandler, times(2)).handle(anyList());
    }

    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/pushed_all_types.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushStocksAllSuccessful() {
        pushStocks("requests/push/normal_push_all_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(skuEventAuditService, times(2)).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku0", 12L, 1)), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku1", 12L, 1)), any(Long.class));
        verify(stockEventsHandler, times(3)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/old_pushed_stocks_not_changed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushOutdatedFitStocksSkippedButRestUpdated() {
        pushStocks("requests/push/normal_push_all_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, never()).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(stockEventsHandler, times(1)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/pushed_defect_availability_change.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushOnlyDefectStock() {
        pushStocks("requests/push/normal_push_defect_single_stock.json", status().is2xxSuccessful());

        verify(skuEventAuditService, never()).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(stockEventsHandler, times(1)).handle(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_unupdatable_one_sku.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/unupdatable_stocks_after_push.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushUnupdatableStocksSkippedButRestUpdated() {
        pushStocks("requests/push/normal_push_all_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
    }

    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/partial_update_stock.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void multiplePushSuccessful() {
        pushStocks("requests/push/normal_push_all_stocks.json", status().is2xxSuccessful());

        pushStocks("requests/push/push_different_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, never()).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService, times(2)).logNewStockAmountPushed(anyList());
        verify(stockEventsHandler, times(2)).handle(anyList());

    }

    /**
     *
     * Для CHANGED_STOCKS_AMOUNT_EVENT таска не появляется, так как количество PREORDER стока не изменилось
     * Для CHANGED_ANY_TYPE_OF_STOCKS_AMOUNT_EVENT появляется таска, так как изменился хотя бы один тип стока (FIT)
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_pushed_disable_one_sku.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/disabled_stocks_after_push.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void successfulPushWithDisabledSku() {
        pushStocks("requests/push/normal_push_all_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, never()).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(stockEventsHandler, times(1)).handle(anyList());

    }

    /**
     * Проверяем пуш только преордер стока на пустой БД.
     * Должен правильно сохраниться.
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/pushed_preorder.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushStocksPreorderSuccessful() {
        pushStocks("requests/push/normal_push_preorder_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(skuEventAuditService, times(2)).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku0", 12L, 1)), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku1", 12L, 1)), any(Long.class));
    }

    /**
     * Проверяем пуш только surplus стока на пустой БД.
     * Должен правильно сохраниться.
     */
    @Test
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/pushed_surplus.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushStocksSurplusSuccessful() {
        pushStocks("requests/push/normal_push_surplus_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
        verify(skuEventAuditService, times(2)).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku0", 12L, 1)), any(Long.class));
        verify(skuEventAuditService).logSkuCreated(eq(new UnitId("sku1", 12L, 1)), any(Long.class));
    }

    /**
     * Проверяем пуш без стоков на пустой БД.
     * Должен правильно сохраниться.
     */
    @Test
    public void pushStocksWithoutStocksClientError() throws Exception {
        String contentAsString = mockMvc.perform(post(STOCKS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/push/normal_push_without_stocks.json")))
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse()
                .getContentAsString();
        softly
                .assertThat(contentAsString)
                .contains("itemStocks[0].stocks" + "\":\"" + "must not be empty",
                        "itemStocks[1].stocks" + "\":\"" + "must not be empty");
    }

    /**
     * В БД есть сток с PREORDER=1 и FIT-FREEZE=0
     * Проверяем пуш FIT > 0.
     * Должен правильно сохраниться.
     * Для CHANGED_STOCKS_AMOUNT_EVENT таска не появляется, так как количество PREORDER стока не изменилось
     * Для CHANGED_ANY_TYPE_OF_STOCKS_AMOUNT_EVENT появляется таска, так как изменился хотя бы один тип стока
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_preorder_and_unavailable_fit.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/preoprder_after_fit_availability_change.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void preorderAvailabilityWhenFitBecomeAvailable() {
        pushStocks("requests/push/normal_push_fit_single_stock.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
    }

    /**
     * В БД есть сток с PREORDER=1 и FIT-FREEZE>0
     * Проверяем пуш FIT-FREEZE <= 0.
     * Должен правильно сохраниться.
     * Для CHANGED_STOCKS_AMOUNT_EVENT таска не появляется, так как количество PREORDER стока не изменилось
     * Для CHANGED_ANY_TYPE_OF_STOCKS_AMOUNT_EVENT появляется таска, так как изменился хотя бы один тип стока
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_preorder_and_unavailable_fit.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/preoprder_after_fit_availability_change.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void preorderAvailabilityWhenFitBecomeUnavailable() {
        pushStocks("requests/push/normal_push_fit_single_stock.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
    }

    /**
     * В БД есть сток с PREORDER=1 и FIT-FREEZE>0
     * Проверяем пуш PREORDER-FREEZE <= 0.
     * Должен правильно сохраниться.
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_preorder_and_available_fit.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/preoprder_become_unavailable.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void preorderAvailabilityWhenPreorderBecomeUnavailable() {
        pushStocks("requests/push/normal_push_single_preorder_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
    }

    /**
     * В БД есть сток с PREORDER-FREEZE=0 и FIT-FREEZE>0
     * Проверяем пуш PREORDER-FREEZE > 0.
     * Должен правильно сохраниться.
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_unavailable_preorder_and_available_fit.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/preoprder_become_available.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void preorderAvailabilityWhenPreorderBecomeAvailable() {
        pushStocks("requests/push/normal_push_single_preorder_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
    }

    /**
     * В БД есть сток с PREORDER-FREEZE<0 и FIT-FREEZE<0
     * Проверяем пуш PREORDER = 0.
     * Должен правильно сохраниться.
     * Должен появиться SKUFILTER
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_unavailable_preorder_and_unavailable_fit.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/empty_preoprder_unavailable_fit.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void stockNotAvailableWhenPreorderIsRemoved() {
        pushStocks("requests/push/zero_push_single_preorder_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
    }


    /**
     * В БД есть сток с PREORDER-FREEZE>0 и FIT-FREEZE>0
     * Проверяем пуш PREORDER = 0.
     * Должен правильно сохраниться.
     * Должен появиться SKUFILTER
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_preorder_and_available_fit.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/empty_preoprder_available_fit.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void stockAvailableWhenPreorderIsRemoved() {
        pushStocks("requests/push/zero_push_single_preorder_stocks.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
    }

    /**
     * Проверяем, что при исполнении последовательных двух запросов на пуш двух идентичных с
     * т.з. комбинации shop_sku + vendor_id товаров, но разных с т.з. source.warehouseId- информация будет сохранена
     * в БД.
     */
    @Test
    @ExpectedDatabase(value = "classpath:database/expected/push/multi_warehouse_push.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void sequentialPushesOnDifferentWarehouses() {
        pushStocks("requests/push/seq_push_1.json", status().is2xxSuccessful());

        pushStocks("requests/push/seq_push_2.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(2)).logSkuCreated(any(UnitId.class), any(Long.class));
        verify(skuEventAuditService, times(2)).logNewStockAmountPushed(anyList());
    }

    /**
     * Проверяем, что при пуше стоков из ростовского склада не произойдет изменений в таблице stock
     * даже с учетом того, что присланное значение ffUpdated более новое, чем то, что в БД.
     */
    //TODO https://st.yandex-team.ru/MARKETWMS-37
    //TODO Отпилить костыль с Ростовским складом как только Infor доделает со своей стороны доработку.
    @Test
    @DatabaseSetup("classpath:database/states/rostov_stocks_pushed.xml")
    @ExpectedDatabase(value = "classpath:database/expected/push/rostov_stocks_pushed.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void rostovWarehouseUpdateIgnored() {
        pushStocks("requests/push/rostov_warehouse_push.json", status().is2xxSuccessful());
    }

    /**
     * Проверяем, что в случае стратегии checkOnlyDate, мы записываем в event_audit инфу только по тем стокам,
     * по которым изменилось кол-во.
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_with_check_only_date_strategy.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/push_stocks_with_partially_updating_event_audit.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushStocksWithPartiallyUpdatingEventAudit() {
        pushStocks("requests/push/push_with_partially_amount_updating.json", status().is2xxSuccessful());

        verify(skuEventAuditService, times(1)).logNewStockAmountPushed(anyList());
    }

    /**
     * Проверяем, что пуш стока на главный склад в группе создает события и обновление только по этому главному складу.
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_update/shared_stocks.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/shared_main_stocks.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushSharedStocksToMainWarehouse() {
        pushStocks("requests/push/shared_main_stocks.json", status().is2xxSuccessful());
    }

    /**
     * Проверяем, что пуш стока на второстепенный склад в группе создает события и обновление по этому
     * второстепенному складу.
     */
    @Test
    @DatabaseSetup("classpath:database/states/stocks_update/shared_stocks.xml")
    @ExpectedDatabase(
            value = "classpath:database/expected/stocks_update/shared_second_stocks.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void pushSharedStocksToSecondaryWarehouse() {
        pushStocks("requests/push/shared_second_stocks.json", status().is2xxSuccessful());
    }

    @SneakyThrows
    public void pushStocks(String request, ResultMatcher responseCodeMatcher) {
        mockMvc.perform(post(STOCKS_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent(request)))
                .andExpect(responseCodeMatcher)
                .andReturn();

        pushStocksEventExecutionQueueConsumer.consume();
    }
}
