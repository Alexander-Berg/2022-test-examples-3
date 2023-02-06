package ru.yandex.market.mboc.monetization.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mbo.jooq.repo.JooqWithDeletedFieldRepository;
import ru.yandex.market.mboc.common.BaseJooqWithDeletedFieldRepositoryTestClass;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupingConfig;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;

/**
 * @author eremeevvo
 * @since 17.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class GroupingConfigRepositoryTest
    extends BaseJooqWithDeletedFieldRepositoryTestClass<GroupingConfig, Long> {

    @Autowired
    private GroupingConfigRepository repository;

    public GroupingConfigRepositoryTest() {
        super(GroupingConfig.class, GroupingConfig::getId);
        generatedFields = new String[]{"modifiedAt", "createdAt", "deleted"};
    }

    @Override
    protected JooqWithDeletedFieldRepository<GroupingConfig, ?, Long, ?, ?> repository() {
        return repository;
    }
}
