package ru.yandex.market.mbo.mdm.tms.executors;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SilverSskuYtStorageQueue;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class EnqueueAllSilverSskusToYtStorageToolTest extends MdmBaseDbTestClass {
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private StorageKeyValueService skv;
    @Autowired
    private SilverSskuYtStorageQueue silverSskuYtStorageQueue;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;

    private EnhancedRandom random;
    private CopySilverToYtExecutor tool;

    @Before
    public void setup() {
        tool = new CopySilverToYtExecutor(
            silverSskuYtStorageQueue,
            skv,
            jdbcTemplate,
            transactionTemplate,
            mdmSskuGroupManager
        );
        random = TestDataUtils.defaultRandom(43535009002L);
        skv.putValue(MdmProperties.SHOULD_RUN_COPY_SILVER_TO_YT, true);
        skv.invalidateCache();
    }

    @Test
    public void testEnqueueAll() {
        Set<SilverSskuKey> expected = prepareSskus(100);

        tool.execute();

        List<SilverSskuKey> enqueued = silverSskuYtStorageQueue.findAll()
            .stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .collect(Collectors.toList());
        Assertions.assertThat(enqueued).containsExactlyInAnyOrderElementsOf(expected);
    }

    private Set<SilverSskuKey> prepareSskus(int count) {
        List<SilverCommonSsku> sskus = IntStream.range(0, count).mapToObj(i -> ssku()).collect(Collectors.toList());
        silverSskuRepository.insertOrUpdateSskus(sskus);
        return sskus.stream()
            .map(SilverCommonSsku::getBusinessKey)
            .collect(Collectors.toSet());
    }

    private SilverCommonSsku ssku() {
        var key = key();
        return new SilverCommonSsku(key)
            .addBaseValue(numeric(key))
            .addBaseValue(string(key))
            .addBaseValue(option(key))
            .addBaseValue(numeric(key))
            .addBaseValue(string(key))
            .addBaseValue(option(key))
            .addBaseValue(numeric(key))
            .addBaseValue(string(key))
            .addBaseValue(option(key));
    }

    private SilverSskuKey key() {
        return new SilverSskuKey(
            random.nextInt(Integer.MAX_VALUE),
            random.nextObject(String.class),
            random.nextObject(MasterDataSourceType.class),
            random.nextObject(String.class)
        );
    }

    private SskuSilverParamValue numeric(SilverSskuKey key) {
        SskuSilverParamValue v = new SskuSilverParamValue();
        v.setMasterDataSource(key.getMasterDataSource());
        v.setShopSkuKey(key.getShopSkuKey());
        v.setNumeric(BigDecimal.valueOf(random.nextInt()));
        v.setXslName(random.nextObject(String.class));
        v.setMdmParamId(Math.abs(random.nextLong()));
        return v;
    }

    private SskuSilverParamValue string(SilverSskuKey key) {
        SskuSilverParamValue v = new SskuSilverParamValue();
        v.setMasterDataSource(key.getMasterDataSource());
        v.setShopSkuKey(key.getShopSkuKey());
        v.setString(random.nextObject(String.class));
        v.setXslName(random.nextObject(String.class));
        v.setMdmParamId(Math.abs(random.nextLong()));
        return v;
    }

    private SskuSilverParamValue option(SilverSskuKey key) {
        SskuSilverParamValue v = new SskuSilverParamValue();
        v.setMasterDataSource(key.getMasterDataSource());
        v.setShopSkuKey(key.getShopSkuKey());
        v.setOption(new MdmParamOption(random.nextInt(10)));
        v.setXslName(random.nextObject(String.class));
        v.setMdmParamId(Math.abs(random.nextLong()));
        return v;
    }
}
