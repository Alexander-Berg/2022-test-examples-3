package ru.yandex.market.checkout.pushapi.web;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.IOUtils;
import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.entity.StocksResponse;
import ru.yandex.market.checkout.pushapi.client.entity.stock.Stock;
import ru.yandex.market.checkout.pushapi.client.entity.stock.StockItem;
import ru.yandex.market.checkout.pushapi.client.entity.stock.StockType;
import ru.yandex.market.checkout.pushapi.helpers.PushApiGetStocksHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiQueryStocksParameters;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.util.PushApiTestSerializationService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetStocksTest extends AbstractWebTestBase {

    @Autowired
    private TestableClock clock;

    @Autowired
    private PushApiGetStocksHelper getStocksHelper;
    @Autowired
    private PushApiTestSerializationService pushApiTestSerializationService;

    @Test
    public void shouldGetStocksViaJSON() throws Exception {
        PushApiQueryStocksParameters parameters = new PushApiQueryStocksParameters(
                clock, (params) -> pushApiTestSerializationService.serializeJson(params.getResponse()));
        parameters.setDataType(DataType.JSON);
        mockSettingsForDifferentParameters(parameters);
        StocksResponse response = getStocksHelper.queryStocks(parameters, status().isOk());
        checkSuccessfulResponse(response);

        Assertions.assertThat(getStocksHelper.getServeEvents())
                .hasSize(1)
                .allSatisfy(event -> Assertions.assertThat(event.getRequest())
                        .returns("{\"warehouseId\":123456,\"partnerWarehouseId\":\"123456\",\"skus\":[\"asdasd\"]}",
                                LoggedRequest::getBodyAsString));
    }

    @Test
    public void shouldGetStocksViaJsonMoreThan10Seconds() throws Exception {
        PushApiQueryStocksParameters parameters = new PushApiQueryStocksParameters(
                clock, (params) -> pushApiTestSerializationService.serializeJson(params.getResponse()));
        parameters.setDataType(DataType.JSON);
        parameters.setResponseDelay(20000);
        mockSettingsForDifferentParameters(parameters);
        StocksResponse response = getStocksHelper.queryStocksTimeout(parameters, status().isOk());
        checkSuccessfulResponse(response);
    }

    @Test
    public void shouldValidateGetStockResponse() throws Exception {

        PushApiQueryStocksParameters parameters = new PushApiQueryStocksParameters(clock,
                PushApiQueryStocksParameters::getContent);
        String invalidJson = IOUtils.readInputStream(GetStocksTest.class
                .getResourceAsStream("/files/invalidStockResponse.json"));
        parameters.setContent(invalidJson);
        parameters.setDataType(DataType.JSON);
        mockSettingsForDifferentParameters(parameters);
        getStocksHelper.queryStocks(parameters, status().isUnprocessableEntity());
    }

    @Test
    public void deserializeStocksResponseWithNumberValues() throws Exception {
        String responseJson = IOUtils.readInputStream(GetStocksTest.class
                .getResourceAsStream("/files/stocksResponseWithNumbers.json"));

        PushApiQueryStocksParameters parameters = new PushApiQueryStocksParameters(clock,
                PushApiQueryStocksParameters::getContent);
        parameters.setContent(responseJson);
        parameters.setDataType(DataType.JSON);
        mockSettingsForDifferentParameters(parameters);
        StocksResponse response = getStocksHelper.queryStocks(parameters, status().isOk());

        checkSuccessfulResponse(response);
    }

    @Test
    public void deserializeStocksResponseWithStringValues() throws Exception {
        String responseJson = IOUtils.readInputStream(GetStocksTest.class
                .getResourceAsStream("/files/stocksResponseWithStrings.json"));


        PushApiQueryStocksParameters parameters = new PushApiQueryStocksParameters(clock,
                PushApiQueryStocksParameters::getContent);
        parameters.setContent(responseJson);
        parameters.setDataType(DataType.JSON);
        mockSettingsForDifferentParameters(parameters);
        StocksResponse response = getStocksHelper.queryStocks(parameters, status().isOk());

        checkSuccessfulResponse(response);
    }

    private void checkSuccessfulResponse(StocksResponse response) {
        assertThat(response.getSkus(), hasSize(1));

        Stock stock = response.getSkus().get(0);
        assertThat(stock.getWarehouseId(), is(123456L));
        assertThat(stock.getSku(), is("asdasd"));
        assertThat(stock.getItems(), hasSize(1));

        StockItem stockItem = stock.getItems().get(0);
        assertThat(stockItem.getCount(), is(1));
        assertThat(stockItem.getType(), is(StockType.FIT));
        assertThat(stockItem.getUpdatedAt(), notNullValue());
    }
}
