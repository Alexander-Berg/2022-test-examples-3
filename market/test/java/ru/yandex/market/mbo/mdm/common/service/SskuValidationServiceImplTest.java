package ru.yandex.market.mbo.mdm.common.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierSalesModel;
import ru.yandex.market.mbo.mdm.common.masterdata.model.utils.ValidationContextHelperService;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuPartnerVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuGoldenVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.verdict.SskuPartnerVerdictRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.MasterDataValidationService;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationByMdHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.verdict.VerdictCalculationHelper;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.VghValidationRequirementsProvider;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.services.document.DocumentService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.offers.repository.MboMappingsServiceMock;
import ru.yandex.market.mboc.common.utils.ErrorInfo;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.http.MboMappingsService;

public class SskuValidationServiceImplTest extends MdmBaseDbTestClass {

    private static final ShopSkuKey SHOP_SKU_KEY1 = new ShopSkuKey(14, "12312");
    private static final ShopSkuKey SHOP_SKU_KEY2 = new ShopSkuKey(777, "1738917");
    private static final ShopSkuKey SHOP_SKU_KEY3 = new ShopSkuKey(999, "12312");

    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private SskuPartnerVerdictRepository sskuPartnerVerdictRepository;
    @Autowired
    private SskuGoldenVerdictRepository sskuGoldenVerdictRepository;
    @Autowired
    private MasterDataValidationService masterDataValidationService;
    @Autowired
    private DocumentService documentService;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private MdmSupplierRepository supplierRepository;
    @Autowired
    private VghValidationRequirementsProvider vghValidationRequirementsProvider;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MdmSupplierCachingService mdmSupplierCachingService;

    private SskuValidationService sskuValidationService;

    @Before
    public void before() {
        MboMappingsService mboMappingsService = new MboMappingsServiceMock();

        storageKeyValueService.putValue("calculateMdPartnerVerdictsEnabled", true);
        storageKeyValueService.putValue("saveSskuSilverParamValues", true);

        VerdictCalculationHelper verdictCalculationHelper = new VerdictCalculationByMdHelper(
            sskuGoldenVerdictRepository,
            sskuPartnerVerdictRepository,
            masterDataValidationService,
            serviceSskuConverter
        );

        ValidationContextHelperService validationContextHelperService = new ValidationContextHelperService(
            mboMappingsService,
            new CategoryParamValueRepositoryMock()
        );

        this.sskuValidationService = new SskuValidationServiceImpl(
            masterDataValidationService,
            validationContextHelperService,
            mappingsCacheRepository,
            vghValidationRequirementsProvider,
            mdmSskuGroupManager,
            documentService);
        storageKeyValueService.invalidateCache();
        prepareSuppliers();
    }

    @Test
    public void masterDataValidationTest() {
        var masterData1 = new MasterData();
        masterData1.setShopSkuKey(SHOP_SKU_KEY1);
        masterData1.setManufacturerCountries(List.of("Индия"));
        masterData1.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null).build());

        Map<ShopSkuKey, List<ErrorInfo>> result = sskuValidationService.validateMasterData(List.of(masterData1));
        List<SskuSilverParamValue> writtenParamValues = silverSskuRepository.findParametrizedSsku(SHOP_SKU_KEY1);
        List<SskuPartnerVerdictResult> writtenPartnerVerdicts = sskuPartnerVerdictRepository.findByIds(List.of(SHOP_SKU_KEY1));

        Assertions.assertThat(result.get(SHOP_SKU_KEY1)).isEmpty();
        Assertions.assertThat(writtenParamValues).isEmpty();
        Assertions.assertThat(writtenPartnerVerdicts).isEmpty();

        var masterData2 = new MasterData();
        masterData2.setShopSkuKey(SHOP_SKU_KEY2);
        masterData2.setManufacturerCountries(List.of("Индия"));
        masterData2.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 999.0, null, null).build());

        result = sskuValidationService.validateMasterData(List.of(masterData2));
        writtenParamValues = silverSskuRepository.findParametrizedSsku(SHOP_SKU_KEY2);
        writtenPartnerVerdicts = sskuPartnerVerdictRepository.findByIds(List.of(SHOP_SKU_KEY2));

        Assertions.assertThat(result.get(SHOP_SKU_KEY2)).hasSize(2);
        Assertions.assertThat(writtenParamValues).isEmpty();
        Assertions.assertThat(writtenPartnerVerdicts).isEmpty();
    }

    @Test
    public void whenValidateMasterDataWithoutShippingUnitReturnErrors() {
        var masterData1 = new MasterData();
        masterData1.setShopSkuKey(SHOP_SKU_KEY1);
        masterData1.setManufacturerCountries(List.of("Индия"));
        masterData1.setItemShippingUnit(
            ItemWrapperTestUtil.generateShippingUnit(10.0, 10.0, 10.0, 1.0, null, null).build());
        masterData1.setCategoryId(0L);

        var masterData2 = new MasterData();
        masterData2.setShopSkuKey(SHOP_SKU_KEY2);
        masterData2.setCategoryId(0L);
        masterData2.setManufacturerCountries(List.of("Индия"));

        storageKeyValueService.putValue(MdmProperties.CATEGORY_IDS_RETURN_ERRORS_ON_EMPTY_SHIPPING_UNIT,
            List.of(0L));
        storageKeyValueService.invalidateCache();

        Map<ShopSkuKey, List<ErrorInfo>> result = sskuValidationService
            .validateMasterData(List.of(masterData1, masterData2));

        Assertions.assertThat(result.get(masterData1.getShopSkuKey())).isEmpty();
        Assertions.assertThat(result.get(masterData2.getShopSkuKey())).isNotEmpty();
    }

    @Test
    public void whenValidateMasterDataWithoutShippingUnitShouldReturnErrorsDueToSupplierSalesModel() {
        storageKeyValueService.putValue(MdmProperties.SALES_MODELS_RETURN_ERRORS_ON_EMPTY_SHIPPING_UNIT,
            List.of("FULFILLMENT", "DROPSHIP"));
        storageKeyValueService.invalidateCache();

        // Подготовим данные: проставим маппинги.

        int defaultCategoryId = 0;
        MappingCacheDao mapping1 = new MappingCacheDao().setShopSkuKey(SHOP_SKU_KEY1).setCategoryId(defaultCategoryId);
        MappingCacheDao mapping2 = new MappingCacheDao().setShopSkuKey(SHOP_SKU_KEY2).setCategoryId(defaultCategoryId);
        mappingsCacheRepository.insertBatch(mapping1, mapping2);

        // У обоих объектов нет shipping unit, но ошибка вернется только при валидации masterData1,
        // т.к. для одной из моделей продаж 1-го поставщика требуются ВГХ.
        var masterData1 = new MasterData();
        masterData1.setShopSkuKey(SHOP_SKU_KEY1);
        masterData1.setCategoryId((long) defaultCategoryId);
        masterData1.setManufacturerCountries(List.of("Индия"));

        var masterData2 = new MasterData();
        masterData2.setShopSkuKey(SHOP_SKU_KEY2);
        masterData2.setCategoryId((long) defaultCategoryId);
        masterData2.setManufacturerCountries(List.of("Китай"));

        Map<ShopSkuKey, List<ErrorInfo>> result = sskuValidationService
            .validateMasterData(List.of(masterData1, masterData2));

        Assertions.assertThat(result.get(masterData1.getShopSkuKey()).stream().map(ErrorInfo::render)
            .collect(Collectors.toSet())).containsExactlyInAnyOrder(
            "Отсутствует значение для колонки 'Вес в упаковке в килограммах'",
            "Отсутствует значение для колонки 'Габариты в сантиметрах с учетом упаковки'");

        Assertions.assertThat(result.get(masterData2.getShopSkuKey())).isEmpty();
    }

    private void prepareSuppliers() {
        MdmSupplier supplier1 = new MdmSupplier()
            .setId(SHOP_SKU_KEY1.getSupplierId())
            // ВГХ нужны для одной из моделей продаж, вернется ошибка валидации
            .setSalesModels(List.of(MdmSupplierSalesModel.FULFILLMENT, MdmSupplierSalesModel.CLICK_AND_COLLECT));
        MdmSupplier supplier2 = new MdmSupplier()
            .setId(SHOP_SKU_KEY2.getSupplierId())
            // ВГХ не нужны для этой модели продаж, ошибки валидации не будет
            .setSalesModels(List.of(MdmSupplierSalesModel.CROSSDOCK));
        MdmSupplier supplier3 = new MdmSupplier()
            .setId(SHOP_SKU_KEY3.getSupplierId())
            .setSalesModels(List.of(MdmSupplierSalesModel.FULFILLMENT, MdmSupplierSalesModel.DROPSHIP));
        supplierRepository.insertBatch(supplier1, supplier2, supplier3);
        mdmSupplierCachingService.refresh();
    }
}
