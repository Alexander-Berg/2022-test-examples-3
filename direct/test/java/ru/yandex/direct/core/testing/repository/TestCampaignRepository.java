package ru.yandex.direct.core.testing.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.math.RandomUtils;
import org.jooq.InsertSetMoreStep;
import org.jooq.TableField;
import org.jooq.UpdateSetMoreStep;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.campaign.model.CampaignOpts;
import ru.yandex.direct.core.entity.campaign.model.CampaignSource;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.StrategyData;
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampOptionsStatuspostmoderate;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsCurrencyconverted;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsDayBudgetShowMode;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusactive;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusbssynced;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusempty;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusmoderate;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusshow;
import ru.yandex.direct.dbschema.ppc.enums.MetrikaCountersSource;
import ru.yandex.direct.dbschema.ppc.tables.Campaigns;
import ru.yandex.direct.dbschema.ppc.tables.records.CampOptionsRecord;
import ru.yandex.direct.dbschema.ppc.tables.records.CampaignsInternalRecord;
import ru.yandex.direct.dbutil.SqlUtils;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.time.LocalDateTime.now;
import static ru.yandex.direct.common.util.RepositoryUtils.setToDb;
import static ru.yandex.direct.common.util.RepositoryUtils.zeroableDateTimeToDb;
import static ru.yandex.direct.core.entity.campaign.repository.CampaignMappings.currencyCodeToDb;
import static ru.yandex.direct.core.testing.data.TestCampaigns.EMPTY_ORDER_ID;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS_INTERNAL;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_OPTIONS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_SECONDARY_OPTIONS;
import static ru.yandex.direct.dbschema.ppc.tables.MetrikaCounters.METRIKA_COUNTERS;
import static ru.yandex.direct.utils.CommonUtils.nvl;
import static ru.yandex.direct.utils.JsonUtils.toJson;

public class TestCampaignRepository {

    private final DslContextProvider dslContextProvider;

    @Autowired
    public TestCampaignRepository(DslContextProvider dslContextProvider) {
        this.dslContextProvider = dslContextProvider;
    }

    /**
     * Пометить кампанию как пустую
     *
     * @param shard   Шард
     * @param cid     ID кампании
     * @param isEmpty true если кампания пустая
     */
    public void setStatusEmpty(int shard, Long cid, Boolean isEmpty) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_EMPTY, CampaignMappings.statusEmptyToDb(isEmpty))
                .where(CAMPAIGNS.CID.eq(cid))
                .execute();
    }

    public void makeCampaignReadyToDelete(int shard, Long cid) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.SUM, BigDecimal.ZERO)
                .set(CAMPAIGNS.SUM_TO_PAY, BigDecimal.ZERO)
                .set(CAMPAIGNS.SUM_LAST, BigDecimal.ZERO)
                .set(CAMPAIGNS.STATUS_EMPTY, CampaignsStatusempty.Yes)
                .set(CAMPAIGNS.ORDER_ID, EMPTY_ORDER_ID)
                .where(CAMPAIGNS.CID.eq(cid))
                .execute();
    }

    /**
     * Обновить статус модерации для кампаний
     *
     * @param shard          Шард
     * @param campIds        Список ID кампаний
     * @param statusModerate Новый статус
     */
    public void updateStatusModerate(int shard, Collection<Long> campIds,
                                     @NotNull CampaignStatusModerate statusModerate) {
        if (campIds.isEmpty()) {
            return;
        }
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MODERATE, CampaignStatusModerate.toSource(statusModerate))
                .set(CAMPAIGNS.LAST_CHANGE, CAMPAIGNS.LAST_CHANGE)
                .where(CAMPAIGNS.CID.in(campIds))
                .execute();
    }

    /**
     * Обновить статус синхронизации с БК для кампаний
     *
     * @param shard          Шард
     * @param campIds        Список ID кампаний
     * @param statusBsSynced Новый статус
     */
    public void updateStatusBsSynced(int shard, Collection<Long> campIds,
                                     @NotNull CampaignStatusBsSynced statusBsSynced) {
        if (campIds.isEmpty()) {
            return;
        }
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_BS_SYNCED, CampaignStatusBsSynced.toSource(statusBsSynced))
                .where(CAMPAIGNS.CID.in(campIds))
                .execute();
    }

    /**
     * Пометить кампанию как перенесенную в архив
     *
     * @param shard      Шард
     * @param campaignId ID кампании
     */
    public void archiveCampaign(int shard, long campaignId) {
        setStatusArchive(shard, campaignId, CampaignsArchived.Yes);
    }

    /**
     * Пометить кампанию как неперенесенную в архив
     *
     * @param shard      Шард
     * @param campaignId ID кампании
     */
    public void unarchiveCampaign(int shard, long campaignId) {
        setStatusArchive(shard, campaignId, CampaignsArchived.No);
    }

    /**
     * Обновить статус архивности
     *
     * @param shard             Шард
     * @param campaignId        ID кампании
     * @param campaignsArchived Новый статус
     */
    public void setStatusArchive(int shard, long campaignId, CampaignsArchived campaignsArchived) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ARCHIVED, campaignsArchived)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    /**
     * Установить время последнего подсчета прогноза autobudget равным текущему времени
     *
     * @param shard      Шард
     * @param campaignId ID кампании
     */
    public void setAutobudgetForecastDate(int shard, Long campaignId) {
        dslContextProvider.ppc(shard)
                .update(Campaigns.CAMPAIGNS)
                .set(Campaigns.CAMPAIGNS.AUTOBUDGET_FORECAST_DATE, now())
                .where(Campaigns.CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    /**
     * Получить время последнего подсчета прогноза autobudget
     *
     * @param shard      Шард
     * @param campaignId ID кампании
     * @return Время последнего подсчета
     */
    public LocalDateTime getAutobudgetForecastDate(int shard, Long campaignId) {
        return dslContextProvider.ppc(shard)
                .select(Campaigns.CAMPAIGNS.AUTOBUDGET_FORECAST_DATE)
                .from(Campaigns.CAMPAIGNS)
                .where(Campaigns.CAMPAIGNS.CID.eq(campaignId))
                .fetchOne()
                .value1();
    }

    /**
     * Положить деньги на кампанию
     *
     * @param shard      Шард
     * @param campaignId ID кампании
     * @param sum        сумма на счету (в валюте клиента)
     * @param sumSpent   сумма потраченная (в валюте клиента)
     */
    public void setCampaignMoney(long campaignId,
                                 int shard,
                                 @NotNull BigDecimal sum,
                                 @NotNull BigDecimal sumSpent) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.SUM, sum)
                .set(CAMPAIGNS.SUM_SPENT, sumSpent)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    /**
     * Обновить идентификатор заказа в БК для кампании
     *
     * @param shard      Шард
     * @param campaignId id кампании
     * @param orderId    Новый номер заказа
     */
    public void updateCampaignOrderIdByCid(int shard, Long campaignId, Long orderId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.ORDER_ID, orderId)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    /**
     * Создаёт для кампаний внутренней рекламы запись в таблице campaigns_internal с обязательными полями
     * Что кампания существует и у неё тип internal_*, не проверяется.
     *
     * @param shard      шард
     * @param campaignId идентификатор кампании внутренней рекламы, для которой нужно задать параметры
     * @param placeId    идентификатор места внутренней рекламы
     */
    public void setServiceAndPlaceToInternalCampaign(int shard, Long campaignId, Long placeId) {
        InsertSetMoreStep<CampaignsInternalRecord> step = dslContextProvider.ppc(shard)
                .insertInto(CAMPAIGNS_INTERNAL)
                .set(CAMPAIGNS_INTERNAL.CID, campaignId)
                .set(CAMPAIGNS_INTERNAL.PLACE_ID, placeId);
        SqlUtils.onConflictUpdate(step, List.of(CAMPAIGNS_INTERNAL.PLACE_ID))
                .execute();
    }

    public void addCampSecondaryOptions(int shard, Long cid, String key, String options) {
        dslContextProvider.ppc(shard)
                .insertInto(CAMP_SECONDARY_OPTIONS)
                .set(CAMP_SECONDARY_OPTIONS.CID, cid)
                .set(CAMP_SECONDARY_OPTIONS.KEY, key)
                .set(CAMP_SECONDARY_OPTIONS.OPTIONS, options)
                .execute();
    }

    /**
     * Обновить поля rf и rfReset для кампании.
     *
     * @param shard   шард
     * @param cid     id кампании
     * @param rf      rf
     * @param rfReset rfReset
     * @return количество измененных строк в таблице
     */
    public int updateRfAndRfReset(int shard, Long cid, Long rf, Long rfReset) {
        return dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.RF, rf)
                .set(CAMPAIGNS.RF_RESET, rfReset)
                .where(CAMPAIGNS.CID.eq(cid))
                .execute();
    }

    public void updateBrandSurveyIdForCampaignId(int shard, Long campaignId, String brandSurveyId) {
        dslContextProvider.ppc(shard)
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.BRAND_SURVEY_ID, brandSurveyId)
                .where(CAMP_OPTIONS.CID.eq(campaignId))
                .execute();
    }

    public void updateSums(int shard, Long campaignId, BigDecimal sum, BigDecimal sumSpent) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.SUM, sum)
                .set(CAMPAIGNS.SUM_SPENT, sumSpent)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    /**
     * Изменить email кампании из camp_options
     */
    public void updateEmail(int shard, Long campaignId, String email) {
        dslContextProvider.ppc(shard)
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.EMAIL, email)
                .where(CAMP_OPTIONS.CID.eq(campaignId))
                .execute();
    }

    /**
     * Возвращает значение поля для кампании.
     *
     * @param shard      шард
     * @param campaignId id кампании
     * @param field      поле, значение которого нужно найти
     * @param <FieldT>   тип поля
     * @return значение поля
     */
    public <FieldT> FieldT getCampaignFieldValue(int shard, Long campaignId, TableField<?, FieldT> field) {
        List<FieldT> values = dslContextProvider.ppc(shard)
                .select(field)
                .from(CAMPAIGNS)
                .join(CAMP_OPTIONS).on(CAMPAIGNS.CID.eq(CAMP_OPTIONS.CID))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .fetch(field);

        return values.isEmpty() ? null : values.get(0);
    }

    /**
     * Удаляет кампанию из таблицы ppc.campaigns
     *
     * @param shard      шард
     * @param campaignId id удаляемой кампании
     */
    public void deleteCampaign(int shard, Long campaignId) {
        dslContextProvider.ppc(shard)
                .deleteFrom(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    public void makeCampaignFullyModerated(int shard, Long campaignId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MODERATE, CampaignsStatusmoderate.Yes)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();

        dslContextProvider.ppc(shard)
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.STATUS_POST_MODERATE, CampOptionsStatuspostmoderate.Accepted)
                .where(CAMP_OPTIONS.CID.eq(campaignId))
                .execute();
    }

    public void makeCampaignReadyForDelete(int shard, Long campaignId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MODERATE, CampaignsStatusmoderate.No)
                .set(CAMPAIGNS.STATUS_ACTIVE, CampaignsStatusactive.No)
                .set(CAMPAIGNS.STATUS_BS_SYNCED, CampaignsStatusbssynced.No)
                .set(CAMPAIGNS.ARCHIVED, CampaignsArchived.No)
                .set(CAMPAIGNS.CURRENCY_CONVERTED, CampaignsCurrencyconverted.No)
                .set(CAMPAIGNS.SUM, BigDecimal.ZERO)
                .set(CAMPAIGNS.SUM_SPENT, BigDecimal.ZERO)
                .set(CAMPAIGNS.SUM_TO_PAY, BigDecimal.ZERO)
                .set(CAMPAIGNS.SUM_LAST, BigDecimal.ZERO)
                .set(CAMPAIGNS.CLICKS, 0L)
                .set(CAMPAIGNS.SHOWS, 0L)
                .set(CAMPAIGNS.ORDER_ID, 0L)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    public void makeCampaignActive(int shard, Long campaignId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MODERATE, CampaignsStatusmoderate.Yes)
                .set(CAMPAIGNS.STATUS_ACTIVE, CampaignsStatusactive.Yes)
                .set(CAMPAIGNS.STATUS_BS_SYNCED, CampaignsStatusbssynced.Yes)
                .set(CAMPAIGNS.STATUS_SHOW, CampaignsStatusshow.Yes)
                .set(CAMPAIGNS.ORDER_ID, RandomUtils.nextLong())
                .set(CAMPAIGNS.LAST_SHOW_TIME, LocalDateTime.now().minusDays(10L))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();

        dslContextProvider.ppc(shard)
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.STATUS_POST_MODERATE, CampOptionsStatuspostmoderate.Accepted)
                .where(CAMP_OPTIONS.CID.eq(campaignId))
                .execute();
    }

    public void makeCampaignStopped(int shard, Long campaignId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MODERATE, CampaignsStatusmoderate.Yes)
                .set(CAMPAIGNS.STATUS_ACTIVE, CampaignsStatusactive.No)
                .set(CAMPAIGNS.STATUS_BS_SYNCED, CampaignsStatusbssynced.No)
                .set(CAMPAIGNS.STATUS_SHOW, CampaignsStatusshow.No)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    public void makeCampaignStopped(int shard, Long campaignId, LocalDateTime stopTime) {
        makeCampaignStopped(shard, campaignId);
        dslContextProvider.ppc(shard)
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.STOP_TIME, stopTime)
                .where(CAMP_OPTIONS.CID.eq(campaignId))
                .execute();
    }


    public void makeNewCampaignReadyForSendingToBS(int shard, Long campaignId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MODERATE, CampaignsStatusmoderate.Yes)
                .set(CAMPAIGNS.SUM, BigDecimal.valueOf(10000L))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();

        dslContextProvider.ppc(shard)
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.STATUS_POST_MODERATE, CampOptionsStatuspostmoderate.Accepted)
                .where(CAMP_OPTIONS.CID.eq(campaignId))
                .execute();
    }

    /**
     * todo удалить после перехода на новые степы
     */
    public void setOptsToUniversal(int shard, Long campaignId) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.OPTS, setToDb(Set.of(CampaignOpts.IS_UNIVERSAL), CampaignOpts::getTypedValue))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    public void setSource(int shard, Long campaignId, CampaignSource source) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.SOURCE, CampaignSource.toSource(source))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    public void setDayBudget(Integer shard, Long campaignId, BigDecimal sum,
                             @Nullable CampaignsDayBudgetShowMode dayBudgetShowMode, @Nullable Integer changesCount) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.DAY_BUDGET, sum)
                .set(CAMPAIGNS.DAY_BUDGET_SHOW_MODE, nvl(dayBudgetShowMode, CampaignsDayBudgetShowMode.default_))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();

        UpdateSetMoreStep<CampOptionsRecord> step =
                dslContextProvider.ppc(shard)
                        .update(CAMP_OPTIONS)
                        .set(CAMP_OPTIONS.DAY_BUDGET_LAST_CHANGE, now());

        if (changesCount == null) {
            step.set(CAMP_OPTIONS.DAY_BUDGET_DAILY_CHANGE_COUNT, CAMP_OPTIONS.DAY_BUDGET_DAILY_CHANGE_COUNT.plus(1));
        } else {
            step.set(CAMP_OPTIONS.DAY_BUDGET_DAILY_CHANGE_COUNT, (long) changesCount);
        }

        step.where(CAMP_OPTIONS.CID.eq(campaignId))
                .execute();
    }

    public void updateLastChange(int shard, Long campaignId, LocalDateTime lastChange) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.LAST_CHANGE, zeroableDateTimeToDb(lastChange))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    public void updateCreateTime(int shard, Long campaignId, LocalDateTime createTime) {
        dslContextProvider.ppc(shard)
                .update(CAMP_OPTIONS)
                .set(CAMP_OPTIONS.CREATE_TIME, zeroableDateTimeToDb(createTime))
                .where(CAMP_OPTIONS.CID.eq(campaignId))
                .execute();
    }

    public void setCounterSource(int shard, long cid, long counterId, MetrikaCountersSource source) {
        dslContextProvider.ppc(shard)
                .update(METRIKA_COUNTERS)
                .set(METRIKA_COUNTERS.SOURCE, source)
                .where(METRIKA_COUNTERS.CID.eq(cid)
                        .and(METRIKA_COUNTERS.METRIKA_COUNTER.eq(counterId)))
                .execute();
    }

    public void setCurrency(int shard, Long campaignId, CurrencyCode currencyCode) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.CURRENCY, currencyCodeToDb(currencyCode))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    public List<MetrikaCountersSource> getMetrikaCountersSources(int shard, Long campaignId, Long counterId) {
        return List.copyOf(getMetrikaCountersSources(shard, campaignId, List.of(counterId)).values());
    }

    public Map<Long, MetrikaCountersSource> getMetrikaCountersSources(int shard, Long campaignId,
                                                                      List<Long> counterIds) {
        return dslContextProvider.ppc(shard)
                .select(METRIKA_COUNTERS.METRIKA_COUNTER, METRIKA_COUNTERS.SOURCE)
                .from(METRIKA_COUNTERS)
                .where(METRIKA_COUNTERS.CID.eq(campaignId)
                        .and(METRIKA_COUNTERS.METRIKA_COUNTER.in(counterIds)))
                .fetchMap(METRIKA_COUNTERS.METRIKA_COUNTER, METRIKA_COUNTERS.SOURCE);
    }

    /**
     * Обновить поле strategy_data кампании
     *
     * @param shard
     * @param campaignId
     * @param strategyData
     **/
    public void setStrategyData(int shard, Long campaignId, StrategyData strategyData) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STRATEGY_DATA, toJson(strategyData))
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    /**
     * Обновить параметры ограничения показов на кампании
     */
    public void setImpressionRate(int shard, Long campaignId, Long rateCount, Long rateIntervalDays) {
        dslContextProvider.ppc(shard).update(CAMPAIGNS)
                .set(CAMPAIGNS.RF, rateCount)
                .set(CAMPAIGNS.RF_RESET, rateIntervalDays)
                .set(CAMPAIGNS.STATUS_BS_SYNCED, CampaignsStatusbssynced.No)
                .set(CAMPAIGNS.LAST_CHANGE, CAMPAIGNS.LAST_CHANGE)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }

    public void updateStatusShow(int shard, Long campaignId, boolean statusShow) {
        dslContextProvider.ppc(shard)
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_SHOW, statusShow ? CampaignsStatusshow.Yes : CampaignsStatusshow.No)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }
}
