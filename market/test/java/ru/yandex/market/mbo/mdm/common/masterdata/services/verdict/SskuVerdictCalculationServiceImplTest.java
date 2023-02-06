package ru.yandex.market.mbo.mdm.common.masterdata.services.verdict;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierSalesModel;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.SskuVerdictResult;
import ru.yandex.market.mbo.mdm.common.masterdata.model.verdict.VerdictFeature;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ReferenceItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.services.cccode.ExistingCCCodeCacheMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.VghValidationRequirementsProvider;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionsValidator;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.block.CustomsCommodityCodeBlockValidator;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

/**
 * @author dmserebr
 * @date 20/10/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SskuVerdictCalculationServiceImplTest extends MdmBaseDbTestClass {
    private static final ShopSkuKey SHOP_SKU_KEY = new ShopSkuKey(14, "12312");

    @Autowired
    private VerdictCalculationByMdHelper verdictCalculationByMdHelper;

    @Autowired
    private StorageKeyValueService storageKeyValueService;

    @Autowired
    private MdmSupplierRepository supplierRepository;

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;

    @Autowired
    private ServiceSskuConverter converter;

    @Autowired
    private VghValidationRequirementsProvider vghValidationRequirementsProvider;

    @Autowired
    ExistingCCCodeCacheMock existingCCCodeCacheMock;

    @Autowired
    BeruId beruId;

    @Before
    public void before() {
        storageKeyValueService.putValue(MdmProperties.CALCULATE_MD_GOLDEN_VERDICTS_WITH_REFERENCE_ITEM_KEY, true);
        storageKeyValueService.putValue(MdmProperties.CREATE_FORBIDDING_VERDICTS_ON_EMPTY_SHIPPING_UNIT, true);
        storageKeyValueService.putValue(MdmProperties.MERGE_SSKU_GOLDEN_VERDICTS_ENABLED_KEY, true);
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void calculateVerdictAllOk() {
        var masterData = new MasterData();
        masterData.setShopSkuKey(SHOP_SKU_KEY);
        masterData.setManufacturerCountries(List.of("Индия"));
        ReferenceItemWrapper referenceItem = createReferenceItem(SHOP_SKU_KEY, 10.0, 10.0, 10.0, 1.0);

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of(referenceItem));

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isTrue();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED));
    }

    @Test
    public void calculateVerdictNoManufacturerCountry() {
        var masterData = new MasterData();
        masterData.setShopSkuKey(SHOP_SKU_KEY);
        ReferenceItemWrapper referenceItem = createReferenceItem(SHOP_SKU_KEY, 10.0, 10.0, 10.0, 1.0);

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of(referenceItem));

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY))));
    }

    @Test
    public void calculateVerdictNoShippingUnit() {
        var masterData = new MasterData();
        masterData.setShopSkuKey(SHOP_SKU_KEY);
        masterData.setManufacturerCountries(List.of("Индия"));

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of());

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS),
                    MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS))));
    }

    @Test
    public void calculateVerdictDimensionsWithoutWeight() {
        var masterData = new MasterData();
        masterData.setShopSkuKey(SHOP_SKU_KEY);
        masterData.setManufacturerCountries(List.of("Индия"));
        ReferenceItemWrapper referenceItem = createReferenceItem(SHOP_SKU_KEY, 10.0, 10.0, 10.0, null);

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of(referenceItem));

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS))));
    }

    @Test
    public void calculateVerdictWithoutShippingUnitShouldReturnVghValidationError() {
        storageKeyValueService.putValue(MdmProperties.CREATE_FORBIDDING_VERDICTS_ON_EMPTY_SHIPPING_UNIT, false);
        storageKeyValueService.putValue(MdmProperties.CALCULATE_MD_GOLDEN_VERDICTS_WITH_REFERENCE_ITEM_KEY, true);
        storageKeyValueService.putValue(MdmProperties.SALES_MODELS_CREATE_FORBIDDING_VERDICTS_ON_EMPTY_SHIPPING_UNIT,
            List.of("FULFILLMENT", "DROPSHIP"));
        storageKeyValueService.invalidateCache();

        // Подготовим данные: заведем поставщика и проставим маппинги.
        ShopSkuKey key1 = new ShopSkuKey(1, "волшебник-изумрудного-города");
        ShopSkuKey key2 = new ShopSkuKey(2, "атлант-расправил-плечи");
        MdmSupplier supplier1 = new MdmSupplier()
            .setId(key1.getSupplierId())
            // ВГХ нужны для одной из моделей продаж, вернется ошибка валидации
            .setSalesModels(List.of(MdmSupplierSalesModel.FULFILLMENT, MdmSupplierSalesModel.CLICK_AND_COLLECT));
        MdmSupplier supplier2 = new MdmSupplier()
            .setId(key2.getSupplierId())
            // ВГХ не нужны для этой модели продаж, ошибки валидации не будет
            .setSalesModels(List.of(MdmSupplierSalesModel.CROSSDOCK));
        supplierRepository.insertBatch(supplier1, supplier2);

        int defaultCategoryId = 0;
        MappingCacheDao mapping1 = new MappingCacheDao().setShopSkuKey(key1).setCategoryId(defaultCategoryId);
        MappingCacheDao mapping2 = new MappingCacheDao().setShopSkuKey(key2).setCategoryId(defaultCategoryId);
        mappingsCacheRepository.insertBatch(mapping1, mapping2);

        var masterData1 = new MasterData();
        masterData1.setShopSkuKey(key1);
        masterData1.setManufacturerCountries(List.of("Индия"));

        var masterData2 = new MasterData();
        masterData2.setShopSkuKey(key2);
        masterData2.setManufacturerCountries(List.of("Китай"));

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData1, masterData2), List.of());

        // Возвращается ошибка валидации ВГХ, т.к. для одной из моделей продаж 1-го поставщика ВГХ обязательны.
        Assertions.assertThat(result.size()).isEqualTo(2);
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS),
                    MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS))));

        // Ошибка валидации ВГХ отсутствует, несмотря на то, что ВГХ не представлены:
        // модель продаж 2-го поставщика не попадает под требование обязательности ВГХ.
        Assertions.assertThat(result.get(1).isValid()).isTrue();
        Assertions.assertThat(result.get(1).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(1).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createOkVerdict(VerdictFeature.UNSPECIFIED));
    }

    @Test
    public void calculateVerdictWeightTooLarge() {
        var masterData = new MasterData();
        masterData.setShopSkuKey(SHOP_SKU_KEY);
        masterData.setManufacturerCountries(List.of("Индия"));
        ReferenceItemWrapper referenceItem = createReferenceItem(SHOP_SKU_KEY, 10.0, 10.0, 10.0, 999.0);

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of(referenceItem));

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(
                    MbocErrors.get().excelValueMustBeInRange(
                        SskuMasterDataFields.WEIGHT_GROSS, "999",
                        WeightDimensionsValidator.WEIGHT_MIN.toString(),
                        WeightDimensionsValidator.WEIGHT_MAX.toString()),
                    MbocErrors.get().excelWeightDimensionsInconsistent(
                        SskuMasterDataFields.WEIGHT_GROSS,
                        new BigDecimal(999), new BigDecimal(10), new BigDecimal(10), new BigDecimal(10)))));
    }

    @Test
    public void calculateVerdictNoManufacturerCountryAndShippingUnit() {
        var masterData = new MasterData();
        masterData.setShopSkuKey(SHOP_SKU_KEY);

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of());

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(
                    MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.MANUFACTURER_COUNTRY),
                    MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS),
                    MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS)
                )));
    }

    @Test
    public void calculateVerdictNoManufacturerCountryAndShippingUnitWhenManufacturerCountryIsNotRequired() {
        var masterData = new MasterData();
        masterData.setShopSkuKey(SHOP_SKU_KEY);
        storageKeyValueService.putValue(MdmProperties.IGNORE_MISSING_COUNTRY_VALIDATION, true);
        storageKeyValueService.invalidateCache();
        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of());

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS),
                    MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS))));
    }

    @Test
    public void calculateVerdictIncompleteDimensions() {
        var masterData = new MasterData();
        masterData.setShopSkuKey(SHOP_SKU_KEY);
        masterData.setManufacturerCountries(List.of("Индия"));
        ReferenceItemWrapper referenceItem = createReferenceItem(SHOP_SKU_KEY, null, 10.0, 10.0, 1.0);

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of(referenceItem));

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                List.of(MbocErrors.get().excelIncompleteDimensions(SskuMasterDataFields.BOX_DIMENSIONS))));
    }

    @Test
    public void calculateVerdictDimensionNotInRange() {
        var masterData = new MasterData();
        masterData.setShopSkuKey(SHOP_SKU_KEY);
        masterData.setManufacturerCountries(List.of("Индия"));
        ReferenceItemWrapper referenceItem = createReferenceItem(SHOP_SKU_KEY, 512.0, 4.0, 1024.0, 32.0);

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of(referenceItem));

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().keySet())
            .containsExactlyInAnyOrder(VerdictFeature.UNSPECIFIED);

        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(VerdictFeature.UNSPECIFIED,
                    List.of(
                        MbocErrors.get().excelValueMustBeInRange(
                            SskuMasterDataFields.BOX_HEIGHT, "1024",
                            WeightDimensionsValidator.SIZE_MIN.toString(),
                            WeightDimensionsValidator.SIZE_LONG_MAX.toString()),
                        MbocErrors.get().excelValueMustBeInRange(
                            SskuMasterDataFields.BOX_LENGTH, "512",
                            WeightDimensionsValidator.SIZE_MIN.toString(),
                            WeightDimensionsValidator.SIZE_MIDDLE_MAX.toString())
                    )
                )
            );
    }

    @Test
    public void calculateVerdictUnknownCustomsCommodityCode1p() {
        existingCCCodeCacheMock.deleteAll();
        storageKeyValueService.putValue(
            MdmProperties.CCC_VALIDATION_MODE,
            CustomsCommodityCodeBlockValidator.ValidationMode.GENERATE_ERROR
        );
        storageKeyValueService.invalidateCache();

        String code = "1604320010";
        var masterData = new MasterData();
        ShopSkuKey shopSku1p = new ShopSkuKey(beruId.getId(), "sku");
        masterData.setShopSkuKey(shopSku1p);
        masterData.setManufacturerCountries(List.of("Индия"));
        masterData.setCustomsCommodityCode(code);
        ReferenceItemWrapper referenceItem = createReferenceItem(shopSku1p, 10.0, 10.0, 10.0, 1.0);

        List<SskuVerdictResult> result = calculateVerdicts(List.of(masterData), List.of(referenceItem));

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isFalse();
        Assertions.assertThat(result.get(0).getSingleVerdictResults().get(VerdictFeature.UNSPECIFIED))
            .isEqualTo(VerdictGeneratorHelper.createForbiddingVerdict(
                VerdictFeature.UNSPECIFIED,
                List.of(
                    MbocErrors.get().mdUnknownCustomsCommodityCode(SskuMasterDataFields.CUSTOMS_COMMODITY_CODE, code)
                )
            ));

        existingCCCodeCacheMock.add(code);
        result = calculateVerdicts(List.of(masterData), List.of(referenceItem));

        Assertions.assertThat(result.size()).isOne();
        Assertions.assertThat(result.get(0).isValid()).isTrue();
    }

    private List<SskuVerdictResult> calculateVerdicts(
        List<MasterData> masterData,
        List<ReferenceItemWrapper> referenceItems
    ) {
        Map<ShopSkuKey, CommonSsku> sskus = masterData.stream()
            .map(md -> new CommonSsku(md.getShopSkuKey()).setBaseValues(converter.fromMasterData(md)))
            .collect(Collectors.toMap(CommonSsku::getKey, Function.identity()));

        Set<ShopSkuKey> keys = Stream.concat(masterData.stream().map(MasterData::getShopSkuKey),
                referenceItems.stream().map(ItemWrapper::getShopSkuKey))
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return verdictCalculationByMdHelper.calculateGoldenVerdictsInGroups(
            new GoldenVerdictCalculationData(
                sskus,
                Map.of(),
                referenceItems.stream()
                    .collect(Collectors.toMap(ReferenceItemWrapper::getKey, Function.identity())),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                vghValidationRequirementsProvider.loadVghValidationRequirementsForVerdictsCalculation(keys)
            )
        );
    }

    private static ReferenceItemWrapper createReferenceItem(ShopSkuKey key,
                                                            Double length,
                                                            Double width,
                                                            Double height,
                                                            Double weightGross) {
        var shippingUnit =
            ItemWrapperTestUtil.generateShippingUnit(length, width, height, weightGross, null, null);
        var item =
            ItemWrapperTestUtil.createItem(key, MdmIrisPayload.MasterDataSource.SUPPLIER, shippingUnit);
        return new ReferenceItemWrapper(item);
    }
}
