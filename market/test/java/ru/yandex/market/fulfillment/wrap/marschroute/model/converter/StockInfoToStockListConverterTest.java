package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.StockInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

import static ru.yandex.market.fulfillment.wrap.marschroute.factory.StockInfos.stockInfo;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(StockInfoToStockListConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class StockInfoToStockListConverterTest extends BaseIntegrationTest {

    @Autowired
    private StockInfoToStockListConverter stockInfoToStockListConverter;

    @Test
    void testStockConversion() {
        StockInfo stockInfo = stockInfo(25, 15, 20, 5, 3);

        List<Stock> stocks = stockInfoToStockListConverter.convert(stockInfo);

        softly.assertThat(stocks)
            .as("Asserting stocks array size")
            .hasSize(5);

        assertStock(stocks.get(0), StockType.FIT, 15, Stock.DEFAULT_UPDATED_VALUE);
        assertStock(stocks.get(1), StockType.AVAILABLE, 25, Stock.DEFAULT_UPDATED_VALUE);
        assertStock(stocks.get(2), StockType.EXPIRED, 3, Stock.DEFAULT_UPDATED_VALUE);
        assertStock(stocks.get(3), StockType.QUARANTINE, 20, Stock.DEFAULT_UPDATED_VALUE);
        assertStock(stocks.get(4), StockType.DEFECT, 5, Stock.DEFAULT_UPDATED_VALUE);
    }

    private void assertStock(Stock stock, StockType stockType, int expectedCount, DateTime expectedUpdatedValue) {
        softly.assertThat(stock.getType())
            .as("Asserting that stock has type of [" + stockType + "]")
            .isEqualTo(stockType);

        softly.assertThat(stock.getCount())
            .as("Asserting that [" + stockType + "] has [" + expectedCount + "] items")
            .isEqualTo(expectedCount);

        softly.assertThat(stock.getUpdated())
            .as("Asserting that [" + stockType + "] has updated value = [" + expectedUpdatedValue + "]")
            .isEqualTo(expectedUpdatedValue);
    }
}
