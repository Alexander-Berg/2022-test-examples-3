package ru.yandex.direct.core.testing.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.InsertSetMoreStep;
import org.jooq.InsertSetStep;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Tuple3;
import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithVcard;
import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.ImageType;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.image.model.AvatarHost;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.image.model.BannerImageFormatNamespace;
import ru.yandex.direct.core.entity.image.model.ImageFormat;
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta;
import ru.yandex.direct.core.entity.image.model.ImageSizeMeta;
import ru.yandex.direct.core.entity.image.model.ImageSmartCenter;
import ru.yandex.direct.core.entity.image.repository.BannerImageFormatRepository;
import ru.yandex.direct.core.entity.moderation.repository.ModerationRepository;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.AutoAcceptanceType;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.dbschema.ppc.enums.AdditionsItemCalloutsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.BannersAdditionsAdditionsType;
import ru.yandex.direct.dbschema.ppc.enums.BannersStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.MobileContentStatusiconmoderate;
import ru.yandex.direct.dbschema.ppc.enums.ModEditType;
import ru.yandex.direct.dbschema.ppc.enums.ModObjectVersionObjType;
import ru.yandex.direct.dbschema.ppc.enums.ModReasonsType;
import ru.yandex.direct.dbschema.ppc.enums.ModerateBannerPagesStatusmoderate;
import ru.yandex.direct.dbschema.ppc.tables.records.AdditionsItemCalloutsRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.AutoModerateRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerButtonsRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerDisplayHrefsRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerImagesRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerLogosRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.BannerTurbolandingsRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.BannersPerformanceRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.ModerateBannerPagesRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.PreModerateBannersRecord;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.utils.JsonUtils;

import static ru.yandex.direct.dbschema.ppc.Tables.ADDITIONS_ITEM_CALLOUTS;
import static ru.yandex.direct.dbschema.ppc.Tables.ADDITIONS_ITEM_DISCLAIMERS;
import static ru.yandex.direct.dbschema.ppc.Tables.ADGROUPS_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.AUTO_MODERATE;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_ADDITIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNERS_PERFORMANCE;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_BUTTONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_BUTTONS_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_CREATIVES_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_DISPLAYHREFS_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_DISPLAY_HREFS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_IMAGES_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_LOGOS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_LOGOS_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_SITELINKS_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_TURBOLANDINGS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_TURBO_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.BANNER_VCARDS_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.CALLOUT_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGN_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.MOBILE_CONTENT;
import static ru.yandex.direct.dbschema.ppc.Tables.MOBILE_CONTENT_ICON_MODERATION_VERSIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.MODERATE_BANNER_PAGES;
import static ru.yandex.direct.dbschema.ppc.Tables.MOD_EDIT;
import static ru.yandex.direct.dbschema.ppc.Tables.MOD_OBJECT_VERSION;
import static ru.yandex.direct.dbschema.ppc.Tables.MOD_REASONS;
import static ru.yandex.direct.dbschema.ppc.Tables.POST_MODERATE;
import static ru.yandex.direct.dbschema.ppc.Tables.PRE_MODERATE_BANNERS;
import static ru.yandex.direct.dbschema.ppc.Tables.PROMOACTIONS;

@QueryWithoutIndex("Тестовый репозиторий")
public class TestModerationRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ModerationRepository moderationRepository;

    @Autowired
    BannerTypedRepository bannerTypedRepository;

    @Autowired
    private BannerImageFormatRepository bannerImageFormatRepository;

    public Long getModReasonVcardIdByBannerId(int shard, Long bannerId) {
        return getModReasonIdByBannerId(shard, bannerId, ModReasonsType.contactinfo);
    }

    public Long getModReasonSitelinkSetIdByBannerId(int shard, Long bannerId) {
        return getModReasonIdByBannerId(shard, bannerId, ModReasonsType.sitelinks_set);
    }

    public Long getModReasonImageIdByBannerId(int shard, Long bannerId) {
        return getModReasonIdByBannerId(shard, bannerId, ModReasonsType.image);
    }

    public Long getModReasonDisplayHrefIdByBannerId(int shard, Long bannerId) {
        return getModReasonIdByBannerId(shard, bannerId, ModReasonsType.display_href);
    }

    public Long getModReasonVideoAdditionByBannerId(int shard, Long bannerId) {
        return getModReasonIdByBannerId(shard, bannerId, ModReasonsType.video_addition);
    }

    public Long getModReasonBannerPageByBannerPageId(int shard, Long moderateBannerPageId) {
        return getModReasonIdByBannerId(shard, moderateBannerPageId, ModReasonsType.banner_page);
    }

    public Long getModReasonTurboLandingByBannerId(int shard, Long bannerId) {
        return getModReasonIdByBannerId(shard, bannerId, ModReasonsType.turbolanding);
    }

    public Long getModObjectVersionSitelinkSetIdByBannerId(int shard, Long bannerId) {
        return getModObjectVersionIdByBannerId(shard, bannerId, ModObjectVersionObjType.sitelinks_set);
    }

    public Long getModObjectVersionImageIdByBannerId(int shard, Long bannerId) {
        return getModObjectVersionIdByBannerId(shard, bannerId, ModObjectVersionObjType.image);
    }

    public Long getModObjectVersionDisplayHrefIdByBannerId(int shard, Long bannerId) {
        return getModObjectVersionIdByBannerId(shard, bannerId, ModObjectVersionObjType.display_href);
    }

    public Long getModObjectVersionVideoAdditionByBannerId(int shard, Long bannerId) {
        return getModObjectVersionIdByBannerId(shard, bannerId, ModObjectVersionObjType.video_addition);
    }

    public Long getModObjectVersionTurboLandingByBannerId(int shard, Long bannerId) {
        return getModObjectVersionIdByBannerId(shard, bannerId, ModObjectVersionObjType.turbolanding);
    }

    public Long getPostModerateId(int shard, Long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(POST_MODERATE.BID)
                .from(POST_MODERATE)
                .where(POST_MODERATE.BID.eq(bannerId))
                .fetchOne(POST_MODERATE.BID);
    }

    public Long getAutoModerateId(int shard, Long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(AUTO_MODERATE.BID)
                .from(AUTO_MODERATE)
                .where(AUTO_MODERATE.BID.eq(bannerId))
                .fetchOne(AUTO_MODERATE.BID);
    }

    public Long getBannerModEditId(int shard, Long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(MOD_EDIT.ID)
                .from(MOD_EDIT)
                .where(MOD_EDIT.ID.eq(bannerId))
                .fetchOne(MOD_EDIT.ID);
    }


    public void addPostModerate(int shard, Long bannerId) {
        dslContextProvider.ppc(shard)
                .insertInto(POST_MODERATE)
                .set(POST_MODERATE.BID, bannerId)
                .execute();
    }

    public void addAutoModerate(int shard, Long bannerId) {
        dslContextProvider.ppc(shard)
                .insertInto(AUTO_MODERATE)
                .set(AUTO_MODERATE.BID, bannerId)
                .execute();
    }

    public void addBannerModEdit(int shard, Long bannerId) {
        dslContextProvider.ppc(shard)
                .insertInto(MOD_EDIT)
                .set(MOD_EDIT.ID, bannerId)
                .set(MOD_EDIT.TYPE, ModEditType.banner)
                .set(MOD_EDIT.OLD, "old")
                .set(MOD_EDIT.NEW, "new")
                .execute();
    }

    public void addModReasonVcard(int shard, Long bannerId) {
        addModReason(shard, bannerId, ModReasonsType.contactinfo);
    }

    public void addModReasonSitelinkSet(int shard, Long bannerId) {
        addModReason(shard, bannerId, ModReasonsType.sitelinks_set);
    }

    public void addModReasonImage(int shard, Long bannerId) {
        addModReason(shard, bannerId, ModReasonsType.image);
    }

    public void addModReasonDisplayHref(int shard, Long bannerId) {
        addModReason(shard, bannerId, ModReasonsType.display_href);
    }

    public void addModReasonVideoAddition(int shard, Long bannerId) {
        addModReason(shard, bannerId, ModReasonsType.video_addition);
    }

    public void addModReasonTurboLanding(int shard, Long bannerId) {
        addModReason(shard, bannerId, ModReasonsType.turbolanding);
    }

    public void addModObjectVersionVcard(int shard, Long bannerId) {
        addModObjectVersion(shard, bannerId, ModObjectVersionObjType.contactinfo);
    }

    public void addModObjectVersionSitelinkSet(int shard, Long bannerId) {
        addModObjectVersion(shard, bannerId, ModObjectVersionObjType.sitelinks_set);
    }

    public void addModObjectVersionImage(int shard, Long bannerId) {
        addModObjectVersion(shard, bannerId, ModObjectVersionObjType.image);
    }

    public void addModObjectVersionDisplayHref(int shard, Long bannerId) {
        addModObjectVersion(shard, bannerId, ModObjectVersionObjType.display_href);
    }

    public void addModObjectVersionVideoAddition(int shard, Long bannerId) {
        addModObjectVersion(shard, bannerId, ModObjectVersionObjType.video_addition);
    }

    public void addModObjectVersionTurboLanding(int shard, Long bannerId) {
        addModObjectVersion(shard, bannerId, ModObjectVersionObjType.turbolanding);
    }

    private void addModObjectVersion(int shard, Long bannerId, ModObjectVersionObjType modObjectVersionObjType) {
        dslContextProvider.ppc(shard)
                .insertInto(MOD_OBJECT_VERSION)
                .set(MOD_OBJECT_VERSION.OBJ_ID, bannerId)
                .set(MOD_OBJECT_VERSION.OBJ_TYPE, modObjectVersionObjType)
                .set(MOD_OBJECT_VERSION.EXPORT_VERSION, "1")
                .execute();
    }

    private void addModReason(int shard, Long bannerId, ModReasonsType modReasonsType) {
        dslContextProvider.ppc(shard)
                .insertInto(MOD_REASONS)
                .set(MOD_REASONS.ID, bannerId)
                .set(MOD_REASONS.TYPE, modReasonsType)
                .execute();
    }

    private Long getModObjectVersionIdByBannerId(int shard, Long bannerId,
                                                 ModObjectVersionObjType modObjectVersionObjType) {
        return dslContextProvider.ppc(shard)
                .select(MOD_OBJECT_VERSION.OBJ_ID)
                .from(MOD_OBJECT_VERSION)
                .where(MOD_OBJECT_VERSION.OBJ_ID.eq(bannerId))
                .and(MOD_OBJECT_VERSION.OBJ_TYPE.eq(modObjectVersionObjType))
                .fetchOne(MOD_OBJECT_VERSION.OBJ_ID);
    }

    private Long getModReasonIdByBannerId(int shard, Long bannerId, ModReasonsType modReasonsType) {
        return dslContextProvider.ppc(shard)
                .select(MOD_REASONS.RID)
                .from(MOD_REASONS)
                .where(MOD_REASONS.ID.eq(bannerId))
                .and(MOD_REASONS.TYPE.eq(modReasonsType))
                .fetchOne(MOD_REASONS.RID);
    }

    public void createBannerVersion(Integer shard, long bannerId, long version, LocalDateTime updateTime) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_MODERATION_VERSIONS)
                .set(BANNER_MODERATION_VERSIONS.BID, bannerId)
                .set(BANNER_MODERATION_VERSIONS.VERSION, version)
                .set(BANNER_MODERATION_VERSIONS.CREATE_TIME, updateTime)
                .execute();
    }

    public void createBannerVersion(Integer shard, long bannerId, long version) {
        createBannerVersion(shard, bannerId, version, LocalDateTime.now());
    }

    public void deleteBannerVersion(Integer shard, long bannerId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_MODERATION_VERSIONS)
                .where(BANNER_MODERATION_VERSIONS.BID.eq(bannerId))
                .execute();
    }

    public long getBannerVersion(Integer shard, long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_MODERATION_VERSIONS.VERSION)
                .from(BANNER_MODERATION_VERSIONS)
                .where(BANNER_MODERATION_VERSIONS.BID.eq(bannerId))
                .fetchOne()
                .get(BANNER_MODERATION_VERSIONS.VERSION);
    }

    public ModerationVersion getAdGroupVersionObj(int shard, long adGroupId) {
        return getVersion(ADGROUPS_MODERATION_VERSIONS, ADGROUPS_MODERATION_VERSIONS.PID,
                ADGROUPS_MODERATION_VERSIONS.VERSION, ADGROUPS_MODERATION_VERSIONS.CREATE_TIME, shard, adGroupId);
    }

    public ModerationVersion getCampaignVersionObj(int shard, long campaignId) {
        return getVersion(CAMPAIGN_MODERATION_VERSIONS, CAMPAIGN_MODERATION_VERSIONS.CID,
                CAMPAIGN_MODERATION_VERSIONS.VERSION, CAMPAIGN_MODERATION_VERSIONS.CREATE_TIME, shard, campaignId);
    }

    public ModerationVersion getDisplayHrefVersionObj(int shard, long bannerId) {
        return getVersion(BANNER_DISPLAYHREFS_MODERATION_VERSIONS, BANNER_DISPLAYHREFS_MODERATION_VERSIONS.BID,
                BANNER_DISPLAYHREFS_MODERATION_VERSIONS.VERSION, BANNER_DISPLAYHREFS_MODERATION_VERSIONS.CREATE_TIME,
                shard, bannerId);
    }

    public ModerationVersion getVcardVersionObj(int shard, long bannerId) {
        return getVersion(BANNER_VCARDS_MODERATION_VERSIONS, BANNER_VCARDS_MODERATION_VERSIONS.BID,
                BANNER_VCARDS_MODERATION_VERSIONS.VERSION, BANNER_VCARDS_MODERATION_VERSIONS.CREATE_TIME,
                shard, bannerId);
    }

    public ModerationVersion getBannerCreativeVersionObj(int shard, long bannerId, long creativeId) {
        return getVersion(BANNER_CREATIVES_MODERATION_VERSIONS, BANNER_CREATIVES_MODERATION_VERSIONS.BID,
                BANNER_CREATIVES_MODERATION_VERSIONS.CREATIVE_ID, BANNER_CREATIVES_MODERATION_VERSIONS.VERSION,
                BANNER_CREATIVES_MODERATION_VERSIONS.CREATE_TIME, shard, bannerId, creativeId);
    }

    public ModerationVersion getBannerVersionObj(int shard, long bannerId) {
        return getVersion(BANNER_MODERATION_VERSIONS, BANNER_MODERATION_VERSIONS.BID,
                BANNER_MODERATION_VERSIONS.VERSION, BANNER_MODERATION_VERSIONS.CREATE_TIME, shard, bannerId);
    }

    public ModerationVersion getCalloutVersionObj(int shard, long calloutId) {
        return getVersion(CALLOUT_MODERATION_VERSIONS, CALLOUT_MODERATION_VERSIONS.CALLOUT_ID,
                CALLOUT_MODERATION_VERSIONS.VERSION, CALLOUT_MODERATION_VERSIONS.CREATE_TIME,
                shard, calloutId);
    }

    public ModerationVersion getMobileContentIconVersionObj(int shard, long mobileContentId) {
        return getVersion(MOBILE_CONTENT_ICON_MODERATION_VERSIONS, MOBILE_CONTENT_ICON_MODERATION_VERSIONS.MOBILE_CONTENT_ID,
                MOBILE_CONTENT_ICON_MODERATION_VERSIONS.VERSION, MOBILE_CONTENT_ICON_MODERATION_VERSIONS.CREATE_TIME,
                shard, mobileContentId);
    }

    private <R extends Record> ModerationVersion getVersion(
            Table<R> table,
            TableField<R, Long> idField,
            TableField<R, Long> versionField,
            TableField<R, LocalDateTime> updateTimeField,
            int shard,
            long id) {
        return dslContextProvider.ppc(shard)
                .select(versionField, updateTimeField)
                .from(table)
                .where(idField.eq(id))
                .fetchOne()
                .map(record -> new ModerationVersion(record.get(versionField), record.get(updateTimeField)));
    }

    private <R extends Record> ModerationVersion getVersion(
            Table<R> table,
            TableField<R, Long> idField,
            TableField<R, Long> creativeIdField,
            TableField<R, Long> versionField,
            TableField<R, LocalDateTime> updateTimeField,
            int shard,
            long id,
            long creativeId) {
        return dslContextProvider.ppc(shard)
                .select(versionField, updateTimeField)
                .from(table)
                .where(idField.eq(id))
                .and(creativeIdField.eq(creativeId))
                .fetchOne()
                .map(record -> new ModerationVersion(record.get(versionField), record.get(updateTimeField)));
    }

    private <R extends Record> void createVersion(int shard, long id, long version, LocalDateTime updateTime,
                                                  TableField<R, Long> idField,
                                                  TableField<R, Long> versionField,
                                                  TableField<R, LocalDateTime> updateTimeField) {
        dslContextProvider.ppc(shard)
                .insertInto(idField.getTable())
                .set(idField, id)
                .set(versionField, version)
                .set(updateTimeField, updateTime)
                .execute();
    }

    private <R extends Record> void createVersion(int shard, long id, long creativeId, long version, LocalDateTime updateTime,
                                                  TableField<R, Long> idField,
                                                  TableField<R, Long> creativeIdField,
                                                  TableField<R, Long> versionField,
                                                  TableField<R, LocalDateTime> updateTimeField) {
        dslContextProvider.ppc(shard)
                .insertInto(idField.getTable())
                .set(idField, id)
                .set(creativeIdField, creativeId)
                .set(versionField, version)
                .set(updateTimeField, updateTime)
                .execute();
    }

    public void createAdGroupVersion(int shard, long adGroupId, long version, LocalDateTime updateTime) {
        createVersion(shard, adGroupId, version, updateTime, ADGROUPS_MODERATION_VERSIONS.PID,
                ADGROUPS_MODERATION_VERSIONS.VERSION, ADGROUPS_MODERATION_VERSIONS.CREATE_TIME);
    }

    public void createCampaignVersion(int shard, long campaignId, long version, LocalDateTime updateTime) {
        createVersion(shard, campaignId, version, updateTime, CAMPAIGN_MODERATION_VERSIONS.CID,
                CAMPAIGN_MODERATION_VERSIONS.VERSION, CAMPAIGN_MODERATION_VERSIONS.CREATE_TIME);
    }

    public void createDisplayHrefsVersion(Integer shard, long bannerId, long version) {
        createDisplayHrefsVersion(shard, bannerId, version, LocalDateTime.now());
    }

    public void createDisplayHrefsVersion(Integer shard, long bannerId, long version, LocalDateTime updateTime) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_DISPLAYHREFS_MODERATION_VERSIONS)
                .set(BANNER_DISPLAYHREFS_MODERATION_VERSIONS.BID, bannerId)
                .set(BANNER_DISPLAYHREFS_MODERATION_VERSIONS.VERSION, version)
                .set(BANNER_DISPLAYHREFS_MODERATION_VERSIONS.CREATE_TIME, updateTime)
                .execute();
    }

    public void createBannerCreativeVersion(int shard, Long bannerId, Long creativeId, long version) {
        createBannerCreativeVersion(shard, bannerId, creativeId, version, LocalDateTime.now());
    }

    public void createBannerCreativeVersion(int shard, Long bannerId, Long creativeId, long version,
                                            LocalDateTime updateTime) {
        createVersion(shard, bannerId, creativeId, version, updateTime, BANNER_CREATIVES_MODERATION_VERSIONS.BID,
                BANNER_CREATIVES_MODERATION_VERSIONS.CREATIVE_ID, BANNER_CREATIVES_MODERATION_VERSIONS.VERSION,
                BANNER_CREATIVES_MODERATION_VERSIONS.CREATE_TIME);
    }

    public void createVcardVersion(int shard, Long bannerId, long version) {
        createVcardVersion(shard, bannerId, version, LocalDateTime.now());
    }

    public void createVcardVersion(int shard, Long bannerId, long version, LocalDateTime updateTime) {
        createVersion(shard, bannerId, version, updateTime, BANNER_VCARDS_MODERATION_VERSIONS.BID,
                BANNER_VCARDS_MODERATION_VERSIONS.VERSION, BANNER_VCARDS_MODERATION_VERSIONS.CREATE_TIME);
    }

    public void createBannerCreativeVersion(Integer shard, Long bannerId, Long creativeId, long version) {
        createBannerCreativeVersion(shard, bannerId, creativeId, version, LocalDateTime.now());
    }

    private void createBannerCreativeVersion(Integer shard, Long bannerId, Long creativeId, long version, LocalDateTime updateTime) {
        createVersion(shard, bannerId, creativeId, version, updateTime, BANNER_CREATIVES_MODERATION_VERSIONS.BID,
                BANNER_CREATIVES_MODERATION_VERSIONS.CREATIVE_ID, BANNER_CREATIVES_MODERATION_VERSIONS.VERSION,
                BANNER_CREATIVES_MODERATION_VERSIONS.CREATE_TIME);
    }

    public void createCalloutVersion(int shard, Long calloutId, long version, LocalDateTime updateTime) {
        createVersion(shard, calloutId, version, updateTime, CALLOUT_MODERATION_VERSIONS.CALLOUT_ID,
                CALLOUT_MODERATION_VERSIONS.VERSION, CALLOUT_MODERATION_VERSIONS.CREATE_TIME);
    }

    public void createMobileContentIconVersion(int shard, Long mobileContentId, long version, LocalDateTime updateTime) {
        createVersion(shard, mobileContentId, version, updateTime, MOBILE_CONTENT_ICON_MODERATION_VERSIONS.MOBILE_CONTENT_ID,
                MOBILE_CONTENT_ICON_MODERATION_VERSIONS.VERSION, MOBILE_CONTENT_ICON_MODERATION_VERSIONS.CREATE_TIME);
    }

    public void createBannerImageVersion(Integer shard, long bannerId, long version) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_IMAGES_MODERATION_VERSIONS)
                .set(BANNER_IMAGES_MODERATION_VERSIONS.BID, bannerId)
                .set(BANNER_IMAGES_MODERATION_VERSIONS.VERSION, version)
                .set(BANNER_IMAGES_MODERATION_VERSIONS.CREATE_TIME, LocalDateTime.now())
                .execute();
    }

    public void createBannerButtonsVersion(int shard, Long bannerId, long version) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_BUTTONS_MODERATION_VERSIONS)
                .set(BANNER_BUTTONS_MODERATION_VERSIONS.BID, bannerId)
                .set(BANNER_BUTTONS_MODERATION_VERSIONS.VERSION, version)
                .set(BANNER_BUTTONS_MODERATION_VERSIONS.CREATE_TIME, LocalDateTime.now())
                .execute();
    }

    public void createBannerLogosVersion(int shard, Long bannerId, long version) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_LOGOS_MODERATION_VERSIONS)
                .set(BANNER_LOGOS_MODERATION_VERSIONS.BID, bannerId)
                .set(BANNER_LOGOS_MODERATION_VERSIONS.VERSION, version)
                .set(BANNER_LOGOS_MODERATION_VERSIONS.CREATE_TIME, LocalDateTime.now())
                .execute();
    }

    public void createSitelinksVersion(Integer shard, long bannerId, long version) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_SITELINKS_MODERATION_VERSIONS)
                .set(BANNER_SITELINKS_MODERATION_VERSIONS.BID, bannerId)
                .set(BANNER_SITELINKS_MODERATION_VERSIONS.VERSION, version)
                .set(BANNER_SITELINKS_MODERATION_VERSIONS.CREATE_TIME, LocalDateTime.now())
                .execute();
    }

    public void setPromoExtensionVersion(int shard, long id, long version) {
        dslContextProvider.ppc(shard)
                .update(PROMOACTIONS)
                .set(PROMOACTIONS.MOD_VERSION, version)
                .where(PROMOACTIONS.ID.eq(id))
                .execute();
    }

    public Long getDisplayHrefsVersion(Integer shard, long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_DISPLAYHREFS_MODERATION_VERSIONS.VERSION)
                .from(BANNER_DISPLAYHREFS_MODERATION_VERSIONS)
                .where(BANNER_DISPLAYHREFS_MODERATION_VERSIONS.BID.eq(bannerId))
                .fetchOne()
                .get(BANNER_DISPLAYHREFS_MODERATION_VERSIONS.VERSION);
    }

    public Long getBannerImageVersion(Integer shard, long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_IMAGES_MODERATION_VERSIONS.VERSION)
                .from(BANNER_IMAGES_MODERATION_VERSIONS)
                .where(BANNER_IMAGES_MODERATION_VERSIONS.BID.eq(bannerId))
                .fetchOne()
                .get(BANNER_IMAGES_MODERATION_VERSIONS.VERSION);
    }


    public Long getSitelinksVersion(Integer shard, long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_SITELINKS_MODERATION_VERSIONS.VERSION)
                .from(BANNER_SITELINKS_MODERATION_VERSIONS)
                .where(BANNER_SITELINKS_MODERATION_VERSIONS.BID.eq(bannerId))
                .fetchOne()
                .get(BANNER_SITELINKS_MODERATION_VERSIONS.VERSION);
    }


    public void createTurbolandingVersion(Integer shard, long bannerId, long version) {
        dslContextProvider.ppc(shard)
                .insertInto(BANNER_TURBO_MODERATION_VERSIONS)
                .set(BANNER_TURBO_MODERATION_VERSIONS.BID, bannerId)
                .set(BANNER_TURBO_MODERATION_VERSIONS.VERSION, version)
                .set(BANNER_TURBO_MODERATION_VERSIONS.CREATE_TIME, LocalDateTime.now())
                .execute();
    }

    public Long getTurbolandingVersion(Integer shard, long bannerId) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_TURBO_MODERATION_VERSIONS.VERSION)
                .from(BANNER_TURBO_MODERATION_VERSIONS)
                .where(BANNER_TURBO_MODERATION_VERSIONS.BID.eq(bannerId))
                .fetchOne()
                .get(BANNER_TURBO_MODERATION_VERSIONS.VERSION);
    }

    public Long getPromoExtensionVersion(int shard, long promoExtensionId) {
        return dslContextProvider.ppc(shard)
                .select(PROMOACTIONS.MOD_VERSION)
                .from(PROMOACTIONS)
                .where(PROMOACTIONS.ID.eq(promoExtensionId))
                .fetchOne(PROMOACTIONS.MOD_VERSION);
    }

    public List<Long> getModerateBannerPages(int shard, long bid) {
        return dslContextProvider.ppc(shard)
                .select(MODERATE_BANNER_PAGES.PAGE_ID)
                .from(MODERATE_BANNER_PAGES)
                .where(MODERATE_BANNER_PAGES.BID.eq(bid))
                .and(MODERATE_BANNER_PAGES.IS_REMOVED.eq(RepositoryUtils.FALSE))
                .fetch(Record1::value1);
    }

    public Map<Long, Long> getModerateBannerPagesToIsRemoved(int shard, long bid) {
        return dslContextProvider.ppc(shard)
                .select(MODERATE_BANNER_PAGES.PAGE_ID, MODERATE_BANNER_PAGES.IS_REMOVED)
                .from(MODERATE_BANNER_PAGES)
                .where(MODERATE_BANNER_PAGES.BID.eq(bid))
                .fetchMap(MODERATE_BANNER_PAGES.PAGE_ID, MODERATE_BANNER_PAGES.IS_REMOVED);
    }

    public BannersStatusmoderate getStatusModerate(int shard, long bid) {
        return dslContextProvider.ppc(shard)
                .select(BANNERS.STATUS_MODERATE)
                .from(BANNERS)
                .where(BANNERS.BID.eq(bid))
                .fetchOne()
                .get(BANNERS.STATUS_MODERATE);
    }

    public void createReModerationRecord(int shard, long bid, Set<RemoderationType> flags) {
        var insertStep = dslContextProvider.ppc(shard).insertInto(PRE_MODERATE_BANNERS);

        InsertSetMoreStep<PreModerateBannersRecord> insertSetMoreStep = insertStep.set(PRE_MODERATE_BANNERS.BID, bid);
        for (var type : RemoderationType.values()) {
            insertSetMoreStep = insertStep.set(type.getTableFieldRef(), flags.contains(type) ? 1L : 0L);
        }

        insertSetMoreStep.execute();
    }

    public Set<RemoderationType> getReModerationRecord(int shard, long bid) {
        List<TableField<PreModerateBannersRecord, Long>> fields =
                Stream.of(RemoderationType.values()).map(RemoderationType::getTableFieldRef).collect(Collectors.toList());

        Record record = dslContextProvider.ppc(shard)
                .select(fields).from(PRE_MODERATE_BANNERS)
                .where(PRE_MODERATE_BANNERS.BID.eq(bid))
                .fetchOne();

        if (record == null) {
            return null;
        }

        return Stream.of(RemoderationType.values())
                .filter(e -> record.get(e.getTableFieldRef()) > 0)
                .collect(Collectors.toSet());
    }

    public Set<AutoAcceptanceType> getAutoAcceptanceRecord(int shard, long bid) {
        List<TableField<AutoModerateRecord, Long>> fields =
                Stream.of(AutoAcceptanceType.values()).map(AutoAcceptanceType::getTableFieldRef).collect(Collectors.toList());

        Record record = dslContextProvider.ppc(shard)
                .select(fields).from(AUTO_MODERATE)
                .where(AUTO_MODERATE.BID.eq(bid))
                .fetchOne();

        if (record == null) {
            return null;
        }

        return Stream.of(AutoAcceptanceType.values())
                .filter(e -> record.get(e.getTableFieldRef()) > 0)
                .collect(Collectors.toSet());
    }

    public void createAutoAcceptRecord(int shard, long bid, Set<AutoAcceptanceType> flags) {
        InsertSetStep<AutoModerateRecord> insertStep = dslContextProvider.ppc(shard).insertInto(AUTO_MODERATE);

        InsertSetMoreStep<AutoModerateRecord> insertSetMoreStep = insertStep.set(AUTO_MODERATE.BID, bid);
        for (var type : AutoAcceptanceType.values()) {
            insertSetMoreStep = insertStep.set(type.getTableFieldRef(), flags.contains(type) ? 1L : 0L);
        }

        insertSetMoreStep.execute();
    }

    public void setCampaignStatusModerate(int shard, long cid, CampaignStatusModerate statusModerate) {
        dslContextProvider.ppc(shard).update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MODERATE, CampaignStatusModerate.toSource(statusModerate))
                .where(CAMPAIGNS.CID.eq(cid))
                .execute();
    }

    public CampaignStatusModerate getCampaignStatusModerate(int shard, long cid) {
        Record rec = dslContextProvider.ppc(shard)
                .select(CAMPAIGNS.STATUS_MODERATE)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(cid))
                .fetchOne();

        if (rec == null) {
            return null;
        }

        return CampaignStatusModerate.fromSource(rec.get(CAMPAIGNS.STATUS_MODERATE));
    }

    public void insertModerateBannerPages(Integer shard, List<ModerateBannerPagesRecord> recs) {
        insertModerateBannerPages(dslContextProvider.ppc(shard).configuration(), recs);
    }

    private void insertModerateBannerPages(Configuration configuration, List<ModerateBannerPagesRecord> recs) {
        if (recs.isEmpty()) {
            return;
        }

        DSLContext dsl = DSL.using(configuration);

        for (ModerateBannerPagesRecord rec : recs) {
            dsl.insertInto(MODERATE_BANNER_PAGES).set(rec).onDuplicateKeyIgnore().execute();
        }
    }

    public ModerateBannerPagesStatusmoderate getStatusModerate(int shard,
                                                               Tuple3<Long, Long, Long> bidAndPageAndVersion) {
        return dslContextProvider.ppc(shard)
                .select(MODERATE_BANNER_PAGES.STATUS_MODERATE)
                .from(MODERATE_BANNER_PAGES)
                .where(MODERATE_BANNER_PAGES.BID.eq(bidAndPageAndVersion.get1()).and(
                        MODERATE_BANNER_PAGES.PAGE_ID.eq(bidAndPageAndVersion.get2()).and(
                                MODERATE_BANNER_PAGES.VERSION.eq(bidAndPageAndVersion.get3()))))
                .and(MODERATE_BANNER_PAGES.IS_REMOVED.eq(RepositoryUtils.FALSE))
                .fetchOne()
                .get(MODERATE_BANNER_PAGES.STATUS_MODERATE);
    }

    public List<BannerTurbolandingsRecord> getBannerTurbolandings(int shard, Collection<Long> bids) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_TURBOLANDINGS.BID, BANNER_TURBOLANDINGS.STATUS_MODERATE)
                .from(BANNER_TURBOLANDINGS)
                .where(BANNER_TURBOLANDINGS.BID.in(bids))
                .fetch(e -> new BannerTurbolandingsRecord()
                        .with(BANNER_TURBOLANDINGS.BID, e.get(BANNER_TURBOLANDINGS.BID))
                        .with(BANNER_TURBOLANDINGS.STATUS_MODERATE, e.get(BANNER_TURBOLANDINGS.STATUS_MODERATE))
                )
                ;
    }

    public void setBannerStatusModerate(int shard, long bid, BannerStatusModerate statusModerate) {
        dslContextProvider.ppc(shard).update(BANNERS)
                .set(BANNERS.STATUS_MODERATE, BannerStatusModerate.toSource(statusModerate))
                .where(BANNERS.BID.eq(bid))
                .execute();
    }

    public void setBannerTurbolandingStatusModerate(int shard, long bid,
                                                    BannerTurboLandingStatusModerate statusModerate) {
        dslContextProvider.ppc(shard).update(BANNER_TURBOLANDINGS)
                .set(BANNER_TURBOLANDINGS.STATUS_MODERATE, BannerTurboLandingStatusModerate.toSource(statusModerate))
                .where(BANNER_TURBOLANDINGS.BID.eq(bid))
                .execute();
    }

    public List<BannerDisplayHrefsRecord> getBannerDisplayHrefs(int shard, Collection<Long> bids) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_DISPLAY_HREFS.BID, BANNER_DISPLAY_HREFS.STATUS_MODERATE,
                        BANNER_DISPLAY_HREFS.DISPLAY_HREF)
                .from(BANNER_DISPLAY_HREFS)
                .where(BANNER_DISPLAY_HREFS.BID.in(bids))
                .fetch(e -> new BannerDisplayHrefsRecord().with(BANNER_DISPLAY_HREFS.BID,
                        e.get(BANNER_DISPLAY_HREFS.BID))
                        .with(BANNER_DISPLAY_HREFS.DISPLAY_HREF, e.get(BANNER_DISPLAY_HREFS.DISPLAY_HREF))
                        .with(BANNER_DISPLAY_HREFS.STATUS_MODERATE, e.get(BANNER_DISPLAY_HREFS.STATUS_MODERATE))
                )
                ;
    }

    public void setCalloutStatusModerate(int shard, Long id, AdditionsItemCalloutsStatusmoderate statusModerate) {
        dslContextProvider.ppc(shard)
                .update(ADDITIONS_ITEM_CALLOUTS)
                .set(ADDITIONS_ITEM_CALLOUTS.STATUS_MODERATE, statusModerate)
                .where(ADDITIONS_ITEM_CALLOUTS.ADDITIONS_ITEM_ID.eq(id))
                .execute();
    }

    public List<AdditionsItemCalloutsRecord> getCallouts(int shard, Collection<Long> ids) {
        return dslContextProvider.ppc(shard)
                .select(ADDITIONS_ITEM_CALLOUTS.ADDITIONS_ITEM_ID, ADDITIONS_ITEM_CALLOUTS.STATUS_MODERATE)
                .from(ADDITIONS_ITEM_CALLOUTS)
                .where(ADDITIONS_ITEM_CALLOUTS.ADDITIONS_ITEM_ID.in(ids))
                .fetch(e -> new AdditionsItemCalloutsRecord()
                        .with(ADDITIONS_ITEM_CALLOUTS.ADDITIONS_ITEM_ID, e.get(ADDITIONS_ITEM_CALLOUTS.ADDITIONS_ITEM_ID))
                        .with(ADDITIONS_ITEM_CALLOUTS.STATUS_MODERATE, e.get(ADDITIONS_ITEM_CALLOUTS.STATUS_MODERATE))
                );
    }

    public BannerImageFormat addBannerImageFormat(int shard, String imageHash, ImageSize size) {
        BannerImageFormat bannerImageFormat = new BannerImageFormat()
                .withImageHash(imageHash)
                .withFormats(Map.of("orig", new ImageFormat().withHeight(100).withWidth(640)))
                .withMdsMeta(JsonUtils.toJson(createDefaultMdsMeta()))
                .withImageType(ImageType.IMAGE_AD)
                .withAvatarsHost(AvatarHost.AVATARS_MDST_YANDEX_NET)
                .withMdsGroupId(1)
                .withNamespace(BannerImageFormatNamespace.DIRECT_PICTURE)
                .withSize(size);

        bannerImageFormatRepository
                .addBannerImageFormat(shard, Collections.singletonList(bannerImageFormat));
        return bannerImageFormat;
    }

    private ImageMdsMeta createDefaultMdsMeta() {
        ImageSmartCenter smartCenter = new ImageSmartCenter()
                .withHeight(1)
                .withWidth(1)
                .withX(0)
                .withY(0);
        ImageSizeMeta sizeMeta = new ImageSizeMeta()
                .withHeight(10)
                .withWidth(10)
                .withPath("path")
                .withSmartCenters(ImmutableMap.of("1:1", smartCenter));
        return new ImageMdsMeta()
                .withSizes(ImmutableMap.of(
                        "x150", sizeMeta,
                        "x300", sizeMeta));
    }

    private static long additionId = 14500000;

    public void createBannerDisclaimer(int shard, long bid, String text) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNERS_ADDITIONS)
                .where(BANNERS_ADDITIONS.BID.eq(bid)).execute();

        dslContextProvider.ppc(shard)
                .insertInto(BANNERS_ADDITIONS)
                .columns(BANNERS_ADDITIONS.BID, BANNERS_ADDITIONS.ADDITIONS_ITEM_ID, BANNERS_ADDITIONS.ADDITIONS_TYPE,
                        BANNERS_ADDITIONS.SEQUENCE_NUM)
                .values(bid, additionId++, BannersAdditionsAdditionsType.disclaimer, 1L)
                .execute();
    }

    public void removeDisclaimers(int shard) {
        var context = dslContextProvider.ppc(shard);
        context.deleteFrom(BANNERS_ADDITIONS).execute();
        context.deleteFrom(ADDITIONS_ITEM_DISCLAIMERS).execute();
    }

    public List<BannerImagesRecord> getBannerImages(int shard, Collection<Long> bids) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_IMAGES.BID, BANNER_IMAGES.STATUS_MODERATE)
                .from(BANNER_IMAGES)
                .where(BANNER_IMAGES.BID.in(bids))
                .fetch(e -> new BannerImagesRecord().with(BANNER_IMAGES.BID,
                        e.get(BANNER_IMAGES.BID))
                        .with(BANNER_IMAGES.STATUS_MODERATE, e.get(BANNER_IMAGES.STATUS_MODERATE))
                )
                ;
    }

    public List<BannerButtonsRecord> getBannerButtons(int shard, Set<Long> bids) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_BUTTONS.BID, BANNER_BUTTONS.STATUS_MODERATE)
                .from(BANNER_BUTTONS)
                .where(BANNER_BUTTONS.BID.in(bids))
                .fetch(e -> new BannerButtonsRecord().with(BANNER_BUTTONS.BID,
                        e.get(BANNER_BUTTONS.BID))
                        .with(BANNER_BUTTONS.STATUS_MODERATE, e.get(BANNER_BUTTONS.STATUS_MODERATE))
                );
    }

    public List<BannerWithVcard> getBanners(int shard, Set<Long> bids) {
        return bannerTypedRepository.getStrictly(dslContextProvider.ppc(shard), bids, BannerWithVcard.class);
    }

    public List<BannerLogosRecord> getBannerLogos(int shard, Set<Long> bids) {
        return dslContextProvider.ppc(shard)
                .select(BANNER_LOGOS.BID, BANNER_LOGOS.STATUS_MODERATE)
                .from(BANNER_LOGOS)
                .where(BANNER_LOGOS.BID.in(bids))
                .fetch(e -> new BannerLogosRecord().with(BANNER_LOGOS.BID,
                        e.get(BANNER_LOGOS.BID))
                        .with(BANNER_LOGOS.STATUS_MODERATE, e.get(BANNER_LOGOS.STATUS_MODERATE))
                );
    }

    public List<BannersPerformanceRecord> getBannersPerformance(int shard, Set<Long> bids) {
        return dslContextProvider.ppc(shard)
                .select(BANNERS_PERFORMANCE.BID, BANNERS_PERFORMANCE.CREATIVE_ID, BANNERS_PERFORMANCE.STATUS_MODERATE)
                .from(BANNERS_PERFORMANCE)
                .where(BANNERS_PERFORMANCE.BID.in(bids))
                .fetch(e -> new BannersPerformanceRecord().with(BANNERS_PERFORMANCE.BID,
                        e.get(BANNERS_PERFORMANCE.BID))
                        .with(BANNERS_PERFORMANCE.STATUS_MODERATE, e.get(BANNERS_PERFORMANCE.STATUS_MODERATE))
                        .with(BANNERS_PERFORMANCE.CREATIVE_ID, e.get(BANNERS_PERFORMANCE.CREATIVE_ID))
                );
    }

    public MobileContentStatusiconmoderate getMobileContentIconStatusModerate(int shard, Long mobileContentId) {
        return dslContextProvider.ppc(shard)
                .select(MOBILE_CONTENT.STATUS_ICON_MODERATE)
                .from(MOBILE_CONTENT)
                .where(MOBILE_CONTENT.MOBILE_CONTENT_ID.eq(mobileContentId))
                .fetchOne(MOBILE_CONTENT.STATUS_ICON_MODERATE);
    }

    public void deleteSitelinksVersion(int shard, long defaultObjectId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_SITELINKS_MODERATION_VERSIONS)
                .where(BANNER_SITELINKS_MODERATION_VERSIONS.BID.eq(defaultObjectId))
                .execute();
    }

    public void deleteImageVersion(int shard, long defaultObjectId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_IMAGES_MODERATION_VERSIONS)
                .where(BANNER_IMAGES_MODERATION_VERSIONS.BID.eq(defaultObjectId))
                .execute();
    }

    public void deleteTurbolandingsVersion(int shard, long defaultObjectId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_TURBO_MODERATION_VERSIONS)
                .where(BANNER_TURBO_MODERATION_VERSIONS.BID.eq(defaultObjectId))
                .execute();
    }

    public void deleteBannerLogoVersion(int shard, long defaultObjectId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_LOGOS_MODERATION_VERSIONS)
                .where(BANNER_LOGOS_MODERATION_VERSIONS.BID.eq(defaultObjectId))
                .execute();
    }

    public void deleteBannerButtonVersion(int shard, long defaultObjectId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_BUTTONS_MODERATION_VERSIONS)
                .where(BANNER_BUTTONS_MODERATION_VERSIONS.BID.eq(defaultObjectId))
                .execute();
    }


    public void deleteBannerCreativeVersion(int shard, long defaultObjectId, long creativeId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_CREATIVES_MODERATION_VERSIONS)
                .where(
                        BANNER_CREATIVES_MODERATION_VERSIONS.BID.eq(defaultObjectId),
                        BANNER_CREATIVES_MODERATION_VERSIONS.CREATIVE_ID.eq(creativeId)
                )
                .execute();
    }

    public void deleteVcardsVersion(int shard, long defaultObjectId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_VCARDS_MODERATION_VERSIONS)
                .where(BANNER_VCARDS_MODERATION_VERSIONS.BID.eq(defaultObjectId))
                .execute();
    }

    public void deleteDisplayhrefsVersion(int shard, Long id) {
        dslContextProvider.ppc(shard)
                .deleteFrom(BANNER_DISPLAYHREFS_MODERATION_VERSIONS)
                .where(BANNER_DISPLAYHREFS_MODERATION_VERSIONS.BID.eq(id))
                .execute();
    }

    public void deleteCalloutVersion(int shard, Long id) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CALLOUT_MODERATION_VERSIONS)
                .where(CALLOUT_MODERATION_VERSIONS.CALLOUT_ID.eq(id))
                .execute();
    }

    public void deleteBsBannerIds(int shard, Collection<Long> bids) {
        dslContextProvider.ppc(shard)
                .update(BANNERS)
                .set(BANNERS.BANNER_ID, 0L)
                .where(BANNERS.BID.in(bids))
                .execute();
    }

    public void deleteMobileContentIconVersion(int shard, Long mobileContentId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(MOBILE_CONTENT_ICON_MODERATION_VERSIONS)
                .where(MOBILE_CONTENT_ICON_MODERATION_VERSIONS.MOBILE_CONTENT_ID.eq(mobileContentId))
                .execute();
    }

    public static class ModerationVersion {
        private Long version;
        private LocalDateTime time;

        public ModerationVersion(Long version, LocalDateTime time) {
            this.version = version;
            this.time = time;
        }

        public Long getVersion() {
            return version;
        }

        public LocalDateTime getTime() {
            return time;
        }
    }
}
