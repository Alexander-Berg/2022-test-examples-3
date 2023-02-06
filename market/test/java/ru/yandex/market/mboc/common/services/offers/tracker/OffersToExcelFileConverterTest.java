package ru.yandex.market.mboc.common.services.offers.tracker;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.excel.Header;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.config.OffersToExcelFileConverterConfig;
import ru.yandex.market.mboc.common.offers.MatchingOffer;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.services.category.CategoryCachingServiceMock;
import ru.yandex.market.mboc.common.services.converter.OffersExcelFileConverter;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResult;
import ru.yandex.market.mboc.common.services.excel.ExcelHeaders;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.modelstorage.models.Model;
import ru.yandex.market.mboc.common.utils.DateTimeUtils;
import ru.yandex.market.mboc.common.utils.MbocComparators;

@SuppressWarnings("checkstyle:magicNumber")
public class OffersToExcelFileConverterTest {
    private static final int TEST_OFFERS_COUNT = 5;
    private ModelStorageCachingServiceMock modelStorageCachingServiceMock;
    private CategoryCachingServiceMock categoryCachingServiceMock;
    private OffersExcelFileConverter<MatchingOffer> converter;
    private OffersToExcelFileConverterConfig config;
    private OfferRepositoryMock offerRepository;

    @Before
    public void setUp() {
        this.modelStorageCachingServiceMock = new ModelStorageCachingServiceMock();
        this.categoryCachingServiceMock = new CategoryCachingServiceMock();

        for (int i = 0; i < TEST_OFFERS_COUNT; i++) {
            Model model = new Model();
            model.setId(i + 1);
            model.setTitle("Test");
            model.setCategoryId(100);
            model.setVendorId(101);
            model.setModelType(Model.ModelType.SKU);
            model.setPublishedOnBlueMarket(true);
            model.setSkuParentModelId(21L);
            this.modelStorageCachingServiceMock.addModel(model);
        }

        config = new OffersToExcelFileConverterConfig(categoryCachingServiceMock);
        offerRepository = new OfferRepositoryMock();
        converter = config.matchingConverter(this.modelStorageCachingServiceMock, offerRepository);
    }

    @Test
    public void testMatchingHeadersHasNoDups() {
        Assertions.assertThat(
            config.getMatchingHeaders().stream()
                .collect(Collectors.groupingBy(x -> x, Collectors.counting())).entrySet().stream() // посчитаем
                .filter(x -> x.getValue() != 1) // возьмём повторяющиеся заголовки
                .map(Map.Entry::getKey)
                .map(Header::getTitle) // оставим от них только заголовки
                .collect(Collectors.toSet())
        ).isEmpty();
    }

    // Простой пример
    @Test
    public void testSimpleFile() {
        List<Offer> offers = createTestOffers();
        ExcelFile excelFile = converter.convert(offers);
        List<Header> matchingHeaders = config.getMatchingHeaders();

        int j;
        for (j = 0; j < matchingHeaders.size(); j++) {
            Header header = matchingHeaders.get(j);
            Assert.assertEquals(header.getTitle(), excelFile.getHeader(j));
        }
        Assert.assertEquals("Random header", excelFile.getHeader(j++));
        Assert.assertEquals("Параметр 1", excelFile.getHeader(j++));

        MbocAssertions.assertThat(excelFile)
            .hasHeaderSize(matchingHeaders.size() + TEST_OFFERS_COUNT + 1)
            .hasLastLine(TEST_OFFERS_COUNT);

        for (int i = 0; i < offers.size(); i++) {
            Offer testOffer = offers.get(i);
            checkExcelRow(excelFile, i + 1, testOffer);
        }
    }

    private void checkExcelRow(ExcelFile excelFile, int row, Offer sourceOffer) {
        int column = 0;
        MbocAssertions.assertThat(excelFile)
            .containsValue(row, column++, sourceOffer.getId())
            .containsValue(row, column++, sourceOffer.getShopSku())
            .containsValue(row, column++, sourceOffer.getTitle())
            .containsValue(row, column++, sourceOffer.getShopCategoryName())
            .containsValue(row, column++, sourceOffer.getVendor())
            .containsValue(row, column++, sourceOffer.getVendorCode())
            .containsValue(row, column++, sourceOffer.getBarCode())
            .containsValue(row, column++, sourceOffer.extractOfferContent().getUrls().isEmpty()
                ? null : sourceOffer.extractOfferContent().getUrls().get(0))
            .containsValue(row, column++, sourceOffer.extractOfferContent().getDescription())
            .containsValue(row, column++, sourceOffer.getCategoryId())
            .containsValue(row, column++, categoryCachingServiceMock.getCategoryName(sourceOffer.getCategoryId()))
            .containsValue(row, column++, sourceOffer.getVendorId())
            .containsValue(row, column++, sourceOffer.getMarketVendorName())
            .containsValue(row, column++, sourceOffer.getMarketModelName())
            .containsValue(row, column++, sourceOffer.getModelId())
            .containsValue(row, column++, sourceOffer.getSuggestedSkuIdStr())
            .containsValue(row, column++, sourceOffer.getSupplierSkuIdStr())
            .containsValue(row, column++, sourceOffer.getApprovedSkuIdStr())
            .containsValue(row, column++, sourceOffer.getContentComment());

        column = config.getMatchingHeaders().size();
        MbocAssertions.assertThat(excelFile)
            .containsValue(row, column, "test " + row);

        column = config.getMatchingHeaders().size() + row;
        MbocAssertions.assertThat(excelFile)
            .containsValue(row, column, "странное значение");
    }

    @Test
    public void testSomeCellsOnHyperlink() throws IOException, InvalidFormatException {
        List<Offer> offers = createTestOffers();
        ExcelFile excelFile = converter.convert(offers);
        Workbook workbook = WorkbookFactory.create(ExcelFileConverter.convert(excelFile));
        Sheet sheet = workbook.getSheetAt(0);

        IntStream.rangeClosed(1, sheet.getLastRowNum())
            .mapToObj(sheet::getRow)
            .forEach(row -> Assertions.assertThat(row.getCell(7).getHyperlink()).isNotNull());
    }

    @Test
    public void testConvertOffersWithSeveralUrls() {
        Offer offer0 = new Offer()
            .setIsOfferContentPresent(true)
            .storeOfferContent(
                OfferContent.initEmptyContent());
        Offer offer1 = new Offer()
            .setIsOfferContentPresent(true)
            .storeOfferContent(
                OfferContent.builder().urls(
                    Arrays.asList("https://yandex.ru", "https://google.com", "https://market.yandex.com")
                ).build());
        Offer offer2 = new Offer()
            .setIsOfferContentPresent(true)
            .storeOfferContent(
                OfferContent.builder().urls(
                    Collections.singletonList("https://yandex.ru")
                ).build());

        ExcelFile excelFile = converter.convert(Arrays.asList(offer0, offer1, offer2));

        String urlTitle = ExcelHeaders.URL.getTitle();
        MbocAssertions.assertThat(excelFile)
            .containsHeaders(urlTitle, urlTitle + " 2", urlTitle + " 3")

            .doesntContainValue(1, urlTitle)
            .doesntContainValue(1, urlTitle + " 2")
            .doesntContainValue(1, urlTitle + " 3")

            .containsValue(2, urlTitle, "https://yandex.ru")
            .containsValue(2, urlTitle + " 2", "https://google.com")
            .containsValue(2, urlTitle + " 3", "https://market.yandex.com")

            .containsValue(3, urlTitle, "https://yandex.ru")
            .doesntContainValue(3, urlTitle + " 2")
            .doesntContainValue(3, urlTitle + " 3");
    }

    @Test
    public void testParseCorrectTrackerFile() {
        Offer offer = new Offer()
            .setId(1)
            .setTitle("Offer 1")
            .setVendor("Vendor 1")
            .setShopSku("224455")
            .setIsOfferContentPresent(true)
            .storeOfferContent(
                OfferContent.builder()
                .addExtraShopFields("Экстра параметр", "фщжоыивамдтшфыови тмш фвдрышаиф дошывм").build());
        var givenOffers = List.of(offer);
        offerRepository.setOffers(givenOffers);

        Offer.Mapping skuMapping = new Offer.Mapping(1, DateTimeUtils.dateTimeNow());
        MatchingOffer expectedMatchingOffer = new MatchingOffer()
            .setId(1)
            .setSku(new Model().setId(1L).setCategoryId(100).setVendorId(101).setSkuParentModelId(21L))
            .setContentSkuMapping(skuMapping);

        ExcelFile.Builder excelFileBuilder = converter.convert(givenOffers).toBuilder();
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), 1);

        OffersParseResult<MatchingOffer> parseResult = converter.convert(excelFileBuilder.build());
        Assertions.assertThat(parseResult.throwIfFailedOrGet()).hasSize(1);
        Assertions.assertThat(parseResult.getOffer(0))
            .usingComparatorForType(MbocComparators.OFFERS_MAPPING_COMPARATOR, Offer.Mapping.class)
            .isEqualToComparingFieldByField(expectedMatchingOffer);
    }

    @Test
    public void testParseTrackerFileWithEmptyFieldWillFail() {
        Offer givenOffer = new Offer()
            .setId(1)
            .setTitle("Offer 1")
            .setVendor("Vendor 1")
            .setShopSku("224455")
            .setIsOfferContentPresent(true)
            .storeOfferContent(
                OfferContent.builder()
                .addExtraShopFields("Экстра параметр", "фщжоыивамдтшфыови тмш фвдрышаиф дошывм")
                .build()
            );

        ExcelFile excelFile = converter.convert(Collections.singletonList(givenOffer));

        OffersParseResult<MatchingOffer> parseResult = converter.convert(excelFile);
        Assertions.assertThat(parseResult.isFailed()).isTrue();

        Assertions.assertThatThrownBy(parseResult::throwIfFailed)
            .hasMessage("Ошибка на строке 2: " +
                "В оффере обязательно должен быть заполнен маппинг до sku или комментарий.");
    }

    @Test
    public void testErrorIfSkuDoesNotExist() {
        List<Offer> givenOffers = Collections.singletonList(new Offer()
            .setId(1)
            .setTitle("Offer 1")
            .setVendor("Vendor 1")
            .setShopSku("11111")
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category 1")
            .storeOfferContent(OfferContent.builder().build()));
        offerRepository.setOffers(givenOffers);

        ExcelFile.Builder excelFileBuilder = converter.convert(givenOffers).toBuilder();
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_MODEL_ID.getTitle(), 21);
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), 666);

        OffersParseResult<MatchingOffer> parseResult = converter.convert(excelFileBuilder.build());
        Assertions.assertThat(parseResult.isFailed()).isTrue();

        Assertions.assertThatThrownBy(parseResult::throwIfFailed)
            .hasMessage("Ошибка на строке 2: SKU 666 не существует.");
    }

    @Test
    public void testErrorIfModelNotPublished() {
        Model model = new Model();
        model.setId(777);
        model.setCategoryId(1);
        model.setTitle("Test");
        model.setModelType(Model.ModelType.SKU);

        modelStorageCachingServiceMock.addModel(model);

        List<Offer> givenOffers = Collections.singletonList(new Offer()
            .setId(1)
            .setTitle("Offer 1")
            .setVendor("Vendor 1")
            .setShopSku("11111")
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category 1")
            .storeOfferContent(OfferContent.builder().build()));
        offerRepository.setOffers(givenOffers);

        ExcelFile.Builder excelFileBuilder = converter.convert(givenOffers).toBuilder();
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_MODEL_ID.getTitle(), 21);
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), 777);

        OffersParseResult<MatchingOffer> parseResult = converter.convert(excelFileBuilder.build());
        Assertions.assertThat(parseResult.isFailed()).isTrue();

        Assertions.assertThatThrownBy(parseResult::throwIfFailed)
            .hasMessage("Ошибка на строке 2: Модель (sku id: 777) должна быть опубликована на синем маркете.");
    }

    @Test
    public void testErrorIfModelNotSku() {
        Model model = new Model();
        model.setId(777);
        model.setCategoryId(1);
        model.setTitle("Test");
        model.setPublishedOnBlueMarket(true);
        model.setModelType(Model.ModelType.GURU);

        modelStorageCachingServiceMock.addModel(model);

        List<Offer> givenOffers = Collections.singletonList(new Offer()
            .setId(1)
            .setTitle("Offer 1")
            .setVendor("Vendor 1")
            .setShopSku("11111")
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category 1")
            .storeOfferContent(OfferContent.builder().build()));
        offerRepository.setOffers(givenOffers);

        ExcelFile.Builder excelFileBuilder = converter.convert(givenOffers).toBuilder();
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_MODEL_ID.getTitle(), 21);
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), 777);

        OffersParseResult<MatchingOffer> parseResult = converter.convert(excelFileBuilder.build());
        Assertions.assertThat(parseResult.isFailed()).isTrue();

        Assertions.assertThatThrownBy(parseResult::throwIfFailed)
            .hasMessage("Ошибка на строке 2: Модель (sku id: 777) " +
                "должна быть SKU или иметь проставленный признак 'Модель является SKU'.");
    }

    @Test
    public void testErrorIfPartnerSkuHasNoSupplierId() {
        Model model = new Model();
        model.setId(777);
        model.setCategoryId(1);
        model.setTitle("Test");
        model.setPublishedOnBlueMarket(true);
        model.setModelType(Model.ModelType.PARTNER_SKU);

        modelStorageCachingServiceMock.addModel(model);

        List<Offer> givenOffers = Collections.singletonList(new Offer()
            .setId(1)
            .setTitle("Offer 1")
            .setVendor("Vendor 1")
            .setShopSku("11111")
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category 1")
            .storeOfferContent(OfferContent.builder().build()));
        offerRepository.setOffers(givenOffers);

        ExcelFile.Builder excelFileBuilder = converter.convert(givenOffers).toBuilder();
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_MODEL_ID.getTitle(), 21);
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), 777);

        OffersParseResult<MatchingOffer> parseResult = converter.convert(excelFileBuilder.build());
        Assertions.assertThat(parseResult.isFailed()).isTrue();

        Assertions.assertThatThrownBy(parseResult::throwIfFailed)
            .hasMessage("Ошибка на строке 2: Партнерская модель (sku id: 777) не содержит поставщика.");
    }

    @Test
    public void testErrorIfPsku10HasAnotherSupplierId() {
        Model psku10 = new Model();
        psku10.setId(777);
        psku10.setCategoryId(1);
        psku10.setTitle("Test");
        psku10.setPublishedOnBlueMarket(true);
        psku10.setSupplierId(2L);
        psku10.setModelType(Model.ModelType.PARTNER_SKU);

        modelStorageCachingServiceMock.addModel(psku10);

        List<Offer> givenOffers = Collections.singletonList(new Offer()
            .setId(1)
            .setBusinessId(1)
            .setTitle("Offer 1")
            .setVendor("Vendor 1")
            .setShopSku("11111")
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category 1")
            .storeOfferContent(OfferContent.builder().build()));
        offerRepository.setOffers(givenOffers);

        ExcelFile.Builder excelFileBuilder = converter.convert(givenOffers).toBuilder();
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_MODEL_ID.getTitle(), 21);
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), 777);

        OffersParseResult<MatchingOffer> parseResult = converter.convert(excelFileBuilder.build());
        Assertions.assertThat(parseResult.isFailed()).isTrue();

        Assertions.assertThatThrownBy(parseResult::throwIfFailed)
            .hasMessage("Ошибка на строке 2: Партнерская модель (sku id: 777) " +
                "содержит идентификатор поставщика (modelSupplierId: 2), " +
                "не совпадающий с поставщиком в маппинге (supplierId: 1).");
    }

    @Test
    public void testNoErrorIfPsku20HasAnotherSupplierId() {
        Model psku20 = new Model();
        psku20.setId(777);
        psku20.setCategoryId(1);
        psku20.setTitle("Test");
        psku20.setPublishedOnBlueMarket(true);
        psku20.setSupplierId(2L);
        psku20.setModelType(Model.ModelType.SKU);
        psku20.setModelQuality(Model.ModelQuality.PARTNER);

        modelStorageCachingServiceMock.addModel(psku20);

        List<Offer> givenOffers = Collections.singletonList(new Offer()
            .setId(1)
            .setBusinessId(1)
            .setTitle("Offer 1")
            .setVendor("Vendor 1")
            .setShopSku("11111")
            .setIsOfferContentPresent(true)
            .setShopCategoryName("Category 1")
            .storeOfferContent(OfferContent.builder().build()));
        offerRepository.setOffers(givenOffers);

        ExcelFile.Builder excelFileBuilder = converter.convert(givenOffers).toBuilder();
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_MODEL_ID.getTitle(), 21);
        excelFileBuilder.setValue(1, ExcelHeaders.MARKET_SKU_ID.getTitle(), 777);

        OffersParseResult<MatchingOffer> parseResult = converter.convert(excelFileBuilder.build());
        Assertions.assertThat(parseResult.isFailed()).isFalse();
    }

    private List<Offer> createTestOffers() {
        List<Offer> offers = new ArrayList<>();

        IntStream.range(0, TEST_OFFERS_COUNT).forEach(i -> {
            Offer offer = new Offer();
            OfferContent.OfferContentBuilder builder = OfferContent.builder();
            builder.urls(Collections.singletonList("https://my-shop.ru/shop/toys/" + i + ".html"));
            builder.description("Очень важный оффер");
            builder.addExtraShopFields("Random header", "test " + (i + 1));
            builder.addExtraShopFields("Параметр " + (i + 1), "странное значение");

            offer.setId(i + 1);
            offer.setTitle("Набор дорожных знаков №3 (" + i + " элементов) (в пакете)");
            offer.setVendor("Полесье");
            offer.setModelId(1234L + i);
            offer.setShopSku("12222" + i);
            offer.setCategoryIdForTests(1235235L - i, Offer.BindingKind.SUGGESTED);
            offer.setIsOfferContentPresent(true);
            offer.storeOfferContent(builder.build());

            offer.setSuggestSkuMapping(
                new Offer.Mapping(1L, LocalDateTime.parse("2007-12-03T10:15:30")));
            offer.setSupplierSkuMapping(
                new Offer.Mapping(2L, LocalDateTime.parse("2007-12-03T10:15:30")));
            offer.updateApprovedSkuMapping(
                new Offer.Mapping(3L, LocalDateTime.parse("2007-12-03T10:15:30")),
                Offer.MappingConfidence.CONTENT
            );
            offer.markLoadedContent();
            offers.add(offer);
        });

        return offers;
    }
}
