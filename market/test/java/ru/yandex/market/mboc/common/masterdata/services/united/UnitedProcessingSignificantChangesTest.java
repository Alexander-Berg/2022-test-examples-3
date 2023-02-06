package ru.yandex.market.mboc.common.masterdata.services.united;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.ImpersonalSourceId;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmEnqueueReason;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.ssku.CommonSskuBuilder;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class UnitedProcessingSignificantChangesTest extends UnitedProcessingServiceSetupBaseTest {
    private static final int BUSINESS_ID = 12;
    private static final int SERVICE_ID = 13;
    private static final ShopSkuKey KEY = new ShopSkuKey(BUSINESS_ID, "ooo");
    private static final ShopSkuKey SERVICE_KEY = new ShopSkuKey(SERVICE_ID, "ooo");
    private static final long MSKU_ID = 92;
    private static final MappingCacheDao MAPPING = new MappingCacheDao().setCategoryId(1).setMskuId(MSKU_ID)
        .setShopSkuKey(KEY);

    @Before
    @Override
    public void setup() {
        storageKeyValueService.putValue(MdmProperties.WRITE_GOLD_WD_TO_SSKU_GOLD_TABLE_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.WRITE_OWN_SSKU_WD_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.USE_OWN_SSKU_WD_FOR_MSKU_GOLD_GLOBALLY, true);
        storageKeyValueService.putValue(MdmProperties.APPLY_FORCE_INHERITANCE_GLOBALLY, true);
        storageKeyValueService.invalidateCache();
        super.setup();

        MdmSupplier business = new MdmSupplier()
            .setId(BUSINESS_ID)
            .setType(MdmSupplierType.BUSINESS);
        MdmSupplier service = new MdmSupplier()
            .setId(SERVICE_ID)
            .setType(MdmSupplierType.THIRD_PARTY)
            .setBusinessId(BUSINESS_ID)
            .setBusinessEnabled(true);
        mdmSupplierRepository.insertOrUpdateAll(List.of(business, service));
        sskuExistenceRepository.markExistence(SERVICE_KEY, true);
        mdmSupplierCachingService.refresh();
        mappingsCacheRepository.insert(MAPPING);
    }

    /**
     * Даже если собственные значения ССКУ не изменились, всё равно считаем МСКУ, т.к. мог поменяться маппинг и МСКУ
     * должна перепровериться об своих (пусть даже не изменившихся) свежих дочерей на предмет интересного промежолота.
     */
    @Test
    public void whenNoChangesInIntergoldOrMdShouldStillComputeMsku() {
        var ownSource = new MasterDataSource(MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name());
        var inheritedSource = new MasterDataSource(MasterDataSourceType.MSKU_INHERIT,
            MasterDataSourceType.addMskuSourcePrefix(MSKU_ID, ""));

        var sskuWithFinalVgh = new CommonSskuBuilder(mdmParamCache, KEY)
            .withVghAfterInheritance(10, 11, 12, 1)
            .customized(v -> v.setMasterDataSource(inheritedSource))
            .build();
        var silverSsku = SilverCommonSsku.fromCommonSsku(sskuWithFinalVgh, ownSource);

        silverSskuRepository.insertOrUpdateSsku(silverSsku);

        mskuAndSskuQueue.enqueueSsku(KEY, MdmEnqueueReason.CHANGED_MAPPING_EOX, 1);

        // Сперва просто просчитаем золото, чтобы оно появилось
        execute();

        // Теперь золото есть. И поскольку серебро не поменяется, то и золото тоже.
        // Мы хотим проверить, что в условиях отсутствия существенных изменений пайплайн всё равно дойдёт до МСКУ.
        // Потому ради прикола посетим в МСКУ какие-нибудь рандомные ВГХ, которые в случае прохода через варку МСКУ
        // должны будут исчезнуть.
        var msku = mskuRepository.findMsku(MSKU_ID).get();
        msku.getParamValue(KnownMdmParams.WIDTH).get().setNumeric(BigDecimal.valueOf(88L));
        msku.getParamValue(KnownMdmParams.LENGTH).get().setNumeric(BigDecimal.valueOf(88L));
        msku.getParamValue(KnownMdmParams.HEIGHT).get().setNumeric(BigDecimal.valueOf(88L));
        msku.getParamValue(KnownMdmParams.WEIGHT_GROSS).get().setNumeric(BigDecimal.valueOf(2L));
        mskuRepository.insertOrUpdateMsku(msku);

        execute();

        msku = mskuRepository.findMsku(MSKU_ID).get();
        for (var paramId : KnownMdmParams.MANDATORY_WEIGHT_DIMENSIONS_PARAMS) {
            Assertions.assertThat(
                msku.getParamValue(paramId).get().valueEquals(sskuWithFinalVgh.getBaseValue(paramId).get())).isTrue();
        }
    }

    /**
     * Даже если собственное золото ССКУ и золото МСКУ не поменялось, мы всё равно должны вычислить итоговое золото
     * ССКУ третьим этапом. Это необходимо для случая, когда ССКУшка перемапилась, сама не изменилась, на МСКУ
     * не повлияла (на МСКУ MEASUREMENT, на сску SUPPLIER), но при этом должна как раз отнаследовать ВГХ с новой текущей
     * родительской МСКУ по форс-пайпу.
     */
    @Test
    public void whenNoChangesInIntergoldAndMskuShouldStillComputeFinalSsku() {
        var ownSource = new MasterDataSource(MasterDataSourceType.SUPPLIER, ImpersonalSourceId.DATACAMP.name());
        var inheritedSource = new MasterDataSource(MasterDataSourceType.MSKU_INHERIT,
            MasterDataSourceType.addMskuSourcePrefix(MSKU_ID, ""));

        var sskuWithFinalVgh = new CommonSskuBuilder(mdmParamCache, KEY)
            .withVghAfterInheritance(10, 11, 12, 1)
            .customized(v -> v.setMasterDataSource(inheritedSource))
            .build();
        var silverSsku = SilverCommonSsku.fromCommonSsku(sskuWithFinalVgh, ownSource);

        silverSskuRepository.insertOrUpdateSsku(silverSsku);

        mskuAndSskuQueue.enqueueSsku(KEY, MdmEnqueueReason.CHANGED_MAPPING_EOX, 1);

        // Сперва просто просчитаем золото, чтобы оно появилось
        execute();

        // Теперь золото есть. И поскольку серебро не поменяется, то и золото тоже. Мы хотим проверить, что в условиях
        // отсутствия существенных изменений пайплайн всё равно дойдёт до наследования.
        // Потому дропнем из промежолота отнаследованные ВГХ и перепроверим, что они снова появятся.
        var hackedIntergold = goldSskuRepository.findSsku(KEY).get();
        hackedIntergold.removeBaseValue(KnownMdmParams.WIDTH);
        hackedIntergold.removeBaseValue(KnownMdmParams.HEIGHT);
        hackedIntergold.removeBaseValue(KnownMdmParams.LENGTH);
        hackedIntergold.removeBaseValue(KnownMdmParams.WEIGHT_GROSS);
        goldSskuRepository.deleteAllSskus();
        goldSskuRepository.insertOrUpdateSsku(hackedIntergold);

        execute();

        var inheritedGold = goldSskuRepository.findSsku(KEY).get();
        for (var paramId : KnownMdmParams.MANDATORY_WEIGHT_DIMENSIONS_PARAMS) {
            Assertions.assertThat(
                inheritedGold.getBaseValue(paramId).get().valueEquals(
                    sskuWithFinalVgh.getBaseValue(paramId).get())).isTrue();
        }
    }
}
