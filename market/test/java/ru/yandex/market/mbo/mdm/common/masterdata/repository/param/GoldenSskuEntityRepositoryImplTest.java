package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmGenericMapperRepositoryTestBase;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCacheMock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;

public class GoldenSskuEntityRepositoryImplTest extends MdmGenericMapperRepositoryTestBase<
    GoldenSskuEntityRepositoryImpl,
    GoldenSskuEntityRepositoryImpl.GoldenEntity,
    ShopSkuKey> {

    @Autowired
    MdmParamCache mdmParamCache;

    @Override
    protected GoldenSskuEntityRepositoryImpl.GoldenEntity randomRecord() {
        return random.nextObject(GoldenSskuEntityRepositoryImpl.GoldenEntity.class);
    }

    @Override
    protected Function<GoldenSskuEntityRepositoryImpl.GoldenEntity, ShopSkuKey> getIdSupplier() {
        return (entity) -> new ShopSkuKey(entity.getSupplierId(), entity.getShopSku());
    }

    @Override
    protected List<BiConsumer<Integer, GoldenSskuEntityRepositoryImpl.GoldenEntity>> getUpdaters() {
        return List.of(
            (i, record) -> record.setEntity(new byte[]{0xf, 0x0, 0x0}),
            (i, record) -> record.setUpdateTs(Instant.now())
        );
    }

    @Test
    public void testCRUDCommonSsku() {
        var key = new ShopSkuKey(1, "sku");
        CommonSsku expectedSsku = new CommonSsku(key)
            .addBaseValue(
                new SskuParamValue()
                    .setShopSkuKey(key)
                    .setMdmParamId(KnownMdmParams.MANUFACTURER_COUNTRY)
                    .setString("Россия")
                    .setXslName("manufacturerCountry")
            )
            .addBaseValue(
                new SskuParamValue()
                    .setShopSkuKey(key)
                    .setMdmParamId(KnownMdmParams.WEIGHT_NET)
                    .setNumeric(BigDecimal.valueOf(30))
                    .setXslName("mdm_weight_net")
            )
            .addBaseValue(
                new SskuParamValue()
                    .setShopSkuKey(key)
                    .setMdmParamId(KnownMdmParams.BMDM_ID)
                    .setNumeric(BigDecimal.ZERO)
                    .setXslName("bmdmId")
            );

        var additionalParam = new SskuParamValue()
            .setShopSkuKey(key)
            .setMdmParamId(KnownMdmParams.WEIGHT_GROSS)
            .setNumeric(BigDecimal.valueOf(30))
            .setXslName("mdm_weight_gross");

        repository.insertOrUpdateSskus(List.of(expectedSsku));
        CommonSsku actualSsku = repository.findSskus(List.of(expectedSsku.getKey())).get(expectedSsku.getKey());
        Assertions.assertThat(actualSsku).isEqualTo(expectedSsku);

        expectedSsku.addBaseValue(additionalParam);
        repository.insertOrUpdateSskus(List.of(expectedSsku));
        actualSsku = repository.findSskus(List.of(expectedSsku.getKey())).get(expectedSsku.getKey());
        Assertions.assertThat(actualSsku).isEqualTo(expectedSsku);

        repository.deleteSsku(expectedSsku);
        actualSsku = repository.findSskus(List.of(expectedSsku.getKey())).get(expectedSsku.getKey());
        Assertions.assertThat(actualSsku).isNull();
    }

    @Test
    public void testInsertOrIgnore() {
        Random random = new Random("Во благо БМДМ работать по-стахановски!".hashCode());
        ShopSkuKey key1 = new ShopSkuKey(1, "1");
        ShopSkuKey key2 = new ShopSkuKey(2, "2");
        MdmParamCacheMock paramCache = TestMdmParamUtils.createParamCacheMock();

        // existing
        CommonSsku ssku1 = new CommonSsku(key1);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(paramCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(ssku1::addBaseValue);
        repository.insertOrIgnoreSskus(List.of(ssku1));
        Assertions.assertThat(repository.findAllSskus()).containsExactly(ssku1);

        // update of ssku1 + creating ssku2
        CommonSsku ssku1Updated = new CommonSsku(key1)
            .setBaseValues(ssku1.getBaseValues())
            .addBaseValue(
                TestMdmParamUtils.createRandomMdmParamValue(random, paramCache.get(KnownMdmParams.SHELF_LIFE_COMMENT)));
        CommonSsku ssku2 = new CommonSsku(key2);
        KnownMdmParams.WEIGHT_DIMENSIONS_PARAMS.stream()
            .map(paramCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(ssku2::addBaseValue);
        repository.insertOrIgnoreSskus(List.of(ssku1Updated, ssku2));
        List<CommonSsku> result = repository.findAllSskus();
        Assertions.assertThat(result).containsExactlyInAnyOrder(ssku1, ssku2);
        Assertions.assertThat(result).doesNotContain(ssku1Updated);
    }
}
