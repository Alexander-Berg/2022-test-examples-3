package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_CALLTRACKING_SETTINGS;

public class TestCampCalltrackingSettingsRepository {
    @Autowired
    private DslContextProvider dslContextProvider;

    public void link(int shard, Long cid, Long calltrackingSettingsId) {
        dslContextProvider.ppc(shard)
                .insertInto(CAMP_CALLTRACKING_SETTINGS)
                .set(CAMP_CALLTRACKING_SETTINGS.CID, cid)
                .set(CAMP_CALLTRACKING_SETTINGS.CALLTRACKING_SETTINGS_ID, calltrackingSettingsId)
                .onDuplicateKeyUpdate()
                .set(CAMP_CALLTRACKING_SETTINGS.CALLTRACKING_SETTINGS_ID, calltrackingSettingsId)
                .execute();
    }

    @QueryWithoutIndex("очищаем таблицу для тестов")
    public void deleteAll(int shard) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CAMP_CALLTRACKING_SETTINGS)
                .execute();
    }
}
