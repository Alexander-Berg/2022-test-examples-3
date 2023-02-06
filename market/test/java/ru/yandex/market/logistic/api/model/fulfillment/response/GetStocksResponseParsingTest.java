package ru.yandex.market.logistic.api.model.fulfillment.response;

import java.util.List;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class GetStocksResponseParsingTest extends ParsingWrapperTest<ResponseWrapper, GetStocksResponse> {

    public GetStocksResponseParsingTest() {
        super(ResponseWrapper.class, GetStocksResponse.class, "fixture/response/get_stocks_response.xml");
    }

    @Override
    protected void performAdditionalAssertions(ResponseWrapper responseWrapper) {
        List<ItemStocks> itemStocksList = ((GetStocksResponse) responseWrapper.getResponse()).getItemStocksList();

        assertions().assertThat(itemStocksList)
            .as("Asserting that item stocks list has two itemStocks")
            .hasSize(2);
    }
}
