package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignForBlockedMoneyCheck;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

/**
 * Тест скопирован из перлового unit_tests/Models/CampaignOperations/mass_check_block_money_camps.t почти без изменений,
 * но с небольшими дополнениями (это часть про кампании без кошелька)
 */
@RunWith(MockitoJUnitRunner.class)
public class CampaignServiceCheckBlockedMoneyNoWalletTest {
    @Mock
    private ShardHelper shardHelper;

    @InjectMocks
    private CampaignService campaignService;

    private static List<CampaignForBlockedMoneyCheck> campsAlwaysTrue = Arrays.asList(
            // { cid => 1, type => 'text', statusPostModerate => 'Yes', sum => 1, sum_to_pay => 0, },
            camp(11L, CampaignType.TEXT, 0L, 0L, CampaignStatusPostmoderate.YES, CampaignStatusModerate.YES,
                    BigDecimal.ONE, BigDecimal.ZERO),
            camp(12L, CampaignType.TEXT, 0L, 0L, CampaignStatusPostmoderate.NO, CampaignStatusModerate.NEW,
                    BigDecimal.ONE, BigDecimal.ZERO),
            // { cid => 4, type => 'mcb', statusPostModerate => 'Yes', sum => 0, sum_to_pay => 1, },
            camp(13L, CampaignType.MCB, 0L, 0L, CampaignStatusPostmoderate.YES, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ONE)
    );

    private static List<CampaignForBlockedMoneyCheck> campsTrueOnTypeOnly = Arrays.asList(
            // { cid => 2, type => 'text', statusPostModerate => 'Yes', sum => 0, sum_to_pay => 0, },
            camp(21L, CampaignType.TEXT, 0L, 0L, CampaignStatusPostmoderate.YES, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ZERO),
            // { cid => 5, type => 'mcb', statusPostModerate => 'Yes', sum => 0, sum_to_pay => 0, },
            camp(22L, CampaignType.MCB, 0L, 0L, CampaignStatusPostmoderate.YES, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ZERO)
    );

    private static List<CampaignForBlockedMoneyCheck> campsAlwaysFalse = Arrays.asList(
            // { cid => 3, type => 'text', statusPostModerate => 'No', sum => 1, sum_to_pay => 0, },
            camp(31L, CampaignType.TEXT, 0L, 0L, CampaignStatusPostmoderate.NO, CampaignStatusModerate.YES,
                    BigDecimal.ONE, BigDecimal.ZERO),
            // { cid => 6, type => 'mcb', statusPostModerate => 'No', sum => 0, sum_to_pay => 1, },
            camp(32L, CampaignType.MCB, 0L, 0L, CampaignStatusPostmoderate.NO, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ONE),
            // { cid => 7, type => 'text', ManagerUID => 1, },
            camp(33L, CampaignType.TEXT, 1L, 0L, CampaignStatusPostmoderate.YES, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ZERO),
            // { cid => 8, type => 'text', AgencyUID => 2, },
            camp(34L, CampaignType.TEXT, 0L, 2L, CampaignStatusPostmoderate.YES, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ZERO),
            // { cid => 9, type => 'text', statusPostModerate => 'Accepted', }, # fake tables join
            camp(35L, CampaignType.TEXT, 0L, 0L, CampaignStatusPostmoderate.ACCEPTED, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ZERO),
            // { cid => 10, type => 'text', statusPostModerate => 'No', statusModerate => 'yes', }, # fake tables join
            camp(36L, CampaignType.TEXT, 0L, 0L, CampaignStatusPostmoderate.NO, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ZERO)
    );

    private static List<CampaignForBlockedMoneyCheck> campsToCheck = new ArrayList<>();

    // my $results = { 1 => 1, 2 => 0, 3 => 0, 4 => 1, 5 => 0, 6 => 0, 7 => 0, 8 => 0, 9 => 0, 10 => 0, };
    private static Map<Long, Boolean> idToExpectedResult = new HashMap<>();
    private static Map<Long, Boolean> idToExpectedResultTypeOnly = new HashMap<>();

    @BeforeClass
    public static void beforeClass() {
        for (CampaignForBlockedMoneyCheck camp : campsAlwaysTrue) {
            idToExpectedResult.put(camp.getId(), true);
            idToExpectedResultTypeOnly.put(camp.getId(), true);
            campsToCheck.add(camp);
        }
        for (CampaignForBlockedMoneyCheck camp : campsTrueOnTypeOnly) {
            idToExpectedResult.put(camp.getId(), false);
            idToExpectedResultTypeOnly.put(camp.getId(), true);
            campsToCheck.add(camp);
        }
        for (CampaignForBlockedMoneyCheck camp : campsAlwaysFalse) {
            idToExpectedResult.put(camp.getId(), false);
            idToExpectedResultTypeOnly.put(camp.getId(), false);
            campsToCheck.add(camp);
        }
    }

    private static CampaignForBlockedMoneyCheck camp(Long id, CampaignType type,
                                                     Long managerUserId, Long agencyUserId,
                                                     CampaignStatusPostmoderate statusPostModerate,
                                                     CampaignStatusModerate statusModerate, BigDecimal sum,
                                                     BigDecimal sumToPay) {
        return new Campaign()
                .withId(id)
                .withType(type)
                .withWalletId(0L)
                .withUserId(0L)
                .withManagerUserId(managerUserId)
                .withAgencyUserId(agencyUserId)
                .withStatusPostModerate(statusPostModerate)
                .withStatusModerate(statusModerate)
                .withSum(sum)
                .withSumToPay(sumToPay);
    }

    @Test
    public void testCheckBlockedMoney() {
        Map<Long, Boolean> idToResult = campaignService.moneyOnCampaignsIsBlocked(campsToCheck, false, false);

        verify(shardHelper, never()).groupByShard(any(), any());

        assertThat(
                "При массовом получении результаты совпали с ожидаемыми для noCheckCampaignsUnderWallet = false и " +
                        "typeOnly = false",
                idToResult,
                beanDiffer(idToExpectedResult));
    }

    @Test
    public void testCheckBlockedMoneyNoWalletTrue() {
        Map<Long, Boolean> idToResult = campaignService.moneyOnCampaignsIsBlocked(campsToCheck, true, false);

        verify(shardHelper, never()).groupByShard(any(), any());

        // noCheckCampaignsUnderWallet не должен влиять на результат для кампаний без кошелька
        assertThat(
                "При массовом получении результаты совпали с ожидаемыми для noCheckCampaignsUnderWallet = true и " +
                        "typeOnly = false",
                idToResult,
                beanDiffer(idToExpectedResult));
    }

    @Test
    public void testCheckBlockedMoneyTypeOnly() {
        Map<Long, Boolean> idToResult = campaignService.moneyOnCampaignsIsBlocked(campsToCheck, false, true);

        verify(shardHelper, never()).groupByShard(any(), any());

        assertThat(
                "При массовом получении результаты совпали с ожидаемыми для noCheckCampaignsUnderWallet = false и " +
                        "typeOnly = true",
                idToResult,
                beanDiffer(idToExpectedResultTypeOnly));
    }

    @Test
    public void testCheckBlockedMoneyTypeOnlyNoWalletTrue() {
        Map<Long, Boolean> idToResult = campaignService.moneyOnCampaignsIsBlocked(campsToCheck, true, true);

        verify(shardHelper, never()).groupByShard(any(), any());

        // noCheckCampaignsUnderWallet не должен влиять на результат для кампаний без кошелька
        assertThat(
                "При массовом получении результаты совпали с ожидаемыми для noCheckCampaignsUnderWallet = true и " +
                        "typeOnly = true",
                idToResult,
                beanDiffer(idToExpectedResultTypeOnly));
    }
}
