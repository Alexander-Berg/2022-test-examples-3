package ru.yandex.market.logistic.api.model.fulfillment.request;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class PushStocksRequestParsingTest extends ParsingWrapperTest<RequestWrapper, PushStocksRequest> {

    public PushStocksRequestParsingTest() {
        super(RequestWrapper.class, PushStocksRequest.class, "fixture/request/push_stocks_request.xml");
    }

    @Override
    protected Map<String, Object> fieldValues() {
        return ImmutableMap.<String, Object>builder()
            .put("hash", "36fc8f6373206300cd2d3350611cc50c")
            .build();
    }

    @Override
    protected void performAdditionalAssertions(RequestWrapper requestWrapper) {
        List<ItemStocks> itemStocksList = ((PushStocksRequest) requestWrapper.getRequest()).getItemStocksList();

        assertions().assertThat(itemStocksList)
            .as("Asserting that item stocks list has two itemStocks")
            .hasSize(2);
    }
}
