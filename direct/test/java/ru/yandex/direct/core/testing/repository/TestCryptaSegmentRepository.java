package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.dbschema.ppcdict.tables.records.CryptaGoalsRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppcdict.tables.CryptaGoals.CRYPTA_GOALS;

public class TestCryptaSegmentRepository {

    private final CryptaSegmentRepository cryptaSegmentRepository;
    private final DslContextProvider dslContextProvider;

    public TestCryptaSegmentRepository(CryptaSegmentRepository cryptaSegmentRepository,
                                       DslContextProvider dslContextProvider) {
        this.cryptaSegmentRepository = cryptaSegmentRepository;
        this.dslContextProvider = dslContextProvider;
    }

    public void clean() {
        dslContextProvider.ppcdict().truncate(CRYPTA_GOALS).execute();
    }

    /**
     * Добавление записей таблицы
     */
    public void addAll(Collection<Goal> goals) {
        addAll(goals, Set.of(CryptaGoalScope.COMMON));
    }

    /**
     * Добавление записей таблицы
     */
    public void addAll(Collection<Goal> goals, Set<CryptaGoalScope> scopes) {
        String scopeOfGoals = scopes.stream()
                .map(CryptaGoalScope::getTypedValue)
                .collect(Collectors.joining(","));
        if (!goals.isEmpty()) {
            InsertHelper<CryptaGoalsRecord> insertHelper =
                    new InsertHelper<>(dslContextProvider.ppcdict(), CRYPTA_GOALS);
            for (Goal goal : goals) {
                insertHelper.add(cryptaSegmentRepository.mapper, goal)
                        .set(CRYPTA_GOALS.SCOPE, scopeOfGoals)
                        .newRecord();
            }
            insertHelper
                    .onDuplicateKeyIgnore()
                    .execute();
        }
    }

}
