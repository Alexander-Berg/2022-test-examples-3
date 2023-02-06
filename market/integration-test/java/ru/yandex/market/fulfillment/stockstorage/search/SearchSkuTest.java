package ru.yandex.market.fulfillment.stockstorage.search;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.fulfillment.stockstorage.AbstractContextualTest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.validator.ValidSearchSkuFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchRestClient.SEARCH;
import static ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchRestClient.SKU;

public class SearchSkuTest extends AbstractContextualTest {

    /**
     * Сценарий #1
     * <p>
     * В SS уходит пустой запрос на поиск sku.
     * В БД SS нету информации о каких-либо SKU.
     * <p>
     * В ответ должен вернуться ответ с дефолтными значениями пагинации и фильтров.
     */
    @Test
    public void emptyRequestOnEmptyDatabase() throws Exception {
        executePositiveScenario("1");
    }

    /**
     * Сценарий #2
     * <p>
     * В SS уходит пустой запрос на поиск sku.
     * В БД SS есть информация о нескольких (сильно меньше, чем дефолтный limit в пагинации) SKU и их состояния стоков.
     * <p>
     * В ответ должна вернуться эта информация.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/2.xml")
    public void emptyRequestOnFilledDatabase() throws Exception {
        executePositiveScenario("2");
    }

    /**
     * Сценарий #3.1
     * <p>
     * В SS уходит запрос с пагинацией (limit=2/offset=0).
     * <p>
     * В ответ должны вернуться первые два ску с id возрастающем порядке.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/3.xml")
    public void firstPaginationOnFilledDatabase() throws Exception {
        executePositiveScenario("3.1");
    }

    /**
     * Сценарий #3.2
     * <p>
     * В SS уходит запрос с пагинацией (limit=2/offset=2).
     * <p>
     * В ответ должны вернуться последующие два ску с id возрастающем порядке.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/3.xml")
    public void secondPaginationOnFilledDatabase() throws Exception {
        executePositiveScenario("3.2");
    }

    /**
     * Сценарий #4.3
     * <p>
     * В SS уходит запрос без фильтра по lifetime.
     * <p>
     * В ответ должны вернуться все СКУ независимо от их значения lifetime.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/4.xml")
    public void searchByHasLifetimeNotSet() throws Exception {
        executePositiveScenario("4.3");
    }

    /**
     * Сценарий #5.1
     * <p>
     * В SS уходит запрос со значением фильтра updatable = true.
     * <p>
     * В ответ должны вернуться все СКУ, с updatable = true.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/5.xml")
    public void searchByUpdatableIsTrue() throws Exception {
        executePositiveScenario("5.1");
    }

    /**
     * Сценарий #5.2
     * <p>
     * В SS уходит запрос со значением фильтра updatable = false.
     * <p>
     * В ответ должны вернуться все СКУ, с updatable = false.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/5.xml")
    public void searchByUpdatableIsFalse() throws Exception {
        executePositiveScenario("5.2");
    }

    /**
     * Сценарий #5.3
     * <p>
     * В SS уходит запрос со значением фильтра updatable = false.
     * <p>
     * В ответ должны вернуться все СКУ, с updatable = false.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/5.xml")
    public void searchByUpdatableNotSet() throws Exception {
        executePositiveScenario("5.3");
    }

    /**
     * Сценарий #6.1
     * <p>
     * В SS уходит запрос со значением фильтра enabled = true
     * <p>
     * В ответ должны вернуться все СКУ, со значением enabled = true.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/6.xml")
    public void searchByEnabledIsTrue() throws Exception {
        executePositiveScenario("6.1");
    }

    /**
     * Сценарий #6.2
     * <p>
     * В SS уходит запрос со значением фильтра enabled = false
     * <p>
     * В ответ должны вернуться все СКУ, со значением enabled = false.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/6.xml")
    public void searchByEnabledIsFalse() throws Exception {
        executePositiveScenario("6.2");
    }

    /**
     * Сценарий #6.3
     * <p>
     * В SS уходит запрос со значением фильтра enabled = false
     * <p>
     * В ответ должны вернуться все СКУ, со значением enabled = false.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/6.xml")
    public void searchByEnabledNotSet() throws Exception {
        executePositiveScenario("6.3");
    }

    /**
     * Сценарий #7
     * <p>
     * В SS уходит запрос с фильтром по значению vendorId
     * <p>
     * В ответ должны быть возвращены все ску, у которых совпадает значение vendorId.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/7.xml")
    public void searchByVendorId() throws Exception {
        executePositiveScenario("7");
    }

    /**
     * Сценарий #8
     * <p>
     * В SS уходит запрос с фильтром по значению warehouseId
     * <p>
     * В ответ должны быть возвращены все ску, у которых совпадает значение warehouseId.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/8.xml")
    public void searchByWarehouseId() throws Exception {
        executePositiveScenario("8");
    }

    /**
     * Сценарий #9
     * <p>
     * В SS отправляется запрос, который ищет по совпадению связок warehouseId/vendorId
     * <p>
     * В ответ должны вернуться только те SKU,
     * у которых связка vendorId/warehouseId совпадает со значениями из запроса
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/9.xml")
    public void searchByVendorAndWarehouseIds() throws Exception {
        executePositiveScenario("9");
    }

    /**
     * Сценарий #10
     * <p>
     * В SS отправляется запрос, который ищет одновременно по связке warehouseId/vendorId
     * + оставшиеся фильтры (кроме unitIds).
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/10.xml")
    public void searchByWarehouseVendorAndOtherFilters() throws Exception {
        executePositiveScenario("10");
    }

    /**
     * Сценарий #11
     * <p>
     * В SS отправляется запрос, который ищет по набору unitIds.
     * <p>
     * В ответ должны вернуться только те sku, которые имеют идентичные unitId.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/11.xml")
    public void searchByUnitIds() throws Exception {
        executePositiveScenario("11");
    }

    /**
     * Сценарий #12
     * <p>
     * В SS отправляется запрос, который ищет по набору unitIds
     * + оставшиеся фильтры (кроме vendorId/warehouseId).
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/12.xml")
    public void searchByUnitIdsAndOtherFilters() throws Exception {
        executePositiveScenario("12");
    }

    /**
     * Сценарий #13
     * <p>
     * В SS отправляется запрос, который ищет по набору unitIds
     * + оставшиеся фильтры (кроме vendorId/warehouseId) + пагинация.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/13.xml")
    public void searchByUnitIdsAndOtherFiltersAndPagination() throws Exception {
        executePositiveScenario("13");
    }

    /**
     * Сценарий #14
     * <p>
     * В SS отправляется запрос, в котором одновременно задействованны и фильтр.
     * <p>
     * В ответ должна вернуться ошибка с текстом, сообщающим о том, что эти фильтры взаимоисключающие.
     */
    @Test
    public void searchByUnitIdsAndWarehouseIdVendorId() throws Exception {
        String actualJson = mockMvc.perform(post(SEARCH + SKU)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/sku/search/14.json")))
                .andExpect(status().is4xxClientError())
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly.assertThat(actualJson)
                .as("Assert that response contains error")
                .containsIgnoringCase(ValidSearchSkuFilter.ERROR_MESSAGE);
    }

    /**
     * Сценарий #17
     * <p>
     * В SS отправляется запрос, который ищет по набору unitIds с большим offset. В результате приходит пустой ответ.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/17.xml")
    public void searchWithTooBigOffset() throws Exception {
        executePositiveScenario("17");
    }

    /**
     * Сценарий #18
     * <p>
     * Пропуск Sku у которых пустой Stock включён
     * В ответе только Sku с хотя бы одним ненулевым стоком.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/18.xml")
    public void searchWithSkippingOfSkuWithEmptyStocks() throws Exception {
        executePositiveScenario("18");
    }

    /**
     * Сценарий #19
     * <p>
     * Пропуск Sku у которых пустой Stock и фильтрация по типу стока
     * В ответе только Sku с ненулевым стоком заданного типа.
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/19.xml")
    public void searchWithSkippingOfSkuWithEmptyStocksAndFilteringByType() throws Exception {
        executePositiveScenario("19");
    }

    /**
     * Сценарий #20
     * <p>
     * В запросе указан как offset так и offsetId, а также тип стока
     * Т.к. указан offsetId, в ответе получаем также lastId
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/20.xml")
    public void searchWithOffsetAndOffsetIdSimultaneously() throws Exception {
        executePositiveScenario("20");
    }

    /**
     * Сценарий #21
     * <p>
     * Пропусков пустых стоков включен (есть джойн), фильтра по типу нет
     * Смещение задано offset и offsetId
     * Проверяем что смещение работает на уровне Sku а не отдельных стоков
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/21.xml")
    public void searchWithSkippingAndCheckPaging() throws Exception {
        executePositiveScenario("21");
    }

    /**
     * Сценарий #22
     * <p>
     * Всё так же как в сценарии #21, плюс выставлен флаг ненужности count операции
     * Проверяем что позвращается -1 в totalAmount
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/22.xml")
    public void searchWithoutCountTotal() throws Exception {
        executePositiveScenario("22");
    }

    /**
     * Сценарий #23
     * <p>
     * Поиск всех офферов склада с фризами
     */
    @Test
    @DatabaseSetup("classpath:database/states/sku/search/23.xml")
    public void searchWithFreezeOnly() throws Exception {
        executePositiveScenario("23");
    }

    private void executePositiveScenario(String scenarioNumber) throws Exception {
        String actualJson = mockMvc.perform(post(SEARCH + SKU)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(extractFileContent("requests/sku/search/" + scenarioNumber + ".json")))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse()
                .getContentAsString();

        softly
                .assertThat(actualJson)
                .is(jsonMatchingWithoutOrder(extractFileContent("response/sku/search/" + scenarioNumber + ".json")));
    }
}
