package ru.yandex.market.mbo.mdm.common.masterdata.services.ssku;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSilverItem;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.MasterDataIntoBlocksSplitterImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ParamValuesToCommonSskuGoldenMerger;
import ru.yandex.market.mbo.mdm.common.masterdata.model.msku.CommonMsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MskuParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.CommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverServiceSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.MdmParamCache;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.TestMdmParamUtils;
import ru.yandex.market.mbo.mdm.common.service.FeatureSwitchingAssistantImpl;
import ru.yandex.market.mbo.mdm.common.util.TimestampUtil;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.EXPIR_DATE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.IS_TRACEABLE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.LENGTH;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.QUANTUM_OF_SUPPLY;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE;
import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams.SHELF_LIFE_UNIT;

public class TraceableSskuGoldenItemServiceTest {
    private Random random;
    private MdmParamCache mdmParamCache;
    private TraceableSskuGoldenItemService traceableSskuGIS;

    @Before
    public void setUp() {
        random = new Random("Ave @dmserebr".hashCode());
        mdmParamCache = TestMdmParamUtils.createParamCacheMock();
        traceableSskuGIS = new TraceableSskuGoldenItemService(
            new TraceableSskuSilverSplitter(mdmParamCache, new MasterDataIntoBlocksSplitterImpl(mdmParamCache)),
            new ParamValuesToCommonSskuGoldenMerger(),
            new FeatureSwitchingAssistantImpl(new StorageKeyValueServiceMock())
        );
    }

    @Test
    public void testComputeOnlyTraceable() {
        // given
        SilverSskuKey silverSskuKey = new SilverSskuKey(123, "456", MasterDataSourceType.SUPPLIER, "123");
        SilverServiceSsku silver = new SilverServiceSsku(silverSskuKey);
        Stream.of(IS_TRACEABLE, LENGTH, QUANTUM_OF_SUPPLY, SHELF_LIFE, SHELF_LIFE_UNIT)
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .forEach(silver::addParamValue);

        // when
        CommonSsku computationResult =
            traceableSskuGIS.calculateGoldenItem(silverSskuKey.getShopSkuKey(), List.of(silver), null).orElseThrow();

        // then
        Assertions.assertThat(computationResult)
            .isEqualTo(new CommonSsku(silverSskuKey.getShopSkuKey())
                .addBaseValue(silver.getParamValue(IS_TRACEABLE).orElseThrow()));
    }

    @Test
    public void whenNoSilverThenNoGold() {
        Assertions.assertThat(traceableSskuGIS.calculateGoldenItem(new ShopSkuKey(123, "456"), List.of(), null))
            .isEmpty();
    }

    @Test
    public void testComputingFromMasterData() {
        // given
        ShopSkuKey shopSkuKey = new ShopSkuKey(123, "456");
        Instant ts = Instant.parse("2007-12-03T10:15:30.00Z");
        MasterData masterData = new MasterData()
            .setShopSkuKey(shopSkuKey)
            .setTraceable(true)
            .setCustomsCommodityCode("ignored")
            .setModifiedTimestamp(TimestampUtil.toLocalDateTime(ts));

        // when
        CommonSsku computationResult =
            traceableSskuGIS.calculateGoldenItem(shopSkuKey, List.of(new MasterDataSilverItem(masterData)), null)
                .orElseThrow();

        // then
        Assertions.assertThat(computationResult.getKey()).isEqualTo(shopSkuKey);
        Assertions.assertThat(computationResult.getBaseValues()).hasSize(1);
        Assertions.assertThat(computationResult.getBaseValue(IS_TRACEABLE))
            .hasValueSatisfying(pv -> Assertions.assertThat(pv.getMasterDataSource())
                .isEqualTo(MasterDataSource.DEFAULT_SOURCE))
            .hasValueSatisfying(pv -> Assertions.assertThat(pv.getBool().orElseThrow()).isTrue());
    }

    @Test
    public void testMskuInherit() {
        // given
        ShopSkuKey shopSkuKey = new ShopSkuKey(456, "789");
        long mskuId = 123;
        List<MskuParamValue> mskuParamValues = Stream.of(IS_TRACEABLE, EXPIR_DATE, LENGTH)
            .map(mdmParamCache::get)
            .map(param -> TestMdmParamUtils.createRandomMdmParamValue(random, param))
            .map(param -> param.setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT))
            .map(pv -> {
                MskuParamValue mskuParamValue = new MskuParamValue().setMskuId(mskuId);
                pv.copyTo(mskuParamValue);
                return mskuParamValue;
            })
            .collect(Collectors.toList());
        CommonMsku msku = new CommonMsku(mskuId, mskuParamValues);
        msku.getParamValue(IS_TRACEABLE).orElseThrow().setBool(true);

        // when
        CommonSsku computationResult =
            traceableSskuGIS.calculateGoldenItem(shopSkuKey, List.of(msku), null).orElseThrow();

        // then
        Assertions.assertThat(computationResult)
            .isEqualTo(new CommonSsku(shopSkuKey).addBaseValue(msku.getParamValue(IS_TRACEABLE).orElseThrow()));
    }


    @Test
    public void testMskuInheritBeatsMasterData() {
        // given
        ShopSkuKey shopSkuKey = new ShopSkuKey(456, "789");
        long mskuId = 123;
        Instant ts = Instant.parse("2007-12-03T10:15:30.00Z");

        MasterData masterData = new MasterData()
            .setShopSkuKey(shopSkuKey)
            .setTraceable(true)
            .setModifiedTimestamp(TimestampUtil.toLocalDateTime(ts));

        MskuParamValue traceableMskuParamValue = new MskuParamValue().setMskuId(mskuId);
        TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(IS_TRACEABLE))
            .copyTo(traceableMskuParamValue);
        traceableMskuParamValue.setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT);
        traceableMskuParamValue.setBool(false);
        traceableMskuParamValue.setUpdatedTs(ts.minusSeconds(123));
        CommonMsku msku = new CommonMsku(mskuId, List.of(traceableMskuParamValue));

        // when
        CommonSsku computationResult = traceableSskuGIS.calculateGoldenItem(
            shopSkuKey,
            List.of(msku, new MasterDataSilverItem(masterData)),
            null
        ).orElseThrow();

        // then
        Assertions.assertThat(computationResult)
            .isEqualTo(new CommonSsku(shopSkuKey).addBaseValue(msku.getParamValue(IS_TRACEABLE).orElseThrow()));
    }

    @Test
    public void testOperatorBeatsMskuInherit() {
        // given
        ShopSkuKey shopSkuKey = new ShopSkuKey(456, "789");
        long mskuId = 123;
        Instant ts = Instant.parse("2007-12-03T10:15:30.00Z");

        MasterDataSource silverSource = new MasterDataSource(MasterDataSourceType.MDM_OPERATOR, "maxkilin");
        SilverSskuKey silverSskuKey = new SilverSskuKey(shopSkuKey, silverSource);
        SilverServiceSsku silver = new SilverServiceSsku(silverSskuKey);
        silver.addParamValue(
            TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(IS_TRACEABLE))
                .setBool(true)
                .setUpdatedTs(ts.minusSeconds(123))
        );

        MskuParamValue traceableMskuParamValue = new MskuParamValue().setMskuId(mskuId);
        TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(IS_TRACEABLE))
            .copyTo(traceableMskuParamValue);
        traceableMskuParamValue.setMasterDataSourceType(MasterDataSourceType.MSKU_INHERIT);
        traceableMskuParamValue.setBool(false);
        traceableMskuParamValue.setUpdatedTs(ts);
        CommonMsku msku = new CommonMsku(mskuId, List.of(traceableMskuParamValue));

        // when
        CommonSsku computationResult = traceableSskuGIS.calculateGoldenItem(
            shopSkuKey,
            List.of(msku, silver),
            null
        ).orElseThrow();

        // then
        Assertions.assertThat(computationResult)
            .isEqualTo(new CommonSsku(shopSkuKey).addBaseValue(silver.getParamValue(IS_TRACEABLE).orElseThrow()));
    }

    @Test
    public void testAdminBeatsOperator() {
        // given
        ShopSkuKey shopSkuKey = new ShopSkuKey(456, "789");
        Instant ts = Instant.parse("2007-12-03T10:15:30.00Z");

        MasterDataSource silverSourceOperator = new MasterDataSource(MasterDataSourceType.MDM_OPERATOR, "maxkilin");
        SilverSskuKey silverSskuKeyOperator = new SilverSskuKey(shopSkuKey, silverSourceOperator);
        SilverServiceSsku operatorSilver = new SilverServiceSsku(silverSskuKeyOperator);
        operatorSilver.addParamValue(
            TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(IS_TRACEABLE))
                .setBool(false)
                .setUpdatedTs(ts)
        );

        MasterDataSource silverSourceAdmin = new MasterDataSource(MasterDataSourceType.MDM_ADMIN, "maxkilin");
        SilverSskuKey silverSskuKeyAdmin = new SilverSskuKey(shopSkuKey, silverSourceAdmin);
        SilverServiceSsku adminSilver = new SilverServiceSsku(silverSskuKeyAdmin);
        adminSilver.addParamValue(
            TestMdmParamUtils.createRandomMdmParamValue(random, mdmParamCache.get(IS_TRACEABLE))
                .setBool(true)
                .setUpdatedTs(ts.minusSeconds(123))
        );

        // when
        CommonSsku computationResult = traceableSskuGIS.calculateGoldenItem(
            shopSkuKey,
            List.of(operatorSilver, adminSilver),
            null
        ).orElseThrow();

        // then
        Assertions.assertThat(computationResult)
            .isEqualTo(new CommonSsku(shopSkuKey).addBaseValue(adminSilver.getParamValue(IS_TRACEABLE).orElseThrow()));
    }

    @Test
    public void shouldChooseLatestBetween2MasterDatas() {
        // given
        ShopSkuKey shopSkuKey = new ShopSkuKey(456, "789");
        Instant ts = Instant.parse("2007-12-03T10:15:30.00Z");

        MasterData masterDataEarliest = new MasterData()
            .setShopSkuKey(shopSkuKey)
            .setTraceable(true)
            .setModifiedTimestamp(TimestampUtil.toLocalDateTime(ts));

        MasterData masterDataLatest = new MasterData()
            .setShopSkuKey(shopSkuKey)
            .setTraceable(false)
            .setModifiedTimestamp(TimestampUtil.toLocalDateTime(ts.plusSeconds(123)));

        // when
        CommonSsku computationResult = traceableSskuGIS.calculateGoldenItem(
            shopSkuKey,
            Stream.of(masterDataEarliest, masterDataLatest)
                .map(MasterDataSilverItem::new)
                .collect(Collectors.toList()),
            null
        ).orElseThrow();

        // then
        Assertions.assertThat(computationResult.getKey()).isEqualTo(shopSkuKey);
        Assertions.assertThat(computationResult.getBaseValues()).hasSize(1);
        Assertions.assertThat(computationResult.getBaseValue(IS_TRACEABLE))
            .hasValueSatisfying(pv -> Assertions.assertThat(pv.getMasterDataSource())
                .isEqualTo(MasterDataSource.DEFAULT_SOURCE))
            .hasValueSatisfying(pv -> Assertions.assertThat(pv.getBool().orElseThrow()).isFalse())
            .hasValueSatisfying(pv -> Assertions.assertThat(pv.getUpdatedTs()).isEqualTo(ts.plusSeconds(123)));
    }
}
