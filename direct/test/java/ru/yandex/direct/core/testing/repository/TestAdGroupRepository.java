package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;

import one.util.streamex.StreamEx;
import org.jooq.DSLContext;
import org.jooq.util.mysql.MySQLDSL;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.PageBlock;
import ru.yandex.direct.core.entity.adgroup.model.StatusBLGenerated;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.dbschema.ppc.enums.PhrasesStatusbssynced;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.core.entity.adgroup.repository.typesupport.Common.statusBLGeneratedToDbAdgroupsDynamic;
import static ru.yandex.direct.core.entity.adgroup.repository.typesupport.Common.statusBLGeneratedToDbAdgroupsText;
import static ru.yandex.direct.core.entity.adgroup.repository.typesupport.CpmOutdoorAdGroupSupport.pageBlocksToDb;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_DYNAMIC;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_PERFORMANCE;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_TEXT;
import static ru.yandex.direct.dbschema.ppc.tables.AdgroupPageTargets.ADGROUP_PAGE_TARGETS;
import static ru.yandex.direct.dbschema.ppc.tables.Campaigns.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.tables.Phrases.PHRASES;

/**
 * Работа с группами объявлений в тестах
 */
public class TestAdGroupRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * Обновить статус модерации групп объявлений
     *
     * @param shard          Шард
     * @param adGroupIds     Список групп объявлений
     * @param statusModerate Новый статус модерации
     */
    public void updateStatusModerate(int shard, Collection<Long> adGroupIds, @NotNull StatusModerate statusModerate) {
        if (adGroupIds.isEmpty()) {
            return;
        }
        dslContextProvider.ppc(shard)
                .update(PHRASES)
                .set(PHRASES.STATUS_MODERATE, StatusModerate.toSource(statusModerate))
                .set(PHRASES.LAST_CHANGE, PHRASES.LAST_CHANGE)
                .where(PHRASES.PID.in(adGroupIds))
                .execute();
    }

    public void updateStatusBsSynced(int shard, Long adGroupId, PhrasesStatusbssynced statusBsSynced) {
        dslContextProvider.ppc(shard)
                .update(PHRASES)
                .set(PHRASES.STATUS_BS_SYNCED, statusBsSynced)
                .where(PHRASES.PID.eq(adGroupId))
                .execute();
    }

    public void updateStatusBlGenerated(int shard, Collection<Long> adGroupIds, @NotNull StatusBLGenerated statusBlGenerated) {
        DSLContext context = dslContextProvider.ppc(shard);
        context.batch(
                context.update(ADGROUPS_TEXT)
                        .set(ADGROUPS_TEXT.STATUS_BL_GENERATED, statusBLGeneratedToDbAdgroupsText(statusBlGenerated))
                        .where(ADGROUPS_TEXT.PID.in(adGroupIds)),
                context.update(ADGROUPS_DYNAMIC)
                        .set(ADGROUPS_DYNAMIC.STATUS_BL_GENERATED, statusBLGeneratedToDbAdgroupsDynamic(statusBlGenerated))
                        .where(ADGROUPS_DYNAMIC.PID.in(adGroupIds)),
                context.update(ADGROUPS_PERFORMANCE)
                        .set(ADGROUPS_PERFORMANCE.STATUS_BL_GENERATED, StatusBLGenerated.toSource(statusBlGenerated))
                        .where(ADGROUPS_PERFORMANCE.PID.in(adGroupIds)))
                .execute();
    }

    public void updateAdGroupName(int shard, long id, String name) {
        dslContextProvider.ppc(shard)
                .update(PHRASES)
                .set(PHRASES.GROUP_NAME, name)
                .where(PHRASES.PID.eq(id))
                .execute();
    }

    /**
     * Обновить тип группы объявлений
     *
     * @param shard Шард
     * @param id    ID группы объявлений
     * @param type  Новый тип
     */
    public void updateAdGroupType(int shard, long id, AdGroupType type) {
        dslContextProvider.ppc(shard)
                .update(PHRASES)
                .set(PHRASES.ADGROUP_TYPE, AdGroupType.toSource(type))
                .where(PHRASES.PID.eq(id))
                .execute();
    }

    public Set<Long> getClientAdGroupIds(int shard, ClientId clientId) {
        return dslContextProvider.ppc(shard)
                .select(PHRASES.PID)
                .from(PHRASES)
                .join(CAMPAIGNS).on(CAMPAIGNS.CID.eq(PHRASES.CID))
                .where(CAMPAIGNS.CLIENT_ID.eq(clientId.asLong()))
                .fetchSet(PHRASES.PID);
    }

    /**
     * Обновить гео группы объявлений
     *
     * @param shard     шард
     * @param adGroupId id группы
     * @param geoIds    новое гео
     */
    public void updateAdGroupGeo(int shard, Long adGroupId, List<Long> geoIds) {
        dslContextProvider.ppc(shard)
                .update(PHRASES)
                .set(PHRASES.GEO, StreamEx.of(geoIds).joining(","))
                .where(PHRASES.PID.eq(adGroupId))
                .execute();
    }

    public void addOutdoorPageTargets(int shard, @Nonnull Long adGroupId, @Nonnull List<PageBlock> pageBlocks) {
        dslContextProvider.ppc(shard)
                .insertInto(ADGROUP_PAGE_TARGETS)
                .set(ADGROUP_PAGE_TARGETS.PID, adGroupId)
                .set(ADGROUP_PAGE_TARGETS.PAGE_BLOCKS, pageBlocksToDb(pageBlocks))
                .onDuplicateKeyUpdate()
                .set(ADGROUP_PAGE_TARGETS.PAGE_BLOCKS, MySQLDSL.values(ADGROUP_PAGE_TARGETS.PAGE_BLOCKS))
                .execute();
    }

    public Map<Long, PhrasesStatusbssynced> getStatusBsSynced(int shard, Collection<Long> pids) {
        return dslContextProvider.ppc(shard)
                .select(PHRASES.PID, PHRASES.STATUS_BS_SYNCED)
                .from(PHRASES)
                .where(PHRASES.PID.in(pids))
                .fetchMap(PHRASES.PID, PHRASES.STATUS_BS_SYNCED);
    }

    public void updateAdGroupBsRarelyLoaded(int shard, Long adGroupId, boolean isBsRarelyLoaded) {
        dslContextProvider.ppc(shard)
                .update(PHRASES)
                .set(PHRASES.IS_BS_RARELY_LOADED, RepositoryUtils.booleanToLong(isBsRarelyLoaded))
                .where(PHRASES.PID.eq(adGroupId))
                .execute();
    }
}
