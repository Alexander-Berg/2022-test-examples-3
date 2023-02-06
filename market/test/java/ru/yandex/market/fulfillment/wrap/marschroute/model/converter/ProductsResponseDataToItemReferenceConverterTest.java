package ru.yandex.market.fulfillment.wrap.marschroute.model.converter;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.fulfillment.wrap.marschroute.configuration.ConversionConfiguration;
import ru.yandex.market.fulfillment.wrap.marschroute.model.base.MarschrouteDimensions;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.MarschrouteProductsResponseData;
import ru.yandex.market.fulfillment.wrap.marschroute.service.SystemPropertyService;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.GeoInformationProvider;
import ru.yandex.market.logistic.api.model.fulfillment.Barcode;
import ru.yandex.market.logistic.api.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference;
import ru.yandex.market.logistics.test.integration.BaseIntegrationTest;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ConversionConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@MockBean(GeoInformationProvider.class)
@MockBean(SystemPropertyService.class)
class ProductsResponseDataToItemReferenceConverterTest extends BaseIntegrationTest {

    private ProductsResponseDataToItemReferenceConverter converter = new ProductsResponseDataToItemReferenceConverter(new BarcodeSourceDeterminer());

    @Test
    void convert() {
        MarschrouteProductsResponseData productsResponseData = new MarschrouteProductsResponseData();
        productsResponseData.setItemId("100500.123");
        productsResponseData.setSize(new MarschrouteDimensions(1000, 2000, 3000));
        productsResponseData.setWeight(5000);
        productsResponseData.setLifetime(777);
        productsResponseData.setBarcode(ImmutableList.of("286751", "two"));

        ItemReference itemReference = converter.convert(productsResponseData);

        UnitId unitId = itemReference.getUnitId();
        softly.assertThat(unitId.getArticle())
            .as("Asserting the item SKU")
            .isEqualTo("100500");
        softly.assertThat(unitId.getVendorId())
            .as("Asserting the vendor id")
            .isEqualTo(123);

        Korobyte korobyte = itemReference.getKorobyte();
        softly.assertThat(korobyte.getHeight())
            .as("Asserting the item height")
            .isEqualTo(100);
        softly.assertThat(korobyte.getWidth())
            .as("Asserting the item width")
            .isEqualTo(200);
        softly.assertThat(korobyte.getLength())
            .as("Asserting the item length")
            .isEqualTo(300);
        softly.assertThat(korobyte.getWeightGross())
            .as("Asserting the item weight gross")
            .isEqualTo(BigDecimal.valueOf(5.0));

        softly.assertThat(itemReference.getLifeTime())
            .as("Asserting the item life time")
            .isEqualTo(777);
        softly.assertThat(itemReference.getBarcodes())
            .as("Asserting the item barcodes ")
            .hasSize(2).containsExactlyInAnyOrder(
            new Barcode("286751", null, BarcodeSource.PARTNER),
            new Barcode("two", null, BarcodeSource.UNKNOWN)
        );

        Item item = itemReference.getItem();
        korobyte = item.getKorobyte();
        softly.assertThat(korobyte.getHeight())
                .as("Asserting the item height")
                .isEqualTo(100);
        softly.assertThat(korobyte.getWidth())
                .as("Asserting the item width")
                .isEqualTo(200);
        softly.assertThat(korobyte.getLength())
                .as("Asserting the item length")
                .isEqualTo(300);
        softly.assertThat(korobyte.getWeightGross())
                .as("Asserting the item weight gross")
                .isEqualTo(BigDecimal.valueOf(5.0));

        softly.assertThat(item.getLifeTime())
                .as("Asserting the item life time")
                .isEqualTo(777);
        softly.assertThat(item.getBarcodes())
                .as("Asserting the item barcodes ")
                .hasSize(2).containsExactlyInAnyOrder(
                new Barcode("286751", null, BarcodeSource.PARTNER),
                new Barcode("two", null, BarcodeSource.UNKNOWN)
        );
    }

    @Test
    void convertSizeNull() {
        MarschrouteProductsResponseData productsResponseData = new MarschrouteProductsResponseData();
        productsResponseData.setItemId("100500.123");
        productsResponseData.setSize(null);
        productsResponseData.setWeight(5000);
        productsResponseData.setLifetime(777);

        ItemReference itemReference = converter.convert(productsResponseData);

        UnitId unitId = itemReference.getUnitId();
        softly.assertThat(unitId.getArticle())
            .as("Asserting the item SKU")
            .isEqualTo("100500");
        softly.assertThat(unitId.getVendorId())
            .as("Asserting the vendor id")
            .isEqualTo(123);

        Korobyte korobyte = itemReference.getKorobyte();
        softly.assertThat(korobyte)
            .as("Asserting the item korobyte")
            .isNull();

        softly.assertThat(itemReference.getLifeTime())
            .as("Asserting the item life time")
            .isEqualTo(777);
        softly.assertThat(itemReference.getBarcodes())
            .as("Asserting the item barcodes")
            .isNull();

        Item item = itemReference.getItem();
        korobyte = item.getKorobyte();
        softly.assertThat(korobyte)
                .as("Asserting the item korobyte")
                .isNull();


        softly.assertThat(item.getLifeTime())
                .as("Asserting the item life time")
                .isEqualTo(777);
        softly.assertThat(item.getBarcodes())
                .as("Asserting the item barcodes")
                .isNull();

    }

    @Test
    void convertWeightNull() {
        MarschrouteProductsResponseData productsResponseData = new MarschrouteProductsResponseData();
        productsResponseData.setItemId("100500.123");
        productsResponseData.setSize(new MarschrouteDimensions(1000, 2000, 3000));
        productsResponseData.setWeight(null);
        productsResponseData.setLifetime(777);

        ItemReference itemReference = converter.convert(productsResponseData);

        UnitId unitId = itemReference.getUnitId();
        softly.assertThat(unitId.getArticle())
            .as("Asserting the item SKU")
            .isEqualTo("100500");
        softly.assertThat(unitId.getVendorId())
            .as("Asserting the vendor id")
            .isEqualTo(123);

        Korobyte korobyte = itemReference.getKorobyte();
        softly.assertThat(korobyte)
            .as("Asserting the item korobyte")
            .isNull();

        softly.assertThat(itemReference.getLifeTime())
            .as("Asserting the item life time")
            .isEqualTo(777);

        Item item = itemReference.getItem();
        korobyte = item.getKorobyte();
        softly.assertThat(korobyte)
                .as("Asserting the item korobyte")
                .isNull();

        softly.assertThat(item.getLifeTime())
                .as("Asserting the item life time")
                .isEqualTo(777);
    }

    @Test
    void convertSizeAndWeightNull() {
        MarschrouteProductsResponseData productsResponseData = new MarschrouteProductsResponseData();
        productsResponseData.setItemId("100500.123");
        productsResponseData.setSize(null);
        productsResponseData.setWeight(null);
        productsResponseData.setLifetime(777);

        ItemReference itemReference = converter.convert(productsResponseData);

        UnitId unitId = itemReference.getUnitId();
        softly.assertThat(unitId.getArticle())
            .as("Asserting the item SKU")
            .isEqualTo("100500");
        softly.assertThat(unitId.getVendorId())
            .as("Asserting the vendor id")
            .isEqualTo(123);

        softly.assertThat(itemReference.getKorobyte())
            .as("Asserting the item korobyte")
            .isNull();

        softly.assertThat(itemReference.getLifeTime())
            .as("Asserting the item life time")
            .isEqualTo(777);

        Item item = itemReference.getItem();
        Korobyte korobyte = item.getKorobyte();
        softly.assertThat(korobyte)
                .as("Asserting the item korobyte")
                .isNull();

        softly.assertThat(item.getLifeTime())
                .as("Asserting the item life time")
                .isEqualTo(777);
    }

    @Test
    void convertLifeTimeNull() {
        MarschrouteProductsResponseData productsResponseData = new MarschrouteProductsResponseData();
        productsResponseData.setItemId("100500.123");
        productsResponseData.setSize(new MarschrouteDimensions(1000, 2000, 3000));
        productsResponseData.setWeight(5000);
        productsResponseData.setLifetime(null);

        ItemReference itemReference = converter.convert(productsResponseData);

        UnitId unitId = itemReference.getUnitId();
        softly.assertThat(unitId.getArticle())
            .as("Asserting the item SKU")
            .isEqualTo("100500");
        softly.assertThat(unitId.getVendorId())
            .as("Asserting the vendor id")
            .isEqualTo(123);

        Korobyte korobyte = itemReference.getKorobyte();
        softly.assertThat(korobyte.getHeight())
            .as("Asserting the item height")
            .isEqualTo(100);
        softly.assertThat(korobyte.getWidth())
            .as("Asserting the item width")
            .isEqualTo(200);
        softly.assertThat(korobyte.getLength())
            .as("Asserting the item length")
            .isEqualTo(300);
        softly.assertThat(korobyte.getWeightGross())
            .as("Asserting the item weight gross")
            .isEqualTo(BigDecimal.valueOf(5.0));

        softly.assertThat(itemReference.getLifeTime())
            .as("Asserting the item life time")
            .isNull();

        Item item = itemReference.getItem();
        korobyte = item.getKorobyte();
        softly.assertThat(korobyte.getHeight())
                .as("Asserting the item height")
                .isEqualTo(100);
        softly.assertThat(korobyte.getWidth())
                .as("Asserting the item width")
                .isEqualTo(200);
        softly.assertThat(korobyte.getLength())
                .as("Asserting the item length")
                .isEqualTo(300);
        softly.assertThat(korobyte.getWeightGross())
                .as("Asserting the item weight gross")
                .isEqualTo(BigDecimal.valueOf(5.0));

        softly.assertThat(item.getLifeTime())
                .as("Asserting the item life time")
                .isNull();
    }
}
