package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CALLTRACKING_SETTINGS;

@QueryWithoutIndex("тестовый репозиторий")
public class TestCalltrackingSettingsRepository {
    @Autowired
    private DslContextProvider dslContextProvider;

    public void deleteAll(int shard) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CALLTRACKING_SETTINGS)
                .execute();
    }

    public void updateIsCounterAvailable(int shard, long calltrackingSettingsId, boolean isAvailable) {
        dslContextProvider.ppc(shard)
                .update(CALLTRACKING_SETTINGS)
                .set(CALLTRACKING_SETTINGS.IS_AVAILABLE_COUNTER, RepositoryUtils.booleanToLong(isAvailable))
                .where(CALLTRACKING_SETTINGS.CALLTRACKING_SETTINGS_ID.eq(calltrackingSettingsId))
                .execute();
    }
}
