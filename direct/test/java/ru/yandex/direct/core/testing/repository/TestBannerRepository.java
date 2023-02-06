package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jooq.InsertValuesStep4;
import org.jooq.TableField;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusPhoneFlagModerate;
import ru.yandex.direct.core.entity.banner.model.old.StatusSitelinksModerate;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.dbschema.ppc.enums.BannersMinusGeoType;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusarch;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusbssynced;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusshow;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatussitelinksmoderate;
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsType;
import ru.yandex.direct.dbschema.ppc.enums.RedirectCheckQueueObjectType;
import ru.yandex.direct.dbschema.ppc.tables.records.PreModerateBannersRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.RedirectCheckQueueRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.utils.FunctionalUtils;

import static ru.yandex.direct.common.util.RepositoryUtils.booleanToLong;
import static ru.yandex.direct.core.entity.banner.type.internal.BannerWithInternalInfoRepositoryTypeSupport.moderationInfoToDb;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_INTERNAL;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_MINUS_GEO;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_DISPLAY_HREFS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.MOD_REASONS;
import static ru.yandex.direct.dbschema.ppc.Tables.PRE_MODERATE_BANNERS;
import static ru.yandex.direct.dbschema.ppc.tables.RedirectCheckQueue.REDIRECT_CHECK_QUEUE;

public class TestBannerRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    public List<RedirectCheckQueueRecord> getRedirectQueueRecords(int shard, Collection<Long> bannerIds) {
        return dslContextProvider.ppc(shard)
                .select(REDIRECT_CHECK_QUEUE.ID, REDIRECT_CHECK_QUEUE.OBJECT_ID, REDIRECT_CHECK_QUEUE.OBJECT_TYPE,
                        REDIRECT_CHECK_QUEUE.LOGTIME, REDIRECT_CHECK_QUEUE.TRIES)
                .from(REDIRECT_CHECK_QUEUE)
                .where(REDIRECT_CHECK_QUEUE.OBJECT_ID.in(bannerIds))
                .and(REDIRECT_CHECK_QUEUE.OBJECT_TYPE.eq(RedirectCheckQueueObjectType.banner))
                .fetch()
                .into(REDIRECT_CHECK_QUEUE);
    }

    public List<Long> addToRedirectCheckQueue(int shard, List<RedirectCheckQueueRecord> records) {
        InsertValuesStep4<RedirectCheckQueueRecord, Long, RedirectCheckQueueObjectType, Long, LocalDateTime> insert =
                dslContextProvider.ppc(shard)
                        .insertInto(REDIRECT_CHECK_QUEUE,
                                REDIRECT_CHECK_QUEUE.OBJECT_ID,
                                REDIRECT_CHECK_QUEUE.OBJECT_TYPE,
                                REDIRECT_CHECK_QUEUE.TRIES,
                                REDIRECT_CHECK_QUEUE.LOGTIME);

        records.forEach(
                r -> insert.values(r.getObjectId(), RedirectCheckQueueObjectType.banner, r.getTries(), r.getLogtime()));

        return insert.returning(REDIRECT_CHECK_QUEUE.ID)
                .fetch()
                .map(r -> r.getValue(REDIRECT_CHECK_QUEUE.ID));
    }

    /**
     * Обновить ID баннера для БК по его ID в таблице banners
     *
     * @param shard      Шард
     * @param bannerInfo Информация о баннере включая его ID
     * @param bsBannerId Новый ID баннера для БК
     */
    public void updateBannerId(int shard, AbstractBannerInfo<? extends OldBanner> bannerInfo, Long bsBannerId) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.BANNER_ID, bsBannerId)
                .where(BANNERS.BID.eq(bannerInfo.getBannerId()))
                .execute();
    }

    public void addBannerReModerationFlag(int shard, Long bid, Collection<RemoderationType> subObjects) {
        List<TableField<PreModerateBannersRecord, Long>> columns = new ArrayList<>();
        columns.add(PRE_MODERATE_BANNERS.BID);

        List<Long> values = new ArrayList<>();
        values.add(bid);

        for (RemoderationType type : subObjects) {
            columns.add(type.getTableFieldRef());
            values.add(1L);
        }

        dslContextProvider.ppc(shard)
                .insertInto(PRE_MODERATE_BANNERS)
                .columns(columns)
                .values(values)
                .execute();
    }

    public boolean isBannerReModerationFlagPresent(int shard, Long bid) {
        return dslContextProvider.ppc(shard)
                .select(PRE_MODERATE_BANNERS.BID)
                .from(PRE_MODERATE_BANNERS)
                .where(PRE_MODERATE_BANNERS.BID.eq(bid))
                .fetch().isNotEmpty();
    }

    public boolean isBannerReModerationFlagPresentForType(int shard, Long bid,
                                                          TableField<PreModerateBannersRecord, Long> field) {
        return dslContextProvider.ppc(shard)
                .select(field)
                .from(PRE_MODERATE_BANNERS)
                .where(PRE_MODERATE_BANNERS.BID.eq(bid))
                .fetchOne(field).equals(1L);
    }

    /**
     * Обновить статус модерации для визиток
     *
     * @param shard                   Шард
     * @param bannerIds               Список ID баннеров
     * @param statusPhoneFlagModerate Новый статус модерации
     */
    public void updateVcardStatusModerate(int shard, Collection<Long> bannerIds,
                                          StatusPhoneFlagModerate statusPhoneFlagModerate) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.PHONEFLAG, StatusPhoneFlagModerate.toSource(statusPhoneFlagModerate))
                .where(BANNERS.BID.in(bannerIds))
                .execute();
    }

    public void updateSitelinkSetStatusModerate(int shard, Collection<Long> bannerIds,
                                                StatusSitelinksModerate statusSitelinksModerate) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.STATUS_SITELINKS_MODERATE, StatusSitelinksModerate.toSource(statusSitelinksModerate))
                .where(BANNERS.BID.in(bannerIds))
                .execute();
    }

    /**
     * Обновить статус пост-модерации для баннера
     *
     * @param shard              Шард
     * @param bannerId           ID баннера
     * @param statusPostModerate Новый статус пост-модерации
     */
    public void updateStatusPostModerate(int shard, Long bannerId, OldBannerStatusPostModerate statusPostModerate) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.STATUS_POST_MODERATE, OldBannerStatusPostModerate.toSource(statusPostModerate))
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    public void deleteSitelinkSetFromBanner(int shard, Long bannerId) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.SITELINKS_SET_ID, (Long) null)
                .set(BANNERS.STATUS_SITELINKS_MODERATE, BannersStatussitelinksmoderate.New)
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    /**
     * Удалить визитку
     *
     * @param shard    Шард
     * @param bannerId ID баннера
     */
    public void deleteVcardFromBanner(int shard, Long bannerId) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.VCARD_ID, (Long) null)
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    /**
     * Убрать баннеры из таблицы banner_display_hrefs (показ расширенных урлов)
     *
     * @param shard     Шард
     * @param bannerIds Список ID баннеров
     */
    public void deleteDiplayHrefFromBanner(int shard, Collection<Long> bannerIds) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_DISPLAY_HREFS)
                .where(BANNER_DISPLAY_HREFS.BID.in(bannerIds))
                .execute();
    }

    /**
     * Добавить дефолтный минус-гео 977 для баннера.
     *
     * @param shard        Шард
     * @param bannerId     ID баннера
     * @param minusGeoType Тип минус-регионов
     */
    public void addMinusGeo(int shard, Long bannerId, BannersMinusGeoType minusGeoType) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNERS_MINUS_GEO)
                .set(BANNERS_MINUS_GEO.BID, bannerId)
                .set(BANNERS_MINUS_GEO.TYPE, minusGeoType)
                .set(BANNERS_MINUS_GEO.MINUS_GEO, "977")
                .execute();
    }

    /**
     * Добавить минус-гео для баннера.
     *
     * @param shard        Шард
     * @param bannerId     ID баннера
     * @param minusGeoType Тип минус-регионов
     * @param minusGeo     Минус-гео в виде строки из положительных чисел, разделенных запятой
     */
    public void addMinusGeo(int shard, Long bannerId, BannersMinusGeoType minusGeoType, String minusGeo) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNERS_MINUS_GEO)
                .set(BANNERS_MINUS_GEO.BID, bannerId)
                .set(BANNERS_MINUS_GEO.TYPE, minusGeoType)
                .set(BANNERS_MINUS_GEO.MINUS_GEO, minusGeo)
                .execute();
    }

    public void setEmptyFlags(int shard, Long bannerId) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.FLAGS, "")
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    public void updateFlags(int shard, Long bannerId, BannerFlags bannerFlags) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.FLAGS, BannerFlags.toSource(bannerFlags))
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    /**
     * Получить минус-гео для баннера
     *
     * @param shard    Шард
     * @param bannerId ID баннера
     * @return Минус-гео для баннера
     */
    public Long getMinusGeoBannerId(int shard, Long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNERS_MINUS_GEO.BID)
                .from(BANNERS_MINUS_GEO)
                .where(BANNERS_MINUS_GEO.BID.eq(bannerId))
                .fetchOne(BANNERS_MINUS_GEO.BID);
    }

    /**
     * Получить минус-гео для баннера по ID баннера и типу минус-гео
     *
     * @param shard               Шард
     * @param bannerId            ID баннера
     * @param bannersMinusGeoType Тип минус-гео
     * @return Значения минус-гео для баннера
     */
    public Long getMinusGeoBannerId(int shard, Long bannerId, BannersMinusGeoType bannersMinusGeoType) {
        return dslContextProvider.ppc(shard)
                .select(BANNERS_MINUS_GEO.BID)
                .from(BANNERS_MINUS_GEO)
                .where(BANNERS_MINUS_GEO.BID.eq(bannerId))
                .and(BANNERS_MINUS_GEO.TYPE.eq(bannersMinusGeoType))
                .fetchOne(BANNERS_MINUS_GEO.BID);
    }

    /**
     * Получить id баннеров, домены которых в очереди на редиректы
     */
    public List<Long> getBannersIdsWithDomainsInRedirectQueue(int shard, List<OldTextBanner> banners) {
        return dslContextProvider.ppc(shard)
                .select(REDIRECT_CHECK_QUEUE.OBJECT_ID)
                .from(REDIRECT_CHECK_QUEUE)
                .where(REDIRECT_CHECK_QUEUE.OBJECT_ID.in(FunctionalUtils.mapList(banners, OldTextBanner::getId))
                        .and(REDIRECT_CHECK_QUEUE.OBJECT_TYPE.eq(RedirectCheckQueueObjectType.banner)))
                .fetch(REDIRECT_CHECK_QUEUE.OBJECT_ID);
    }

    /**
     * Установить указанный domain_id для заданного баннера.
     *
     * @param shard    шард
     * @param bannerId id обновляемого баннера
     * @param domainId id нового домена
     * @return количество обновленных строк в таблице
     */
    public int setDomainId(int shard, Long bannerId, Long domainId) {
        return dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.DOMAIN_ID, domainId)
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    /**
     * Установить указанный статус архивности для заданного баннера.
     *
     * @param shard         шард
     * @param bannerId      id обновляемого баннера
     * @param statusArchive новый статус архивности баннера
     * @return количество обновленных строк в таблице
     */
    public int updateStatusArchive(int shard, Long bannerId, BannersStatusarch statusArchive) {
        return dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.STATUS_ARCH, statusArchive)
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    /**
     * Установить указанный статус показа для заданного баннера.
     *
     * @param shard      шард
     * @param bannerId   id обновляемого баннера
     * @param statusShow новый статус показа баннера
     */
    public void updateStatusShow(int shard, Long bannerId, BannersStatusshow statusShow) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.STATUS_SHOW, statusShow)
                .where(BANNERS.BID.eq(bannerId))
                .execute();
    }

    public List<String> getModReasons(int shard, long id, ModReasonsType type) {
        return dslContextProvider.ppc(shard)
                .select(MOD_REASONS.REASON)
                .from(MOD_REASONS)
                .where(MOD_REASONS.ID.eq(id).and(
                        MOD_REASONS.TYPE.eq(type)))
                .fetch(MOD_REASONS.REASON);
    }

    public String getMinusGeo(int shard, long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNERS_MINUS_GEO.MINUS_GEO)
                .from(BANNERS_MINUS_GEO)
                .where(BANNERS_MINUS_GEO.BID.eq(bannerId))
                .fetchOne(BANNERS_MINUS_GEO.MINUS_GEO);
    }

    public void setVersion(int shard, long bid, long version) {
        dslContextProvider.ppc(shard)
                .update(BANNER_MODERATION_VERSIONS)
                .set(BANNER_MODERATION_VERSIONS.VERSION, version)
                .where(BANNER_MODERATION_VERSIONS.BID.eq(bid))
                .execute();
    }

    public boolean isBsSyncStatusReset(int shard, long bid) {
        return dslContextProvider.ppc(shard)
                .fetchExists(dslContextProvider.ppc(shard)
                        .selectOne()
                        .from(BANNERS)
                        .where(BANNERS.BID.eq(bid))
                        .and(BANNERS.STATUS_BS_SYNCED.eq(BannersStatusbssynced.No)));
    }

    public void updateInternalBannerModerationInfo(int shard, long bid, InternalModerationInfo moderationInfo) {
        dslContextProvider.ppc(shard)
                .update(BANNERS_INTERNAL)
                .set(BANNERS_INTERNAL.MODERATION_INFO, moderationInfoToDb(moderationInfo))
                .where(BANNERS_INTERNAL.BID.eq(bid))
                .execute();
    }

    public void updateInternalBannerStoppedByUrlMonitoring(int shard, long bid, boolean isStoppedByUrlMonitoring) {
        dslContextProvider.ppc(shard)
                .update(BANNERS_INTERNAL)
                .set(BANNERS_INTERNAL.IS_STOPPED_BY_URL_MONITORING, booleanToLong(isStoppedByUrlMonitoring))
                .where(BANNERS_INTERNAL.BID.eq(bid))
                .execute();
    }

}
