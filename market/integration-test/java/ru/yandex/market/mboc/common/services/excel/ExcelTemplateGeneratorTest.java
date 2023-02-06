package ru.yandex.market.mboc.common.services.excel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileAssertions;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.common.BaseIntegrationTestClass;
import ru.yandex.market.mboc.common.assertions.MbocAssertions;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataAsJsonDTO;
import ru.yandex.market.mboc.common.masterdata.model.MasterDataDto;
import ru.yandex.market.mboc.common.masterdata.model.SupplyEvent;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.SupplyScheduleConverter;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.TimeInUnitsConverter;
import ru.yandex.market.mboc.common.offers.model.OfferContent;
import ru.yandex.market.mboc.common.offers.model.OfferForService;
import ru.yandex.market.mboc.common.offers.repository.IMasterDataRepository;
import ru.yandex.market.mboc.common.offers.repository.MasterDataRepositoryMock;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierHelper;
import ru.yandex.market.mboc.common.services.business.BusinessSupplierService;
import ru.yandex.market.mboc.common.services.excel.template.ExcelGeneratorTemplateCacheDownloader;
import ru.yandex.market.mboc.common.services.excel.template.ExcelTemplateGenerator;
import ru.yandex.market.mboc.common.services.excel.template.GeneratorContext;
import ru.yandex.market.mboc.common.services.excel.template.TemplateExcelColumnSpecs;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.ThrowingConsumer;

/**
 * Интеграционные тесты для тестирования {@link ExcelTemplateGenerator}.
 *
 * @author zilberoman
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ExcelTemplateGeneratorTest extends BaseIntegrationTestClass {
    private static final int TEMPLATE_HEADERS_OFFSET = 4;
    private static final long SEED = 15369765L;

    @Resource
    private ExcelGeneratorTemplateCacheDownloader excelGeneratorTemplateCacheDownloader;

    @Resource
    private TemplateExcelColumnSpecs excelColumnSpecs;

    private ExcelTemplateGenerator generator;

    @Resource
    private OfferRepository offerRepository;

    @Resource
    private SupplierRepository supplierRepository;

    @Resource
    private TransactionHelper transactionHelper;

    private List<OfferForService> offers;
    private List<Supplier> suppliers;

    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;
    private IMasterDataRepository masterDataRepository;

    private BusinessSupplierService businessSupplierService;

    private EnhancedRandom defaultRandom;

    @Before
    public void setUp() throws Exception {
        defaultRandom = TestDataUtils.defaultRandom(SEED);
        masterDataRepository = new MasterDataRepositoryMock();

        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        var storageKeyValueService = new StorageKeyValueServiceMock();
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        businessSupplierService = new BusinessSupplierService(supplierRepository, offerRepository);

        generator = new ExcelTemplateGenerator(
            excelGeneratorTemplateCacheDownloader,
            masterDataHelperService,
            excelColumnSpecs,
            masterDataRepository
        );

        var baseOffers = YamlTestUtil.readOffersFromResources("offers/offers-for-export.yml");
        System.out.println(baseOffers);
        offers = BusinessSupplierHelper.getAllOffersForService(baseOffers);
        suppliers = YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml");
    }

    @Test
    public void testSimpleGeneration() throws IOException {
        GeneratorContext context = new GeneratorContext()
            .setFillSuggestColumns(true);

        processTemplateFile(offers, context, false, file -> {
            // Если вы пришли сюда, так как тест стал неожиданно падать,
            // то значит поменялся xlsm шаблон, по которому файл генерируется

            MbocAssertions.assertSoftly(softly -> {
                var excelFileAssertions = softly.assertThat(file);

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Ваш SKU", "sku1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Название товара", "Title1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Категория", "Category1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "SKU на Яндексе", "101")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET, "Возможный SKU на Яндексе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET, "Название товара на маркетплейсе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET, "Категория на маркетплейсе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET, "Страница товара на маркетплейсе");

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 1, "Ваш SKU", "sku2")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 1, "Название товара", "Title2")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 1, "Категория", "Category2")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 1, "SKU на Яндексе", "201")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 1, "Возможный SKU на Яндексе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 1, "Название товара на маркетплейсе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 1, "Категория на маркетплейсе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 1, "Страница товара на маркетплейсе");

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 2, "Ваш SKU", "sku3")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 2, "Название товара", "Title3")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 2, "Категория", "Category3")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 2, "SKU на Яндексе")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 2, "Название товара на маркетплейсе", "marketModelName3")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 2, "Категория на маркетплейсе")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 2, "Возможный SKU на Яндексе", "300")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 2, "Страница товара на маркетплейсе",
                        "https://pokupki.market.yandex.ru/product/300");

                excelFileAssertions
                    .doesntContainValuesOnLine(TEMPLATE_HEADERS_OFFSET + offers.size());
            });
        });
    }

    @Test
    public void testGenerationWithoutSuggestedColumns() throws IOException {
        GeneratorContext context = new GeneratorContext()
            .setFillSuggestColumns(false);

        processTemplateFile(offers, context, false, file -> {
            MbocAssertions.assertSoftly(softly -> {
                ExcelFileAssertions excelFileAssertions = softly.assertThat(file);

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Ваш SKU", "sku1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Название товара", "Title1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Категория", "Category1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "SKU на Яндексе", "101")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET, "Возможный SKU на Яндексе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET, "Ссылка на товар")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET, "Название товара на маркетплейсе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET, "Категория на маркетплейсе");

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 1, "Ваш SKU", "sku2")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 1, "Название товара", "Title2")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 1, "Категория", "Category2")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 1, "SKU на Яндексе", "201")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 1, "Возможный SKU на Яндексе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 1, "Ссылка на товар")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 1, "Название товара на маркетплейсе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 1, "Категория на маркетплейсе");

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 2, "Ваш SKU", "sku3")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 2, "Название товара", "Title3")
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 2, "Категория", "Category3")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 2, "SKU на Яндексе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 2, "Возможный SKU на Яндексе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 2, "Ссылка на товар")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 2, "Название товара на маркетплейсе")
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 2, "Категория на маркетплейсе");

                excelFileAssertions
                    .doesntContainValuesOnLine(TEMPLATE_HEADERS_OFFSET + offers.size());
            });
        });
    }

    @Test
    public void testMasterDataAreAlsoExportToTemplate() throws IOException {
        supplierRepository.insertBatch(suppliers);
        insertOffersForService(offers);

        MasterData masterData = TestDataUtils.generateMasterData("sku1", 1, defaultRandom);
        masterData.setManufacturerCountries(Arrays.asList("Россия", "Китай"));
        masterData.addSupplyEvent(new SupplyEvent(DayOfWeek.MONDAY));
        masterData.addSupplyEvent(new SupplyEvent(DayOfWeek.THURSDAY));
        masterDataRepository.insert(new MasterDataAsJsonDTO(MasterDataDto.from(masterData), "sku1", 1));

        GeneratorContext context = new GeneratorContext()
            .setFillSuggestColumns(false)
            .setFillMasterData(true);

        processTemplateFile(offers, context, false, file -> {
            MbocAssertions.assertSoftly(softly -> {
                ExcelFileAssertions excelFileAssertions = softly.assertThat(file);

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Ваш SKU", "sku1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Название товара", "Title1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Категория", "Category1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "SKU на Яндексе", "101")
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.MANUFACTURER.getTitle(), masterData.getManufacturer())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.MANUFACTURER_COUNTRY.getTitle(), "Россия,Китай")
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.SHELF_LIFE.getTitle(),
                        TimeInUnitsConverter.convertToStringRussian(masterData.getShelfLife()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.LIFE_TIME.getTitle(),
                        TimeInUnitsConverter.convertToStringRussian(masterData.getLifeTime()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.GUARANTEE_PERIOD.getTitle(),
                        TimeInUnitsConverter.convertToStringRussian(masterData.getGuaranteePeriod()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.MIN_SHIPMENT.getTitle(), masterData.getMinShipment())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.TRANSPORT_UNIT_SIZE.getTitle(), masterData.getTransportUnit())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.QUANTUM_OF_SUPPLY.getTitle(), masterData.getQuantumOfSupply())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.DELIVERY_TIME.getTitle(), masterData.getDeliveryTime())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.SUPPLY_SCHEDULE.getTitle(),
                        SupplyScheduleConverter.convertToString(masterData.getSupplySchedule())
                    );

                excelFileAssertions
                    .doesntContainValuesOnLineExcept(TEMPLATE_HEADERS_OFFSET + 1, "Ваш SKU",
                        "Название товара", "Категория", "SKU на Яндексе");
            });
        });
    }

    @Test
    public void testMasterDataOrderIsCorrect() throws IOException {
        supplierRepository.insertBatch(suppliers);
        insertOffersForService(offers);

        MasterData masterData = TestDataUtils.generateMasterData("sku1", 1, defaultRandom);
        masterData.setManufacturerCountries(Arrays.asList("Россия", "Китай"));
        masterData.addSupplyEvent(new SupplyEvent(DayOfWeek.MONDAY));
        masterData.addSupplyEvent(new SupplyEvent(DayOfWeek.THURSDAY));
        MasterData masterData2 = TestDataUtils.generateMasterData("sku2", 2, defaultRandom);
        masterData2.setManufacturerCountries(Arrays.asList("Россия", "Китай"));
        masterData2.addSupplyEvent(new SupplyEvent(DayOfWeek.FRIDAY));
        masterData2.addSupplyEvent(new SupplyEvent(DayOfWeek.WEDNESDAY));
        MasterData masterData3 = TestDataUtils.generateMasterData("sku3", 3, defaultRandom);
        masterData3.setManufacturerCountries(Arrays.asList("Россия", "Китай"));
        masterData3.addSupplyEvent(new SupplyEvent(DayOfWeek.FRIDAY));
        masterData3.addSupplyEvent(new SupplyEvent(DayOfWeek.WEDNESDAY));
        masterDataRepository.insert(new MasterDataAsJsonDTO(MasterDataDto.from(masterData), "sku1", 1));
        masterDataRepository.insert(new MasterDataAsJsonDTO(MasterDataDto.from(masterData2), "sku2", 2));
        masterDataRepository.insert(new MasterDataAsJsonDTO(MasterDataDto.from(masterData3), "sku3", 3));
        List<MasterData> masterDataList = List.of(masterData, masterData2, masterData3);

        GeneratorContext context = new GeneratorContext()
            .setFillSuggestColumns(false)
            .setFillMasterData(true);

        processTemplateFile(offers, context, false, file -> {
            MbocAssertions.assertSoftly(softly -> {
                ExcelFileAssertions excelFileAssertions = softly.assertThat(file);
                for (int i = 0; i < offers.size(); i++) {
                    excelFileAssertions
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i, "Ваш SKU", offers.get(i).getShopSku())
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i, "Название товара",
                            offers.get(i).getBaseOffer().getTitle())
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i, "Категория",
                            offers.get(i).getBaseOffer().getShopCategoryName())
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i, "SKU на Яндексе",
                            offers.get(i).getBaseOffer().getApprovedSkuId())
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.MANUFACTURER.getTitle(), masterDataList.get(i).getManufacturer())
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.MANUFACTURER_COUNTRY.getTitle(), "Россия,Китай")
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.SHELF_LIFE.getTitle(),
                            TimeInUnitsConverter.convertToStringRussian(masterDataList.get(i).getShelfLife()))
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.LIFE_TIME.getTitle(),
                            TimeInUnitsConverter.convertToStringRussian(masterDataList.get(i).getLifeTime()))
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.GUARANTEE_PERIOD.getTitle(),
                            TimeInUnitsConverter.convertToStringRussian(masterDataList.get(i).getGuaranteePeriod()))
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.MIN_SHIPMENT.getTitle(), masterDataList.get(i).getMinShipment())
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.TRANSPORT_UNIT_SIZE.getTitle(), masterDataList.get(i).getTransportUnit())
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.QUANTUM_OF_SUPPLY.getTitle(), masterDataList.get(i).getQuantumOfSupply())
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.DELIVERY_TIME.getTitle(), masterDataList.get(i).getDeliveryTime())
                        .containsValue(TEMPLATE_HEADERS_OFFSET + i,
                            ExcelHeaders.SUPPLY_SCHEDULE.getTitle(),
                            SupplyScheduleConverter.convertToString(masterDataList.get(i).getSupplySchedule())
                        );
                }
            });
        });
    }

    @Test
    public void testSuccessfulExportWithIncorrectUrl() throws IOException {
        final String url = "https://www.arabsoap.ru/upload/iblock/660/Z1011_0231_Aleppo Premium Soap №13.jpg";
        OfferForService offer = this.offers.get(0);
        offer.getBaseOffer().storeOfferContent(OfferContent.builder().urls(Collections.singletonList(url)).build());
        processTemplateFile(Collections.singletonList(offer), new GeneratorContext(), false, file -> {
            MbocAssertions.assertSoftly(softly -> {
                ExcelFileAssertions excelFileAssertions = softly.assertThat(file);

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Ссылка на товар", url);
            });
        });
    }

    @Test
    public void testCommaInPrice() throws IOException {
        OfferForService offer = OfferTestUtils.simpleOfferForService();
        offer.getBaseOffer().storeOfferContent(
            OfferContent.builder().extraShopFields(Collections.singletonMap(ExcelHeaders.PRICE.getTitle(), "6,99")).build()
        );

        processTemplateFile(Collections.singletonList(offer), new GeneratorContext(), false, file -> {
            MbocAssertions.assertSoftly(softly -> {
                ExcelFileAssertions excelFileAssertions = softly.assertThat(file);

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Текущая цена", "6.99");
            });
        });
    }

    @Test
    public void testMasterDataExportTo1PTemplate() throws IOException {
        supplierRepository.insertBatch(suppliers);
        insertOffersForService(offers);

        MasterData masterData = TestDataUtils.generateMasterData("sku1", 1, defaultRandom);
        masterData.setManufacturerCountries(Arrays.asList("Россия", "Китай"));
        masterData.addSupplyEvent(new SupplyEvent(DayOfWeek.MONDAY));
        masterData.addSupplyEvent(new SupplyEvent(DayOfWeek.THURSDAY));
        masterData.setUseInMercury(true);
        masterData.setVetisGuids(List.of("0b9a220f-a1d4-4ba9-8a55-b785c4d99f5d"));
        masterDataRepository.insert(new MasterDataAsJsonDTO(MasterDataDto.from(masterData), "sku1", 1));

        GeneratorContext context = new GeneratorContext()
            .setFillSuggestColumns(false)
            .setFillMasterData(true);

        processTemplateFile(offers, context, true, file -> {
            MbocAssertions.assertSoftly(softly -> {
                ExcelFileAssertions excelFileAssertions = softly.assertThat(file);

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Ваш SKU", "sku1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Название товара", "Title1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Категория", "Category1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "SKU на Маркете", "101")
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.MANUFACTURER.getTitle(), masterData.getManufacturer())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.MANUFACTURER_COUNTRY.getTitle(), "Россия,Китай")
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.SHELF_LIFE.getTitle(),
                        TimeInUnitsConverter.convertToStringRussian(masterData.getShelfLife()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.LIFE_TIME.getTitle(),
                        TimeInUnitsConverter.convertToStringRussian(masterData.getLifeTime()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.GUARANTEE_PERIOD.getTitle(),
                        TimeInUnitsConverter.convertToStringRussian(masterData.getGuaranteePeriod()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.MIN_SHIPMENT.getTitle(), masterData.getMinShipment())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.TRANSPORT_UNIT_SIZE.getTitle(), masterData.getTransportUnit())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.QUANTUM_OF_SUPPLY.getTitle(), masterData.getQuantumOfSupply())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.DELIVERY_TIME.getTitle(), masterData.getDeliveryTime())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.SUPPLY_SCHEDULE.getTitle(),
                        SupplyScheduleConverter.convertToString(masterData.getSupplySchedule()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.USE_IN_MERCURY.getTitle(), masterData.getUseInMercury() ? "да" : "нет")
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.VETIS_GUIDS.getTitle(), masterData.getVetisGuids().get(0)
                    );

                excelFileAssertions
                    .doesntContainValuesOnLineExcept(TEMPLATE_HEADERS_OFFSET + 1, "Ваш SKU",
                        "Название товара", "Категория", "SKU на Маркете");
            });
        });
    }

    @Test
    public void testParallelImportedExportTo1PTemplate() throws IOException {
        supplierRepository.insertBatch(suppliers);
        offerRepository.deleteAllInTest();

        var baseOffers = YamlTestUtil.readOffersFromResources("offers/offers-parallel-imported-for-export.yml");
        System.out.println(baseOffers);
        var offers = BusinessSupplierHelper.getAllOffersForService(baseOffers);
        insertOffersForService(offers);

        GeneratorContext context = new GeneratorContext()
            .setFillSuggestColumns(false)
            .setFillMasterData(false);

        processTemplateFile(offers, context, true, file ->
            MbocAssertions.assertSoftly(softly -> {
                ExcelFileAssertions excelFileAssertions = softly.assertThat(file);

                // Parallel imported - cell should contain value
                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Ваш SKU", "sku1");
                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Параллельный импорт", "Да");

                // Not parallel imported - cell should be empty
                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET + 1, "Ваш SKU", "sku2");
                excelFileAssertions
                    .doesntContainValue(TEMPLATE_HEADERS_OFFSET + 1, "Параллельный импорт");
            }));

        offerRepository.deleteAllInTest();
    }

    private void processTemplateFile(List<OfferForService> offers, GeneratorContext context,
                                     boolean use1PTemplate, ThrowingConsumer<ExcelFile> consumer)
        throws IOException {
        //offers for template fetched from db
        offers.forEach(offer -> offer.getBaseOffer().markLoadedContent());
        MbocSupplierType supplierType = use1PTemplate ? MbocSupplierType.REAL_SUPPLIER : MbocSupplierType.THIRD_PARTY;
        generator.generateExcelByTemplate(offers, context, supplierType, path -> {
            Assertions.assertThat(path.toFile())
                .exists()
                .isFile()
                .canRead();

            try (InputStream inputStream = Files.newInputStream(path)) {
                ExcelFile file = ExcelFileConverter.convert(inputStream, ExcelHeaders.MBOC_EXCEL_IGNORES_CONFIG);

                consumer.consume(file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void insertOffersForService(List<OfferForService> offersForService) {
        var baseOffersToSave = offersForService.stream()
            .map(OfferForService::getBaseOffer)
            .collect(Collectors.toList());
        transactionHelper.doInTransactionVoid(__ -> {
            offerRepository.insertOffers(baseOffersToSave);
        });
    }

    @Test
    public void testSskuAvailabilityExport() throws IOException {
        supplierRepository.insertBatch(suppliers);
        insertOffersForService(offers);
        MasterData masterData = TestDataUtils.generateMasterData("sku1", 1, defaultRandom);
        masterData.setManufacturerCountries(Arrays.asList("Россия", "Китай"));
        masterData.addSupplyEvent(new SupplyEvent(DayOfWeek.MONDAY));
        masterData.addSupplyEvent(new SupplyEvent(DayOfWeek.THURSDAY));
        masterDataRepository.insert(new MasterDataAsJsonDTO(MasterDataDto.from(masterData), "sku1", 1));

        GeneratorContext context = new GeneratorContext()
            .setFillSuggestColumns(false)
            .setFillMasterData(true);

        processTemplateFile(offers, context, false, file -> {
            MbocAssertions.assertSoftly(softly -> {
                ExcelFileAssertions excelFileAssertions = softly.assertThat(file);

                excelFileAssertions
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Ваш SKU", "sku1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Название товара", "Title1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "Категория", "Category1")
                    .containsValue(TEMPLATE_HEADERS_OFFSET, "SKU на Яндексе", "101")
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.MANUFACTURER.getTitle(), masterData.getManufacturer())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.MANUFACTURER_COUNTRY.getTitle(), "Россия,Китай")
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.SHELF_LIFE.getTitle(),
                        TimeInUnitsConverter.convertToStringRussian(masterData.getShelfLife()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.LIFE_TIME.getTitle(),
                        TimeInUnitsConverter.convertToStringRussian(masterData.getLifeTime()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.GUARANTEE_PERIOD.getTitle(),
                        TimeInUnitsConverter.convertToStringRussian(masterData.getGuaranteePeriod()))
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.MIN_SHIPMENT.getTitle(), masterData.getMinShipment())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.TRANSPORT_UNIT_SIZE.getTitle(), masterData.getTransportUnit())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.QUANTUM_OF_SUPPLY.getTitle(), masterData.getQuantumOfSupply())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.DELIVERY_TIME.getTitle(), masterData.getDeliveryTime())
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        ExcelHeaders.SUPPLY_SCHEDULE.getTitle(),
                        SupplyScheduleConverter.convertToString(masterData.getSupplySchedule())
                    )
                    .containsValue(TEMPLATE_HEADERS_OFFSET,
                        "Планы по поставкам",
                        "");

                excelFileAssertions
                    .doesntContainValuesOnLineExcept(TEMPLATE_HEADERS_OFFSET + 1, "Ваш SKU",
                        "Название товара", "Категория", "SKU на Яндексе");
            });
        });
    }
}
