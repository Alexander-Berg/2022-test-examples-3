package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_CALLTRACKING_PHONES;

@QueryWithoutIndex("тестовый репозиторий")
public class TestCampCalltrackingPhonesRepository {
    @Autowired
    private DslContextProvider dslContextProvider;

    public void deleteAll(int shard) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CAMP_CALLTRACKING_PHONES)
                .execute();
    }
}
