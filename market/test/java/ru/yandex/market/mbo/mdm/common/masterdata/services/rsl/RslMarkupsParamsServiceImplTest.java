package ru.yandex.market.mbo.mdm.common.masterdata.services.rsl;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.CategoryRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.MskuRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.RslThreshold;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.ShopSkuRslParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SskuRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SupplierRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.CategoryRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.MskuRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.SskuRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.rsl.SupplierRslRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruId;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.rsl.RslType;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.services.msku.ModelKey;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.RealConverter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author as14
 * @date 7/29/21
 */
public class RslMarkupsParamsServiceImplTest extends MdmBaseDbTestClass {
    private static final EnhancedRandom RANDOM = new EnhancedRandomBuilder().seed(1337).build();

    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private MskuRslRepository mskuRslRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private SskuRslRepository sskuRslRepository;
    @Autowired
    private CategoryRslRepository categoryRslRepository;
    @Autowired
    private SupplierRslRepository supplierRslRepository;
    @Autowired
    private BeruId beruId;

    private RslMarkupsParamsService service;

    @Before
    public void before() {
        service = new RslMarkupsParamsServiceImpl(
            mappingsCacheRepository, mskuRslRepository, mskuRepository,
            sskuRslRepository, categoryRslRepository, supplierRslRepository, beruId);
    }

    @Test
    public void shouldContainShopSkuKeysAndSupplierRealId() {
        //given
        var offer1p = nextShopSku1p();
        var offer3p = nextShopSku3p();

        //when
        var params = service.loadRslParams(List.of(offer1p, offer3p));

        //then
        assertThat(params).hasSize(2);
        assertThat(params)
            .containsExactlyInAnyOrder(
                new ShopSkuRslParam(offer1p)
                    .setFirstParty(true)
                    .setSupplierRealId(RealConverter.getRealSupplierId(offer1p.getShopSku())),
                new ShopSkuRslParam(offer3p)
                    .setFirstParty(false)
                    .setSupplierRealId("")
            );
    }

    @Test
    public void mappedMskuShouldHaveReleatedParams() {
        //given
        var offer1p = nextShopSku1p();
        var msku1p = 10L;
        var category1p = 110;
        mapSskuMsku(offer1p, msku1p, category1p);
        var commonMsku1p = new CommonMsku(
            new ModelKey(category1p, msku1p),
            List.of(
                prepareExpirDateParamValue(msku1p, true),
                prepareCargoType(msku1p, KnownMdmParams.FOOD),
                prepareShelfLifeUnit(msku1p, TimeInUnits.TimeUnit.DAY),
                prepareShelfLife(msku1p, 10)
            )
        );
        mskuRepository.insertOrUpdateMsku(commonMsku1p);

        var offer3p = nextShopSku3p();
        var msku3p = 30L;
        var category3p = 130;
        mapSskuMsku(offer3p, msku3p, category3p);
        var commonMsku3p = new CommonMsku(
            new ModelKey(category3p, msku3p),
            List.of(
                prepareExpirDateParamValue(msku3p, false),
                prepareCargoType(msku3p, KnownMdmParams.INTIMATE_GOOD),
                prepareShelfLife(msku3p, 11)
            )
        );
        mskuRepository.insertOrUpdateMsku(commonMsku3p);

        //when
        var result = service.loadRslParams(List.of(offer1p, offer3p));

        //then
        assertThat(result)
            .hasSize(2)
            .containsExactlyInAnyOrder(
                new ShopSkuRslParam(offer1p)
                    .setMskuId(msku1p)
                    .setFirstParty(true)
                    .setCategoryId(category1p)
                    .setCargoType750(true)
                    .setExpirDate(true)
                    .setSupplierRealId(RealConverter.getRealSupplierId(offer1p.getShopSku()))
                    .setShelfLife(new TimeInUnits(10, TimeInUnits.TimeUnit.DAY)),
                new ShopSkuRslParam(offer3p)
                    .setMskuId(msku3p)
                    .setCategoryId(category3p)
                    .setExpirDate(false)
                    .setSupplierRealId("")
            );
    }

    @Test
    public void sskuRslsShouldBeLoaded() {
        //given
        LocalDate dateTo = LocalDate.now();
        var offer1p = nextShopSku1p();
        var offer1pParam = new ShopSkuRslParam(offer1p);
        var offer3p = nextShopSku3p();
        var offer3pParam = new ShopSkuRslParam(offer3p);
        var rslOffer1p = prepareSskuRslParam(offer1p);
        var rslOffer3p = prepareSskuRslParam(offer3p);
        sskuRslRepository.insertBatch(rslOffer1p, rslOffer3p);

        //when
        var result = service.loadRslMarkups(List.of(offer1pParam, offer3pParam), dateTo);

        //then
        assertThat(result.getSskuRsls())
            .hasSize(2)
            .containsOnlyKeys(offer1p, offer3p);
        assertThat(result.getSskuRsls().get(offer1p))
            .containsExactly(rslOffer1p);
        assertThat(result.getSskuRsls().get(offer3p))
            .containsExactly(rslOffer3p);
    }

    @Test
    public void allRslsShouldBeLoaded() {
        //given
        LocalDate dateTo = LocalDate.now();
        var globalSupplierRslsParams = List.of(
            prepareGlobalSupplierRsl(RslType.GLOBAL_FIRST_PARTY, true),
            prepareGlobalSupplierRsl(RslType.GLOBAL_THIRD_PARTY, true),
            prepareGlobalSupplierRsl(RslType.GLOBAL_FIRST_PARTY, false),
            prepareGlobalSupplierRsl(RslType.GLOBAL_THIRD_PARTY, false)
        );
        supplierRslRepository.insertBatch(globalSupplierRslsParams);


        var category = 111;
        var categoriesRsls = List.of(
            prepareCategoryRsl(category).setActivatedAt(LocalDate.now().minusDays(1)),
            prepareCategoryRsl(category).setActivatedAt(LocalDate.now()),
            prepareCategoryRsl(category)
        );
        categoryRslRepository.insertBatch(categoriesRsls);
        categoryRslRepository.insert(prepareCategoryRsl(10));

        var offer1p = nextShopSku1p();
        var msku1p = 10;
        mapSskuMsku(offer1p, msku1p, category);
        var commonMsku1p = new CommonMsku(
            new ModelKey(category, msku1p),
            List.of(
                prepareCargoType(msku1p, KnownMdmParams.INTIMATE_GOOD),
                prepareShelfLife(msku1p, 10)
            )
        );
        mskuRepository.insertOrUpdateMsku(commonMsku1p);

        var sskuRsls1p = List.of(
            prepareSskuRslParam(offer1p).setActivatedAt(LocalDate.now().minusDays(1)),
            prepareSskuRslParam(offer1p).setActivatedAt(LocalDate.now()),
            prepareSskuRslParam(offer1p)
        );
        sskuRslRepository.insertBatch(sskuRsls1p);


        var offer3p = nextShopSku3p();
        var msku3p = 20;
        mapSskuMsku(offer3p, msku3p, category);
        var commonMsku3p = new CommonMsku(
            new ModelKey(category, msku3p),
            List.of(
                prepareExpirDateParamValue(msku3p, true),
                prepareCargoType(msku3p, KnownMdmParams.FOOD),
                prepareShelfLifeUnit(msku3p, TimeInUnits.TimeUnit.DAY),
                prepareShelfLife(msku3p, 10)
            )
        );
        mskuRepository.insertOrUpdateMsku(commonMsku3p);

        var mskuRsls3p = List.of(
            prepareMskuRsl(msku3p).setActivatedAt(LocalDate.now().minusDays(1)),
            prepareMskuRsl(msku3p).setActivatedAt(LocalDate.now())
        );
        mskuRslRepository.insertBatch(mskuRsls3p);

        //when
        var keys = service.loadRslParams(List.of(offer1p, offer3p));
        var result = service.loadRslMarkups(keys, dateTo);

        //then
        assertThat(result.getGlobalSupplierRsls())
            .isNotEmpty()
            .containsExactlyInAnyOrderElementsOf(globalSupplierRslsParams);
        assertThat(result.getSskuRsls().get(offer1p))
            .isNotEmpty()
            .containsExactlyInAnyOrderElementsOf(sskuRsls1p);
        assertThat(result.getCategoryRsls().get((long) category))
            .isNotEmpty()
            .containsExactlyInAnyOrderElementsOf(categoriesRsls);
        assertThat(result.getMskuRsls().get((long) msku3p))
            .containsExactlyInAnyOrderElementsOf(mskuRsls3p);

    }

    @Test
    public void shouldIncludeSupplierRslByAllCategory() {
        //given
        var offer1p = nextShopSku1p();
        var msku1p = 10;
        var category = 111;

        mapSskuMsku(offer1p, msku1p, category);
        var commonMsku1p = new CommonMsku(
            new ModelKey(category, msku1p),
            List.of(
                prepareExpirDateParamValue(msku1p, true),
                prepareCargoType(msku1p, KnownMdmParams.INTIMATE_GOOD),
                prepareShelfLife(msku1p, 10)
            )
        );
        mskuRepository.insertOrUpdateMsku(commonMsku1p);

        LocalDate dateTo = LocalDate.now();
        var expected1pRsl = new SupplierRsl()
            .setSupplierId(offer1p.getSupplierId())
            .setRealId(RealConverter.getRealSupplierId(offer1p.getShopSku()))
            .setType(RslType.FIRST_PARTY)
            .setCategoryId(0)
            .setCargoType750(false)
            .setRslThresholds(RANDOM.objects(RslThreshold.class, 3).collect(Collectors.toList()))
            .setActivatedAt(dateTo.minusDays(100));
        var supplierRslsParamsFor1pOffer = List.of(expected1pRsl);
        supplierRslRepository.insertBatch(supplierRslsParamsFor1pOffer);

        var offer3p = nextShopSku3p();
        var msku3p = 20;
        mapSskuMsku(offer3p, msku3p, category);
        var commonMsku3p = new CommonMsku(
            new ModelKey(category, msku3p),
            List.of(
                prepareExpirDateParamValue(msku3p, true),
                prepareCargoType(msku3p, KnownMdmParams.FOOD),
                prepareShelfLifeUnit(msku3p, TimeInUnits.TimeUnit.DAY),
                prepareShelfLife(msku3p, 10)
            )
        );
        mskuRepository.insertOrUpdateMsku(commonMsku3p);

        var expected3pRsl = new SupplierRsl()
            .setSupplierId(offer3p.getSupplierId())
            .setRealId("")
            .setType(RslType.THIRD_PARTY)
            .setCategoryId(category)
            .setCargoType750(true)
            .setRslThresholds(RANDOM.objects(RslThreshold.class, 3).collect(Collectors.toList()))
            .setActivatedAt(dateTo.minusDays(100));
        var supplierRslsParamsFor3pOffer = List.of(
            expected3pRsl,
            new SupplierRsl()
                .setSupplierId(offer3p.getSupplierId())
                .setRealId("")
                .setType(RslType.THIRD_PARTY)
                .setCategoryId(0)
                .setCargoType750(true)
                .setRslThresholds(RANDOM.objects(RslThreshold.class, 3).collect(Collectors.toList()))
                .setActivatedAt(dateTo.minusDays(100))
        );
        supplierRslRepository.insertBatch(supplierRslsParamsFor3pOffer);

        var keys = service.loadRslParams(List.of(offer1p, offer3p));
        var result = service.loadRslMarkups(keys, dateTo);

        assertThat(result.getSupplierRsls())
            .isNotEmpty()
            .containsValues(List.of(expected1pRsl), List.of(expected3pRsl));
    }


    private SupplierRsl prepareGlobalSupplierRsl(RslType rslType, boolean isCargoType750) {
        return RANDOM.nextObject(SupplierRsl.class)
            .setType(rslType)
            .setCargoType750(isCargoType750);
    }

    private MskuRsl prepareMskuRsl(long mskuId) {
        return RANDOM.nextObject(MskuRsl.class).setMskuId(mskuId);
    }

    private CategoryRsl prepareCategoryRsl(long category) {
        return RANDOM.nextObject(CategoryRsl.class).setCategoryId(category);
    }

    private SskuRsl prepareSskuRslParam(ShopSkuKey offer) {
        return RANDOM.nextObject(SskuRsl.class)
            .setActivatedAt(LocalDate.now().minusDays(100))
            .setSupplierId(offer.getSupplierId())
            .setShopSku(offer.getShopSku());
    }

    private MskuParamValue prepareShelfLife(long mskuId, int time) {
        return (MskuParamValue) new MskuParamValue()
            .setMskuId(mskuId)
            .setMdmParamId(KnownMdmParams.SHELF_LIFE)
            .setNumeric(BigDecimal.valueOf(time));
    }

    private MskuParamValue prepareShelfLifeUnit(long mskuId, TimeInUnits.TimeUnit unit) {
        return (MskuParamValue) new MskuParamValue()
            .setMskuId(mskuId)
            .setMdmParamId(KnownMdmParams.SHELF_LIFE_UNIT)
            .setOption(new MdmParamOption().setId(KnownMdmParams.TIME_UNITS_OPTIONS.inverse().get(unit)));
    }

    private MskuParamValue prepareCargoType(long mskuId, long cargoId) {
        return (MskuParamValue) new MskuParamValue()
            .setMskuId(mskuId)
            .setMdmParamId(cargoId)
            .setBool(true);
    }

    private MskuParamValue prepareExpirDateParamValue(long msku1p, boolean expirDate) {
        return (MskuParamValue) new MskuParamValue()
            .setMskuId(msku1p)
            .setMdmParamId(KnownMdmParams.EXPIR_DATE)
            .setBool(expirDate);
    }

    private void mapSskuMsku(ShopSkuKey offer, long mskuId, int categoryId) {
        mappingsCacheRepository.insert(new MappingCacheDao()
            .setShopSkuKey(offer)
            .setMskuId(mskuId)
            .setCategoryId(categoryId));
    }

    private ShopSkuKey nextShopSku3p() {
        return RANDOM.nextObject(ShopSkuKey.class);
    }

    private ShopSkuKey nextShopSku1p() {
        return new ShopSkuKey(beruId.getId(), RANDOM.nextInt() + "." + RANDOM.nextObject(String.class));
    }
}
