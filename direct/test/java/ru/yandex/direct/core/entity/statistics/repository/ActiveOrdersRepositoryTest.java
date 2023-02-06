package ru.yandex.direct.core.entity.statistics.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.statistics.container.ProceededActiveOrder;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.BalanceInfo;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusactive;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusbssynced;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static java.lang.Math.abs;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_AUTOBUDGET;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_DISTRIB;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_FREE;
import static ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeBalanceInfo;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalAutobudgetCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalDistribCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeInternalFreeCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.emptyBalanceInfo;
import static ru.yandex.direct.dbschema.ppc.tables.Campaigns.CAMPAIGNS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ActiveOrdersRepositoryTest {
    private static final BigDecimal SCALED_ZERO = BigDecimal.valueOf(0, 6);
    private static final LocalDateTime DEFAULT_LAST_SHOW_TIME = LocalDateTime.of(2010, 1, 1, 1, 1);

    @Autowired
    private Steps steps;

    @Autowired
    private ActiveOrdersRepository activeOrdersRepository;

    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * Тест проверяет, что статус активности проставляется после обновления поля sum_spent для завершенных кампаний
     */
    @Test
    public void updateCampaignsStatusActiveUpdatesAfterSumSpent() {
        var campaignInfo = createCampaign(TEXT, emptyBalanceInfo(CurrencyCode.RUB)
                                .withSum(BigDecimal.valueOf(10))
                                .withSumSpent(BigDecimal.valueOf(9)));

        var activeOrderProceeded = new ProceededActiveOrder(campaignInfo.getCampaignId(), TEXT,
                2, 1, BigDecimal.valueOf(10), 0, 0);

        activeOrderProceeded.setFinished(true);
        var activeOrdersProceeded = List.of(activeOrderProceeded);

        int updatedCamps = activeOrdersRepository.updateCampaigns(campaignInfo.getShard(), activeOrdersProceeded);

        var actual = getActiveOrdersUpdateResult(campaignInfo.getShard(),
                campaignInfo.getCampaignId());

        var expected = new ActiveOrdersUpdateResult(campaignInfo.getCampaignId(), 2, 1,
                BigDecimal.valueOf(10_000_000, 6), BigDecimal.valueOf(10_000_000, 6),
                0, 2, CampaignsStatusbssynced.Yes,
                DEFAULT_LAST_SHOW_TIME, CampaignsStatusactive.No);

        assertResult(updatedCamps, actual, expected);
    }

    /**
     * Тест проверяет, что если сумма на кампании стала меньше, то statusBsSynced станет No
     */
    @Test
    public void updateCampaignsChangeStatusBsSynced() {
        var campaignInfo = createCampaign(TEXT, activeBalanceInfo(CurrencyCode.RUB));

        var activeOrderProceeded = new ProceededActiveOrder(campaignInfo.getCampaignId(), TEXT,
                2, 1, BigDecimal.valueOf(1000), 0, 0);

        activeOrderProceeded.setRollbacked(true);
        var activeOrdersProceeded = List.of(activeOrderProceeded);

        int updatedCamps = activeOrdersRepository.updateCampaigns(campaignInfo.getShard(), activeOrdersProceeded);

        var actual = getActiveOrdersUpdateResult(campaignInfo.getShard(),
                campaignInfo.getCampaignId());

        var expected = new ActiveOrdersUpdateResult(
                campaignInfo.getCampaignId(), 2, 1,
                BigDecimal.valueOf(100_000_000_000L, 6), BigDecimal.valueOf(1000_000_000, 6),
                0, 2, CampaignsStatusbssynced.No,
                DEFAULT_LAST_SHOW_TIME, CampaignsStatusactive.Yes);

        assertResult(updatedCamps, actual, expected);
    }

    /**
     * Только открутка (rollback) кампании приводит к изменению статуса statusBsSynced
     */
    @Test
    public void updateCampaignsNotChangedStatusBsSynced() {
        var campaignInfo = createCampaign(TEXT, activeBalanceInfo(CurrencyCode.RUB));

        var activeOrderProceeded = new ProceededActiveOrder(campaignInfo.getCampaignId(), TEXT,
                2, 1, BigDecimal.valueOf(11_000), 0, 0);
        var activeOrdersProceeded = List.of(activeOrderProceeded);

        int updatedCamps = activeOrdersRepository.updateCampaigns(campaignInfo.getShard(), activeOrdersProceeded);

        var actual = getActiveOrdersUpdateResult(campaignInfo.getShard(),
                campaignInfo.getCampaignId());

        var expected = new ActiveOrdersUpdateResult(
                campaignInfo.getCampaignId(), 2, 1,
                BigDecimal.valueOf(100_000_000_000L, 6),
                BigDecimal.valueOf(11_000_000_000L, 6),
                0, 2, CampaignsStatusbssynced.Yes,
                DEFAULT_LAST_SHOW_TIME, CampaignsStatusactive.Yes);

        assertResult(updatedCamps, actual, expected);
    }

    /**
     * Тест проверяет, что если на компании увеличились показы, то поле lastShowTime изменится
     */
    @Test
    public void updateCampaignsChangeLastShow() {
        var campaignInfo = steps.campaignSteps().createCampaign(activeTextCampaign(null, null)
                        .withLastShowTime(DEFAULT_LAST_SHOW_TIME)
        );

        var activeOrderProceeded = new ProceededActiveOrder(campaignInfo.getCampaignId(), TEXT,
                2, 1, BigDecimal.valueOf(1000), 0, 0);

        activeOrderProceeded.setNewShows(true);
        var activeOrdersProceeded = List.of(activeOrderProceeded);

        int updatedCamps = activeOrdersRepository.updateCampaigns(campaignInfo.getShard(), activeOrdersProceeded);

        var actual = getActiveOrdersUpdateResult(campaignInfo.getShard(),
                campaignInfo.getCampaignId());

        var expected = new ActiveOrdersUpdateResult(
                campaignInfo.getCampaignId(), 2, 1,
                BigDecimal.valueOf(100_000_000_000L, 6), BigDecimal.valueOf(1_000_000_000, 6),
                0, 2, CampaignsStatusbssynced.Yes, LocalDateTime.now(), CampaignsStatusactive.Yes);

        assertResult(updatedCamps, actual, expected);
    }

    /**
     * Тест проверяет, что для внутренних бесплатных кампаний обновляются {@code sum_units} и
     * {@code sum_spent_units} (это могут быть показы, дни или клики).
     */
    @Test
    public void updateCampaignsSpentUnitsOnInternalFreeCampaign() {
        var campaignInfo = createCampaign(INTERNAL_FREE, emptyBalanceInfo(CurrencyCode.RUB)
                    .withSumUnits(5L)
                    .withSumSpentUnits(5L));

        var activeOrderProceeded = new ProceededActiveOrder(campaignInfo.getCampaignId(), INTERNAL_FREE,
                10, 1, BigDecimal.ZERO, 10, 9);

        int updatedCamps = activeOrdersRepository.updateCampaigns(campaignInfo.getShard(), Set.of(activeOrderProceeded));

        var actual = getActiveOrdersUpdateResult(campaignInfo.getShard(), campaignInfo.getCampaignId());
        var expected = new ActiveOrdersUpdateResult(campaignInfo.getCampaignId(), 10, 1,
                SCALED_ZERO, SCALED_ZERO, 10, 9, CampaignsStatusbssynced.Yes,
                DEFAULT_LAST_SHOW_TIME, CampaignsStatusactive.Yes);

        assertResult(updatedCamps, actual, expected);
    }

    /**
     * Тест проверяет, что внутренняя бесплатная кампания успешно завершается, если её юниты истекли
     */
    @Test
    public void updateCampaignsInternalFreeCampaignFinishedOnUnitsLimitReached() {
        var campaignInfo = createCampaign(INTERNAL_FREE, emptyBalanceInfo(CurrencyCode.RUB)
                                .withSumUnits(10L)
                                .withSumSpentUnits(10L));

        var activeOrderProceeded = new ProceededActiveOrder(campaignInfo.getCampaignId(), INTERNAL_FREE,
                10, 1, BigDecimal.ZERO, 10, 11);
        activeOrderProceeded.setFinished(true);

        int updatedCamps = activeOrdersRepository.updateCampaigns(campaignInfo.getShard(), Set.of(activeOrderProceeded));

        var actual = getActiveOrdersUpdateResult(campaignInfo.getShard(), campaignInfo.getCampaignId());
        var expected = new ActiveOrdersUpdateResult(campaignInfo.getCampaignId(), 10, 1,
                SCALED_ZERO, SCALED_ZERO, 10, 11, CampaignsStatusbssynced.Yes,
                DEFAULT_LAST_SHOW_TIME, CampaignsStatusactive.No);

        assertResult(updatedCamps, actual, expected);
    }

    /**
     * Тест проверяет, что если дистрибуционная кампания вышла за рамки sum, то она остаётся активной.
     */
    @Test
    public void updateCampaignsInternalDistribCampaignStaysActive() {
        var sum = BigDecimal.valueOf(100_000).setScale(6, RoundingMode.DOWN);
        var campaignInfo = createCampaign(INTERNAL_DISTRIB, activeBalanceInfo(CurrencyCode.RUB)
                .withSum(sum)
                .withSumSpent(sum.subtract(BigDecimal.TEN)));

        var activeOrderProceeded = new ProceededActiveOrder(campaignInfo.getCampaignId(), INTERNAL_DISTRIB,
                10, 1, sum, 0, 0);

        int updatedCamps = activeOrdersRepository.updateCampaigns(campaignInfo.getShard(), Set.of(activeOrderProceeded));

        var actual = getActiveOrdersUpdateResult(campaignInfo.getShard(), campaignInfo.getCampaignId());
        var expected = new ActiveOrdersUpdateResult(campaignInfo.getCampaignId(), 10, 1,
                sum, sum, 0,    10, CampaignsStatusbssynced.Yes,
                DEFAULT_LAST_SHOW_TIME, CampaignsStatusactive.Yes);

        assertResult(updatedCamps, actual, expected);
    }

    /**
     * Тест проверяет, что внутренняя автобюджетная кампания остаётся активной, когда на ней кончаются деньги.
     */
    @Test
    public void updateCampaignsInternalAutobudgetCampaignStaysActive() {
        var sum = BigDecimal.valueOf(100_000).setScale(6, RoundingMode.DOWN);
        var campaignInfo = createCampaign(INTERNAL_AUTOBUDGET, activeBalanceInfo(CurrencyCode.RUB)
                .withSum(sum)
                .withSumSpent(sum.subtract(BigDecimal.TEN)));

        var activeOrderProceeded = new ProceededActiveOrder(campaignInfo.getCampaignId(), INTERNAL_AUTOBUDGET,
                10, 1, sum.add(BigDecimal.TEN), 0, 0);

        int updatedCamps = activeOrdersRepository.updateCampaigns(campaignInfo.getShard(), Set.of(activeOrderProceeded));

        var actual = getActiveOrdersUpdateResult(campaignInfo.getShard(), campaignInfo.getCampaignId());
        var expected = new ActiveOrdersUpdateResult(campaignInfo.getCampaignId(), 10, 1,
                sum, sum.add(BigDecimal.TEN), 0,    10, CampaignsStatusbssynced.Yes,
                DEFAULT_LAST_SHOW_TIME, CampaignsStatusactive.Yes);

        assertResult(updatedCamps, actual, expected);
    }

    private void assertResult(int updatedCamps, ActiveOrdersUpdateResult actual, ActiveOrdersUpdateResult expected) {
        assertThat(updatedCamps).isEqualTo(1);
        assertThat(actual).withFailMessage(String.format("Expected" +
                " %s but got %s", expected, actual)).isEqualTo(expected);
    }

    private CampaignInfo createCampaign(CampaignType type, BalanceInfo balanceInfo) {
        Campaign campaign;
        switch (type) {
            case INTERNAL_FREE:
                campaign = activeInternalFreeCampaign(null, null);
                break;
            case INTERNAL_DISTRIB:
                campaign = activeInternalDistribCampaign(null, null);
                break;
            case INTERNAL_AUTOBUDGET:
                campaign = activeInternalAutobudgetCampaign(null, null);
                break;
            default:
                campaign = activeTextCampaign(null, null);
                break;
        }
        return steps.campaignSteps().createCampaign(campaign
                .withLastShowTime(DEFAULT_LAST_SHOW_TIME)
                .withBalanceInfo(balanceInfo)
        );
    }

    private static class ActiveOrdersUpdateResult {
        long cid;
        long shows;
        long clicks;
        BigDecimal sum;
        BigDecimal sumSpent;
        long sumUnits;
        long sumSpentUnits;
        CampaignsStatusbssynced statusbssynced;
        LocalDateTime lastShowTime;
        CampaignsStatusactive campaignsStatusactive;

        ActiveOrdersUpdateResult(long cid, long shows, long clicks, BigDecimal sum, BigDecimal sumSpent,
                                 long sumUnits, long sumSpentUnits, CampaignsStatusbssynced statusbssynced,
                                 LocalDateTime lastShowTime, CampaignsStatusactive campaignsStatusactive) {
            this.cid = cid;
            this.shows = shows;
            this.clicks = clicks;
            this.sum = sum;
            this.sumSpent = sumSpent;
            this.sumUnits = sumUnits;
            this.sumSpentUnits = sumSpentUnits;
            this.statusbssynced = statusbssynced;
            this.lastShowTime = lastShowTime;
            this.campaignsStatusactive = campaignsStatusactive;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ActiveOrdersUpdateResult that = (ActiveOrdersUpdateResult) o;
            return cid == that.cid &&
                    shows == that.shows &&
                    clicks == that.clicks &&
                    Objects.equals(sumUnits, that.sumUnits) &&
                    Objects.equals(sumSpentUnits, that.sumSpentUnits) &&
                    sum.equals(that.sum) &&
                    sumSpent.equals(that.sumSpent) &&
                    statusbssynced == that.statusbssynced &&
                    abs(Duration.between(lastShowTime, that.lastShowTime).getSeconds()) < 60 &&
                    campaignsStatusactive == that.campaignsStatusactive;
        }

        @Override
        public int hashCode() {
            return Objects.hash(cid, shows, clicks, sum, sumSpent, sumUnits, sumSpentUnits, statusbssynced, lastShowTime,
                    campaignsStatusactive);
        }

        @Override
        public String toString() {
            return "ActiveOrdersUpdateResult{" +
                    "cid=" + cid +
                    ", shows=" + shows +
                    ", clicks=" + clicks +
                    ", sum=" + sum +
                    ", sumSpent=" + sumSpent +
                    ", sumUnits=" + sumUnits +
                    ", sumSpentUnits=" + sumSpentUnits +
                    ", statusbssynced=" + statusbssynced +
                    ", lastShowTime=" + lastShowTime +
                    ", campaignsStatusactive=" + campaignsStatusactive +
                    '}';
        }
    }

    private ActiveOrdersUpdateResult getActiveOrdersUpdateResult(int shard, long cid) {
        return dslContextProvider.ppc(shard)
                .select(CAMPAIGNS.CID, CAMPAIGNS.SHOWS, CAMPAIGNS.CLICKS, CAMPAIGNS.SUM, CAMPAIGNS.SUM_SPENT,
                        CAMPAIGNS.SUM_UNITS, CAMPAIGNS.SUM_SPENT_UNITS,
                        CAMPAIGNS.STATUS_BS_SYNCED, CAMPAIGNS.LAST_SHOW_TIME, CAMPAIGNS.STATUS_ACTIVE)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(cid))
                .fetchOne(r ->
                        new ActiveOrdersUpdateResult(
                                r.get(CAMPAIGNS.CID),
                                r.get(CAMPAIGNS.SHOWS),
                                r.get(CAMPAIGNS.CLICKS),
                                r.get(CAMPAIGNS.SUM),
                                r.get(CAMPAIGNS.SUM_SPENT),
                                r.get(CAMPAIGNS.SUM_UNITS),
                                r.get(CAMPAIGNS.SUM_SPENT_UNITS),
                                r.get(CAMPAIGNS.STATUS_BS_SYNCED),
                                r.get(CAMPAIGNS.LAST_SHOW_TIME),
                                r.get(CAMPAIGNS.STATUS_ACTIVE)
                        ));
    }
}
