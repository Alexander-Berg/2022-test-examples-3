package ru.yandex.market.mboc.common.masterdata.services.united;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuGoldenParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuIdOrOrphanSskuKeyInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmMskuIdOrShopSkuKeyContainer;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.util.SskuGoldenParamUtil;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.model.QualityDocument;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;


public class SskuUnitedProcessingServiceMasterDataTest extends UnitedProcessingServiceSetupBaseTest {

    private static final ShopSkuKey BUSINESS_KEY1 = new ShopSkuKey(1000, "sku");
    private static final ShopSkuKey SERVICE_KEY1 = new ShopSkuKey(1, "sku");
    private static final ShopSkuKey SERVICE_KEY2 = new ShopSkuKey(2, "sku");

    private static final ShopSkuKey BUSINESS_KEY2 = new ShopSkuKey(2000, "sku2");
    private static final ShopSkuKey SERVICE_KEY3 = new ShopSkuKey(3, "sku2");

    // MARKETMDM-43: Change back to 19 after fix MARKETMDM-154.
    private static final int LOWER_OLDER_DELIVERY_TIME = 20;
    // MARKETMDM-43: Change back to 3 after fix MARKETMDM-154.
    private static final int LOWER_OLDER_BOX_COUNT = 4;

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void before() {
        sskuExistenceRepository.markExistence(List.of(BUSINESS_KEY1, BUSINESS_KEY2, SERVICE_KEY1,
            SERVICE_KEY2, SERVICE_KEY3), true);
        prepareSskuGroup();
    }

    @Test
    public void testThatProcessSskuShouldSaveDatacampMDVersionWithoutChanges() {
        QualityDocument[] documents = {
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random),
            TestDataUtils.generateCorrectDocument(random)
        };
        MasterData masterData = TestDataUtils.generateMasterData(BUSINESS_KEY1, random, documents);
        masterData.setCategoryId(98L).setDatacampMasterDataVersion(777L);
        masterDataRepository.insert(masterData);

        unitedProcessingService.processKeys(List.of(
            new MdmMskuIdOrOrphanSskuKeyInfo().setEntityKey(MdmMskuIdOrShopSkuKeyContainer.ofSsku(BUSINESS_KEY1))));
        Assertions.assertThat(masterDataRepository.findByShopSkuKeys(List.of(BUSINESS_KEY1)).size()).isEqualTo(1);
        MasterData savedMasterData = masterDataRepository.findByShopSkuKeys(List.of(BUSINESS_KEY1)).get(0);
        Assertions.assertThat(savedMasterData.getDatacampMasterDataVersion())
            .isEqualTo(masterData.getDatacampMasterDataVersion());
        Assertions.assertThat(savedMasterData.getModifiedTimestamp())
            .isNotEqualTo(masterData.getModifiedTimestamp());
    }

    @Test
    public void testThatShelfLifeBlockIsBuiltIgnoringInfoFromMasterDataTable() {
        silverSskuRepository.deleteAll();

        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");

        initSuppliers(bizKey, serviceKey);

        Instant ts = Instant.now();
        Instant olderTs = ts.minus(17, ChronoUnit.DAYS);
        Instant veryOldTs = ts.minus(100, ChronoUnit.DAYS);

        String worseSource = "worse_source";
        String bestSource = "best_source";

        String worseShelLifeComment = "хранить в сухом помещении!";
        String bestShelLifeComment = "хранить в сухом помещении!!!";

        MasterData masterData = electrumSskuWithOldShelfLifeComment(serviceKey);
        masterData.setModifiedTimestamp(LocalDateTime.now().minusDays(14));
        masterDataRepository.insertOrUpdate(masterData);

        var shelfLifeByService = silverSskuWithShelfLife(serviceKey, worseSource,
            MasterDataSourceType.SUPPLIER, veryOldTs, 0, false, worseShelLifeComment, 1L);
        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, bestSource,
            MasterDataSourceType.SUPPLIER, olderTs, 5, true, bestShelLifeComment, 586L);

        silverSskuRepository.insertOrUpdateSsku(shelfLifeByService);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);

        processShopSkuKeys(List.of(bizKey, serviceKey));

        MasterData resultMasterData = masterDataRepository.findById(serviceKey);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(1);

        MasterData expectedMasterData = electrumSskuWithOldShelfLifeComment(serviceKey)
            .setShelfLife(5, TimeInUnits.TimeUnit.YEAR)
            .setShelfLifeComment(bestShelLifeComment);
        Assertions.assertThat(resultMasterData).isEqualTo(expectedMasterData);
    }

    @Test
    public void testThatShelfLifeBlockIsBuiltUsingOriginalTimestampsFromSilverParamValuesTable() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");

        initSuppliers(bizKey, serviceKey);

        Instant ts = Instant.now();
        Instant olderTs = ts.minus(17, ChronoUnit.DAYS);
        Instant oldestTs = ts.minus(100, ChronoUnit.DAYS);
        Instant fresherTs = ts.plus(17, ChronoUnit.DAYS);

        String worseSource = "worse_source";
        String bestSource = "best_source";

        String worseShelLifeComment = "хранить в сухом помещении!";
        String bestShelLifeComment = "хранить в сухом помещении!!!";

        List<String> regNumbers = List.of("regNumber");
        List<String> countries = List.of("China");

        MasterData masterData = electrumSskuWithOldShelfLifeComment(serviceKey);
        masterData.setModifiedTimestamp(LocalDateTime.now().minusDays(14));
        masterDataRepository.insertOrUpdate(masterData);

        var shelfLifeByBizOldest = silverSskuWithShelfLife(bizKey, worseSource,
            MasterDataSourceType.SUPPLIER, oldestTs, 0, false, worseShelLifeComment, 1L);
        var regNumberByService = silverSsku(serviceKey, worseSource,
            MasterDataSourceType.SUPPLIER, oldestTs, 0, 0, List.of(), regNumbers, null);

        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, bestSource,
            MasterDataSourceType.SUPPLIER, olderTs, 5, true, bestShelLifeComment, 586L);
        var countriesByBiz = silverSsku(serviceKey, worseSource,
            MasterDataSourceType.SUPPLIER, fresherTs, 0, 0, countries, List.of(), null);

        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBizOldest);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);
        silverSskuRepository.insertOrUpdateSsku(regNumberByService);
        silverSskuRepository.insertOrUpdateSsku(countriesByBiz);

        processShopSkuKeys(List.of(serviceKey));

        MasterData resultMasterData = masterDataRepository.findById(serviceKey);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(1);

        MasterData expectedMasterData = electrumSskuWithOldShelfLifeComment(serviceKey)
            .setShelfLife(5, TimeInUnits.TimeUnit.YEAR)
            .setShelfLifeComment(bestShelLifeComment)
            .setManufacturerCountries(countries);
        Assertions.assertThat(resultMasterData).isEqualTo(expectedMasterData);
    }

    @Test
    public void testGuaranteePeriodAppearsInGoldenItemIfShelfLifeAndGuaranteePeriodHaveTheSameValue() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        initSuppliers(bizKey, serviceKey);

        Instant ts = Instant.now();
        String source = "best_source";

        // 1. Сохраняем в серебро Срок Годности и Срок Гарантии с одинаковыми значениями по бизнес-ключу
        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, source,
            MasterDataSourceType.SUPPLIER, ts, 5, true, null, 586L);
        var guaranteePeriodByBiz = silverSskuWithGuaranteePeriod(bizKey, source,
            MasterDataSourceType.SUPPLIER, ts, 5, true, null, 586L);

        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);
        silverSskuRepository.insertOrUpdateSsku(guaranteePeriodByBiz);

        // 2. Вычисляем золото
        processShopSkuKeys(List.of(serviceKey));

        // 3. Проверяем, что в золоте сохранен и Срок Годности, и Срок Гарантии
        MasterData resultMasterData = masterDataRepository.findById(serviceKey);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(1);

        MasterData expectedMasterData = new MasterData()
            .setShopSkuKey(serviceKey)
            .setShelfLife(5, TimeInUnits.TimeUnit.YEAR)
            .setGuaranteePeriod(5, TimeInUnits.TimeUnit.YEAR);
        Assertions.assertThat(resultMasterData).isEqualTo(expectedMasterData);
    }

    @Test
    public void testThatGuaranteePeriodIgnoringInfoFromMasterDataTable() {
        silverSskuRepository.deleteAll();

        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");

        initSuppliers(bizKey, serviceKey);

        Instant ts = Instant.now();
        Instant olderTs = ts.minus(17, ChronoUnit.DAYS);
        Instant veryOldTs = ts.minus(100, ChronoUnit.DAYS);

        String worseSource = "worse_source";
        String bestSource = "best_source";

        String worseComment = "Комментарий про срок гарантии. Bad";
        String bestComment = "Комментарий про срок гарантии. Good";

        MasterData masterData = electrumSskuWithOldGuaranteePeriodComment(serviceKey);
        masterData.setModifiedTimestamp(LocalDateTime.now().minusDays(14));
        masterDataRepository.insertOrUpdate(masterData);

        var serviceSsku = silverSskuWithGuaranteePeriod(serviceKey, worseSource,
            MasterDataSourceType.SUPPLIER, veryOldTs, 0, false, worseComment, 1L);
        var businessSsku = silverSskuWithGuaranteePeriod(bizKey, bestSource,
            MasterDataSourceType.SUPPLIER, olderTs, 1, true, bestComment, 586L);

        silverSskuRepository.insertOrUpdateSsku(serviceSsku);
        silverSskuRepository.insertOrUpdateSsku(businessSsku);

        processShopSkuKeys(List.of(bizKey, serviceKey));

        MasterData resultMasterData = masterDataRepository.findById(serviceKey);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(1);

        MasterData expectedMasterData = electrumSskuWithOldGuaranteePeriodComment(serviceKey)
            .setShelfLife(null)
            .setShelfLifeComment(null)
            .setGuaranteePeriod(1, TimeInUnits.TimeUnit.YEAR)
            .setGuaranteePeriodComment(bestComment);
        Assertions.assertThat(resultMasterData).isEqualTo(expectedMasterData);
    }

    @Test
    public void testSilverValuesMergedToGold() {
        Instant ts = Instant.now();
        var editorSsku = silverSsku(SERVICE_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), null);
        var supplierSsku = silverSsku(SERVICE_KEY2, "supplier", MasterDataSourceType.SUPPLIER, ts,
            LOWER_OLDER_BOX_COUNT, LOWER_OLDER_DELIVERY_TIME, List.of("Монголия"), List.of(), null);
        var anotherSsku = silverSsku(SERVICE_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts.plusSeconds(1),
            4, 20, List.of("Вьетнам"), List.of(), null);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(supplierSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);
        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());

        processShopSkuKeys(List.of(SERVICE_KEY1, SERVICE_KEY2));

        MasterData service1 = masterDataRepository.findById(SERVICE_KEY1);
        MasterData service2 = masterDataRepository.findById(SERVICE_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        MasterData expected1 = electrumSsku(SERVICE_KEY1, 2, 18,
            List.of("Монголия", "Китай", "Вьетнам"), List.of());
        MasterData expected2 = electrumSsku(SERVICE_KEY2, 2, 20,
            List.of("Монголия", "Китай", "Вьетнам"), List.of());
        Assertions.assertThat(service1).isEqualTo(expected1);
        Assertions.assertThat(service2).isEqualTo(expected2);
    }

    @Test
    public void testSilverValuesWithoutEoxPresenceAreNotMergedToGold() {
        Instant ts = Instant.now();
        sskuExistenceRepository.markExistence(SERVICE_KEY1, false);
        var editorSsku = silverSsku(SERVICE_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), null);
        var supplierSsku = silverSsku(SERVICE_KEY2, "supplier", MasterDataSourceType.SUPPLIER, ts,
            LOWER_OLDER_BOX_COUNT, LOWER_OLDER_DELIVERY_TIME, List.of("Монголия"), List.of(), null);
        var anotherSsku = silverSsku(SERVICE_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts.plusSeconds(1),
            4, 20, List.of("Вьетнам"), List.of(), null);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(supplierSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);

        processShopSkuKeys(List.of(SERVICE_KEY1, SERVICE_KEY2));

        MasterData service2 = masterDataRepository.findById(SERVICE_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(1);

        MasterData expected2 = electrumSsku(SERVICE_KEY2, 4, 20,
            List.of("Вьетнам", "Монголия"), List.of());
        Assertions.assertThat(service2).isEqualTo(expected2);
    }

    @Test
    public void testSilverValuesMergedToGoldAndMasterDataVersionsSavedAfterVerdictCalculation() {
        Instant ts = Instant.now();
        long mdVersionBusinessKey = 5L;
        long mdVersionServiceKey1 = 2L;
        long mdVersion1ServiceKey2 = 1L;
        long mdVersion2ServiceKey2 = 3L;
        var eoxSsku = silverSsku(BUSINESS_KEY1, "supplier", MasterDataSourceType.SUPPLIER, ts,
            1, 17, List.of("Россия", "Монголия", "Китай", "Вьетнам"), List.of(), mdVersionBusinessKey);
        // master data version берется из бизнес-части
        var editorSsku = silverSsku(SERVICE_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), mdVersionServiceKey1);
        var supplierSsku = silverSsku(SERVICE_KEY2, "supplier", MasterDataSourceType.SUPPLIER, ts,
            LOWER_OLDER_BOX_COUNT, LOWER_OLDER_DELIVERY_TIME, List.of("Монголия"), List.of(), mdVersion2ServiceKey2);
        var anotherSsku = silverSsku(SERVICE_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts.plusSeconds(1),
            4, 20, List.of("Вьетнам"), List.of(), mdVersion1ServiceKey2);
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(supplierSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);
        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());

        processShopSkuKeys(List.of(SERVICE_KEY1));

        MasterData service1 = masterDataRepository.findById(SERVICE_KEY1);
        MasterData service2 = masterDataRepository.findById(SERVICE_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        MasterData expected1 = electrumSsku(SERVICE_KEY1, 2, 18,
            List.of("Россия", "Монголия", "Китай", "Вьетнам"), List.of());
        MasterData expected2 = electrumSsku(SERVICE_KEY2, 2, 20,
            List.of("Россия", "Монголия", "Китай", "Вьетнам"), List.of());
        Assertions.assertThat(service1).isEqualTo(expected1);
        Assertions.assertThat(service2).isEqualTo(expected2);

        SskuVerdictResult goldenVerdict = sskuGoldenVerdictRepository
            .findById(BUSINESS_KEY1);

        // master data version должна совпадать с тем, что лежит в mdm.ssku_silver_param_value
        Assertions.assertThat(goldenVerdict.getContentVersionId()).isEqualTo(mdVersionBusinessKey);
    }

    @Test
    public void testBaseMultivaluesDominateOther() {
        Instant ts = Instant.now();
        var eoxSsku = silverSsku(BUSINESS_KEY1, "supplier", MasterDataSourceType.SUPPLIER, ts,
            1, 17, List.of("Россия"), List.of(), null);
        var editorSsku = silverSsku(SERVICE_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), null);
        var supplierSsku = electrumSsku(SERVICE_KEY2, 3, 19, List.of("Монголия", "Вьетнам"), List.of());
        var anotherSsku = silverSsku(SERVICE_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts,
            4, 20, List.of("Вьетнам"), List.of(), null);
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);
        sleep(10);
        masterDataRepository.insertBatch(supplierSsku);
        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());

        processShopSkuKeys(List.of(SERVICE_KEY1));

        MasterData service1 = masterDataRepository.findById(SERVICE_KEY1);
        MasterData service2 = masterDataRepository.findById(SERVICE_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        MasterData expected1 = electrumSsku(SERVICE_KEY1, 2, 18,
            List.of("Россия"), List.of());
        MasterData expected2 = electrumSsku(SERVICE_KEY2, 2, 20,
            List.of("Россия"), List.of());
        Assertions.assertThat(service1).isEqualTo(expected1);
        Assertions.assertThat(service2).isEqualTo(expected2);
    }

    @Test
    @Ignore("MARKETMDM-948")
    public void testDominatingMultivaluesPickedByExistenceAndPriority() {
        Instant ts = Instant.now();
        qualityDocumentRepository.insertBatch(List.of(
            generateQualityDocument("1234"), generateQualityDocument("5678")));
        var eoxSsku = silverSsku(BUSINESS_KEY1, "supplier", MasterDataSourceType.SUPPLIER, ts,
            1, 17, List.of("Россия"), List.of(), null);
        var warehouseSsku = silverSsku(BUSINESS_KEY1, "warehouse", MasterDataSourceType.WAREHOUSE, ts,
            1, 17, List.of(), List.of("1234", "5678"), null);
        var editorSsku = silverSsku(SERVICE_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), null);
        var supplierSsku = electrumSsku(SERVICE_KEY2, 3, 19, List.of("Монголия", "Вьетнам"), List.of());
        var anotherSsku = silverSsku(SERVICE_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts,
            4, 20, List.of("Вьетнам"), List.of(), null);
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(warehouseSsku);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);
        sleep(10);
        masterDataRepository.insertBatch(supplierSsku);
        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());

        processShopSkuKeys(List.of(SERVICE_KEY1));

        MasterData service1 = masterDataRepository.findById(SERVICE_KEY1);
        MasterData service2 = masterDataRepository.findById(SERVICE_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        MasterData expected1 = electrumSsku(SERVICE_KEY1, 2, 18,
            List.of("Россия"), List.of("1234", "5678"));
        MasterData expected2 = electrumSsku(SERVICE_KEY2, 2, 20,
            List.of("Россия"), List.of("1234", "5678"));
        Assertions.assertThat(service1).isEqualTo(expected1);
        Assertions.assertThat(service2).isEqualTo(expected2);
    }

    @Test
    public void testSilverWithoutValidBaseMergedToGoldAnyway() {
        Instant ts = Instant.now();
        var editorSsku = silverSsku(SERVICE_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), null);
        var supplierSsku = electrumSsku(SERVICE_KEY2, 3, 19, List.of("Вьетнам"), List.of());
        var anotherSsku = silverSsku(SERVICE_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts,
            4, 20, List.of(), List.of(), null); // нет стран, но для сервиса ОК
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);
        sleep(10);
        masterDataRepository.insertBatch(supplierSsku);
        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());


        processShopSkuKeys(List.of(SERVICE_KEY1));

        MasterData service1 = masterDataRepository.findById(SERVICE_KEY1);
        MasterData service2 = masterDataRepository.findById(SERVICE_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        MasterData expected1 = electrumSsku(SERVICE_KEY1, 2, 18,
            List.of("Китай"), List.of());
        MasterData expected2 = electrumSsku(SERVICE_KEY2, 2, 20, // сервис без стран прошёл и поучаствовал в мерже
            List.of("Китай"), List.of());
        Assertions.assertThat(service1).isEqualTo(expected1);
        Assertions.assertThat(service2).isEqualTo(expected2);
    }

    /**
     * Невалидные значения (и возможно связанные с ними: значение - единицы срока годности) отбрасываются,
     * остальные значения от источника остаются.
     */
    @Test
    public void testInvalidDataFiltering() {
        Instant ts = Instant.now();
        var eoxSsku = silverSsku(BUSINESS_KEY1, "supplier", MasterDataSourceType.SUPPLIER, ts,
            1, 200, List.of("Россия"), List.of(), null); // слишком большой delivery time, но страна валидная
        var editorSsku = silverSsku(SERVICE_KEY1, "cloudcat", MasterDataSourceType.MDM_OPERATOR, ts.plusSeconds(2),
            2, 18, List.of("Китай"), List.of(), null);
        var supplierSsku = electrumSsku(SERVICE_KEY2, 3, 19, List.of(), List.of()); // нет стран
        var anotherSsku = silverSsku(SERVICE_KEY2, "xdoc", MasterDataSourceType.SUPPLIER, ts,
            4, 17, List.of("Вьетнам"), List.of(), null);
        silverSskuRepository.insertOrUpdateSsku(eoxSsku);
        silverSskuRepository.insertOrUpdateSsku(editorSsku);
        silverSskuRepository.insertOrUpdateSsku(anotherSsku);
        sleep(10);
        masterDataRepository.insertBatch(supplierSsku);
        jdbcTemplate.update("update mdm.ssku_silver_param_value set updated_ts = source_updated_ts ", Map.of());

        processShopSkuKeys(List.of(SERVICE_KEY1));

        MasterData service1 = masterDataRepository.findById(SERVICE_KEY1);
        MasterData service2 = masterDataRepository.findById(SERVICE_KEY2);
        Assertions.assertThat(masterDataRepository.totalCount()).isEqualTo(2);

        MasterData expected1 = electrumSsku(SERVICE_KEY1, 2, 18, List.of("Россия"), List.of());
        MasterData expected2 = electrumSsku(SERVICE_KEY2, 2, 17, List.of("Россия"), List.of());

        Assertions.assertThat(service1).isEqualTo(expected1);
        Assertions.assertThat(service2).isEqualTo(expected2);
    }

    @Test
    public void testCalculateGoldenShelfLifeWhenCustomLogicEnabled() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        initSuppliers(bizKey, serviceKey);

        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, "best_source",
            MasterDataSourceType.SUPPLIER, Instant.now(), 5, true,
            "хранить в сухом помещении!!!", 586L);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);

        processShopSkuKeys(List.of(bizKey));

        var expectedCommonSsku = new CommonSsku(bizKey);
        expectedCommonSsku.setBaseValues(shelfLifeByBiz.getBaseValues().stream()
            .map(SskuSilverParamValue::toSskuParamValue)
            .collect(Collectors.toList()));
        List<SskuGoldenParamValue> goldenPVs = findAllGoldenParamValues();
        Assertions.assertThat(goldenPVs).hasSize(3);
        Assertions.assertThat(goldenPVs)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(
                sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                    expectedCommonSsku,
                    SskuGoldenParamUtil.ParamsGroup.SSKU));
        Assertions.assertThat(goldenPVs.stream().allMatch(gpv -> gpv.getMasterDataSource().equals(
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "best_source")))).isTrue();
    }

    @Test
    public void testCalculateGoldenShelfLifeWhenCustomLogicEnabledAndCategoryEnabled() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        var mapping = new MappingCacheDao().setShopSkuKey(bizKey).setCategoryId(1).setMskuId(100500L);
        initSuppliers(bizKey, serviceKey);
        mappingsCacheRepository.insert(mapping);

        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, "best_source",
            MasterDataSourceType.SUPPLIER, Instant.now(), 5, true,
            "хранить в сухом помещении!!!", 586L);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);

        processShopSkuKeys(List.of(bizKey));

        var expectedCommonSsku = new CommonSsku(bizKey);
        expectedCommonSsku.setBaseValues(shelfLifeByBiz.getBaseValues().stream()
            .map(SskuSilverParamValue::toSskuParamValue)
            .collect(Collectors.toList()));
        List<SskuGoldenParamValue> goldenPVs = findAllGoldenParamValues();
        Assertions.assertThat(goldenPVs).hasSize(6);
        Assertions.assertThat(goldenPVs)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsOnlyOnceElementsOf(
                sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                    expectedCommonSsku,
                    SskuGoldenParamUtil.ParamsGroup.SSKU))
            .containsOnlyOnceElementsOf(
                sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                    beforeInherit(expectedCommonSsku),
                    SskuGoldenParamUtil.ParamsGroup.COMMON));
        Assertions.assertThat(goldenPVs.stream().filter(gpv -> gpv.getMasterDataSource().equals(
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "best_source")))).hasSize(3);
        Assertions.assertThat(
            goldenPVs.stream().filter(gpv -> gpv.getMasterDataSourceType() == MasterDataSourceType.MSKU_INHERIT)
        ).hasSize(3);
    }

    @Test
    public void testCalculateGoldenShelfLifeWhenCustomLogicEnabledShouldRewriteGoldIfNewCalculated() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        var mapping = new MappingCacheDao().setShopSkuKey(bizKey).setCategoryId(1).setMskuId(100500L);
        initSuppliers(bizKey, serviceKey);
        mappingsCacheRepository.insert(mapping);

        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, "best_source",
            MasterDataSourceType.MDM_ADMIN, Instant.now(), 5, true,
            "хранить в сухом помещении!!!", 586L);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);
        var existingGoldenPVs = goldenSskuWithShelfLife(bizKey, "a",
            MasterDataSourceType.MDM_OPERATOR, Instant.now(), 10, true, "old comment");
        goldSskuRepository.insertOrUpdateSsku(new CommonSsku(bizKey).setBaseValues(existingGoldenPVs));

        processShopSkuKeys(List.of(bizKey));

        var expectedCommonSsku = new CommonSsku(bizKey);
        expectedCommonSsku.setBaseValues(shelfLifeByBiz.getBaseValues().stream()
            .map(SskuSilverParamValue::toSskuParamValue)
            .collect(Collectors.toList()));
        var goldenPVs = findAllGoldenParamValues();
        Assertions.assertThat(goldenPVs).hasSize(6);
        Assertions.assertThat(goldenPVs)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsOnlyOnceElementsOf(
                sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                    expectedCommonSsku,
                    SskuGoldenParamUtil.ParamsGroup.SSKU))
            .containsOnlyOnceElementsOf(
                sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                    beforeInherit(expectedCommonSsku),
                    SskuGoldenParamUtil.ParamsGroup.COMMON));
        Assertions.assertThat(goldenPVs.stream().filter(gpv -> gpv.getMasterDataSource().equals(
            new MasterDataSource(MasterDataSourceType.MDM_ADMIN, "best_source")))).hasSize(3);
        Assertions.assertThat(
            goldenPVs.stream().filter(gpv -> gpv.getMasterDataSourceType() == MasterDataSourceType.MSKU_INHERIT)
        ).hasSize(3);
    }

    @Test
    public void testCalculateGoldenShelfLifeWhenCustomLogicEnabledShouldNotRewriteIfHaveNotNewItem() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        var mapping = new MappingCacheDao().setShopSkuKey(bizKey).setCategoryId(1).setMskuId(100500L);
        initSuppliers(bizKey, serviceKey);
        mappingsCacheRepository.insert(mapping);

        var existingGoldenPVs = goldenSskuWithShelfLife(bizKey, "a",
            MasterDataSourceType.MDM_OPERATOR, Instant.now(), 5, true, "old comment");
        goldSskuRepository.insertOrUpdateSsku(new CommonSsku(bizKey).setBaseValues(existingGoldenPVs));
        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, "a",
            MasterDataSourceType.MDM_OPERATOR, Instant.now(), 5, true,
            "old comment", 586L);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);

        processShopSkuKeys(List.of(bizKey));

        var silverUpdatedTs = silverSskuRepository.findAll().stream()
            .map(SskuSilverParamValue::getUpdatedTs)
            .max(Comparator.naturalOrder()).orElseThrow();

        var goldenPVs = findAllGoldenParamValues();
        Assertions.assertThat(goldenPVs).hasSize(6);
        Assertions.assertThat(goldenPVs)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsOnlyOnceElementsOf(existingGoldenPVs)
            .containsOnlyOnceElementsOf(sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                new CommonSsku(bizKey).setBaseValues(existingGoldenPVs),
                SskuGoldenParamUtil.ParamsGroup.COMMON));
        Assertions.assertThat(goldenPVs.stream()
            .allMatch(pv -> pv.getUpdatedTs().isBefore(silverUpdatedTs))).isTrue();
    }

    @Test
    public void testCalculateGoldenShelfLifeWhenCustomLogicEnabledShouldNotRewriteIfNewSilverInvalid() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        var mapping = new MappingCacheDao().setShopSkuKey(bizKey).setCategoryId(1).setMskuId(100500L);
        initSuppliers(bizKey, serviceKey);
        mappingsCacheRepository.insert(mapping);

        var existingGoldenPVs = goldenSskuWithShelfLife(bizKey, "a",
            MasterDataSourceType.MDM_OPERATOR, Instant.now(), 5, true, "old comment");
       goldSskuRepository.insertOrUpdateSsku(new CommonSsku(bizKey).setBaseValues(existingGoldenPVs));
        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, "b",
            MasterDataSourceType.MDM_ADMIN, Instant.now(), 10, true,
            "old comment", 586L);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);
        // create category bounds
        var minSlLimit = (CategoryParamValue) new CategoryParamValue()
            .setCategoryId(1L)
            .setMdmParamId(KnownMdmParams.MIN_LIMIT_SHELF_LIFE)
            .setNumeric(BigDecimal.valueOf(1L));
        var maxSlLimit = (CategoryParamValue) new CategoryParamValue()
            .setCategoryId(1L)
            .setMdmParamId(KnownMdmParams.MAX_LIMIT_SHELF_LIFE)
            .setNumeric(BigDecimal.valueOf(6L));
        var minSlUnit = (CategoryParamValue) new CategoryParamValue()
            .setCategoryId(1L)
            .setMdmParamId(KnownMdmParams.MIN_LIMIT_SHELF_LIFE_UNIT)
            .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.DAY)));
        var maxSlUnit = (CategoryParamValue) new CategoryParamValue()
            .setCategoryId(1L)
            .setMdmParamId(KnownMdmParams.MAX_LIMIT_SHELF_LIFE_UNIT)
            .setOption(new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)));
        categoryParamValueRepository.insertOrUpdateAll(List.of(minSlLimit, maxSlLimit, minSlUnit, maxSlUnit));

        processShopSkuKeys(List.of(bizKey));

        var silverUpdatedTs = silverSskuRepository.findAll().stream()
            .map(SskuSilverParamValue::getUpdatedTs)
            .max(Comparator.naturalOrder()).orElseThrow();

        var goldenPVs = findAllGoldenParamValues();
        Assertions.assertThat(goldenPVs).hasSize(6);
        Assertions.assertThat(goldenPVs)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsOnlyOnceElementsOf(existingGoldenPVs)
            .containsOnlyOnceElementsOf(sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                new CommonSsku(bizKey).setBaseValues(existingGoldenPVs),
                SskuGoldenParamUtil.ParamsGroup.COMMON));
        Assertions.assertThat(goldenPVs.stream()
            .allMatch(pv -> pv.getUpdatedTs().isBefore(silverUpdatedTs))).isTrue();
        Assertions.assertThat(goldenPVs.stream().filter(gpv -> gpv.getMasterDataSource().equals(
            new MasterDataSource(MasterDataSourceType.MDM_OPERATOR, "a")))).hasSize(3);
        Assertions.assertThat(
            goldenPVs.stream().filter(gpv -> gpv.getMasterDataSourceType() == MasterDataSourceType.MSKU_INHERIT)
        ).hasSize(3);
    }

    @Test
    public void testCalculateGoldenShelfLifeWhenHaveDifferentPriorities() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        var mapping = new MappingCacheDao().setShopSkuKey(bizKey).setCategoryId(1).setMskuId(100500L);
        initSuppliers(bizKey, serviceKey);
        mappingsCacheRepository.insert(mapping);

        var operatorShelfLifeByBiz = silverSskuWithShelfLife(bizKey, "a",
            MasterDataSourceType.MDM_OPERATOR, Instant.now(), 5, true,
            "old comment", 586L);
        var adminShelfLifeByBiz = silverSskuWithShelfLife(bizKey, "a",
            MasterDataSourceType.MDM_ADMIN, Instant.now(), 10, true,
            "old comment", 586L);
        silverSskuRepository.insertOrUpdateSsku(operatorShelfLifeByBiz);
        silverSskuRepository.insertOrUpdateSsku(adminShelfLifeByBiz);

        processShopSkuKeys(List.of(bizKey));

        var expectedCommonSsku = new CommonSsku(bizKey);
        expectedCommonSsku.setBaseValues(adminShelfLifeByBiz.getBaseValues().stream()
            .map(SskuSilverParamValue::toSskuParamValue)
            .collect(Collectors.toList()));
        var goldenPVs = findAllGoldenParamValues();
        Assertions.assertThat(goldenPVs).hasSize(6);
        Assertions.assertThat(goldenPVs)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsOnlyOnceElementsOf(sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                expectedCommonSsku,
                SskuGoldenParamUtil.ParamsGroup.SSKU))
            .containsOnlyOnceElementsOf(sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                beforeInherit(expectedCommonSsku),
                SskuGoldenParamUtil.ParamsGroup.COMMON));
        Assertions.assertThat(goldenPVs.stream().filter(gpv -> gpv.getMasterDataSource().equals(
            new MasterDataSource(MasterDataSourceType.MDM_ADMIN, "a")))).hasSize(3);
        Assertions.assertThat(
            goldenPVs.stream().filter(gpv -> gpv.getMasterDataSourceType() == MasterDataSourceType.MSKU_INHERIT)
        ).hasSize(3);
    }

    @Test
    public void testCalculateGoldenShelfLifeWhenHaveNewGreaterPriorityBlock() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        var mapping = new MappingCacheDao().setShopSkuKey(bizKey).setCategoryId(1).setMskuId(100500L);
        initSuppliers(bizKey, serviceKey);
        mappingsCacheRepository.insert(mapping);

        var existingGoldenPVs = goldenSskuWithShelfLife(bizKey, "a",
            MasterDataSourceType.MDM_OPERATOR, Instant.now(), 10, true, "old comment");
        goldSskuRepository.insertOrUpdateSsku(new CommonSsku(bizKey).setBaseValues(existingGoldenPVs));
        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, "best_source",
            MasterDataSourceType.MDM_ADMIN, Instant.now(), 5, true,
            "old comment!", 586L);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);

        processShopSkuKeys(List.of(bizKey));

        var expectedCommonSsku = new CommonSsku(bizKey);
        expectedCommonSsku.setBaseValues(shelfLifeByBiz.getBaseValues().stream()
            .map(SskuSilverParamValue::toSskuParamValue)
            .collect(Collectors.toList()));
        var goldenPVs = findAllGoldenParamValues();
        Assertions.assertThat(goldenPVs).hasSize(6);
        Assertions.assertThat(goldenPVs)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsOnlyOnceElementsOf(sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                expectedCommonSsku,
                SskuGoldenParamUtil.ParamsGroup.SSKU))
            .containsOnlyOnceElementsOf(sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                beforeInherit(expectedCommonSsku),
                SskuGoldenParamUtil.ParamsGroup.COMMON));
        Assertions.assertThat(goldenPVs.stream().filter(gpv -> gpv.getMasterDataSource().equals(
            new MasterDataSource(MasterDataSourceType.MDM_ADMIN, "best_source")))).hasSize(3);
        Assertions.assertThat(
            goldenPVs.stream().filter(gpv -> gpv.getMasterDataSourceType() == MasterDataSourceType.MSKU_INHERIT)
        ).hasSize(3);
    }

    @Test
    public void testCalculateGoldenShelfLifeWhenHaveNewLowerPriorityBlock() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        var mapping = new MappingCacheDao().setShopSkuKey(bizKey).setCategoryId(1).setMskuId(100500L);
        initSuppliers(bizKey, serviceKey);
        mappingsCacheRepository.insert(mapping);

        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, "best_source",
            MasterDataSourceType.MDM_OPERATOR, Instant.now(), 10, true,
            "old comment!", 586L);
        var adminShelfLifeByBiz = silverSskuWithShelfLife(bizKey, "a",
            MasterDataSourceType.MDM_ADMIN, Instant.now(), 10, true,
            "old comment", 586L);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);
        silverSskuRepository.insertOrUpdateSsku(adminShelfLifeByBiz);
        var existingGoldenPVs = goldenSskuWithShelfLife(bizKey, "a",
            MasterDataSourceType.MDM_ADMIN, Instant.now(), 10, true, "old comment");
        goldSskuRepository.insertOrUpdateSsku(new CommonSsku(bizKey).setBaseValues(existingGoldenPVs));
        existingGoldenPVs = findAllGoldenParamValues();

        processShopSkuKeys(List.of(bizKey));

        var goldenPVs = findAllGoldenParamValues();
        Assertions.assertThat(goldenPVs).hasSize(6);
        Assertions.assertThat(goldenPVs)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsOnlyOnceElementsOf(existingGoldenPVs)
            .containsOnlyOnceElementsOf(sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                new CommonSsku(bizKey).setBaseValues(existingGoldenPVs),
                SskuGoldenParamUtil.ParamsGroup.COMMON));
        Assertions.assertThat(goldenPVs.stream().filter(gpv -> gpv.getMasterDataSource().equals(
            new MasterDataSource(MasterDataSourceType.MDM_ADMIN, "a")))).hasSize(3);
        Assertions.assertThat(
            goldenPVs.stream().filter(gpv -> gpv.getMasterDataSourceType() == MasterDataSourceType.MSKU_INHERIT)
        ).hasSize(3);
    }

    @Test
    public void testWhenCalculatedNewGoldShelfLifesShouldOverwriteOnlySskuShelfLifeParams() {
        ShopSkuKey bizKey = new ShopSkuKey(5000, "best-sku");
        ShopSkuKey serviceKey = new ShopSkuKey(5, "best-sku");
        initSuppliers(bizKey, serviceKey);

        var shelfLifeByBiz = silverSskuWithShelfLife(bizKey, "best_source",
            MasterDataSourceType.SUPPLIER, Instant.now(), 5, true,
            "хранить в сухом помещении!!!", 586L);
        silverSskuRepository.insertOrUpdateSsku(shelfLifeByBiz);
        var existingGoldenPVs = new ArrayList<SskuGoldenParamValue>();
        SskuGoldenParamValue value = goldenValue(bizKey, "best_source", MasterDataSourceType.SUPPLIER,
            Instant.now());
        value.setMdmParamId(KnownMdmParams.BOX_COUNT);
        value.setXslName(paramCache.get(KnownMdmParams.BOX_COUNT).getXslName());
        value.setNumeric(BigDecimal.valueOf(34));
        existingGoldenPVs.add(value);
        SskuGoldenParamValue anotherValue = goldenValue(bizKey, "best_source", MasterDataSourceType.SUPPLIER,
            Instant.now());
        anotherValue.setMdmParamId(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID);
        anotherValue.setXslName(paramCache.get(KnownMdmParams.CUSTOMS_COMM_CODE_MDM_ID).getXslName());
        anotherValue.setString("330300");
        existingGoldenPVs.add(anotherValue);
        goldSskuRepository.insertOrUpdateSsku(new CommonSsku(bizKey).setBaseValues(existingGoldenPVs));

        processShopSkuKeys(List.of(bizKey));

        var expectedCommonSsku = new CommonSsku(bizKey);
        expectedCommonSsku.setBaseValues(shelfLifeByBiz.getBaseValues().stream()
            .map(SskuSilverParamValue::toSskuParamValue)
            .collect(Collectors.toList()));
        List<SskuGoldenParamValue> goldenPVs = findAllGoldenParamValues();
        Assertions.assertThat(goldenPVs).hasSize(5);
        Assertions.assertThat(goldenPVs)
            .usingElementComparatorIgnoringFields("modificationInfo")
            .containsExactlyInAnyOrderElementsOf(
                Sets.union(new HashSet<>(existingGoldenPVs),
                        new HashSet<>(sskuGoldenParamUtil.createSskuGoldenParamValuesFromCommonSsku(
                            expectedCommonSsku,
                            SskuGoldenParamUtil.ParamsGroup.SSKU))
                    ));
        Assertions.assertThat(goldenPVs.stream().allMatch(gpv -> gpv.getMasterDataSource().equals(
            new MasterDataSource(MasterDataSourceType.SUPPLIER, "best_source")))).isTrue();
    }

    private static CommonSsku beforeInherit(CommonSsku ssku) {
        Map<Long, SskuParamValue> sourceValues = ssku.getBaseValuesByParamId();
        MdmParamValue sl = new MdmParamValue();
        MdmParamValue slu = new MdmParamValue();
        MdmParamValue slc = new MdmParamValue();

        sourceValues.get(KnownMdmParams.SHELF_LIFE).copyTo(sl);
        sourceValues.get(KnownMdmParams.SHELF_LIFE_UNIT).copyTo(slu);
        sourceValues.get(KnownMdmParams.SHELF_LIFE_COMMENT).copyTo(slc);

        sl.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE);
        slu.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE_UNIT);
        slc.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE_COMMENT);

        return new CommonSsku(ssku.getKey()).setBaseValues(List.of(sl, slu, slc));
    }

    private void prepareSskuGroup() {
        MdmSupplier business = new MdmSupplier()
            .setId(BUSINESS_KEY1.getSupplierId())
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier service1 = new MdmSupplier().setId(SERVICE_KEY1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(business.getId());
        MdmSupplier service2 = new MdmSupplier().setId(SERVICE_KEY2.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(business.getId());
        mdmSupplierRepository.insertBatch(business, service1, service2);
        mdmSupplierCachingService.refresh();
        sskuExistenceRepository.markExistence(List.of(SERVICE_KEY1, SERVICE_KEY2), true);
    }

    /**
     * Чтобы не мучиться с заполнением SSKU, выдели по одному параметру под каждый интересный тип бизнес-мержа. На
     * них и будем проверяться.
     *
     * @return
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private SilverCommonSsku silverSsku(ShopSkuKey key,
                                        String sourceId,
                                        MasterDataSourceType type,
                                        Instant updatedTs,
                                        int boxCount, // по последнему
                                        int deliveryTime, // по сервисам
                                        List<String> countries,
                                        List<String> regNumbers, // уникальное объединение
                                        Long masterDataVersion) {
        List<SskuSilverParamValue> result = new ArrayList<>();
        if (boxCount > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.BOX_COUNT);
            value.setXslName(paramCache.get(KnownMdmParams.BOX_COUNT).getXslName());
            value.setNumeric(BigDecimal.valueOf(boxCount));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (deliveryTime > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DELIVERY_TIME);
            value.setXslName(paramCache.get(KnownMdmParams.DELIVERY_TIME).getXslName());
            value.setNumeric(BigDecimal.valueOf(deliveryTime));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (countries.size() > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY);
            value.setXslName(paramCache.get(KnownMdmParams.MANUFACTURER_COUNTRY).getXslName());
            value.setStrings(countries);
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (regNumbers.size() > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.DOCUMENT_REG_NUMBER);
            value.setXslName(paramCache.get(KnownMdmParams.DOCUMENT_REG_NUMBER).getXslName());
            value.setStrings(regNumbers);
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        return TestDataUtils.wrapSilver(result);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private SilverCommonSsku silverSskuWithShelfLife(ShopSkuKey key,
                                                     String sourceId,
                                                     MasterDataSourceType type,
                                                     Instant updatedTs,
                                                     int shelfLifePeriod,
                                                     boolean shelfLifeUnit,
                                                     String shelLifeComment,
                                                     Long masterDataVersion) {
        List<SskuSilverParamValue> result = new ArrayList<>();
        if (shelfLifePeriod > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SHELF_LIFE);
            value.setXslName(paramCache.get(KnownMdmParams.SHELF_LIFE).getXslName());
            value.setNumeric(BigDecimal.valueOf(shelfLifePeriod));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (shelfLifeUnit) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT);
            value.setXslName(paramCache.get(KnownMdmParams.SHELF_LIFE_UNIT).getXslName());
            value.setOption(
                new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (shelLifeComment != null) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SHELF_LIFE_COMMENT);
            value.setXslName(paramCache.get(KnownMdmParams.SHELF_LIFE_COMMENT).getXslName());
            value.setStrings(List.of(shelLifeComment));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        return TestDataUtils.wrapSilver(result);
    }

    private List<SskuGoldenParamValue> goldenSskuWithShelfLife(ShopSkuKey key,
                                                               String sourceId,
                                                               MasterDataSourceType type,
                                                               Instant updatedTs,
                                                               int shelfLifePeriod,
                                                               boolean shelfLifeUnit,
                                                               String shelLifeComment) {
        List<SskuGoldenParamValue> result = new ArrayList<>();
        if (shelfLifePeriod > 0) {
            SskuGoldenParamValue value = goldenValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE);
            value.setXslName(paramCache.get(KnownMdmParams.SSKU_SHELF_LIFE).getXslName());
            value.setNumeric(BigDecimal.valueOf(shelfLifePeriod));
            result.add(value);
        }
        if (shelfLifeUnit) {
            SskuGoldenParamValue value = goldenValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE_UNIT);
            value.setXslName(paramCache.get(KnownMdmParams.SSKU_SHELF_LIFE_UNIT).getXslName());
            value.setOption(
                new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR)));
            result.add(value);
        }
        if (shelLifeComment != null) {
            SskuGoldenParamValue value = goldenValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.SSKU_SHELF_LIFE_COMMENT);
            value.setXslName(paramCache.get(KnownMdmParams.SSKU_SHELF_LIFE_COMMENT).getXslName());
            value.setStrings(List.of(shelLifeComment));
            result.add(value);
        }
        return result;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private SilverCommonSsku silverSskuWithGuaranteePeriod(ShopSkuKey key,
                                                           String sourceId,
                                                           MasterDataSourceType type,
                                                           Instant updatedTs,
                                                           int guaranteePeriod,
                                                           boolean guaranteePeriodUnit,
                                                           String guaranteePeriodComment,
                                                           Long masterDataVersion) {
        List<SskuSilverParamValue> result = new ArrayList<>();
        if (guaranteePeriod > 0) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD);
            value.setXslName(paramCache.get(KnownMdmParams.GUARANTEE_PERIOD).getXslName());
            value.setString(String.valueOf(guaranteePeriod));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (guaranteePeriodUnit) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD_UNIT);
            value.setXslName(paramCache.get(KnownMdmParams.GUARANTEE_PERIOD_UNIT).getXslName());
            value.setOption(
                new MdmParamOption(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(TimeInUnits.TimeUnit.YEAR))
            );
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        if (guaranteePeriodComment != null) {
            SskuSilverParamValue value = silverValue(key, sourceId, type, updatedTs);
            value.setMdmParamId(KnownMdmParams.GUARANTEE_PERIOD_COMMENT);
            value.setXslName(paramCache.get(KnownMdmParams.GUARANTEE_PERIOD_COMMENT).getXslName());
            value.setStrings(List.of(guaranteePeriodComment));
            value.setDatacampMasterDataVersion(masterDataVersion);
            result.add(value);
        }
        return TestDataUtils.wrapSilver(result);
    }

    private MasterData electrumSsku(ShopSkuKey key,
                                    int boxCount, // по последнему
                                    int deliveryTime, // по сервисам
                                    List<String> countries,
                                    List<String> regNumbers) { // уникальное объединение
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(key);
        if (boxCount > 0) {
            masterData.setBoxCount(boxCount);
        }
        if (deliveryTime > 0) {
            masterData.setDeliveryTime(deliveryTime);
        }
        if (countries.size() > 0) {
            masterData.setManufacturerCountries(countries);
        }
        if (regNumbers.size() > 0) {
            masterData.addAllQualityDocuments(
                regNumbers.stream().map(this::generateQualityDocument).collect(Collectors.toList()));
        }
        return masterData;
    }

    private MasterData electrumSskuWithOldShelfLifeComment(ShopSkuKey key) {
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(key);
        masterData.setShelfLifeComment("Старый коммент про срок годности");
        return masterData;
    }

    private MasterData electrumSskuWithOldGuaranteePeriodComment(ShopSkuKey key) {
        MasterData masterData = new MasterData();
        masterData.setShopSkuKey(key);
        masterData.setGuaranteePeriodComment("Старый коммент про срок гарантии");
        masterData.setShelfLife(3, TimeInUnits.TimeUnit.YEAR);
        masterData.setShelfLifeComment("Старый срок годности");
        return masterData;
    }

    private void initSuppliers(ShopSkuKey bizKey1, ShopSkuKey service1) {
        mdmSupplierRepository.deleteAll();
        MdmSupplier business = new MdmSupplier()
            .setId(bizKey1.getSupplierId())
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier serv1 = new MdmSupplier().setId(service1.getSupplierId())
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessEnabled(true)
            .setBusinessId(business.getId());

        mdmSupplierRepository.insertBatch(business, serv1);
        mdmSupplierCachingService.refresh();
        sskuExistenceRepository.markExistence(List.of(bizKey1, service1), true);
    }

    private QualityDocument generateQualityDocument(String regNumber) {
        return new QualityDocument()
            .setId(Long.parseLong(regNumber))
            .setType(QualityDocument.QualityDocumentType.DECLARATION_OF_CONFORMITY)
            .setStartDate(LocalDate.now().minusYears(10))
            .setEndDate(LocalDate.now().plusYears(100))
            .setRegistrationNumber(regNumber);
    }

    private List<SskuGoldenParamValue> findAllGoldenParamValues() {
        return goldSskuRepository.findAllSskus().stream()
            .map(CommonSsku::getBaseValues)
            .flatMap(List::stream)
            .map(SskuGoldenParamValue::fromSskuParamValue)
            .collect(Collectors.toList());
    }
}
