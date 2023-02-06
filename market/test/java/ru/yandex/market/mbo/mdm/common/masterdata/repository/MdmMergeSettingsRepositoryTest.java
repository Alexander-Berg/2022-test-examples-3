package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmBusinessMergeRule;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmMergeSetting;

public class MdmMergeSettingsRepositoryTest
    extends MdmGenericMapperRepositoryTestBase<MdmMergeSettingsRepository, MdmMergeSetting, MdmMergeSetting.Key> {

    @Override
    protected MdmMergeSetting randomRecord() {
        return random.nextObject(MdmMergeSetting.class);
    }

    @Override
    protected String[] getFieldsToIgnore() {
        return new String[]{"updatedTs"};
    }

    @Override
    protected Function<MdmMergeSetting, MdmMergeSetting.Key> getIdSupplier() {
        return MdmMergeSetting::getKey;
    }

    @Override
    protected List<BiConsumer<Integer, MdmMergeSetting>> getUpdaters() {
        return List.of(
            (i, record) -> record.setMergeRules(MdmBusinessMergeRule.LATEST),
            (i, record) -> record.setMergeRules(
                MdmBusinessMergeRule.MOST_SPECIFIC_IN_GROUP, MdmBusinessMergeRule.EARLIEST),
            (i, record) -> record.setMergeRules(
                MdmBusinessMergeRule.SPLIT_BY_SERVICE)
        );
    }
}
