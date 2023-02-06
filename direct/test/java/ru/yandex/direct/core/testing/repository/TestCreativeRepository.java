package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import org.jooq.util.mysql.MySQLDSL;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeBusinessType;
import ru.yandex.direct.core.entity.creative.model.ModerationInfo;
import ru.yandex.direct.core.entity.creative.model.ModerationInfoVideo;
import ru.yandex.direct.core.entity.creative.model.SourceMediaType;
import ru.yandex.direct.core.entity.creative.model.StatusModerate;
import ru.yandex.direct.core.entity.creative.repository.CreativeMappings;
import ru.yandex.direct.dbschema.ppc.enums.PerfCreativesStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.PerfCreativesRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapperhelper.JooqUpdateBuilder;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.utils.DateTimeUtils;
import ru.yandex.direct.utils.JsonUtils;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static ru.yandex.direct.dbschema.ppc.Tables.PERF_CREATIVES;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

public class TestCreativeRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    private AtomicLong nextCreativeId;

    /**
     * Удаляет креативы из базы
     *
     * @param shard       Шард
     * @param creativeIds Список ID креативов
     */
    public void deleteCreativesByIds(int shard, Long... creativeIds) {
        dslContextProvider.ppc(shard).deleteFrom(PERF_CREATIVES)
                .where(PERF_CREATIVES.CREATIVE_ID.in(creativeIds))
                .execute();
    }

    /**
     * Добавляет новый креатив в базу и возвращает его ID
     *
     * @param shard    Шард
     * @param creative Заполненная модель {@link Creative}
     * @return ID созданного креатива
     */
    public long addCreative(int shard, Creative creative) {
        long creativeId = getNextCreativeId();
        // set stockCreativeId as new generated creativeId;
        creative.withStockCreativeId(creativeId);

        dslContextProvider.ppc(shard)
                .insertInto(PERF_CREATIVES,
                        PERF_CREATIVES.CREATIVE_ID,
                        PERF_CREATIVES.CLIENT_ID,
                        PERF_CREATIVES.STOCK_CREATIVE_ID,
                        PERF_CREATIVES.NAME,
                        PERF_CREATIVES.CREATIVE_TYPE,
                        PERF_CREATIVES.SOURCE_MEDIA_TYPE,
                        PERF_CREATIVES.WIDTH,
                        PERF_CREATIVES.HEIGHT,
                        PERF_CREATIVES.ARCHIVE_URL,
                        PERF_CREATIVES.PREVIEW_URL,
                        PERF_CREATIVES.MODERATE_TRY_COUNT,
                        PERF_CREATIVES.LIVE_PREVIEW_URL,
                        PERF_CREATIVES.YABS_DATA,
                        PERF_CREATIVES.STATUS_MODERATE,
                        PERF_CREATIVES.MODERATE_INFO,
                        PERF_CREATIVES.DURATION,
                        PERF_CREATIVES.LAYOUT_ID,
                        PERF_CREATIVES.THEME_ID,
                        PERF_CREATIVES.BUSINESS_TYPE,
                        PERF_CREATIVES.SUM_GEO,
                        PERF_CREATIVES.GROUP_NAME,
                        PERF_CREATIVES.CREATIVE_GROUP_ID,
                        PERF_CREATIVES.ADDITIONAL_DATA,
                        PERF_CREATIVES.HAS_PACKSHOT,
                        PERF_CREATIVES.IS_ADAPTIVE,
                        PERF_CREATIVES.IS_BANNERSTORAGE_PREDEPLOYED
                )
                .values(creativeId,
                        creative.getClientId(),
                        creative.getStockCreativeId(),
                        creative.getName(),
                        CreativeMappings.creativeTypeToDb(creative.getType()),
                        SourceMediaType.toSource(creative.getSourceMediaType()),
                        creative.getWidth(),
                        creative.getHeight(),
                        creative.getArchiveUrl(),
                        creative.getPreviewUrl(),
                        creative.getModerateTryCount(),
                        creative.getLivePreviewUrl(),
                        ifNotNull(creative.getYabsData(), JsonUtils::toJson),
                        StatusModerate.toSource(creative.getStatusModerate()),
                        ifNotNull(creative.getModerationInfo(), JsonUtils::toJson),
                        creative.getDuration(),
                        creative.getLayoutId(),
                        creative.getThemeId(),
                        CreativeBusinessType.toSource(
                                defaultIfNull(creative.getBusinessType(), CreativeBusinessType.RETAIL)),
                        CreativeMappings.sumGeoToDb(creative.getSumGeo()),
                        creative.getGroupName(),
                        creative.getCreativeGroupId(),
                        CreativeMappings.additionalDataToDb(creative.getAdditionalData()),
                        RepositoryUtils.booleanToLong(creative.getHasPackshot()),
                        creative.getIsAdaptive(),
                        RepositoryUtils.nullSafeBooleanToLong(creative.getIsBannerstoragePredeployed()))
                .onDuplicateKeyUpdate()
                .set(PERF_CREATIVES.NAME, MySQLDSL.values(PERF_CREATIVES.NAME))
                .set(PERF_CREATIVES.PREVIEW_URL, MySQLDSL.values(PERF_CREATIVES.PREVIEW_URL))
                .set(PERF_CREATIVES.LIVE_PREVIEW_URL, MySQLDSL.values(PERF_CREATIVES.LIVE_PREVIEW_URL))
                .execute();

        return creativeId;
    }

    /**
     * Обновить время отправки креативов на модерацию
     */
    public void updateModerateSendTime(int shard, Collection<Long> creativeIds, LocalDateTime moderateSendTime) {
        dslContextProvider.ppc(shard)
                .update(PERF_CREATIVES)
                .set(PERF_CREATIVES.MODERATE_SEND_TIME, moderateSendTime)
                .where(PERF_CREATIVES.CREATIVE_ID.in(creativeIds))
                .execute();
    }

    /**
     * Обновить превью раскрывающегося блока расхлопа
     */
    public void updateExpandedPreviewUrl(int shard, Collection<Long> creativeIds, String expandedPreviewUrl) {
        dslContextProvider.ppc(shard)
                .update(PERF_CREATIVES)
                .set(PERF_CREATIVES.EXPANDED_PREVIEW_URL, expandedPreviewUrl)
                .where(PERF_CREATIVES.CREATIVE_ID.in(creativeIds))
                .execute();
    }

    public void updateVideoInfo(int shard, Collection<Long> creativeIds, String videoUrl, Long videoDuration) {
        dslContextProvider.ppc(shard)
                .update(PERF_CREATIVES)
                .set(PERF_CREATIVES.DURATION, videoDuration)
                .set(PERF_CREATIVES.MODERATE_INFO,
                        CreativeMappings.moderationInfoToDb(
                                new ModerationInfo()
                                        .withVideos(List.of(new ModerationInfoVideo()
                                                .withUrl(videoUrl)))
                        ))
                .where(PERF_CREATIVES.CREATIVE_ID.in(creativeIds))
                .execute();
    }

    /**
     * Возвращает следующий доступный ID для создания креатива, генерируя рандомный ключ и инкрементируя его при
     * каждом вызове
     */
    public Long getNextCreativeId() {
        if (nextCreativeId == null) {
            long value = ThreadLocalRandom.current().nextLong(Integer.MAX_VALUE, Long.MAX_VALUE - 100_000);
            value += DateTimeUtils.getNowEpochSeconds();
            nextCreativeId = new AtomicLong(value);
        }
        return nextCreativeId.incrementAndGet();
    }

    /**
     * Возвращает следующий доступный ID для создания группы креативов (max + 1)
     */
    public Long getNextCreativeGroupId(int shard) {
        return UtilRepository
                .getNextId(dslContextProvider.ppc(shard), PERF_CREATIVES, PERF_CREATIVES.CREATIVE_GROUP_ID);
    }

    public Map<Long, String> getCreativesGeo(int shard, Collection<Long> creativeIds) {
        return dslContextProvider.ppc(shard)
                .select(PERF_CREATIVES.CREATIVE_ID, PERF_CREATIVES.SUM_GEO)
                .from(PERF_CREATIVES)
                .where(PERF_CREATIVES.CREATIVE_ID.in(creativeIds))
                .fetchMap(PERF_CREATIVES.CREATIVE_ID, PERF_CREATIVES.SUM_GEO);
    }

    public void updateCreativesGeo(int shard, Long creativeId, List<Long> geo) {
        dslContextProvider.ppc(shard)
                .update(PERF_CREATIVES)
                .set(PERF_CREATIVES.SUM_GEO, CreativeMappings.sumGeoToDb(geo))
                .where(PERF_CREATIVES.CREATIVE_ID.eq(creativeId))
                .execute();
    }

    public void update(int shard, Collection<AppliedChanges<Creative>> changes) {
        JooqUpdateBuilder<PerfCreativesRecord, Creative> ub =
                new JooqUpdateBuilder<>(PERF_CREATIVES.CREATIVE_ID, changes);
        ub.processProperty(Creative.NAME, PERF_CREATIVES.NAME);
        ub.processProperty(Creative.PREVIEW_URL, PERF_CREATIVES.PREVIEW_URL);
        ub.processProperty(Creative.YABS_DATA, PERF_CREATIVES.YABS_DATA, CreativeMappings::yabsDataToDb);
        ub.processProperty(Creative.MODERATION_INFO, PERF_CREATIVES.MODERATE_INFO,
                CreativeMappings::moderationInfoToDb);
        ub.processProperty(Creative.STATUS_MODERATE, PERF_CREATIVES.STATUS_MODERATE,
                TestCreativeRepository::statusModerateToDb);
        ub.processProperty(Creative.LIVE_PREVIEW_URL, PERF_CREATIVES.LIVE_PREVIEW_URL);
        ub.processProperty(Creative.ARCHIVE_URL, PERF_CREATIVES.ARCHIVE_URL);
        ub.processProperty(Creative.ADDITIONAL_DATA, PERF_CREATIVES.ADDITIONAL_DATA,
                CreativeMappings::additionalDataToDb);

        dslContextProvider.ppc(shard).update(PERF_CREATIVES)
                .set(ub.getValues())
                .where(PERF_CREATIVES.CREATIVE_ID.in(ub.getChangedIds()))
                .execute();
    }

    @Nullable
    private static PerfCreativesStatusmoderate statusModerateToDb(StatusModerate statusModerate) {
        if (statusModerate == null) {
            return null;
        }
        switch (statusModerate) {
            case NEW:
                return PerfCreativesStatusmoderate.New;
            case NO:
                return PerfCreativesStatusmoderate.No;
            case SENDING:
                return PerfCreativesStatusmoderate.Sending;
            case SENT:
                return PerfCreativesStatusmoderate.Sent;
            case READY:
                return PerfCreativesStatusmoderate.Ready;
            case ERROR:
                return PerfCreativesStatusmoderate.Error;
            case YES:
                return PerfCreativesStatusmoderate.Yes;
            default:
                throw new IllegalStateException("No such value: " + statusModerate);
        }
    }


}
