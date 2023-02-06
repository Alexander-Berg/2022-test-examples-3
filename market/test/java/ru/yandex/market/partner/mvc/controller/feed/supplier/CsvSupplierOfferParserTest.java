package ru.yandex.market.partner.mvc.controller.feed.supplier;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.excel.MarketTemplate;
import ru.yandex.market.core.feed.supplier.CsvSupplierOfferParser;
import ru.yandex.market.core.supplier.model.SupplierOffer;
import ru.yandex.market.core.tax.model.VatRate;

public class CsvSupplierOfferParserTest {


    @ParameterizedTest(name = "template {0}, file {1}")
    @MethodSource("getTemplates")
    public void testOzonConversion(CsvSupplierOfferParser parser, String file, Consumer<SupplierOffer> consumer) throws IOException {
        LineIterator lineIterator = IOUtils.lineIterator(
                getClass().getResourceAsStream(file), StandardCharsets.UTF_8);
        parser.initHeader(csvToList(lineIterator));
        consumer.accept(parser.parseRow(csvToList(lineIterator)));
    }

    private static List<String> csvToList(LineIterator iterator) {
        return List.of(iterator.nextLine().split(";"));
    }

    private static Stream<Arguments> getTemplates() {
        return List.of(
                Arguments.arguments(new CsvSupplierOfferParser(MarketTemplate.OZON_ASSORTMENT), "ozon_feed.converted",
                        (Consumer<SupplierOffer>) offer -> {
                            Assertions.assertEquals("K0020101", offer.getShopSku());
                            Assertions.assertEquals("Детский горшок", offer.getName());
                            Assertions.assertEquals(BigDecimal.valueOf(1001), offer.getPrice());
                            Assertions.assertEquals(BigDecimal.valueOf(1002), offer.getOldPrice());
                            Assertions.assertEquals(VatRate.VAT_20, offer.getVat());
                            Assertions.assertEquals("Горшок детский", offer.getCategory());
                            Assertions.assertEquals("4660064160315", offer.getBarCode());
                            Assertions.assertEquals("Kidwick", offer.getVendor());
                            Assertions.assertEquals(BigDecimal.valueOf(0.276), offer.getWeight());
                            // сантиметры
                            Assertions.assertEquals("27/24/15", offer.getDimensions());
                            Assertions.assertEquals("Россия", offer.getCountryOfOrigin());
                        }),
                Arguments.arguments(new CsvSupplierOfferParser(MarketTemplate.KUPIVIP_ASSORTMENT), "kupvip_feed.converted",
                        (Consumer<SupplierOffer>) offer -> {
                            Assertions.assertEquals("", offer.getShopSku());
                            Assertions.assertEquals("Колготки", offer.getName());
                            Assertions.assertNull(offer.getPrice());
                            Assertions.assertEquals(BigDecimal.valueOf(740), offer.getOldPrice());
                            Assertions.assertEquals(VatRate.VAT_10, offer.getVat());
                            Assertions.assertEquals("Детское_белье", offer.getCategory());
                            Assertions.assertEquals("4605100788153", offer.getBarCode());
                            Assertions.assertEquals("yula", offer.getVendor());
                            Assertions.assertEquals("LY-Bordo-128/134", offer.getVendorCode());
                            Assertions.assertEquals("10/10/10", offer.getDimensions());
                            Assertions.assertEquals("Россия", offer.getCountryOfOrigin());
                            Assertions.assertEquals(5, offer.getStocksCount());
                        }),
                Arguments.arguments(new CsvSupplierOfferParser(MarketTemplate.WLB_ASSORTMENT), "wlb_feed.converted",
                        (Consumer<SupplierOffer>) offer -> {
                            Assertions.assertNull(offer.getShopSku());
                            Assertions.assertEquals("Полотенце махровое Зайчик", offer.getName());
                            Assertions.assertNull(offer.getPrice());
                            Assertions.assertNull(offer.getOldPrice());
                            Assertions.assertEquals(VatRate.VAT_10, offer.getVat());
                            Assertions.assertEquals("Полотенца банные", offer.getCategory());
                            Assertions.assertEquals("4640031991722", offer.getBarCode());
                            Assertions.assertEquals("Lucky Child", offer.getVendor());
                            Assertions.assertEquals("П1-1/", offer.getVendorCode());
                            // миллиметры
                            Assertions.assertEquals("30/75/75", offer.getDimensions());
                            Assertions.assertEquals("Россия", offer.getCountryOfOrigin());
                            Assertions.assertEquals("6111209000", offer.getCustomsCommodityCodes());
                        })
        ).stream();
    }
}
