package ru.yandex.market.fulfillment.wrap.marschroute.functional.scenario.concrete;

import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.springframework.web.client.RestTemplate;
import ru.yandex.market.fulfillment.wrap.marschroute.service.common.FulfillmentWarehouseIdProvider;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.request.GetStocksRequest;
import ru.yandex.market.logistic.api.model.fulfillment.response.GetStocksResponse;
import ru.yandex.market.logistic.api.utils.DateTime;

import java.util.List;

import static ru.yandex.market.logistic.api.model.fulfillment.StockType.AVAILABLE;
import static ru.yandex.market.logistic.api.model.fulfillment.StockType.DEFECT;
import static ru.yandex.market.logistic.api.model.fulfillment.StockType.EXPIRED;
import static ru.yandex.market.logistic.api.model.fulfillment.StockType.FIT;
import static ru.yandex.market.logistic.api.model.fulfillment.StockType.QUARANTINE;

public class GetStocksPositiveScenario extends GetStocksBaseScenario {

    private static final DateTime EXPECTED_DATE_TIME = new DateTime("2017-08-22T16:48");
    private final static String RESPONSE_PATH = "functional/get_stocks/positive/response.json";


    public GetStocksPositiveScenario(RestTemplate restTemplate,
                                     String requestUrl) {
        super(RESPONSE_PATH, restTemplate, requestUrl);
    }

    public GetStocksPositiveScenario(String responseFilePath, RestTemplate restTemplate, String requestUrl) {
        super(responseFilePath, restTemplate, requestUrl);
    }

    @Override
    public void doAssertions(SoftAssertions assertions,
                             RequestWrapper<GetStocksRequest> request,
                             ResponseWrapper<GetStocksResponse> response) {

        super.doAssertions(assertions, request, response);

        assertions.assertThat(response.getRequestState().hasErrors())
            .as("Asserting that response has no errors")
            .isFalse();

        List<ItemStocks> itemStocks = response.getResponse().getItemStocksList();

        assertions.assertThat(itemStocks)
            .as("Asserting that item stocks have exactly 1 element")
            .hasSize(1);

        ItemStocks itemStock = itemStocks.get(0);

        UnitId unitId = itemStock.getUnitId();

        assertions.assertThat(unitId.getVendorId())
            .as("Asserting unit id vendor id value")
            .isEqualTo(12);

        assertions.assertThat(unitId.getArticle())
            .as("Asserting unit id article value")
            .isEqualTo("R212");

        assertions.assertThat(unitId.getId())
            .as("Asserting unit id yandex id value")
            .isNull();

        ResourceId warehouseId = itemStock.getWarehouseId();

        assertions.assertThat(warehouseId.getYandexId())
            .as("Asserting warehouse yandex id")
            .isEqualTo(FulfillmentWarehouseIdProvider.YANDEX_WAREHOUSE_ID);

        assertions.assertThat(warehouseId.getPartnerId())
            .as("Asserting warehouse fulfillment id")
            .isEqualTo(FulfillmentWarehouseIdProvider.PARTNER_WAREHOUSE_ID);

        List<Stock> stocks = itemStock.getStocks();

        assertions.assertThat(stocks)
            .as("Asserting that itemStock has info about 4 stocks")
            .hasSize(5);

        assertStock(assertions, stocks.get(0), FIT, 100);
        assertStock(assertions, stocks.get(1), AVAILABLE, 75);
        assertStock(assertions, stocks.get(2), EXPIRED, 20);
        assertStock(assertions, stocks.get(3), QUARANTINE, 0);
        assertStock(assertions, stocks.get(4), DEFECT, 10);
    }

    private void assertStock(SoftAssertions assertions, Stock stock, StockType stockType, int stockCount) {
        assertions.assertThat(stock)
            .is(new Condition<>(s -> (stockType == s.getType() && s.getCount() == stockCount && s.getUpdated().equals(EXPECTED_DATE_TIME)),
                "Asserting that there is stock with type [" + stockType + "]" +
                    " and count [" + stockCount + "]"));
    }
}
