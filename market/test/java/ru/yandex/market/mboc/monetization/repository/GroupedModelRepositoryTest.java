package ru.yandex.market.mboc.monetization.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mbo.jooq.repo.JooqRepository;
import ru.yandex.market.mboc.common.BaseJooqRepositoryTestClass;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupedModel;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;

/**
 * @author danfertev
 * @since 23.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class GroupedModelRepositoryTest extends BaseJooqRepositoryTestClass<GroupedModel, Long> {
    @Autowired
    private GroupedModelRepository repository;

    public GroupedModelRepositoryTest() {
        super(GroupedModel.class, GroupedModel::getId);
        generatedFields = new String[]{"createdAt"};
    }

    @Override
    protected JooqRepository<GroupedModel, ?, Long, ?, ?> repository() {
        return repository;
    }
}
