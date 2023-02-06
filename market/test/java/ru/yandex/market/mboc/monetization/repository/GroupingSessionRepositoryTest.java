package ru.yandex.market.mboc.monetization.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.mbo.jooq.repo.JooqRepository;
import ru.yandex.market.mboc.common.BaseJooqRepositoryTestClass;
import ru.yandex.market.mboc.common.db.jooq.generated.monetization.tables.pojos.GroupingSession;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.monetization.config.MonetizationJooqConfig;

/**
 * @author danfertev
 * @since 23.10.2019
 */
@ContextConfiguration(classes = {MonetizationJooqConfig.class})
public class GroupingSessionRepositoryTest extends BaseJooqRepositoryTestClass<GroupingSession, Long> {

    @Autowired
    private GroupingSessionRepository repository;

    public GroupingSessionRepositoryTest() {
        super(GroupingSession.class, GroupingSession::getId);
        generatedFields = new String[]{"startedAt"};
    }

    @Override
    public void setUpRandom() {
        this.random = TestUtils.createMskuRandom(1);
    }

    @Override
    protected JooqRepository<GroupingSession, ?, Long, ?, ?> repository() {
        return repository;
    }
}
