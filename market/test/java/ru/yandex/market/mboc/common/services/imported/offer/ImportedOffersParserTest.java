package ru.yandex.market.mboc.common.services.imported.offer;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.excel.ExcelFileConverterException;
import ru.yandex.market.mbo.excel.StreamExcelParser;
import ru.yandex.market.mbo.excel.exception.DuplicateHeaderException;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierRepositoryMock;
import ru.yandex.market.mboc.common.masterdata.services.category.MboTimeUnitAliasesService;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.converter.ExcelFileToOffersConverter;
import ru.yandex.market.mboc.common.services.converter.ParseError;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResult;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResultException;
import ru.yandex.market.mboc.common.services.excel.ColumnInfo;
import ru.yandex.market.mboc.common.services.excel.ColumnInfoParser;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.mboc.common.masterdata.parsing.MasterDataParsingConfig.CUSTOMS_COMMODITY_CODE_REQUIRED_KEY;

@SuppressWarnings("checkstyle:magicnumber")
public class ImportedOffersParserTest {
    private static final int SUPPLIER_ID = 1;

    private ExcelFileToOffersConverter<ImportedOffer> importedExcelFileConverter;
    private ModelStorageCachingServiceMock modelService;

    @Before
    public void setUp() {
        OffersToExcelFileConverterConfig config =
            new OffersToExcelFileConverterConfig(new CategoryCachingServiceMock());
        modelService = new ModelStorageCachingServiceMock()
            .setAutoModel(
                new Model().setCategoryId(42).setModelType(Model.ModelType.SKU).setPublishedOnBlueMarket(true));
        MboTimeUnitAliasesService timeUnitAliasesService = Mockito.mock(MboTimeUnitAliasesService.class);
        SupplierRepository supplierRepository = new SupplierRepositoryMock();
        supplierRepository.insert(OfferTestUtils.simpleSupplier().setId(SUPPLIER_ID));
        var storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        importedExcelFileConverter = config.importedExcelFileConverter(modelService, timeUnitAliasesService,
            supplierRepository, storageKeyValueServiceMock);

        storageKeyValueServiceMock.putValue(CUSTOMS_COMMODITY_CODE_REQUIRED_KEY, true);
    }

    //Простой пример
    @Test
    public void testCorrectDocExcelParser() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/CorrectSample.xls");
        result.throwIfFailed();

        ImportedOffer firstOffer = result.getOffer(0);
        assertSoftly(softly -> {
            softly.assertThat(firstOffer.getShopSkuId()).isEqualTo("12222");
            softly.assertThat(firstOffer.getTitle()).isEqualTo("Дрель Makita 6413 безударная");
            softly.assertThat(firstOffer.getCategoryName()).isEqualTo("Дрели и миксеры");
            softly.assertThat(firstOffer.getVendorCode()).isEqualTo("12345678");
            softly.assertThat(firstOffer.getBarCode()).isEqualTo("4607004650642");
            softly.assertThat(firstOffer.getPrice()).isEqualTo("2450");
            softly.assertThat(firstOffer.getNds()).isEqualTo("10%");
            softly.assertThat(firstOffer.getMarketSku()).isNull();
            softly.assertThat(firstOffer.getRealization()).isTrue();
            softly.assertThat(firstOffer.getAvilability()).isNull();
            softly.assertThat(firstOffer.isParallelImported()).isFalse();
        });
    }

    @Test
    public void testMasterDataParsing() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/CorrectSample.xls");
        result.throwIfFailed();

        ImportedOffer firstOffer = result.getOffer(0);
        assertSoftly(softly -> {
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.MANUFACTURER_COUNTRY, "Россия");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.SHELF_LIFE, "300");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.LIFE_TIME, "400");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.GUARANTEE_PERIOD, "200");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.MIN_SHIPMENT, "999");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.TRANSPORT_UNIT_SIZE, "1");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.SUPPLY_SCHEDULE, "сб,вс");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.DELIVERY_TIME, "33");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.QUANTUM_OF_SUPPLY, "111");
            softly.assertThat(firstOffer.getMasterData())
                .containsEntry(ExcelHeaders.CUSTOMS_COMMODITY_CODE, "1234001234");
            softly.assertThat(firstOffer.getMasterData())
                .containsEntry(ExcelHeaders.DOCUMENT_TYPE, "Декларация о соответствии");
            softly.assertThat(firstOffer.getMasterData())
                .containsEntry(ExcelHeaders.DOCUMENT_REG_NUMBER, "Тестовый Номер Документа 123");
            softly.assertThat(firstOffer.getMasterData())
                .containsEntry(ExcelHeaders.DOCUMENT_CERTIFICATION_ORG_REG_NUMBER, "Номер центра сертификации");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.DOCUMENT_START_DATE, "2016.04.06");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.DOCUMENT_END_DATE, "2020.04.06");
            softly.assertThat(firstOffer.getMasterData())
                .containsEntry(ExcelHeaders.DOCUMENT_PICTURE, "http://test-scan-document.com/123");
            softly.assertThat(firstOffer.getMasterData())
                .containsEntry(ExcelHeaders.SHELF_LIFE_COMMENT, "при температуре 5 °C");
            softly.assertThat(firstOffer.getMasterData())
                .containsEntry(ExcelHeaders.LIFE_TIME_COMMENT,
                    "конкретный срок службы зависит от условий использования");
            softly.assertThat(firstOffer.getMasterData())
                .containsEntry(ExcelHeaders.GUARANTEE_PERIOD_COMMENT, "Гарантия 5 лет на блок питания");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.BOX_DIMENSIONS, "2/3/4.6");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.WEIGHT_GROSS, "10.4");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.WEIGHT_NET, "8.2");
            softly.assertThat(firstOffer.getMasterData()).containsEntry(ExcelHeaders.USE_IN_MERCURY, "да");
            softly.assertThat(firstOffer.getMasterData())
                .containsEntry(ExcelHeaders.VETIS_GUIDS, "b04cdcd1-ed94-49ad-a0e9-e58f4184f6f6");
        });
    }

    @Test
    public void testYandexMarketFileParser() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/catalog-tovarov-dlya-yandexa.xlsm");
        result.throwIfFailed();
        ImportedOffer firstOffer = result.getOffer(0);
        assertEquals("12222", firstOffer.getShopSkuId());
        assertEquals("Дрель Makita 6413 безударная", firstOffer.getTitle());
        assertEquals("Дрели и миксеры", firstOffer.getCategoryName());
        assertEquals("12345678", firstOffer.getVendorCode());
        assertEquals("4607004650642", firstOffer.getBarCode());
        assertEquals("2450", firstOffer.getPrice());
        assertEquals("VAT_18_118", firstOffer.getNds());
    }

    @Test
    public void testBooksFileParser() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/books.xls");
        result.throwIfFailed();
        ImportedOffer firstOffer = result.getOffer(0);
        assertEquals("Sasha-test-1", firstOffer.getShopSkuId());
        assertEquals("Гейнер BSN", firstOffer.getTitle());
        assertEquals("Гейнеры для спортсменов", firstOffer.getCategoryName());
    }

    @Test
    public void testParsing() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/briz.xlsx");
        result.throwIfFailed();
        ImportedOffer firstOffer = result.getOffer(0);
        assertEquals("45049262", firstOffer.getShopSkuId());
        assertEquals("Дуршлаг", firstOffer.getTitle());
        assertEquals("черный, серебристый", firstOffer.getUnknowns().get("Цвет"));
        assertEquals("Германия", firstOffer.getUnknowns().get("Страна бренда"));
        assertEquals("4008033492621", firstOffer.getBarCode());
        assertEquals("VAT_10", result.getOffer(1).getNds());
        assertEquals("VAT_18", result.getOffer(2).getNds());
        assertEquals("VAT_10_110", result.getOffer(3).getNds());
        assertEquals("VAT_18_118", result.getOffer(4).getNds());
        assertEquals("VAT_0", result.getOffer(5).getNds());
        assertEquals("NO_VAT", result.getOffer(6).getNds());
    }

    //Пример со строкой описания
    @Test
    public void testDescriptionFieldExcelParser() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/DescriptionSample.xls");
        result.throwIfFailed();

        assertThat(result.getOffer(0).getShopSkuId()).isEqualTo("12222");
    }

    //Только заголовок, без данных
    @Test
    public void testTitleFieldExcelParser() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/TitleSample.xls");
        result.throwIfFailed();

        assertThat(result.getOffers()).isEmpty();
    }

    //С лишними колонками, должны собраться в Map
    @Test
    public void testUnknownFieldExcelParser() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/UnknownSample.xls");
        result.throwIfFailed();

        Assertions.assertThat(result.getOffers()).hasSize(1);
        Map<String, String> unknowns = result.getOffer(0).getUnknowns();
        Assertions.assertThat(unknowns).hasSize(1);
        Assertions.assertThat(unknowns.get("TEST")).isEqualTo("Test");
    }


    @Test
    public void testDuplicateHeader() {
        try {
            parseExcel("excel/DuplicateHeader.xls");
        } catch (ExcelFileConverterException e) {
            Assertions.assertThat(e).isInstanceOf(DuplicateHeaderException.class);
            Assertions.assertThat(e.getMessage()).contains("Заголовок 'НДС' повторяется");
        } catch (Throwable t) {
            Assertions.assertThat(true).isEqualTo(false);
        }
    }

    @SuppressWarnings("LineLength")
    @Test
    public void testBrokenHeader() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/BrokenHeader.xls");
        try {
            result.throwIfFailed();
            Assertions.fail("Expected exception to be thrown");
        } catch (OffersParseResultException e) {
            Assertions.assertThat(e.getErrors())
                .containsExactlyInAnyOrder(
                    "Заголовки из файла: one, two, three",
                    "Файл не содержит обязательного заголовка 'Shop_title' или его алиасов 'название товара', " +
                        "'title', 'название', 'name', 'имя товара', 'имя', 'имя модели', 'название модели', 'тайтл', " +
                        "'заголовок', 'наименование'",
                    "Файл не содержит обязательного заголовка 'Shop_category' или его алиасов 'категория', " +
                        "'category', 'категория магазина', 'shop category', 'товарная категория', 'категория товара'," +
                        " 'категория партнера', 'категория модели', 'имя категории', 'название категории'",
                    "Файл не содержит обязательного заголовка 'Shop_sku' или его алиасов 'sku', 'id sku', 'ваш sku', " +
                        "'id', 'идентификатор', 'model id', 'id модели', 'id товара', 'идентификатор модели', " +
                        "'идентификатор товара'",
                    "Файл не содержит обязательного заголовка 'Ссылка на сайт' или его алиасов 'shop_url', 'url', " +
                        "'promourl', 'Страница товара на сайте', 'ссылка', 'страница товара', 'страница модели', " +
                        "'ссылка на сайт производителя', 'ссылка на модель', 'ссылка на страницу модели', 'ссылка на " +
                        "страницу товара', 'модель на сайте производителя', 'Ссылка на товар', 'модель на сайте " +
                        "бренда', 'site', 'товар на сайте производителя'"
                );
        }
    }

    @SuppressWarnings("LineLength")
    @Test
    public void testErrorDescription() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/BrokenSample.xls");

        try {
            result.throwIfFailed();
            Assertions.fail("Expected exception to be thrown");
        } catch (OffersParseResultException e) {
            Collection<String> errors = e.getErrors();
            assertThat(errors)
                .containsExactlyInAnyOrder(
                    "Файл не содержит обязательного заголовка 'Shop_title' или его алиасов 'название товара', " +
                        "'title', 'название', 'name', 'имя товара', 'имя', 'имя модели', 'название модели', 'тайтл', " +
                        "'заголовок', 'наименование'",
                    "Заголовки из файла: Ваш SKU, Категория, Артикул производителя, Штрихкод, Ваша цена, НДС, SKU на " +
                        "Яндексе, Убрать из продажи, Срок поставки, Минимальная партия поставки, Квант поставки, Срок" +
                        " службы, Код ТН ВЭД, Дни поставки, Срок годности, Гарантийный срок, Транспортная единица",
                    "Файл не содержит обязательного заголовка 'Ссылка на сайт' или его алиасов 'shop_url', 'url', " +
                        "'promourl', 'Страница товара на сайте', 'ссылка', 'страница товара', 'страница модели', " +
                        "'ссылка на сайт производителя', 'ссылка на модель', 'ссылка на страницу модели', 'ссылка на " +
                        "страницу товара', 'модель на сайте производителя', 'Ссылка на товар', 'модель на сайте " +
                        "бренда', 'site', 'товар на сайте производителя'"
                );
        }
    }

    @Test
    public void testIncorrectShopSku() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/BadShopSku.xls");

        try {
            result.throwIfFailed();
            Assertions.fail("Expected exception to be thrown");
        } catch (OffersParseResultException e) {
            Collection<String> errors = e.getErrors();
            assertThat(errors).hasSize(1);
            assertThat(errors.iterator().next()).matches(".*shop_sku.*содержит недопустимые символы.*");
        }
    }

    @Test
    public void testParseOutput() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/BadShopSku.xls");
        assertTrue(result.isFailed());
        Assertions.assertThat(result.getOffers()).hasSize(1);

        // Check some fields are parsed nevertheless, this is required later for correct output for MBI
        ImportedOffer offer = result.getOffer(0);
        assertEquals("4607004650642", offer.getBarCode());
        assertEquals("Дрель Makita 6413 безударная", offer.getTitle());

        ImmutableList<ParseError> errors = result.getParseErrors();
        Assertions.assertThat(errors).hasSize(1);
        ParseError error = errors.get(0);
        assertEquals(0, error.getLineIndex());
        Assertions.assertThat(error.getErrorInfo().toString()).contains("содержит недопустимые символы", "shop_sku");
    }

    @Test
    public void testCorrectParsingOfRealData() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/Karavan_otborka_2404.xlsx");
        result.throwIfFailed();

        Collection<ImportedOffer> importedOffers = result.getOffers();
        assertThat(importedOffers).hasSize(839);

        ImportedOffer expectedOffer = new ImportedOffer();
        expectedOffer.setCategoryName("аксессуары для машинок");
        expectedOffer.setBarCode("4810344064219");
        expectedOffer.setShopSkuId("64219");
        expectedOffer.setTitle("Набор дорожных знаков №3 (24 элементов) (в пакете)");
        expectedOffer.setUrls(Collections.singletonList("https://my-shop.ru/shop/toys/2887169.html"));
        expectedOffer.setVendor("Полесье");
        expectedOffer.setNds("18%");

        assertThat(importedOffers.iterator().next()).isEqualTo(expectedOffer);
    }

    @Test
    @SuppressWarnings("checkstyle:linelength")
    public void testSpacesInSkuOrBarcodeWillFail() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/leading-trailing-spaces.xls");
        assertThat(result.isFailed()).isTrue();

        Assertions.assertThatThrownBy(result::throwIfFailed)
            .hasMessage("" +
                "Ошибка на строке 2: shop_sku '64219 ' содержит недопустимые символы. Например, пробел, + или _. " +
                "Допустимые символы: a-z, A-Z, а-я, А-Я, 0-9, -, =, '.', ',', \\, /, (, ), [, ]\n" +
                "Ошибка на строке 3: shop_sku ' 04PB016' содержит недопустимые символы. Например, пробел, + или _. " +
                "Допустимые символы: a-z, A-Z, а-я, А-Я, 0-9, -, =, '.', ',', \\, /, (, ), [, ]\n" +
                "Ошибка на строке 4: Значение ' 123d' для колонки 'barcode' содержит пробелы в начале\n" +
                "Ошибка на строке 5: Значение '123d ' для колонки 'barcode' содержит пробелы в конце\n" +
                "Ошибка на строке 3: shop_sku '' повторяется на строке 2 и 3\n" +
                "Ошибка на строке 5: shop_sku '02CX019/02CX126' повторяется на строке 4 и 5\n" +
                "Ошибка на строке 6: shop_sku '02CX019/02CX126' повторяется на строке 4 и 6"
            );
    }

    @Test
    public void setRawUrls() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("excel/CorrectSample.xls");
        ExcelFile excelFile = ExcelFileConverter.convert(inputStream, ExcelHeaders.MBOC_EXCEL_IGNORES_CONFIG)
            .toBuilder()
            .setValue(1, "Страница товара на сайте",
                "https://asb.ru/123\n" +
                    "http://asb.ru/345, " +
                    "https://asb.ru/65 " +
                    "http://asb.ru/78 \n \n" +
                    " https://asb.ru/9?a=http%3Atest.ru   ")
            .build();

        OffersParseResult<ImportedOffer> result = importedExcelFileConverter.parse(SUPPLIER_ID, excelFile);
        assertThat(result.isFailed()).isFalse();
        assertThat(result.getOffers()).hasSize(1);
        assertThat(result.getOffers().get(0).getUrls()).containsExactly(
            "https://asb.ru/123",
            "http://asb.ru/345",
            "https://asb.ru/65",
            "http://asb.ru/78",
            "https://asb.ru/9?a=http%3Atest.ru");
    }

    @Test
    public void setRawUrlsIsParsedViaComma() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("excel/CorrectSample.xls");
        ExcelFile excelFile = ExcelFileConverter.convert(inputStream, ExcelHeaders.MBOC_EXCEL_IGNORES_CONFIG)
            .toBuilder()
            .setValue(1, "Страница товара на сайте",
                "https://share.tvoe.ru/goods/06.06.19/4660053390044_1.jpg," +
                    "https://share.tvoe.ru/goods/06.06.19/4660053390044_2.jpg," +
                    "https://share.tvoe.ru/goods/06.06.19/4660053390044_3.jpg,")
            .build();

        OffersParseResult<ImportedOffer> result = importedExcelFileConverter.parse(SUPPLIER_ID, excelFile);
        assertThat(result.isFailed()).isFalse();
        assertThat(result.getOffers()).hasSize(1);
        assertThat(result.getOffers().get(0).getUrls()).containsExactly(
            "https://share.tvoe.ru/goods/06.06.19/4660053390044_1.jpg",
            "https://share.tvoe.ru/goods/06.06.19/4660053390044_2.jpg",
            // NOTE: Сохраняем последнюю запятую. Обсуждаемо.
            // Сейчас логика такая, что раз дальше за ней не идёт валидного урла, то это кусочек предыдущего урла.
            // А зачем нужен такой висящий разделитель сам по себе - не очень понятно.
            // Будет мешать, можно перепридумать.
            "https://share.tvoe.ru/goods/06.06.19/4660053390044_3.jpg,");
    }

    @Test
    public void testParseNullUnicode() {
        OffersParseResult<ImportedOffer> parse = importedExcelFileConverter.parse(SUPPLIER_ID, ExcelFile.Builder
            .withHeaders("Ваш SKU", "Название товара", "Категория", "Описание товара")
            .addLine("TEST", "Title", "Category", "Some \u0000 bad description")
            .build());

        assertThat(parse.isFailed()).isTrue();
        assertThat(parse.getErrors()).anySatisfy(error -> assertThat(error).contains("содержит некорректные символы"));
    }

    @Test
    public void testMarketSkuSpecialCasesParsing() {
        // проверяем, что 0 и НЕТ парсятся корректно
        OffersParseResult<ImportedOffer> result = parseExcel("excel/MarketSKU_special_cases.xlsx");
        result.throwIfFailed();
        Assertions.assertThat(result.getOffers())
            .extracting(importedOffer -> importedOffer.getModel().getId())
            .containsExactly(0L, -1L, -1L, -1L, -1L);
    }

    @Test
    public void testPartnerNoSupplierIdSku() {
        Model model = new Model()
            .setId(100L)
            .setCategoryId(1)
            .setModelType(Model.ModelType.PARTNER_SKU)
            .setPublishedOnBlueMarket(true);
        modelService.addModel(model);

        OffersParseResult<ImportedOffer> result = parseExcel(SUPPLIER_ID, "excel/PartnerSku.xls");

        try {
            result.throwIfFailed();
            Assertions.fail("Expected exception to be thrown");
        } catch (OffersParseResultException e) {
            Collection<String> errors = e.getErrors();
            assertThat(errors).hasSize(1);
            assertThat(errors.iterator().next())
                .matches(".*Партнерская модель \\(sku id: " + model.getId() + "\\) не содержит поставщика.*");
        }
    }

    @Test
    public void testPartnerIncorrectSupplierIdSku() {
        Model model = new Model()
            .setId(100L)
            .setCategoryId(1)
            .setModelType(Model.ModelType.PARTNER_SKU)
            .setSupplierId(100L)
            .setPublishedOnBlueMarket(true);
        modelService.addModel(model);

        OffersParseResult<ImportedOffer> result = parseExcel(SUPPLIER_ID, "excel/PartnerSku.xls");

        try {
            result.throwIfFailed();
            Assertions.fail("Expected exception to be thrown");
        } catch (OffersParseResultException e) {
            Collection<String> errors = e.getErrors();
            assertThat(errors).hasSize(1);
            assertThat(errors.iterator().next())
                .matches(".*Партнерская модель \\(sku id: " + model.getId() + "\\) " +
                    "содержит идентификатор поставщика \\(modelSupplierId: " + model.getSupplierId() + "\\), " +
                    "не совпадающий с поставщиком в маппинге \\(supplierId: " + SUPPLIER_ID + "\\).*");
        }
    }

    @Test
    public void testPartnerSkuSuccess() {
        Model model = new Model()
            .setId(100L)
            .setCategoryId(1)
            .setModelType(Model.ModelType.PARTNER_SKU)
            .setSupplierId((long) SUPPLIER_ID)
            .setPublishedOnBlueMarket(true);
        modelService.addModel(model);

        OffersParseResult<ImportedOffer> result = parseExcel(SUPPLIER_ID, "excel/PartnerSku.xls");

        result.throwIfFailed();
        assertThat(result.isFailed()).isFalse();
    }

    @Test
    public void testSkuNoSupplierSuccess() {
        Model model = new Model()
            .setId(100L)
            .setCategoryId(1)
            .setModelType(Model.ModelType.SKU)
            .setPublishedOnBlueMarket(true);
        modelService.addModel(model);

        OffersParseResult<ImportedOffer> result = parseExcel("excel/PartnerSku.xls");

        result.throwIfFailed();
        assertThat(result.isFailed()).isFalse();
    }

    @Test
    public void testParallelImportedOffersParser() {
        OffersParseResult<ImportedOffer> result = parseExcel("excel/WithParallelImportedColumn.xls");
        result.throwIfFailed();
        assertFalse(result.getOffer(0).isParallelImported());
        assertFalse(result.getOffer(1).isParallelImported());
        assertTrue(result.getOffer(2).isParallelImported());
        assertFalse(result.getOffer(3).isParallelImported());
        assertTrue(result.getOffer(4).isParallelImported());
        assertFalse(result.getOffer(5).isParallelImported());
        assertTrue(result.getOffer(6).isParallelImported());
        assertFalse(result.getOffer(7).isParallelImported());
        assertTrue(result.getOffer(8).isParallelImported());
        assertFalse(result.getOffer(9).isParallelImported());
    }

    private OffersParseResult<ImportedOffer> parseExcel(Integer supplierId, String resourceName) {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
        List<StreamExcelParser.Sheet> sheets = StreamExcelParser.parse(inputStream);
        ExcelFile excelFile = ExcelFileConverter.convert(sheets, ExcelHeaders.MBOC_EXCEL_IGNORES_CONFIG);
        Map<String, ColumnInfo> columnInfoMap = ColumnInfoParser.readColumnInfo(sheets);
        return importedExcelFileConverter.parse(supplierId, excelFile, columnInfoMap);
    }

    private OffersParseResult<ImportedOffer> parseExcel(String resourceName) {
        return parseExcel(SUPPLIER_ID, resourceName);
    }
}
