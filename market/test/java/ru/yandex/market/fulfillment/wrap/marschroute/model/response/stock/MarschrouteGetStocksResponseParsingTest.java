package ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.fulfillment.wrap.core.ParsingTest;

import java.util.Map;

class MarschrouteGetStocksResponseParsingTest extends ParsingTest<MarschrouteProductsResponse> {

    MarschrouteGetStocksResponseParsingTest() {
        super(new ObjectMapper(), MarschrouteProductsResponse.class, "get_stock/response.json");
    }

    @Override
    protected void performAdditionalAssertions(MarschrouteProductsResponse response) {
        softly.assertThat(response.isSuccess()).isEqualTo(true);
        softly.assertThat(response.getParams().getTotal()).isEqualTo(100);

        softly.assertThat(response.getCode()).isEqualTo(1404);
        softly.assertThat(response.getComment()).isEqualTo("Comment");

        //WaybillInfo assertion

        softly.assertThat(response.getData()).hasSize(2);
        MarschrouteProductsResponseData data = response.getData().get(0);

        softly.assertThat(data.getItemId()).isEqualTo("article");
        softly.assertThat(data.getName()).isEqualTo("Name");

        softly.assertThat(data.getStockInfo().getFit()).isEqualTo(10);
        softly.assertThat(data.getStockInfo().getQuarantine()).isEqualTo(15);
        softly.assertThat(data.getStockInfo().getDamaged()).isEqualTo(20);
        softly.assertThat(data.getStockInfo().getExpired()).isEqualTo(25);
        softly.assertThat(data.getStockInfo().getAvailable()).isEqualTo(100);
        softly.assertThat(data.getStockInfo().getDateStockUpdate().getValue()).isEqualTo("22.08.2017 16:48:00");

        softly.assertThat(data.getSize().getHeight()).isEqualTo(10);
        softly.assertThat(data.getSize().getWidth()).isEqualTo(15);
        softly.assertThat(data.getSize().getDepth()).isEqualTo(20);


        softly.assertThat(data.getExpiration()).hasSize(1);
        Map.Entry<MarschrouteDate, StockInfo> entry = data.getExpiration().entrySet().iterator().next();

        softly.assertThat(entry.getKey().getValue()).isEqualTo("12.12.2015");
        softly.assertThat(entry.getValue().getFit()).isEqualTo(12);
        softly.assertThat(entry.getValue().getDamaged()).isEqualTo(3);

        softly.assertThat(data.getCountInBox()).isEqualTo(6);
        softly.assertThat(data.getBarcode()).contains("9781903128190", "2867006409095");

    }
}
