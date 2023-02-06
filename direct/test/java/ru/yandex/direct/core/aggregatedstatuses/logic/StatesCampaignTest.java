package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.campaign.CampaignStatesEnum;
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusCampaign;
import ru.yandex.direct.core.entity.campaign.aggrstatus.AggregatedStatusWallet;
import ru.yandex.direct.core.entity.campaign.aggrstatus.CampaignDelayedOperation;
import ru.yandex.direct.core.entity.campaign.aggrstatus.WalletStatus;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.PriceFlightStatusApprove;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Percent;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaPayForConversionStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.averageCpaStrategy;

@RunWith(Parameterized.class)
public class StatesCampaignTest {
    private static CampaignStates campaignStatesCalculator;

    @Parameterized.Parameter
    public AggregatedStatusCampaign campaign;

    @Parameterized.Parameter(1)
    public AggregatedStatusWallet wallet;

    @Parameterized.Parameter(2)
    public Boolean isClientPessimized;

    @Parameterized.Parameter(3)
    public Long goalConversionsCount;

    @Parameterized.Parameter(4)
    public Boolean hasPromoExtensionRejected;

    @Parameterized.Parameter(5)
    public Collection<CampaignStatesEnum> expectedStates;

    @Parameterized.Parameters(name = "{index}: => States: {2}")
    public static Object[][] params() {
        return new Object[][]{
                {
                        getDefaultAggregatedStatusCampaign(),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.NO_MONEY)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withArchived(true),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.ARCHIVED)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withArchived(true)
                                .withDelayedOperation(CampaignDelayedOperation.UNARC),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.UNARCHIVING)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withArchived(true)
                                .withCurrencyCode(CurrencyCode.YND_FIXED)
                                .withCurrencyConverted(true),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.ARCHIVED, CampaignStatesEnum.CANT_BE_UNARCHIVED)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withDelayedOperation(CampaignDelayedOperation.ARC),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.ARCHIVING)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withShowing(false),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.SUSPENDED)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withEmpty(true),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.DRAFT)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withStatusModerate(CampaignStatusModerate.NEW),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.DRAFT)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withStatusModerate(CampaignStatusModerate.NEW),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.DRAFT)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withType(CampaignType.INTERNAL_FREE)
                                .withRestrictionValue(1L)
                                .withSumSpentUnits(1L),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.UNITS_EXHAUSTED)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withSum(BigDecimal.ONE)
                                .withSumSpent(BigDecimal.ONE)
                                .withOrderId(1L),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.NO_MONEY)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withSum(BigDecimal.ONE)
                                .withSumSpent(BigDecimal.ONE)
                                .withOrderId(1L)
                                .withSumToPay(BigDecimal.ONE),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.NO_MONEY, CampaignStatesEnum.AWAIT_PAYMENT)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withWalletId(1L),
                        new AggregatedStatusWallet()
                                .withSum(BigDecimal.ZERO)
                                .withAutoOverdraftAddition(BigDecimal.ZERO),
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.NO_MONEY)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withWalletId(1L),
                        new AggregatedStatusWallet()
                                .withSum(BigDecimal.ZERO)
                                .withAutoOverdraftAddition(BigDecimal.ZERO)
                                .withStatus(new WalletStatus().withWaitingForPayment(true)),
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.NO_MONEY, CampaignStatesEnum.AWAIT_PAYMENT)},

                {
                        getDefaultAggregatedStatusCampaign()
                                .withSum(BigDecimal.ONE),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.PAYED)
                },

                {
                        getDefaultAggregatedStatusCampaign()
                                .withType(CampaignType.CPM_BANNER)
                                .withHasSiteMonitoring(true),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.NO_MONEY, CampaignStatesEnum.DOMAIN_MONITORED)
                },

                {
                        getDefaultAggregatedStatusCampaign()
                                .withType(CampaignType.INTERNAL_DISTRIB),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.PAYED)
                },

                {
                        getDefaultAggregatedStatusCampaign()
                                .withType(CampaignType.INTERNAL_AUTOBUDGET),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.NO_MONEY)
                },

                {
                        getDefaultAggregatedStatusCampaign()
                                .withType(CampaignType.INTERNAL_AUTOBUDGET)
                                .withSum(BigDecimal.TEN),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.PAYED)
                },

                {
                        getDefaultAggregatedStatusCampaign()
                                .withType(CampaignType.INTERNAL_FREE)
                                .withRestrictionValue(10L)
                                .withSumSpentUnits(1L),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.PAYED)
                },

//                 Временно убираем для всех кампаний: DIRECT-140177
//                 {
//                         getDefaultAggregatedStatusCampaign()
//                                 .withStrategy(averageCpaPayForConversionStrategy(BigDecimal.TEN, 1L, null, null))
//                                 .withSum(BigDecimal.valueOf(20)),
//                         new AggregatedStatusWallet()
//                                 .withAutoOverdraftAddition(BigDecimal.ZERO)
//                                 .withSum(BigDecimal.valueOf(5)),
//                         List.of(CampaignStatesEnum.PAYED,
//                                 CampaignStatesEnum.PAY_FOR_CONVERSION_CAMPAIGN_HAS_LACK_OF_FUNDS)
//                 },
                 {
                         getDefaultAggregatedStatusCampaign()
                                 .withStrategy(averageCpaPayForConversionStrategy(BigDecimal.TEN, 1L, null, null))
                                 .withSum(BigDecimal.valueOf(20)),
                         new AggregatedStatusWallet()
                                 .withAutoOverdraftAddition(BigDecimal.ZERO)
                                 .withSum(BigDecimal.valueOf(5)),
                         true,
                         0L,
                         false,
                         List.of(CampaignStatesEnum.PAYED,
                                 CampaignStatesEnum.PAY_FOR_CONVERSION_CAMPAIGN_HAS_LACK_OF_CONVERSION)
                 },
                {
                        getDefaultAggregatedStatusCampaign()
                                .withStrategy(averageCpaPayForConversionStrategy(BigDecimal.TEN, 1L, null, null))
                                .withSum(BigDecimal.valueOf(20)),
                        new AggregatedStatusWallet()
                                .withAutoOverdraftAddition(BigDecimal.ZERO)
                                .withSum(BigDecimal.valueOf(5)),
                        true,
                        null,
                        false,
                        List.of(CampaignStatesEnum.PAYED,
                                CampaignStatesEnum.PAY_FOR_CONVERSION_CAMPAIGN_HAS_LACK_OF_CONVERSION)
                },
                {
                        getDefaultAggregatedStatusCampaign()
                                .withStrategy(averageCpaStrategy(BigDecimal.TEN, 1L, null, null))
                                .withSum(BigDecimal.valueOf(20)),
                        new AggregatedStatusWallet()
                                .withAutoOverdraftAddition(BigDecimal.ZERO)
                                .withSum(BigDecimal.valueOf(5)),
                        true,
                        null,
                        false,
                        List.of(CampaignStatesEnum.PAYED)
                },
                {
                        getDefaultAggregatedStatusCampaign()
                                .withStrategy(averageCpaPayForConversionStrategy(BigDecimal.TEN, 1L, null, null))
                                .withSum(BigDecimal.valueOf(20)),
                        new AggregatedStatusWallet()
                                .withAutoOverdraftAddition(BigDecimal.ZERO)
                                .withSum(BigDecimal.valueOf(5)),
                        false,
                        null,
                        false,
                        List.of(CampaignStatesEnum.PAYED)
                },
                {
                        getDefaultAggregatedStatusCampaign()
                                .withStrategy(averageCpaPayForConversionStrategy(BigDecimal.TEN, 1L, null, null))
                                .withSum(BigDecimal.valueOf(20)),
                        new AggregatedStatusWallet()
                                .withAutoOverdraftAddition(BigDecimal.ZERO)
                                .withSum(BigDecimal.valueOf(5)),
                        true,
                        10L,
                        false,
                        List.of(CampaignStatesEnum.PAYED)
                },

                {
                        getDefaultAggregatedStatusCampaign()
                                .withType(CampaignType.CPM_PRICE)
                                .withFlightStatusApprove(PriceFlightStatusApprove.NEW)
                                .withStatusModerate(CampaignStatusModerate.NEW),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.CPM_PRICE_WAITING_FOR_APPROVE, CampaignStatesEnum.DRAFT)
                },

                {
                        getDefaultAggregatedStatusCampaign()
                                .withType(CampaignType.CPM_PRICE)
                                .withFlightStatusApprove(PriceFlightStatusApprove.NO)
                                .withStatusModerate(CampaignStatusModerate.NEW),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.CPM_PRICE_NOT_APPROVED, CampaignStatesEnum.DRAFT)
                },

                {
                        getDefaultAggregatedStatusCampaign()
                                .withType(CampaignType.CPM_PRICE)
                                .withFlightStatusApprove(PriceFlightStatusApprove.YES)
                                .withStatusModerate(CampaignStatusModerate.NEW),
                        null,
                        false,
                        0L,
                        false,
                        List.of(CampaignStatesEnum.CPM_PRICE_INCORRECT, CampaignStatesEnum.DRAFT)
                },

                {
                        getDefaultAggregatedStatusCampaign()
                                .withSum(BigDecimal.ONE),
                        null,
                        false,
                        0L,
                        true,
                        List.of(CampaignStatesEnum.PAYED, CampaignStatesEnum.PROMO_EXTENSION_REJECTED)
                },
        };
    }

    private static AggregatedStatusCampaign getDefaultAggregatedStatusCampaign() {
        return new AggregatedStatusCampaign()
                .withSum(BigDecimal.ZERO)
                .withSumSpent(BigDecimal.ZERO)
                .withSumLast(BigDecimal.ONE)
                .withOrderId(0L)
                .withCurrencyCode(CurrencyCode.RUB);
    }

    @BeforeClass
    public static void prepare() {
        campaignStatesCalculator = new CampaignStates();
    }

    @Test
    public void test() {
        Collection<CampaignStatesEnum> states = campaignStatesCalculator.calc(campaign, wallet,
                Percent.fromPercent(BigDecimal.valueOf(20)),
                isClientPessimized, goalConversionsCount, hasPromoExtensionRejected);

        assertEquals("got right states", expectedStates, states);
    }
}
