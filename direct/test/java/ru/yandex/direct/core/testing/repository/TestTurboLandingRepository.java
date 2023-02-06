package ru.yandex.direct.core.testing.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jooq.DSLContext;
import org.jooq.InsertSetStep;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.util.mysql.MySQLDSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithTurboLanding;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLandingStatusModerate;
import ru.yandex.direct.core.testing.info.BannerTurboLandingInfo;
import ru.yandex.direct.dbschema.ppc.enums.BannerTurbolandingsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.TurbolandingsStatusmoderateforcpa;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerTurbolandingsRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.TurbolandingMetrikaCountersRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate.toSource;
import static ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository.GOAL_ROLE_COMBINED;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_TURBOLANDINGS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_METRIKA_GOALS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_TURBOLANDING_METRIKA_COUNTERS;
import static ru.yandex.direct.dbschema.ppc.Tables.TURBOLANDINGS;
import static ru.yandex.direct.dbschema.ppc.Tables.TURBOLANDING_METRIKA_COUNTERS;


@Repository
public class TestTurboLandingRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestTurboLandingRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Удаляет указанные турболендинги из таблицы turbolandings
     * Отвязка при этом не происходит.
     */
    public void deleteTurboLandings(int shard, Collection<Long> tlIds) {
        if (tlIds.isEmpty()) {
            return;
        }
        dslContextProvider.ppc(shard)
                .deleteFrom(TURBOLANDINGS)
                .where(TURBOLANDINGS.TL_ID.in(tlIds))
                .execute();
    }

    /**
     * Устанавливает статус модерации statusModerateForCpa для указанных туробостраниц
     */
    public void setTurbolandingStatusModerateForCpa(int shard, Collection<Long> turbolandingIds,
                                                    TurbolandingsStatusmoderateforcpa statusModerateForCpa) {
        if (turbolandingIds.isEmpty()) {
            return;
        }
        dslContextProvider.ppc(shard)
                .update(TURBOLANDINGS)
                .set(TURBOLANDINGS.STATUS_MODERATE_FOR_CPA, statusModerateForCpa)
                .where(TURBOLANDINGS.TL_ID.in(turbolandingIds))
                .execute();
    }

    /**
     * Для переданного идентификатора кампании и списка идентификаторов баннеров возвращает список счетчиков метрики,
     * сохраненных в ppc.camp_turbolanding_metrika_counters
     *
     * @param shard
     * @param cid
     * @param bids
     * @return
     */
    public Collection<Long> getCampMetrikaCounters(int shard, Long cid, Collection<Long> bids) {
        Result<Record1<Long>> records = dslContextProvider.ppc(shard).select(
                CAMP_TURBOLANDING_METRIKA_COUNTERS.METRIKA_COUNTER
        )
                .from(CAMP_TURBOLANDING_METRIKA_COUNTERS)
                .where(
                        CAMP_TURBOLANDING_METRIKA_COUNTERS.CID.eq(cid)
                                .and(
                                        CAMP_TURBOLANDING_METRIKA_COUNTERS.BID.in(bids))
                ).fetch();

        return new ArrayList<>(records.map(Record1::value1));
    }

    /**
     * Для переданного идентификатора кампании и списка целей метрики возвращает map: цель_метрики->значение счетчика,
     * сохраненных в ppc.camp_metrika_goals
     * TODO: узнать у adubinkin, почему описание возврата выше расходится с кодом
     *
     * @param shard — шард
     * @param cid   — номер кампании
     * @param goals — список целей
     * @return — словарь id цели в количество объектов, на неё ссылающихся
     */
    public Map<Long, Long> getCampMetrikaGoalCounters(int shard, Long cid, Collection<Long> goals) {
        Result<Record2<Long, Long>> records = dslContextProvider.ppc(shard).select(
                CAMP_METRIKA_GOALS.GOAL_ID,
                CAMP_METRIKA_GOALS.LINKS_COUNT
        )
                .from(CAMP_METRIKA_GOALS)
                .where(
                        CAMP_METRIKA_GOALS.CID.eq(cid)
                                .and(CAMP_METRIKA_GOALS.GOAL_ID.in(goals))
                                .and(CAMP_METRIKA_GOALS.GOAL_ROLE.contains(GOAL_ROLE_COMBINED))
                ).fetch();

        Map<Long, Long> result = new HashMap<>();
        records.forEach(r -> result.put(r.value1(), r.value2()));

        return result;
    }

    public <B extends OldBannerWithTurboLanding> void addTurbolandingMetricaCounters(int shard, B banner,
                                                                                     List<Long> metricaCounters) {
        if (banner.getTurboLandingId() == null) {
            throw new IllegalArgumentException("No turbolanding!!!");
        }
        addTurbolandingMetricaCounters(shard, banner.getTurboLandingId(), metricaCounters);
    }

    public void addTurbolandingMetricaCounters(int shard, Long tlId, List<Long> metricaCounters) {
        InsertSetStep<TurbolandingMetrikaCountersRecord> insertSetStep =
                dslContextProvider.ppc(shard).insertInto(TURBOLANDING_METRIKA_COUNTERS);

        metricaCounters.stream().map(counter -> new TurbolandingMetrikaCountersRecord()
                .with(TURBOLANDING_METRIKA_COUNTERS.TL_ID, tlId)
                .with(TURBOLANDING_METRIKA_COUNTERS.COUNTER, counter)
        ).forEach(record -> insertSetStep.set(record).onDuplicateKeyIgnore().execute());
    }

    public <B extends BannerWithTurboLanding> void addTurbolandingMetricaCounters(int shard, B banner,
                                                                                  List<Long> metricaCounters) {

        InsertSetStep<TurbolandingMetrikaCountersRecord> insertSetStep =
                dslContextProvider.ppc(shard).insertInto(TURBOLANDING_METRIKA_COUNTERS);

        if (banner.getTurboLandingId() == null) {
            throw new IllegalArgumentException("No turbolanding!!!");
        }
        metricaCounters.stream().map(counter -> new TurbolandingMetrikaCountersRecord()
                .with(TURBOLANDING_METRIKA_COUNTERS.TL_ID, banner.getTurboLandingId())
                .with(TURBOLANDING_METRIKA_COUNTERS.COUNTER, counter)
        ).forEach(record -> insertSetStep.set(record).onDuplicateKeyIgnore().execute());
    }

    public OldBannerTurboLandingStatusModerate getBannerTurboLandingStatusModerate(int shard, Long bannerId) {
        BannerTurbolandingsStatusmoderate sourceStatus = dslContextProvider.ppc(shard)
                .select(BANNER_TURBOLANDINGS.STATUS_MODERATE)
                .from(BANNER_TURBOLANDINGS)
                .where(BANNER_TURBOLANDINGS.BID.eq(bannerId))
                .fetchOne(BANNER_TURBOLANDINGS.STATUS_MODERATE);

        return OldBannerTurboLandingStatusModerate.fromSource(sourceStatus);
    }


    /**
     * Добавляет в таблицу BANNER_TURBOLANDINGS новые записи или обновляет существующие.
     * Статус модерации турболендинга должен быть выставлен до вызова этого метода.
     *
     * @param banners баннеры, которые нужно добавить
     * @param context контекст
     * @param <T>     тип баннеров, поддерживающих турболендинги
     */
    public <T extends BannerWithTurboLanding> void addBannerToBannerTurbolandingsTableOrUpdate(Collection<T> banners,
                                                                                               DSLContext context) {
        InsertHelper<BannerTurbolandingsRecord> insertHelper =
                new InsertHelper<>(context, BANNER_TURBOLANDINGS);

        Predicate<BannerWithTurboLanding> hasTurbolanding = banner -> banner.getTurboLandingId() != null;

        banners.stream().filter(hasTurbolanding)
                .forEach(
                        b -> {
                            if (b.getTurboLandingStatusModerate() == null) {
                                throw new IllegalArgumentException(
                                        "Null turbolandingStatusModerate in banner with id " + b.getId());
                            }
                            insertHelper.set(BANNER_TURBOLANDINGS.TL_ID, b.getTurboLandingId());
                            insertHelper.set(BANNER_TURBOLANDINGS.BID, b.getId());
                            insertHelper.set(BANNER_TURBOLANDINGS.CID, b.getCampaignId());
                            insertHelper.set(BANNER_TURBOLANDINGS.STATUS_MODERATE,
                                    toSource(b.getTurboLandingStatusModerate()));
                            insertHelper.newRecord();
                        }
                );
        if (insertHelper.hasAddedRecords()) {
            insertHelper.onDuplicateKeyUpdate()
                    .set(BANNER_TURBOLANDINGS.TL_ID, MySQLDSL.values(BANNER_TURBOLANDINGS.TL_ID))
                    .set(BANNER_TURBOLANDINGS.STATUS_MODERATE, MySQLDSL.values(BANNER_TURBOLANDINGS.STATUS_MODERATE));
            insertHelper.execute();
        }
    }

    public void linkBannerWithTurboLanding(int shard, BannerTurboLandingInfo bannerTurboLandingInfo) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_TURBOLANDINGS)
                .set(BANNER_TURBOLANDINGS.TL_ID, bannerTurboLandingInfo.getTurboLandingId())
                .set(BANNER_TURBOLANDINGS.BID, bannerTurboLandingInfo.getBannerId())
                .set(BANNER_TURBOLANDINGS.CID, bannerTurboLandingInfo.getCampaignId())
                .set(BANNER_TURBOLANDINGS.IS_DISABLED, bannerTurboLandingInfo.isDisabled() ? 1L : 0L)
                .set(BANNER_TURBOLANDINGS.STATUS_MODERATE, toSource(bannerTurboLandingInfo.getStatusModerate()))
                .execute();
    }
}
