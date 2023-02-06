package ru.yandex.market.mbo.mdm.common.masterdata.services.business;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmBusinessMergeRule;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmMergeSetting;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmMergeSettingsRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

public class MdmMergeSettingsCacheImplTest extends MdmBaseDbTestClass {
    private static final long SEED = 100563L;
    @Autowired
    private MdmMergeSettingsRepository repository;
    private MdmMergeSettingsCache cache;
    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(SEED);
        cache = new MdmMergeSettingsCacheImpl(repository);
    }

    @Test
    public void testCachedValuesPreservedAfterInsertion() {
        long mdmParamId = random.nextLong();
        MdmMergeSetting record = new MdmMergeSetting()
            .setBlockType(ItemBlock.BlockType.MDM_PARAM)
            .setMdmParamId(mdmParamId)
            .setMergeRules(MdmBusinessMergeRule.LATEST);
        repository.insert(record);

        List<MdmBusinessMergeRule> foundByPid = cache.getCachedRulesForParam(mdmParamId);
        Assertions.assertThat(foundByPid).containsExactly(MdmBusinessMergeRule.LATEST);

        record.setMergeRules(MdmBusinessMergeRule.SPLIT_BY_SERVICE, MdmBusinessMergeRule.LATEST);
        repository.update(record);

        // cached values haven't changed yet; assuming test exec time is less than 15 minutes (:
        foundByPid = cache.getCachedRulesForParam(mdmParamId);
        Assertions.assertThat(foundByPid).containsExactly(MdmBusinessMergeRule.LATEST);

        // but they are changed in db
        Assertions.assertThat(repository.findById(record.getKey()).getMergeRules()).containsExactly(
            MdmBusinessMergeRule.SPLIT_BY_SERVICE, MdmBusinessMergeRule.LATEST
        );
    }
}
