package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.MdmGoodGroup;

public class MdmGoodGroupRepositoryImplTest extends MdmGenericMapperRepositoryTestBase<MdmGoodGroupRepositoryImpl,
    MdmGoodGroup, Long> {

    @Override
    protected MdmGoodGroup randomRecord() {
        return random.nextObject(MdmGoodGroup.class);
    }

    @Override
    protected String[] getFieldsToIgnore() {
        return new String[0];
    }

    @Override
    protected Function<MdmGoodGroup, Long> getIdSupplier() {
        return MdmGoodGroup::getId;
    }

    @Override
    protected List<BiConsumer<Integer, MdmGoodGroup>> getUpdaters() {
        return List.of(
            (i, record) -> record.setGroupName("some group"),
            (i, record) -> record.setGroupName("施氏食獅史")
        );
    }

    @Override
    protected boolean isIdGenerated() {
        return true;
    }

    @Test
    public void whenAddFewGoodGroups() {
        MdmGoodGroup goodGroup1 = new MdmGoodGroup();
        goodGroup1.setGroupName("some good group");
        MdmGoodGroup goodGroup2 = new MdmGoodGroup();
        goodGroup2.setGroupName("other good group");
        repository.insertBatch(List.of(goodGroup1, goodGroup2));
        Assertions.assertThat(goodGroup1).isIn(repository.findAll());
        Assertions.assertThat(goodGroup2).isIn(repository.findAll());
    }
}
