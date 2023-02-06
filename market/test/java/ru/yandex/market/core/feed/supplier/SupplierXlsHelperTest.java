package ru.yandex.market.core.feed.supplier;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;

import ru.yandex.market.common.excel.ColumnSpec;
import ru.yandex.market.common.excel.XlsSheet;
import ru.yandex.market.common.excel.wrapper.PoiWorkbook;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper.OfferWithErrors;
import ru.yandex.market.core.feed.supplier.tanker.MessageSource;
import ru.yandex.market.core.feed.supplier.tanker.SupplierTankerService;
import ru.yandex.market.core.feed.validation.result.XlsTestUtils;
import ru.yandex.market.core.indexer.model.IndexerError;
import ru.yandex.market.core.indexer.model.OfferPosition;
import ru.yandex.market.core.indexer.model.TranslatedIndexerError;
import ru.yandex.market.core.language.model.Language;
import ru.yandex.market.core.offer.mapping.AvailabilityStatus;
import ru.yandex.market.core.offer.mapping.OfferProcessingStatus;
import ru.yandex.market.core.supplier.model.PriceSuggestType;
import ru.yandex.market.core.supplier.model.SuggestedPrice;
import ru.yandex.market.core.supplier.model.SupplierOffer;
import ru.yandex.market.core.tax.model.VatRate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.market.core.feed.validation.result.XlsTestUtils.assertSheet;
import static ru.yandex.market.core.feed.validation.result.XlsTestUtils.buildExpectedMap;
import static ru.yandex.market.core.feed.validation.result.XlsTestUtils.getPathConsumer;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class SupplierXlsHelperTest {

    private static final Collection<ColumnSpec<SupplierOffer>> COLUMN_SPECS =
            ImmutableList.<ColumnSpec<SupplierOffer>>builder()
                    .addAll(SupplierXlsHelper.GENERAL_COLUMN_SPECS)
                    .addAll(SupplierXlsHelper.OFFER_COLUMN_SPECS)
                    .addAll(SupplierXlsHelper.SUGGESTED_SKU_COLUMN_SPECS)
                    .addAll(SupplierXlsHelper.SUGGESTED_PRICE_COLUMN_SPEC)
                    .build();

    private final SupplierTankerService tankerService = Mockito.mock(SupplierTankerService.class);

    private final SupplierXlsHelper helper =
            new SupplierXlsHelper(new ClassPathResource("supplier/feed/Stock_xls-sku.xls"),
                    "." + XSSFWorkbookType.XLSM.getExtension());

    private final SupplierXlsHelper helperWithMasterData =
            new SupplierXlsHelper(new ClassPathResource("supplier/feed/sku-template-with-master-data.xlsm"),
                    "." + XSSFWorkbookType.XLSM.getExtension());

    private final SupplierXlsHelper helperWithoutErrorFormatting =
            new SupplierXlsHelper(
                    new ClassPathResource("supplier/feed/Stock_xls-sku_no-error-formatting.xls"),
                    "." + XSSFWorkbookType.XLSM.getExtension());

    private final SupplierXlsHelper helperWithAvailability =
            new SupplierXlsHelper(
                    new ClassPathResource("supplier/feed/template-with-availability.xlsm"),
                    "." + XSSFWorkbookType.XLSM.getExtension()
            );

    private final SupplierXlsHelper helperWithBoxCount =
            new SupplierXlsHelper(
                    new ClassPathResource("supplier/feed/template-with-box-count.xlsm"),
                    "." + XSSFWorkbookType.XLSM.getExtension()
            );

    private final SupplierXlsHelper helperWithVGHAndTnVed =
            new SupplierXlsHelper(
                    new ClassPathResource("supplier/feed/catalog-tovarov-dlya-beru-vgh-tnved.xlsm"),
                    "." + XSSFWorkbookType.XLSM.getExtension()
            );

    private final SupplierXlsHelper priceXlsHelper =
            new SupplierXlsHelper(
                    new ClassPathResource("united/feed/marketplace-prices.xlsm"),
                    "." + XSSFWorkbookType.XLSM.getExtension()
            );

    @BeforeEach
    void onBeforeEach() {
        Mockito.reset(tankerService);
    }

    @DisplayName("Проверка корректности заполнения чистого шаблона. " +
            "Ошибки пока записываются в последнюю строку.")
    @Test
    void fillTemplate() throws IOException {
        helper.fillTemplate(
                COLUMN_SPECS,
                data(),
                list(new TranslatedIndexerError("2:0", "Ошибка1"), new TranslatedIndexerError("3:0", "Ошибка2")),
                getPathConsumer(
                        ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                .putAll(buildExpectedMap(3,
                                        "SSKU1", "Название1", "Категория1", "1234567890", "1000",
                                        "VAT_18", "MSKU1", "Да", "Комментарий1\nОшибка1", "Модель1",
                                        "МКат1", "MSKU2", "http://pokupka.yandex.ru/product/MSKU2", "990", "15001",
                                        "980", "12000", "970", "9000", "1970")
                                )
                                .putAll(buildExpectedMap(4,
                                        "SSKU2", "Название2", "Категория2", "987654321", "2000",
                                        "VAT_18", "MSKU2", "", "Комментарий2\nОшибка2", "Модель2",
                                        "МКат2", "MSKU1", "http://pokupka.yandex.ru/product/MSKU1", "", "",
                                        "", "", "", "", "")
                                )
                                .build(),
                        new XlsTestUtils.SheetInfo(5, 2, "Ассортимент")
                ),
                true
        );
    }

    @DisplayName("Проверка корректности заполнения чистого шаблона. " +
            "Ошибки пока записываются в последнюю строку.")
    @Test
    void fillTemplateWithMasterData() throws IOException {
        helperWithMasterData.fillTemplate(
                COLUMN_SPECS,
                dataWithMasterData(),
                list(new TranslatedIndexerError("2:0", "Ошибка1"), new TranslatedIndexerError("3:0", "Ошибка2")),
                getPathConsumer(
                        ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                .putAll(buildExpectedMap(4,
                                        "Комментарий1\nОшибка1", "SSKU1", "Название1", "Категория1", "",
                                        "", "1234567890", "", "", "ОАО Филипс Россия",
                                        "Российская федерация", "180 дней", "1092 дня", "1800 дней", "50",
                                        "1000", "100", "пн,ср,пт", "3", "MSKU1",
                                        "1000", "", "VAT_18", "Да", "Модель1",
                                        "МКат1", "MSKU2", "http://pokupka.yandex.ru/product/MSKU2", "990", "15001",
                                        "980", "12000", "970", "9000", "",
                                        "", "", "", "1970")
                                )
                                .putAll(buildExpectedMap(5,
                                        "Комментарий2\nОшибка2", "SSKU2", "Название2", "Категория2", "",
                                        "", "987654321", "", "", "ОАО Самсунг Россия",
                                        "Республика Белорусь", "1820 дней", "1820 дней", "728 дней", "25",
                                        "500", "50", "пн,вт,ср", "6", "MSKU2",
                                        "2000", "", "VAT_18", "", "Модель2",
                                        "МКат2", "MSKU1", "http://pokupka.yandex.ru/product/MSKU1", "", "",
                                        "", "", "", "", "",
                                        "", "", "", "")
                                )
                                .build(),
                        new XlsTestUtils.SheetInfo(6, 2, "Ассортимент")
                ),
                true
        );
    }

    @DisplayName("Проверка, что есть новые поля с поставками")
    @Test
    void fillTemplateWithNewFields() throws Exception {
        helperWithAvailability.fillTemplate(
                COLUMN_SPECS,
                dataWithAvailability(),
                list(new TranslatedIndexerError("2:0", "Ошибка1"), new TranslatedIndexerError("3:0", "Ошибка2")),
                getPathConsumer(
                        ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                .putAll(buildExpectedMap(4,
                                        "Комментарий1\nОшибка1", "SSKU1", "Название1", "Категория1", "",
                                        "", "ОАО Филипс Россия", "Российская федерация", "1234567890", "",
                                        "", "180 дней", "ValidityComment", "1092 дня", "ServiceComment",
                                        "1800 дней", "WarrantyComment", "", "Поставки будут", "50",
                                        "1000", "100", "пн,ср,пт", "3", "",
                                        "MSKU1", "1000", "", "VAT_18", "Да",
                                        "Модель1", "МКат1", "MSKU2", "http://pokupka.yandex.ru/product/MSKU2", "990",
                                        "15001", "980", "12000", "970", "9000",
                                        "", "", "", "", "1970"
                                        )
                                )
                                .putAll(buildExpectedMap(5,
                                        "Комментарий2\nОшибка2", "SSKU2", "Название2", "Категория2", "",
                                        "", "ОАО Самсунг Россия", "Республика Белорусь", "987654321", "",
                                        "", "1820 дней", "ValidityComment", "1820 дней", "ServiceComment",
                                        "728 дней", "WarrantyComment", "", "", "25",
                                        "500", "50", "пн,вт,ср", "6", "",
                                        "MSKU2", "2000", "", "VAT_18", "",
                                        "Модель2", "МКат2", "MSKU1", "http://pokupka.yandex.ru/product/MSKU1", "",
                                        "", "", "", "", "",
                                        "", "", "", "", ""
                                        )
                                )
                                .build(),
                        new XlsTestUtils.SheetInfo(6, 2, "Ассортимент")
                ),
                true
        );
    }

    @DisplayName("Проверка, что есть поле с количеством занимаемых мест")
    @Test
    void fillTemplateWithBoxCount() throws Exception {
        helperWithBoxCount.fillTemplate(
                COLUMN_SPECS,
                dataWithBoxCount(),
                list(new TranslatedIndexerError("2:0", "Ошибка1"), new TranslatedIndexerError("3:0", "Ошибка2")),
                getPathConsumer(
                        ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                .putAll(buildExpectedMap(4,
                                        "Комментарий1\nОшибка1", "SSKU1", "Название1", "Категория1", "",
                                        "", "ОАО Филипс Россия", "Российская федерация", "1234567890", "",
                                        "", "180 дней", "ValidityComment", "1092 дня", "ServiceComment",
                                        "1800 дней", "WarrantyComment", "", "Поставки будут", "50",
                                        "1000", "100", "пн,ср,пт", "3", "",
                                        "MSKU1", "1000", "", "VAT_18", "Да",
                                        "", "", "Модель1", "МКат1", "MSKU2",
                                        "http://pokupka.yandex.ru/product/MSKU2", "990", "15001", "980", "12000",
                                        "970", "9000", "", "", "",
                                        "", "", "1970"
                                        )
                                )
                                .putAll(buildExpectedMap(5,
                                        "Комментарий2\nОшибка2", "SSKU2", "Название2", "Категория2", "",
                                        "", "ОАО Самсунг Россия", "Республика Белорусь", "987654321", "",
                                        "", "1820 дней", "ValidityComment", "1820 дней", "ServiceComment",
                                        "728 дней", "WarrantyComment", "", "", "25",
                                        "500", "50", "пн,вт,ср", "6", "2",
                                        "MSKU2", "2000", "", "VAT_18", "",
                                        "", "", "Модель2", "МКат2", "MSKU1",
                                        "http://pokupka.yandex.ru/product/MSKU1", "", "", "", "",
                                        "", "", "", "", "",
                                        "", "", ""
                                        )
                                )
                                .build(),
                        new XlsTestUtils.SheetInfo(6, 2, "Ассортимент")
                ),
                true
        );
    }

    @DisplayName("Проверка что при заполнеии заполнения шаблона учитывается настройка," +
            " отключающая выделение строк с ошибками за счёт добавления к ним красной рамочки.")
    @Test
    void fillTemplateWithoutErrorFormatting() throws IOException {
        helperWithoutErrorFormatting.fillTemplate(
                COLUMN_SPECS,
                data(),
                list(new TranslatedIndexerError("2:0", "Ошибка1"), new TranslatedIndexerError("3:0", "Ошибка2")),
                path -> {
                    try {
                        PoiWorkbook workbook = PoiWorkbook.load(path.toFile());
                        CellReference ref = new CellReference("Ассортимент!A4");
                        CellStyle errorStyle = workbook.getSheet(
                                XlsSheet.newBuilder()
                                        .withName(ref.getSheetName())
                                        .build()
                        )
                                .getRow(ref.getRow())
                                .getCell(ref.getCol())
                                .getCellStyle();
                        assertThat(
                                errorStyle.getBorderBottom(),
                                anyOf(equalTo(BorderStyle.NONE), equalTo(BorderStyle.HAIR))
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                },
                true
        );
    }

    @DisplayName("Проверка корректности заполнения ценового шаблона с рекомендациями по ценам.")
    @Test
    void fillTemplateStreamed_priceTemplateWithSuggestion_success() throws IOException {
        Locale defLocale = Locale.getDefault();
        Locale.setDefault(new Locale("ru"));
        priceXlsHelper.fillTemplateStreamed(
                COLUMN_SPECS,
                Stream.of(
                        SupplierOffer.builder()
                                .withShopSku("300")
                                .withName("Батарейка AG3 щелочная PKCELL AG3-10B 10шт")
                                .withPrice(BigDecimal.valueOf(150.0))
                                .withOldPrice(BigDecimal.valueOf(250.0))
                                .withVat(VatRate.NO_VAT)
                                .withBarCode("31241246")
                                .withPosition(OfferPosition.of(2, 0))
                                .withAvailability(AvailabilityStatus.ACTIVE)
                                .withOfferProcessingStatus(OfferProcessingStatus.UNKNOWN)
                                .withMarketSku("42162")
                                .withSuggestedPrices(Map.of(
                                        PriceSuggestType.BUYBOX, new SuggestedPrice(
                                                BigDecimal.valueOf(150),
                                                BigDecimal.valueOf(500)
                                        ),
                                        PriceSuggestType.MIN_PRICE_MARKET, new SuggestedPrice(
                                                BigDecimal.valueOf(200),
                                                BigDecimal.valueOf(750)
                                        ),
                                        PriceSuggestType.DEFAULT_OFFER, new SuggestedPrice(
                                                BigDecimal.valueOf(250),
                                                BigDecimal.valueOf(1000)
                                        ),
                                        PriceSuggestType.MAX_OLD_PRICE, new SuggestedPrice(
                                                BigDecimal.valueOf(300),
                                                BigDecimal.valueOf(1250)
                                        ),
                                        PriceSuggestType.MAX_DISCOUNT_PRICE, new SuggestedPrice(
                                                BigDecimal.valueOf(270),
                                                BigDecimal.ZERO
                                        )
                                ))
                                .build(),
                        SupplierOffer.builder()
                                .withErrors("Не указана характеристика товара age" +
                                        System.lineSeparator() +
                                        "Не указана характеристика товара age")
                                .withShopSku("301")
                                .withName("Goods")
                                .withPrice(BigDecimal.valueOf(100.0))
                                .withOldPrice(BigDecimal.valueOf(200.0))
                                .withVat(VatRate.VAT_20)
                                .withDisabled(true)
                                .withBarCode("31241286")
                                .withPosition(OfferPosition.of(2, 0))
                                .withAvailability(AvailabilityStatus.ACTIVE)
                                .withOfferProcessingStatus(OfferProcessingStatus.NEED_CONTENT)
                                .withMarketSku("42167")
                                .withSuggestedPrices(Map.of(
                                        PriceSuggestType.BUYBOX, new SuggestedPrice(
                                                BigDecimal.valueOf(150),
                                                BigDecimal.valueOf(500)
                                        ),
                                        PriceSuggestType.MIN_PRICE_MARKET, new SuggestedPrice(
                                                BigDecimal.valueOf(200),
                                                BigDecimal.valueOf(750)
                                        ),
                                        PriceSuggestType.DEFAULT_OFFER, new SuggestedPrice(
                                                BigDecimal.valueOf(250),
                                                BigDecimal.valueOf(1000)
                                        ),
                                        PriceSuggestType.MAX_OLD_PRICE, new SuggestedPrice(
                                                BigDecimal.valueOf(300),
                                                BigDecimal.valueOf(1250)
                                        ),
                                        PriceSuggestType.MAX_DISCOUNT_PRICE, new SuggestedPrice(
                                                BigDecimal.valueOf(270),
                                                BigDecimal.ZERO
                                        )
                                ))
                                .build()
                ),
                getPathConsumer(
                        ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                .putAll(buildExpectedMap(3,
                                        "", "",
                                        "300", "Батарейка AG3 щелочная PKCELL AG3-10B 10шт",
                                        "150", "250", "NO_VAT", "","","","",
                                        "150", "15000", "250", "30000", "200", "22500", "300",
                                        "270", "", "", "", ""
                                ))
                                .putAll(buildExpectedMap(4,
                                        "Не указана характеристика товара age" +
                                                System.lineSeparator() +
                                                "Не указана характеристика товара age", "",
                                        "301", "Goods",
                                        "100", "200", "VAT_20", "Да","","","",
                                        "150", "15000", "250", "30000", "200", "22500", "300",
                                        "270", "", "", "", ""
                                ))
                                .build(),
                        new XlsTestUtils.SheetInfo(5, 2, "Цены")
                )
        );
        Locale.setDefault(defLocale);
    }

    @DisplayName("Проверка корректности заполнения шаблона с ВГХ и ТН-ВЕД.")
    @Test
    void fillTemplateWithVGHAndTnVed() throws IOException {
        Locale defLocale = Locale.getDefault();
        Locale.setDefault(new Locale("ru"));
        helperWithVGHAndTnVed.fillTemplate(
                COLUMN_SPECS,
                dataWithVGHAndMercury(),
                list(new TranslatedIndexerError("2:0", "Ошибка1"), new TranslatedIndexerError("3:0", "Ошибка2")),
                getPathConsumer(
                        ImmutableMap.<XlsTestUtils.CellInfo, String>builder()
                                .putAll(buildExpectedMap(4,
                                        "Комментарий1\nОшибка1", "SSKU1", "Название1", "Категория1", "",
                                        "", "ОАО Филипс Россия", "Российская федерация", "1234567890", "",
                                        "", "180 дней", "", "1092 дня", "",
                                        "1800 дней", "", "", "", "",
                                        "", "", "50", "1000", "100",
                                        "пн,ср,пт", "3", "", "MSKU1", "1000",
                                        "", "VAT_18", "Да", "", "Модель1",
                                        "МКат1", "MSKU2", "http://pokupka.yandex.ru/product/MSKU2", "990", "15001",
                                        "980", "12000", "970", "9000", "")
                                )
                                .putAll(buildExpectedMap(5,
                                        "Комментарий2\nОшибка2", "SSKU2", "Название2", "Категория2", "",
                                        "", "ОАО Самсунг Россия", "Республика Белорусь", "987654321", "",
                                        "", "1820 дней", "", "1820 дней", "",
                                        "728 дней", "", "", "TNV123987,V129845TH", "65.55/50.7/20.0",
                                        "20.5", "", "25", "500", "50",
                                        "пн,вт,ср", "6", "", "MSKU2", "2000",
                                        "", "VAT_18", "", "", "Модель2",
                                        "МКат2", "MSKU1", "http://pokupka.yandex.ru/product/MSKU1", "", "",
                                        "", "", "", "", "")
                                )
                                .build(),
                        new XlsTestUtils.SheetInfo(6, 2, "Ассортимент")
                ),
                true
        );
        Locale.setDefault(defLocale);
    }

    @Nonnull
    private Collection<SupplierOffer> data() {
        return Arrays.asList(
                new SupplierOffer.Builder()
                        .withShopSku("SSKU1")
                        .withName("Название1")
                        .withCategory("Категория1")
                        .withBarCode("1234567890")
                        .withPrice(BigDecimal.valueOf(1000))
                        .withVat(VatRate.VAT_18)
                        .withMarketSku("MSKU1")
                        .withDisabled(true)
                        .withErrors("Комментарий1")
                        .withSuggestedModelName("Модель1")
                        .withSuggestedCategoryName("МКат1")
                        .withSuggestedSku("MSKU2")
                        .withMarketSkuUrl("http://pokupka.yandex.ru/product/MSKU2")
                        .withSuggestedPrice(PriceSuggestType.BUYBOX,
                                new SuggestedPrice(BigDecimal.valueOf(990), BigDecimal.valueOf(50001, 2)))
                        .withSuggestedPrice(PriceSuggestType.DEFAULT_OFFER,
                                new SuggestedPrice(BigDecimal.valueOf(980), BigDecimal.valueOf(400L)))
                        .withSuggestedPrice(PriceSuggestType.MIN_PRICE_MARKET,
                                new SuggestedPrice(BigDecimal.valueOf(970), BigDecimal.valueOf(300L)))
                        .withSuggestedPrice(PriceSuggestType.MAX_OLD_PRICE,
                                new SuggestedPrice(BigDecimal.valueOf(1970)))
                        .withPosition("2:0")
                        .build(),
                new SupplierOffer.Builder()
                        .withShopSku("SSKU2")
                        .withName("Название2")
                        .withCategory("Категория2")
                        .withBarCode("987654321")
                        .withPrice(BigDecimal.valueOf(2000))
                        .withVat(VatRate.VAT_18)
                        .withMarketSku("MSKU2")
                        .withDisabled(false)
                        .withErrors("Комментарий2")
                        .withSuggestedModelName("Модель2")
                        .withSuggestedCategoryName("МКат2")
                        .withSuggestedSku("MSKU1")
                        .withMarketSkuUrl("http://pokupka.yandex.ru/product/MSKU1")
                        .withPosition("3:0")
                        .build()
        );
    }

    @Nonnull
    private Collection<SupplierOffer> dataWithMasterData() {
        return Arrays.asList(
                new SupplierOffer.Builder()
                        .withShopSku("SSKU1")
                        .withName("Название1")
                        .withCategory("Категория1")
                        .withBarCode("1234567890")
                        .withPrice(BigDecimal.valueOf(1000))
                        .withVat(VatRate.VAT_18)
                        .withMarketSku("MSKU1")
                        .withDisabled(true)
                        .withErrors("Комментарий1")
                        .withSuggestedModelName("Модель1")
                        .withSuggestedCategoryName("МКат1")
                        .withSuggestedSku("MSKU2")
                        .withMarketSkuUrl("http://pokupka.yandex.ru/product/MSKU2")
                        .withManufacturer("ОАО Филипс Россия")
                        .withCountryOfOrigin("Российская федерация")
                        .withDeliveryWeekdays("пн,ср,пт")
                        .withLeadtime(3)
                        .withQuantum(100)
                        .withTransportUnit(50)
                        .withWarranty("1800 дней")
                        .withMinDeliveryPieces(1000)
                        .withPeriodOfValidity("180 дней")
                        .withServiceLife("1092 дня")
                        .withSuggestedPrice(PriceSuggestType.BUYBOX,
                                new SuggestedPrice(BigDecimal.valueOf(990), BigDecimal.valueOf(50001, 2)))
                        .withSuggestedPrice(PriceSuggestType.DEFAULT_OFFER,
                                new SuggestedPrice(BigDecimal.valueOf(980), BigDecimal.valueOf(400L)))
                        .withSuggestedPrice(PriceSuggestType.MIN_PRICE_MARKET,
                                new SuggestedPrice(BigDecimal.valueOf(970), BigDecimal.valueOf(300L)))
                        .withSuggestedPrice(PriceSuggestType.MAX_OLD_PRICE,
                                new SuggestedPrice(BigDecimal.valueOf(1970)))
                        .withPosition("2:0")
                        .build(),
                new SupplierOffer.Builder()
                        .withShopSku("SSKU2")
                        .withName("Название2")
                        .withCategory("Категория2")
                        .withBarCode("987654321")
                        .withPrice(BigDecimal.valueOf(2000))
                        .withVat(VatRate.VAT_18)
                        .withMarketSku("MSKU2")
                        .withDisabled(false)
                        .withErrors("Комментарий2")
                        .withSuggestedModelName("Модель2")
                        .withSuggestedCategoryName("МКат2")
                        .withSuggestedSku("MSKU1")
                        .withMarketSkuUrl("http://pokupka.yandex.ru/product/MSKU1")
                        .withManufacturer("ОАО Самсунг Россия")
                        .withCountryOfOrigin("Республика Белорусь")
                        .withDeliveryWeekdays("пн,вт,ср")
                        .withLeadtime(6)
                        .withQuantum(50)
                        .withTransportUnit(25)
                        .withWarranty("728 дней")
                        .withMinDeliveryPieces(500)
                        .withPeriodOfValidity("1820 дней")
                        .withServiceLife("1820 дней")
                        .withPosition("3:0")
                        .build()
        );
    }

    @Nonnull
    private Collection<SupplierOffer> dataWithVGHAndMercury() {
        return Arrays.asList(
                new SupplierOffer.Builder()
                        .withShopSku("SSKU1")
                        .withName("Название1")
                        .withCategory("Категория1")
                        .withBarCode("1234567890")
                        .withPrice(BigDecimal.valueOf(1000))
                        .withVat(VatRate.VAT_18)
                        .withMarketSku("MSKU1")
                        .withDisabled(true)
                        .withErrors("Комментарий1")
                        .withSuggestedModelName("Модель1")
                        .withSuggestedCategoryName("МКат1")
                        .withSuggestedSku("MSKU2")
                        .withMarketSkuUrl("http://pokupka.yandex.ru/product/MSKU2")
                        .withManufacturer("ОАО Филипс Россия")
                        .withCountryOfOrigin("Российская федерация")
                        .withDeliveryWeekdays("пн,ср,пт")
                        .withLeadtime(3)
                        .withQuantum(100)
                        .withTransportUnit(50)
                        .withWarranty("1800 дней")
                        .withMinDeliveryPieces(1000)
                        .withPeriodOfValidity("180 дней")
                        .withServiceLife("1092 дня")
                        .withSuggestedPrice(PriceSuggestType.BUYBOX,
                                new SuggestedPrice(BigDecimal.valueOf(990), BigDecimal.valueOf(50001, 2)))
                        .withSuggestedPrice(PriceSuggestType.DEFAULT_OFFER,
                                new SuggestedPrice(BigDecimal.valueOf(980), BigDecimal.valueOf(400L)))
                        .withSuggestedPrice(PriceSuggestType.MIN_PRICE_MARKET,
                                new SuggestedPrice(BigDecimal.valueOf(970), BigDecimal.valueOf(300L)))
                        .withSuggestedPrice(PriceSuggestType.MAX_OLD_PRICE,
                                new SuggestedPrice(BigDecimal.valueOf(1970)))
                        .withPosition("2:0")
                        .build(),
                new SupplierOffer.Builder()
                        .withShopSku("SSKU2")
                        .withName("Название2")
                        .withCategory("Категория2")
                        .withBarCode("987654321")
                        .withPrice(BigDecimal.valueOf(2000))
                        .withVat(VatRate.VAT_18)
                        .withMarketSku("MSKU2")
                        .withDisabled(false)
                        .withErrors("Комментарий2")
                        .withSuggestedModelName("Модель2")
                        .withSuggestedCategoryName("МКат2")
                        .withSuggestedSku("MSKU1")
                        .withMarketSkuUrl("http://pokupka.yandex.ru/product/MSKU1")
                        .withManufacturer("ОАО Самсунг Россия")
                        .withCountryOfOrigin("Республика Белорусь")
                        .withDeliveryWeekdays("пн,вт,ср")
                        .withLeadtime(6)
                        .withQuantum(50)
                        .withTransportUnit(25)
                        .withWarranty("728 дней")
                        .withMinDeliveryPieces(500)
                        .withPeriodOfValidity("1820 дней")
                        .withServiceLife("1820 дней")
                        .withPosition("3:0")
                        .withDimensions("65.55/50.7/20.0")
                        .withWeight(new BigDecimal("20.5"))
                        .withCustomsCommodityCodes("TNV123987,V129845TH")
                        .build()
        );
    }

    @Nonnull
    private Collection<SupplierOffer> dataWithAvailability() {
        return Arrays.asList(
                firstOfferBuilder().build(),
                secondOfferBuilder().build()
        );
    }

    @Nonnull
    private Collection<SupplierOffer> dataWithBoxCount() {
        return Arrays.asList(
                firstOfferBuilder().build(),
                secondOfferBuilder().withBoxCount(2).build()
        );
    }

    @Nonnull
    private SupplierOffer.Builder firstOfferBuilder() {
        return new SupplierOffer.Builder()
                .withShopSku("SSKU1")
                .withName("Название1")
                .withCategory("Категория1")
                .withBarCode("1234567890")
                .withPrice(BigDecimal.valueOf(1000))
                .withVat(VatRate.VAT_18)
                .withMarketSku("MSKU1")
                .withDisabled(true)
                .withErrors("Комментарий1")
                .withSuggestedModelName("Модель1")
                .withSuggestedCategoryName("МКат1")
                .withSuggestedSku("MSKU2")
                .withMarketSkuUrl("http://pokupka.yandex.ru/product/MSKU2")
                .withManufacturer("ОАО Филипс Россия")
                .withCountryOfOrigin("Российская федерация")
                .withDeliveryWeekdays("пн,ср,пт")
                .withLeadtime(3)
                .withQuantum(100)
                .withTransportUnit(50)
                .withWarranty("1800 дней")
                .withMinDeliveryPieces(1000)
                .withPeriodOfValidity("180 дней")
                .withServiceLife("1092 дня")
                .withAvailability(AvailabilityStatus.ACTIVE)
                .withWarrantyComment("WarrantyComment")
                .withServiceLifeComment("ServiceComment")
                .withPeriodOfValidityComment("ValidityComment")
                .withSuggestedPrice(PriceSuggestType.BUYBOX,
                        new SuggestedPrice(BigDecimal.valueOf(990), BigDecimal.valueOf(50001, 2)))
                .withSuggestedPrice(PriceSuggestType.DEFAULT_OFFER,
                        new SuggestedPrice(BigDecimal.valueOf(980), BigDecimal.valueOf(400L)))
                .withSuggestedPrice(PriceSuggestType.MIN_PRICE_MARKET,
                        new SuggestedPrice(BigDecimal.valueOf(970), BigDecimal.valueOf(300L)))
                .withSuggestedPrice(PriceSuggestType.MAX_OLD_PRICE,
                        new SuggestedPrice(BigDecimal.valueOf(1970)))
                .withPosition("2:0");
    }

    @Nonnull
    private SupplierOffer.Builder secondOfferBuilder() {
        return new SupplierOffer.Builder()
                .withShopSku("SSKU2")
                .withName("Название2")
                .withCategory("Категория2")
                .withBarCode("987654321")
                .withPrice(BigDecimal.valueOf(2000))
                .withVat(VatRate.VAT_18)
                .withMarketSku("MSKU2")
                .withDisabled(false)
                .withErrors("Комментарий2")
                .withSuggestedModelName("Модель2")
                .withSuggestedCategoryName("МКат2")
                .withSuggestedSku("MSKU1")
                .withMarketSkuUrl("http://pokupka.yandex.ru/product/MSKU1")
                .withManufacturer("ОАО Самсунг Россия")
                .withCountryOfOrigin("Республика Белорусь")
                .withDeliveryWeekdays("пн,вт,ср")
                .withLeadtime(6)
                .withQuantum(50)
                .withTransportUnit(25)
                .withWarranty("728 дней")
                .withMinDeliveryPieces(500)
                .withPeriodOfValidity("1820 дней")
                .withServiceLife("1820 дней")
                .withWarrantyComment("WarrantyComment")
                .withServiceLifeComment("ServiceComment")
                .withPeriodOfValidityComment("ValidityComment")
                .withPosition("3:0");
    }

    @DisplayName("Проверяет корректность слияния офферов и ошибок по position")
    @Test
    void mergeOffersWithErrors() {
        List<SupplierOffer> offers = list(
                new SupplierOffer.Builder().withPosition("1:0").withName("offer1").build(),
                new SupplierOffer.Builder().withPosition("4:0").withName("offer4").build(),
                new SupplierOffer.Builder().withPosition("5:0").withName("offer5").build()
        );
        List<TranslatedIndexerError> errors = list(
                new TranslatedIndexerError("1:0", "error1"),
                new TranslatedIndexerError("2:0", "error2"),
                new TranslatedIndexerError("2:0", "error2.1"),
                new TranslatedIndexerError("3:0", "error3")
        );
        Map<OfferPosition, OfferWithErrors> expected = new TreeMap<>();
        expected.put(OfferPosition.of("1:0"), new OfferWithErrors(
                new SupplierOffer.Builder().withPosition("1:0").withName("offer1").build(),
                list(new TranslatedIndexerError("1:0", "error1"))
        ));
        expected.put(OfferPosition.of("2:0"), new OfferWithErrors(list(
                new TranslatedIndexerError("2:0", "error2"),
                new TranslatedIndexerError("2:0", "error2.1")
        )));
        expected.put(OfferPosition.of("3:0"), new OfferWithErrors(list(
                new TranslatedIndexerError("3:0", "error3")
        )));
        expected.put(OfferPosition.of("4:0"), new OfferWithErrors(
                new SupplierOffer.Builder().withPosition("4:0").withName("offer4").build()
        ));
        expected.put(OfferPosition.of("5:0"), new OfferWithErrors(
                new SupplierOffer.Builder().withPosition("5:0").withName("offer5").build()
        ));
        Map<OfferPosition, OfferWithErrors> actual = SupplierXlsHelper.mergeOffersWithErrors(offers, errors);

        assertThat(actual, equalTo(expected));
    }

    @Test
    void decimalConversionWithDotSeparation() {
        Locale defLocale = Locale.getDefault();
        Locale.setDefault(new Locale("ru"));
        String result = SupplierXlsHelper.convertDecimalWithDot(new BigDecimal("5.879"));
        assertEquals(result, "5.879");
        Locale.setDefault(defLocale);
    }

    @Test
    void extendCommentsTest() {
        SupplierOffer supplierOffer = new SupplierOffer.Builder()
                .extendErrors("val","\n").build();
        assertEquals("val", supplierOffer.getErrors());
        supplierOffer = new SupplierOffer.Builder()
                .withErrors(null)
                .extendErrors("val","\n").build();
        assertEquals("val", supplierOffer.getErrors());
        supplierOffer = new SupplierOffer.Builder()
                .extendErrors(null,"\n")
                .extendErrors("val","\n").build();
        assertEquals("val", supplierOffer.getErrors());
        supplierOffer = new SupplierOffer.Builder()
                .withErrors(null)
                .extendErrors(null,"\n")
                .extendErrors("val","\n").build();
        assertEquals("val", supplierOffer.getErrors());
    }
}
