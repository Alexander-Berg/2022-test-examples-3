package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.warnplace.repository.WarnplaceRepository;
import ru.yandex.direct.dbschema.ppc.Tables;
import ru.yandex.direct.dbschema.ppc.enums.WarnplaceDone;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.tables.Warnplace.WARNPLACE;

/**
 * Работа с таблицей warnplace в тестах
 */
@ParametersAreNonnullByDefault
@Repository
public class TestWarnplaceRepository extends WarnplaceRepository {
    @Autowired
    public TestWarnplaceRepository(DslContextProvider dslContextProvider) {
        super(dslContextProvider);
    }

    /**
     * Создает запись в таблице warnplace с заданными параметрами.
     *
     * @param shard      шард
     * @param uid        ID пользователя
     * @param cid        ID кампании
     * @param bid        ID баннера
     * @param managerUid ID менеджера
     * @param agencyUid  ID агентства
     * @param addTime    время создания записи
     * @return Число созданных записей
     */
    public int createWarnplace(int shard, @Nullable Long uid, Long cid, Long bid, Long statusPlace, Long oldPlace,
                               Long id,
                               Long clicks, Long shows, @Nullable Long managerUid, @Nullable Long agencyUid,
                               WarnplaceDone done,
                               LocalDateTime addTime, Long pid) {
        return dslContextProvider.ppc(shard)
                .insertInto(Tables.WARNPLACE)
                .set(Tables.WARNPLACE.UID, uid)
                .set(Tables.WARNPLACE.CID, cid)
                .set(Tables.WARNPLACE.BID, bid)
                .set(Tables.WARNPLACE.STATUS_PLACE, statusPlace)
                .set(Tables.WARNPLACE.OLD_PLACE, oldPlace)
                .set(Tables.WARNPLACE.ID, id)
                .set(Tables.WARNPLACE.CLICKS, clicks)
                .set(Tables.WARNPLACE.SHOWS, shows)
                .set(Tables.WARNPLACE.MANAGERUID, managerUid)
                .set(Tables.WARNPLACE.AGENCYUID, agencyUid)
                .set(Tables.WARNPLACE.DONE, done)
                .set(Tables.WARNPLACE.ADDTIME, addTime)
                .set(Tables.WARNPLACE.PID, pid)
                .execute();
    }

    /**
     * Получает ID всех записей в таблице warnplace с указанного шарда.
     *
     * @param shard шард
     * @return список ID записей
     */
    public List<Long> getAllWarnplaceIds(int shard) {
        return dslContextProvider.ppc(shard).select(Tables.WARNPLACE.ID).from(Tables.WARNPLACE)
                .fetch(Tables.WARNPLACE.ID);
    }

    /**
     * Удаляет все записи из таблицы warnplace с указанного шарда
     *
     * @param shard шард
     */
    public void clearWarnplaceInShard(int shard) {
        dslContextProvider.ppc(shard).truncate(Tables.WARNPLACE).execute();
    }


    /**
     * Возвращает Id записей, существующих в базе
     *
     * @param shard шард
     * @param ids   Id записей, которые необходимо проверить
     * @return Id записей, существующих в базе
     */
    public Collection<Long> getExistentIds(int shard, Collection<Long> ids) {
        if (!ids.isEmpty()) {
            return dslContextProvider.ppc(shard)
                    .select(WARNPLACE.ID)
                    .from(WARNPLACE)
                    .where(WARNPLACE.ID.in(ids))
                    .fetch(WARNPLACE.ID);
        } else {
            return Collections.emptySet();
        }
    }
}
