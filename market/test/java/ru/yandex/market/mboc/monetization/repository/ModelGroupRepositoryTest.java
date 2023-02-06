package ru.yandex.market.mboc.monetization.repository;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mbo.jooq.repo.JooqRepository;
import ru.yandex.market.mboc.common.BaseJooqRepositoryTestClass;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.ModelGroup;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.monetization.GroupingTestUtils;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;
import ru.yandex.market.mboc.monetization.repository.filters.ModelGroupFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author danfertev
 * @since 23.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class ModelGroupRepositoryTest extends BaseJooqRepositoryTestClass<ModelGroup, Long> {

    @Autowired
    private ModelGroupRepository repository;

    public ModelGroupRepositoryTest() {
        super(ModelGroup.class, ModelGroup::getId);
        generatedFields = new String[]{"modifiedAt", "createdAt"};
    }


    @Override
    public void setUpRandom() {
        this.random = TestUtils.createMskuRandom(1L);
    }

    @Override
    protected JooqRepository<ModelGroup, ?, Long, ?, ?> repository() {
        return repository;
    }

    @Test
    public void testFindByGroupName() {
        var group1 = GroupingTestUtils.simpleGroup().setName("Group 66");
        var group2 = GroupingTestUtils.simpleGroup().setName("Группа 66");
        var group3 = GroupingTestUtils.simpleGroup().setName("Group 63");

        repository.save(group1, group2, group3);

        assertThat(repository.find(new ModelGroupFilter().setGroupName("GROUP")))
            .extracting(ModelGroup::getName)
            .containsExactlyInAnyOrder("Group 66", "Group 63");

        assertThat(repository.find(new ModelGroupFilter().setGroupName("группа")))
            .extracting(ModelGroup::getName)
            .containsExactlyInAnyOrder("Группа 66");

        assertThat(repository.find(new ModelGroupFilter().setGroupName("6")))
            .extracting(ModelGroup::getName)
            .containsExactlyInAnyOrder("Group 66", "Group 63", "Группа 66");
    }
}
