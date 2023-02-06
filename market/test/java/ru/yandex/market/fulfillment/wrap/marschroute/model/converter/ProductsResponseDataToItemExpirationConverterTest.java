package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.MarschrouteProductsResponseData;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.StockInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDate;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDateTime;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.Expiration;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemExpiration;
import ru.yandex.market.logistic.api.utils.DateTime;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ProductsResponseDataToItemExpirationConverter.class)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class ProductsResponseDataToItemExpirationConverterTest extends BaseIntegrationTest {

    @Autowired
    private ProductsResponseDataToItemExpirationConverter converter;

    @Test
    void convert() {
        MarschrouteProductsResponseData productsResponseData = new MarschrouteProductsResponseData();
        productsResponseData.setItemId("100500.123");

        Map<MarschrouteDate, StockInfo> expirationsMap = new HashMap<>();
        expirationsMap.put(MarschrouteDate.create("01.01.2001"), new StockInfo()
            .setAvailable(192)
            .setFit(96)
            .setQuarantine(48)
            .setDamaged(24)
            .setExpired(12)
            .setDateStockUpdate(MarschrouteDateTime.create("02.01.2001 12:24")));
        expirationsMap.put(MarschrouteDate.create("03.01.2001"), new StockInfo()
            .setAvailable(1024)
            .setFit(512)
            .setQuarantine(256)
            .setDamaged(128)
            .setExpired(64)
            .setDateStockUpdate(MarschrouteDateTime.create("04.01.2001 12:48")));

        productsResponseData.setExpiration(expirationsMap);

        ItemExpiration itemExpiration = converter.convert(productsResponseData);

        softly.assertThat(itemExpiration.getUnitId().getArticle())
            .as("Item article")
            .isEqualTo("100500");
        softly.assertThat(itemExpiration.getUnitId().getVendorId())
            .as("Item vendor ID")
            .isEqualTo(123);

        softly.assertThat(itemExpiration.getExpirations())
            .as("Item expirations list")
            .hasSize(expirationsMap.size());

        Map<DateTime, Expiration> convertedExpirationsMap = itemExpiration.getExpirations().stream()
            .collect(Collectors.toMap(Expiration::getManufacturedDate, expiration -> expiration));
        softly.assertThat(convertedExpirationsMap)
            .as("Converted expirations map")
            .containsOnlyKeys(new DateTime("2001-01-01"), new DateTime("2001-01-03"));

        Expiration firstExpiration = convertedExpirationsMap.get(new DateTime("2001-01-01"));
        Expiration secondExpiration = convertedExpirationsMap.get(new DateTime("2001-01-03"));

        assertExpiration(firstExpiration, 192, 96, 48, 24, 12,
            new DateTime("2001-01-01"), new DateTime("2001-01-02T12:24"));
        assertExpiration(secondExpiration, 1024, 512, 256, 128, 64,
            new DateTime("2001-01-03"), new DateTime("2001-01-04T12:48"));
    }

    @Test
    void convertNull() {
        MarschrouteProductsResponseData productsResponseData = new MarschrouteProductsResponseData();
        productsResponseData.setItemId("201000.456");
        productsResponseData.setExpiration(null);

        ItemExpiration itemExpiration = converter.convert(productsResponseData);

        softly.assertThat(itemExpiration.getUnitId().getArticle())
            .as("Item article")
            .isEqualTo("201000");
        softly.assertThat(itemExpiration.getUnitId().getVendorId())
            .as("Item vendor ID")
            .isEqualTo(456);

        softly.assertThat(itemExpiration.getExpirations())
            .as("Item expirations list")
            .isNotNull();

        softly.assertThat(itemExpiration.getExpirations())
            .as("Item expirations list")
            .isEmpty();
    }

    private void assertExpiration(Expiration expiration, int available, int fit, int quarantine,
                                  int damaged, int expired, DateTime manufacturedDate, DateTime dateStockUpdate) {
        softly.assertThat(expiration.getManufacturedDate())
            .as("First expiration manufactured date")
            .isEqualTo(manufacturedDate);

        List<Stock> expirationStocks = expiration.getStocks();
        softly.assertThat(expirationStocks)
            .as("Expiration stocks list")
            .hasSize(5);

        Map<StockType, Stock> expirationStocksMap = expirationStocks.stream()
            .collect(Collectors.toMap(Stock::getType, stock -> stock));

        softly.assertThat(expirationStocksMap)
            .as("Expiration stocks map")
            .containsKey(StockType.AVAILABLE);
        softly.assertThat(expirationStocksMap)
            .as("Expiration stocks map")
            .containsKey(StockType.FIT);
        softly.assertThat(expirationStocksMap)
            .as("Expiration stocks map")
            .containsKey(StockType.QUARANTINE);
        softly.assertThat(expirationStocksMap)
            .as("Expiration stocks map")
            .containsKey(StockType.DEFECT);
        softly.assertThat(expirationStocksMap)
            .as("Expiration stocks map")
            .containsKey(StockType.EXPIRED);

        assertStock(expirationStocksMap.get(StockType.AVAILABLE), available, dateStockUpdate);
        assertStock(expirationStocksMap.get(StockType.FIT), fit, dateStockUpdate);
        assertStock(expirationStocksMap.get(StockType.QUARANTINE), quarantine, dateStockUpdate);
        assertStock(expirationStocksMap.get(StockType.DEFECT), damaged, dateStockUpdate);
        assertStock(expirationStocksMap.get(StockType.EXPIRED), expired, dateStockUpdate);
    }

    private void assertStock(Stock actual, Integer count, DateTime updated) {
        softly.assertThat(actual.getCount())
            .as("Actual stock count")
            .isEqualTo(count);
        softly.assertThat(actual.getUpdated())
            .as("Actual stock updated")
            .isEqualTo(updated);
    }
}
