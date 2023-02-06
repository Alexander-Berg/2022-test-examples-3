package ru.yandex.direct.core.testing.steps.campaign.repository0;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.core.entity.campaign.converter.CampaignConverter;
import ru.yandex.direct.core.entity.campaign.model.CampaignMetatype;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants;
import ru.yandex.direct.core.entity.minuskeywordspack.MinusKeywordsPackUtils;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.BaseCampaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.context.ContextLimitType;
import ru.yandex.direct.core.testing.steps.campaign.model0.context.ContextSettings;
import ru.yandex.direct.core.testing.steps.campaign.repository0.strategy.StrategyHelper;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsFairauction;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsStatusmetricacontrol;
import ru.yandex.direct.dbschema.ppc.tables.records.CampMetrikaCountersRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.CampOptionsRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.CampaignPermalinksRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.CampaignsRecord;
import ru.yandex.direct.dbutil.QueryWithoutIndex;
import ru.yandex.direct.dbutil.exception.RollbackException;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplierBuilder;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static java.util.stream.Collectors.toList;
import static ru.yandex.direct.common.jooqmapperex.ReaderWriterBuildersEx.integerProperty;
import static ru.yandex.direct.core.entity.campaign.repository.CampaignRepository.createDbStrategyMapper;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_MOBILE_CONTENT;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_PERFORMANCE;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGN_PERMALINKS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_METRIKA_COUNTERS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.SUBCAMPAIGNS;
import static ru.yandex.direct.jooqmapper.ReaderWriterBuilders.convertibleProperty;
import static ru.yandex.direct.jooqmapper.ReaderWriterBuilders.property;
import static ru.yandex.direct.jooqmapper.read.ReaderBuilders.fromField;
import static ru.yandex.direct.jooqmapper.read.ReaderBuilders.fromSet;
import static ru.yandex.direct.jooqmapper.write.WriterBuilders.fromProperties;
import static ru.yandex.direct.jooqmapper.write.WriterBuilders.fromProperty;
import static ru.yandex.direct.jooqmapper.write.WriterBuilders.fromPropertyToField;
import static ru.yandex.direct.utils.FunctionalUtils.filterList;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * 0 в названии пакета обозначает что код тут не протестирован с достаточной для продакшена тщательностью и до
 * той поры может быть использован только в тестах
 */

@ParametersAreNonnullByDefault
public class CampaignRepository {

    static final String ENABLE_CPC_HOLD = "enable_cpc_hold";
    static final String HIDE_PERMALINK_INFO = "hide_permalink_info";
    private final JooqMapperWithSupplier<Campaign> campaignMapper;
    private final JooqMapperWithSupplier<BalanceInfo> balanceInfoMapper;
    private final JooqMapperWithSupplier<DbStrategy> strategyMapper;
    private final JooqMapperWithSupplier<ContextSettings> contextSettingsMapper;
    private final Collection<Field<?>> allCampaignFields;
    private final Collection<Field<?>> baseCampaignFields;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private StrategyHelper strategyHelper;

    public CampaignRepository() {
        campaignMapper = createCampaignMapper();
        balanceInfoMapper = createBalanceInfoMapper();
        strategyMapper = createDbStrategyMapper(false);
        contextSettingsMapper = createContextSettingsMapper();

        // требует проинициализированные bus-ы, поэтому вызывается только после их инициализации
        allCampaignFields = createAllCampaignFields();
        baseCampaignFields = createBaseCampaignFields();
    }

    private static Long contextLimitToDb(Integer limit, ContextLimitType limitType) {
        switch (limitType) {
            case AUTO:
                return 0L;
            case MANUAL:
                return Long.valueOf(limit);
            case UNLIMITED:
                return 255L;
            default:
                throw new IllegalStateException("unsupported context limit type: " + limitType);
        }
    }

    private static Integer contextLimitFromDb(Long limit) {
        switch (limit.intValue()) {
            case 0:
            case 254:
            case 255:
                return null;
            default:
                return limit.intValue();
        }
    }

    public static ContextLimitType contextLimitTypeFromDb(Long limit) {
        switch (limit.intValue()) {
            case 255: {
                return ContextLimitType.UNLIMITED;
            }
            case 0:
            case 254: {
                return ContextLimitType.AUTO;
            }
            default: {
                return ContextLimitType.MANUAL;
            }
        }
    }

    private static Long broadMatchLimitToDb(@Nullable Integer limit) {
        return limit == null ? CampaignConstants.BROAD_MATCH_LIMIT_DEFAULT : limit.longValue();
    }

    /**
     * Генерирует значене поля ppc.campaigns.opts из настроек показа в рекламных сетях.
     * Это поле представлено несколькими разными полями, которые
     * в том числе могут быть во вложенных объектах кампании,
     * поэтому сюда передаются настройки показа целиком.
     * <p>
     * Дополнить по мере необходимости.
     *
     * @param contextSettings настройки показа в рекламных сетях
     * @return значене поля ppc.campaigns.opts для записи в базу
     * @deprecated Удобнее использовать {@link java.util.EnumSet} для описания поля {@code OPTS}.
     * Этот подход применён в {@link ru.yandex.direct.core.entity.campaign.model.Campaign}.
     * <a href="http://stackoverflow.com/questions/33277111/how-to-write-a-jooq-converter-for-an-enumset-mysql-set-type">Вопрос про SET на SO</a>
     */
    @Deprecated
    private static String optsToDb(@Nullable ContextSettings contextSettings) {
        Boolean enableCpcHold = contextSettings != null ? contextSettings.getEnableCpcHold() : null;
        if (enableCpcHold != null && enableCpcHold) {
            return ENABLE_CPC_HOLD;
        } else {
            return "";
        }
    }

    /**
     * Получает список кампаний Campaign по списку переданных id
     *
     * @param shard       шард
     * @param campaignIds список id извлекаемых кампаний
     * @return список кампаний Campaign
     */
    public List<Campaign> getCampaigns(int shard, List<Long> campaignIds) {
        Result<Record> result = dslContextProvider.ppc(shard)
                .select(allCampaignFields)
                .from(CAMPAIGNS)
                .join(CAMP_OPTIONS)
                .on(CAMPAIGNS.CID.eq(CAMP_OPTIONS.CID))
                .leftJoin(CAMPAIGN_PERMALINKS)
                .on(CAMPAIGNS.CID.eq(CAMPAIGN_PERMALINKS.CID))
                .leftJoin(CAMP_METRIKA_COUNTERS)
                .on(CAMPAIGNS.CID.eq(CAMP_METRIKA_COUNTERS.CID))
                .leftJoin(SUBCAMPAIGNS)
                .on(CAMPAIGNS.CID.eq(SUBCAMPAIGNS.CID))
                .where(CAMPAIGNS.CID.in(campaignIds))
                .fetch();

        List<Campaign> campaigns = new ArrayList<>(result.size());
        for (Record record : result) {
            Campaign campaign = campaignMapper.fromDb(record);
            BalanceInfo balanceInfo = balanceInfoMapper.fromDb(record);
            DbStrategy strategy = strategyMapper.fromDb(record);
            ContextSettings contextSettings = contextSettingsMapper.fromDb(record);
            campaign.withBalanceInfo(balanceInfo)
                    .withStrategy(strategyHelper.read(strategy))
                    .withContextSettings(contextSettings);
            campaigns.add(campaign);
        }
        return campaigns;
    }

    /**
     * Получает список неполных кампаний BaseCampaign по списку переданных id
     *
     * @param shard       шард
     * @param campaignIds список id извлекаемых кампаний
     * @return список неполных кампаний BaseCampaign
     */
    public List<BaseCampaign> getBaseCampaigns(int shard, List<Long> campaignIds) {
        Result<Record> result = dslContextProvider.ppc(shard)
                .select(baseCampaignFields)
                .from(CAMPAIGNS)
                .join(CAMP_OPTIONS)
                .on(CAMPAIGNS.CID.eq(CAMP_OPTIONS.CID))
                .where(CAMPAIGNS.CID.in(campaignIds))
                .fetch();

        List<BaseCampaign> baseCampaigns = new ArrayList<>(result.size());
        for (Record record : result) {
            Campaign campaign = campaignMapper.fromDb(record);
            BalanceInfo balanceInfo = balanceInfoMapper.fromDb(record);
            campaign.withBalanceInfo(balanceInfo);
            baseCampaigns.add(campaign);
        }
        return baseCampaigns;
    }

    /**
     * Сохраняет список неполных кампаний BaseCampaign
     * (остальные поля заполняются значениями по умолчанию)
     * <p>
     * В переданных объектах кампаний так же выставляются сгенерированные id
     *
     * @param shard         шард
     * @param clientId      id клиента, к которому будут привязаны кампании
     * @param baseCampaigns список сохраняемых кампаний
     * @return список id сохраненных кампаний
     */
    public List<Long> addBaseCampaigns(int shard, ClientId clientId, List<BaseCampaign> baseCampaigns) {
        List<Campaign> campaigns = mapList(baseCampaigns, camp -> (Campaign) camp);
        return addCampaigns(shard, clientId, campaigns);
    }

    /**
     * Хелпер для выполнения запросов в рамках транзакции, в которой очищены таблицы с кампанией.
     * После выполнения {@code test} кидается {@link RollbackException}
     *
     * @param shard шард
     * @param test  тест, который получит аргументом транзакционный DSL-контекст
     */
    @QueryWithoutIndex("Чистка тестового репа")
    public void runWithEmptyCampaignsTables(int shard, Consumer<DSLContext> test) {
        dslContextProvider.ppcTransaction(shard, configuration -> {
            DSLContext dsl = configuration.dsl();

            dsl.deleteFrom(CAMPAIGNS_MOBILE_CONTENT).execute();
            dsl.deleteFrom(CAMPAIGNS_PERFORMANCE).execute();
            dsl.deleteFrom(CAMPAIGNS).execute();
            dsl.deleteFrom(CAMP_OPTIONS).execute();
            dsl.deleteFrom(CAMP_METRIKA_COUNTERS).execute();

            test.accept(dsl);

            throw new RollbackException();
        });
    }

    /**
     * Сохраняет список кампаний Campaign
     * В переданных объектах кампаний так же выставляются сгенерированные id
     *
     * @param shard     шард
     * @param clientId  id клиента, к которому будут привязаны кампании
     * @param campaigns список сохраняемых кампаний
     * @return список id сохраненных кампаний
     */
    public List<Long> addCampaigns(int shard, ClientId clientId, List<Campaign> campaigns) {
        addCampaigns(dslContextProvider.ppc(shard), clientId, campaigns);
        return mapList(campaigns, Campaign::getId);
    }

    public void addCampaigns(DSLContext dslContext, ClientId clientId, List<Campaign> campaigns) {
        generateCampaignIds(clientId, campaigns);
        addCampaignsToCampaignsTable(dslContext, campaigns);
        addCampaignsToCampOptionsTable(dslContext, campaigns);
        addCampaignsToSubcampaignsTable(dslContext, campaigns);
        addCampaignsToCampaignPermalinksTable(dslContext, campaigns);
        addCampaignsToCampMetrikaCountersTable(dslContext, campaigns);
    }

    /**
     * Генерирует новые id кампаний (cid) и выставляет эти id каждой кампании в переданном списке
     * <p>
     * Каждый генерируемый id кампании привязывается к переданному clientId
     *
     * @param clientId  id клиента, к которому будут привязаны кампании
     * @param campaigns список кампаний, которым нужно сгенерировать новые id (cid)
     */
    private void generateCampaignIds(ClientId clientId, List<Campaign> campaigns) {
        Iterator<Long> campaignIds = shardHelper.generateCampaignIds(clientId.asLong(), campaigns.size()).iterator();
        campaigns.forEach(campaign -> campaign.setId(campaignIds.next()));
    }

    /**
     * Добавляет кампанию в таблицу ppc.campaigns
     */
    private void addCampaignsToCampaignsTable(DSLContext dslContext, List<Campaign> campaigns) {
        if (campaigns.isEmpty()) {
            return;
        }
        InsertHelper<CampaignsRecord> insertHelper
                = new InsertHelper<>(dslContext, CAMPAIGNS);
        for (Campaign campaign : campaigns) {
            insertHelper
                    .add(campaignMapper, campaign)
                    .add(balanceInfoMapper, campaign.getBalanceInfo())
                    .add(strategyMapper, strategyHelper.write(campaign.getStrategy()))
                    .add(contextSettingsMapper, campaign.getContextSettings())
                    .newRecord();
        }
        insertHelper.execute();
    }


    /*
        Конвертация бизнес объектов в записи в таблицах и обратно
     */

    /**
     * Добавляет кампанию в таблицу ppc.camp_options
     */
    private void addCampaignsToCampOptionsTable(DSLContext dslContext, List<Campaign> campaigns) {
        if (campaigns.isEmpty()) {
            return;
        }
        InsertHelper<CampOptionsRecord> insertHelper
                = new InsertHelper<>(dslContext, CAMP_OPTIONS);
        for (Campaign campaign : campaigns) {
            insertHelper
                    .add(campaignMapper, campaign)
                    .add(strategyMapper, strategyHelper.write(campaign.getStrategy()))
                    .newRecord();
        }
        insertHelper.execute();
    }

    /**
     * Добавляет кампанию в таблицу ppc.subcampaigns
     */
    private void addCampaignsToSubcampaignsTable(DSLContext dslContext, List<Campaign> campaigns) {
        if (campaigns.isEmpty()) {
            return;
        }
        var campaignsToInsert = filterList(campaigns, c -> c.getMasterCid() != null);
        new InsertHelper<>(dslContext, SUBCAMPAIGNS)
                .addAll(campaignMapper, campaignsToInsert)
                .executeIfRecordsAdded();
    }

    /**
     * Добавляет кампанию в таблицу ppc.campaign_permalinks
     */
    private void addCampaignsToCampaignPermalinksTable(DSLContext dslContext, List<Campaign> campaigns) {
        if (campaigns.isEmpty()) {
            return;
        }
        InsertHelper<CampaignPermalinksRecord> insertHelper
                = new InsertHelper<>(dslContext, CAMPAIGN_PERMALINKS);
        for (Campaign campaign : campaigns) {
            if (campaign.getDefaultPermalink() != null) {
                insertHelper
                        .add(campaignMapper, campaign)
                        .newRecord();
            }
        }
        insertHelper.executeIfRecordsAdded();
    }

    /**
     * Добавляет кампанию в таблицу ppc.camp_options
     */
    private void addCampaignsToCampMetrikaCountersTable(DSLContext dslContext, List<Campaign> campaigns) {
        if (campaigns.isEmpty()) {
            return;
        }
        InsertHelper<CampMetrikaCountersRecord> insertHelper
                = new InsertHelper<>(dslContext, CAMP_METRIKA_COUNTERS);
        for (Campaign campaign : campaigns) {
            if (campaign.getMetrikaCounters() != null) {
                insertHelper
                        .add(campaignMapper, campaign)
                        .newRecord();
            }
        }
        if (insertHelper.hasAddedRecords()) {
            insertHelper.execute();
        }
    }

    private JooqMapperWithSupplier<Campaign> createCampaignMapper() {
        return JooqMapperWithSupplierBuilder.builder(Campaign::new)

                // ppc.campaigns
                .map(property(Campaign.ID, CAMPAIGNS.CID))
                .map(property(Campaign.CLIENT_ID, CAMPAIGNS.CLIENT_ID))
                .map(property(Campaign.WALLET_ID, CAMPAIGNS.WALLET_CID))
                .map(property(Campaign.UID, CAMPAIGNS.UID))
                .map(property(Campaign.AGENCY_UID, CAMPAIGNS.AGENCY_UID))
                .map(property(Campaign.AGENCY_ID, CAMPAIGNS.AGENCY_ID))
                .map(property(Campaign.MANAGER_UID, CAMPAIGNS.MANAGER_UID))
                .map(property(Campaign.NAME, CAMPAIGNS.NAME))
                .map(convertibleProperty(Campaign.ALLOWED_PAGE_IDS, CAMP_OPTIONS.ALLOWED_PAGE_IDS,
                        ru.yandex.direct.core.entity.campaign.repository.CampaignMappings::pageIdsFromDbNullWhenEmpty,
                        ru.yandex.direct.core.entity.campaign.repository.CampaignMappings::pageIdsToDb))
                .map(convertibleProperty(Campaign.DISALLOWED_PAGE_IDS, CAMP_OPTIONS.DISALLOWED_PAGE_IDS,
                        ru.yandex.direct.core.entity.campaign.repository.CampaignMappings::pageIdsFromDbNullWhenEmpty,
                        ru.yandex.direct.core.entity.campaign.repository.CampaignMappings::pageIdsToDb))
                .map(convertibleProperty(Campaign.TYPE, CAMPAIGNS.TYPE,
                        CampaignType::fromSource,
                        CampaignType::toSource))
                .map(convertibleProperty(Campaign.ARCHIVED, CAMPAIGNS.ARCHIVED,
                        CampaignMappings::archivedFromDb,
                        CampaignMappings::archivedToDb))
                .map(convertibleProperty(Campaign.STATUS_METRICA_CONTROL, CAMP_OPTIONS.STATUS_METRICA_CONTROL,
                        RepositoryUtils::booleanFromYesNo,
                        boolValue -> RepositoryUtils.booleanToYesNo(boolValue, CampOptionsStatusmetricacontrol.class)))
                .map(convertibleProperty(Campaign.STATUS_EMPTY, CAMPAIGNS.STATUS_EMPTY,
                        CampaignMappings::statusEmptyFromDb,
                        CampaignMappings::statusEmptyToDb))
                .map(convertibleProperty(Campaign.STATUS_MODERATE, CAMPAIGNS.STATUS_MODERATE,
                        CampaignMappings::statusModerateFromDb,
                        CampaignMappings::statusModerateToDb))
                .map(convertibleProperty(Campaign.STATUS_SHOW, CAMPAIGNS.STATUS_SHOW,
                        CampaignMappings::statusShowFromDb,
                        CampaignMappings::statusShowToDb))
                .map(convertibleProperty(Campaign.STATUS_ACTIVE, CAMPAIGNS.STATUS_ACTIVE,
                        CampaignMappings::statusActiveFromDb,
                        CampaignMappings::statusActiveToDb))
                .map(convertibleProperty(Campaign.ORDER_ID, CAMPAIGNS.ORDER_ID,
                        CampaignMappings::orderIdFromDb,
                        CampaignMappings::orderIdToDb))
                .map(convertibleProperty(Campaign.STATUS_BS_SYNCED, CAMPAIGNS.STATUS_BS_SYNCED,
                        CampaignMappings::statusBsSyncedFromDb,
                        CampaignMappings::statusBsSyncedToDb))
                .map(convertibleProperty(Campaign.DISABLED_DOMAINS, CAMPAIGNS.DONT_SHOW,
                        CampaignMappings::stringSetFromDb,
                        CampaignMappings::stringCollectionToDb))
                .map(convertibleProperty(Campaign.DISABLED_SSP, CAMPAIGNS.DISABLED_SSP,
                        CampaignMappings::stringListFromJson,
                        CampaignMappings::stringListToJson))
                .map(convertibleProperty(Campaign.DISABLED_VIDEO_PLACEMENTS, CAMPAIGNS.DISABLED_VIDEO_PLACEMENTS,
                        CampaignMappings::stringListFromJson,
                        CampaignMappings::stringListToJson))
                .map(property(Campaign.LAST_CHANGE, CAMPAIGNS.LAST_CHANGE))
                .map(convertibleProperty(Campaign.SOURCE, CAMPAIGNS.SOURCE,
                        CampaignSource::fromSource,
                        CampaignConverter::campaignSourceOrDefaultToDb))
                .map(convertibleProperty(Campaign.METATYPE, CAMPAIGNS.METATYPE,
                        CampaignMetatype::fromSource,
                        CampaignConverter::campaignMetatypeOrDefaultToDb))
                .writeField(CAMPAIGNS.START_TIME, fromPropertyToField(Campaign.START_TIME)
                        .by(RepositoryUtils::zeroableDateToDb))
                .readProperty(Campaign.START_TIME, fromField(CAMPAIGNS.START_TIME))
                .writeField(CAMPAIGNS.FINISH_TIME, fromPropertyToField(Campaign.FINISH_TIME)
                        .by(RepositoryUtils::zeroableDateToDb))
                .readProperty(Campaign.FINISH_TIME, fromField(CAMPAIGNS.FINISH_TIME))
                .writeField(CAMPAIGNS.LAST_SHOW_TIME, fromPropertyToField(Campaign.LAST_SHOW_TIME)
                        .by(RepositoryUtils::zeroableDateTimeToDb))
                .readProperty(Campaign.LAST_SHOW_TIME, fromField(CAMPAIGNS.LAST_SHOW_TIME))
                .readProperty(Campaign.ENABLE_COMPANY_INFO, fromSet(CAMPAIGNS.OPTS, HIDE_PERMALINK_INFO))
                .map(property(Campaign.AUTOBUDGET_FORECAST_DATE, CAMPAIGNS.AUTOBUDGET_FORECAST_DATE))
                // fixme: чем заполнять autobudget_date для Баяна ?
                .writeField(CAMPAIGNS.AUTOBUDGET_DATE,
                        fromProperty(Campaign.ID).by(id -> LocalDate.of(2016, 5, 5)))
                .writeField(CAMPAIGNS.OPTS,
                        fromProperty(Campaign.CONTEXT_SETTINGS).by(CampaignRepository::optsToDb))

                // ppc.camp_options
                .writeField(CAMP_OPTIONS.CID, fromProperty(Campaign.ID))
                .map(convertibleProperty(Campaign.STATUS_POST_MODERATE, CAMP_OPTIONS.STATUS_POST_MODERATE,
                        CampaignMappings::statusPostModerateFromDb,
                        CampaignMappings::statusPostModerateToDb))
                .map(convertibleProperty(Campaign.BROADMATCH_FLAG, CAMP_OPTIONS.BROAD_MATCH_FLAG,
                        CampaignMappings::broadMatchFlagFromDb,
                        CampaignMappings::broadMatchFlagToDb))
                .map(convertibleProperty(Campaign.BROADMATCH_LIMIT, CAMP_OPTIONS.BROAD_MATCH_LIMIT,
                        Long::intValue, CampaignRepository::broadMatchLimitToDb))
                .map(convertibleProperty(Campaign.MINUS_KEYWORDS, CAMP_OPTIONS.MINUS_WORDS,
                        MinusKeywordsPackUtils::minusKeywordsFromJson,
                        MinusKeywordsPackUtils::minusKeywordsToJson))
                .map(property(Campaign.MINUS_KEYWORDS_ID, CAMP_OPTIONS.MW_ID))
                .map(property(Campaign.TIMEZONE_ID, CAMPAIGNS.TIMEZONE_ID))
                .map(convertibleProperty(Campaign.TIME_TARGET, CAMPAIGNS.TIME_TARGET,
                        ru.yandex.direct.core.entity.campaign.repository.CampaignMappings::timeTargetFromDb,
                        ru.yandex.direct.core.entity.campaign.repository.CampaignMappings::timeTargetToDb))
                .map(convertibleProperty(Campaign.EXCLUDE_PAUSED_COMPETING_ADS, CAMP_OPTIONS.FAIR_AUCTION,
                        RepositoryUtils::booleanFromYesNo,
                        boolValue -> RepositoryUtils.booleanToYesNo(boolValue, CampOptionsFairauction.class)))
                .map(convertibleProperty(Campaign.GEO, CAMPAIGNS.GEO,
                        CampaignMappings::geoFromDb,
                        CampaignMappings::geoToDb))
                // fixme: пока этого нет в модели кампании, заполняем пустыми строками
                .writeField(CAMP_OPTIONS.CONTACTINFO,
                        fromProperty(Campaign.ID).by(id -> ""))
                // CID already got from db. this mapping need to write to camp_metrika_counters
                .writeField(CAMP_METRIKA_COUNTERS.CID, fromProperty(Campaign.ID))
                .map(convertibleProperty(Campaign.METRIKA_COUNTERS, CAMP_METRIKA_COUNTERS.METRIKA_COUNTERS,
                        CampaignMappings::metrikaCountersFromDb,
                        CampaignMappings::metrikaCountersToDb))
                .map(property(Campaign.BRAND_SURVEY_ID, CAMP_OPTIONS.BRAND_SURVEY_ID))
                .map(property(Campaign.EMAIL, CAMP_OPTIONS.EMAIL))

                // ppc.campaign_permalinks
                .writeField(CAMPAIGN_PERMALINKS.CID, fromProperty(Campaign.ID))
                .map(convertibleProperty(Campaign.DEFAULT_PERMALINK, CAMPAIGN_PERMALINKS.PERMALINK_ID,
                        RepositoryUtils::zeroToNull, RepositoryUtils::nullToZero))

                // ppc.subcampaigns
                .writeField(SUBCAMPAIGNS.CID, fromProperty(Campaign.ID))
                .map(property(Campaign.MASTER_CID, SUBCAMPAIGNS.MASTER_CID))

                .map(convertibleProperty(Campaign.HAS_ADD_METRIKA_TAG_TO_URL, CAMP_OPTIONS.STATUS_CLICK_TRACK,
                        RepositoryUtils::booleanFromLong, b -> RepositoryUtils.booleanToLong(Boolean.TRUE.equals(b))))

                .build();
    }

    private JooqMapperWithSupplier<BalanceInfo> createBalanceInfoMapper() {
        return JooqMapperWithSupplierBuilder.builder(BalanceInfo::new)
                .map(convertibleProperty(BalanceInfo.CURRENCY, CAMPAIGNS.CURRENCY,
                        CampaignMappings::currencyCodeFromDb,
                        CampaignMappings::currencyCodeToDb))
                .map(convertibleProperty(BalanceInfo.CURRENCY_CONVERTED, CAMPAIGNS.CURRENCY_CONVERTED,
                        ru.yandex.direct.core.entity.campaign.repository.CampaignMappings::currencyConvertedFromDb,
                        ru.yandex.direct.core.entity.campaign.repository.CampaignMappings::currencyConvertedToDb))
                .map(property(BalanceInfo.WALLET_CID, CAMPAIGNS.WALLET_CID))
                .map(property(BalanceInfo.SUM, CAMPAIGNS.SUM))
                .map(property(BalanceInfo.SUM_BALANCE, CAMPAIGNS.SUM_BALANCE))
                .map(property(BalanceInfo.SUM_SPENT, CAMPAIGNS.SUM_SPENT))
                .map(property(BalanceInfo.SUM_LAST, CAMPAIGNS.SUM_LAST))
                .map(property(BalanceInfo.SUM_TO_PAY, CAMPAIGNS.SUM_TO_PAY))
                .map(property(BalanceInfo.SUM_UNITS, CAMPAIGNS.SUM_UNITS))
                .map(property(BalanceInfo.SUM_SPENT_UNITS, CAMPAIGNS.SUM_SPENT_UNITS))
                .map(property(BalanceInfo.BALANCE_TID, CAMPAIGNS.BALANCE_TID))
                .map(convertibleProperty(BalanceInfo.STATUS_NO_PAY, CAMPAIGNS.STATUS_NO_PAY,
                        CampaignMappings::statusNoPayFromDb,
                        CampaignMappings::statusNoPayToDb))
                .map(property(BalanceInfo.PRODUCT_ID, CAMPAIGNS.PRODUCT_ID))
                .map(convertibleProperty(BalanceInfo.PAID_BY_CERTIFICATE, CAMPAIGNS.PAID_BY_CERTIFICATE,
                        CampaignMappings::paidByCertificateFromDb,
                        CampaignMappings::paidByCertificateToDb))
                .build();
    }

    private JooqMapperWithSupplier<ContextSettings> createContextSettingsMapper() {
         /*
            Запись в каждое поле таблицы должна быть объявлена единожды,
            но в campaigns.opts должны записаться значения из нескольких полей
            кампании и ее вложенных объектов. Поэтому, хотя enableCpcHold живет
            в ContextSettings, его запись в таблицу объявлена на уровне кампании.
         */
        return JooqMapperWithSupplierBuilder.builder(ContextSettings::new)
                .readProperty(ContextSettings.LIMIT, fromField(CAMPAIGNS.CONTEXT_LIMIT)
                        .by(CampaignRepository::contextLimitFromDb))
                .readProperty(ContextSettings.LIMIT_TYPE, fromField(CAMPAIGNS.CONTEXT_LIMIT)
                        .by(CampaignRepository::contextLimitTypeFromDb))
                .writeField(CAMPAIGNS.CONTEXT_LIMIT, fromProperties(ContextSettings.LIMIT, ContextSettings.LIMIT_TYPE)
                        .by(CampaignRepository::contextLimitToDb))
                .map(integerProperty(ContextSettings.PRICE_COEFF, CAMPAIGNS.CONTEXT_PRICE_COEF))
                .readProperty(ContextSettings.ENABLE_CPC_HOLD, fromSet(CAMPAIGNS.OPTS, ENABLE_CPC_HOLD))
                .build();
    }

    private Collection<Field<?>> createAllCampaignFields() {
        return StreamEx.of(campaignMapper.getFieldsToRead())
                .append(balanceInfoMapper.getFieldsToRead())
                .append(strategyMapper.getFieldsToRead())
                .append(contextSettingsMapper.getFieldsToRead())
                .toList();
    }

    private Collection<Field<?>> createBaseCampaignFields() {
        return StreamEx
                .<Field<?>>of(CAMPAIGNS.CID, CAMPAIGNS.UID, CAMPAIGNS.NAME,
                        CAMPAIGNS.TYPE, CAMPAIGNS.STATUS_EMPTY, CAMPAIGNS.STATUS_MODERATE,
                        CAMP_OPTIONS.STATUS_POST_MODERATE, CAMPAIGNS.STATUS_SHOW,
                        CAMPAIGNS.STATUS_ACTIVE, CAMPAIGNS.ORDER_ID, CAMPAIGNS.STATUS_BS_SYNCED,
                        CAMPAIGNS.START_TIME, CAMPAIGNS.FINISH_TIME)
                .append(balanceInfoMapper.getFieldsToRead())
                .collect(toList());
    }
}
