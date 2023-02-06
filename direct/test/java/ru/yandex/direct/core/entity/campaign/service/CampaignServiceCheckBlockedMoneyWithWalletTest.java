package ru.yandex.direct.core.entity.campaign.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignForBlockedMoneyCheck;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusPostmoderate;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardSupport;

import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.utils.CommonUtils.isValidId;

/**
 * Тест скопирован из перлового unit_tests/Models/CampaignOperations/mass_check_block_money_camps.t почти без изменений
 * (это часть про кошельки)
 */
public class CampaignServiceCheckBlockedMoneyWithWalletTest {
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private ShardSupport shardSupport;

    private CampaignService campaignService;

    private static List<CampaignForBlockedMoneyCheck> campsAlwaysTrue = Collections.singletonList(
            // { cid => 2, type => 'text', wallet_cid => 1, uid => 1, statusModerate => 'No', statusEmpty => 'No', },
            camp(2L, CampaignType.TEXT, 1L, 1L, CampaignStatusPostmoderate.YES, CampaignStatusModerate.NO,
                    BigDecimal.ONE, BigDecimal.ZERO)
    );

    private static List<CampaignForBlockedMoneyCheck> campsFalseOnNoWallet = Collections.singletonList(
            // { cid => 1, type => 'wallet', uid => 1, sum => 1, sum_to_pay => 0, statusModerate => 'Sent',
            // statusEmpty => 'No', },
            camp(1L, CampaignType.WALLET, 0L, 1L, CampaignStatusPostmoderate.NEW, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ZERO)
    );

    private static List<CampaignForBlockedMoneyCheck> campsAlwaysFalse = Arrays.asList(
            // { cid => 3, type => 'wallet', uid => 2, statusModerate => 'Sent', statusEmpty => 'No', },
            camp(3L, CampaignType.WALLET, 0L, 2L, CampaignStatusPostmoderate.NEW, CampaignStatusModerate.YES,
                    BigDecimal.ZERO, BigDecimal.ZERO),
            // { cid => 4, type => 'text', wallet_cid => 3, uid => 2, sum => 1, sum_to_pay => 1, statusModerate =>
            // 'Yes', statusEmpty => 'No', },
            camp(4L, CampaignType.TEXT, 3L, 2L, CampaignStatusPostmoderate.NO, CampaignStatusModerate.YES,
                    BigDecimal.ONE, BigDecimal.ONE),
            // { cid => 5, type => 'text', wallet_cid => 3, uid => 2, sum => 0, sum_to_pay => 0, statusModerate =>
            // 'Ready', statusEmpty => 'No', },
            camp(5L, CampaignType.TEXT, 3L, 2L, CampaignStatusPostmoderate.NO, CampaignStatusModerate.READY,
                    BigDecimal.ZERO, BigDecimal.ZERO)
    );

    // my $results = { 1 => 1, 2 => 1, 3 => 0, 4 => 0, 5 => 0, };
    private static Map<Long, Boolean> idToExpectedResult = new HashMap<>();
    private static Map<Long, Boolean> idToExpectedResultNoWallet = new HashMap<>();
    private static List<CampaignForBlockedMoneyCheck> campsToCheck = new ArrayList<>();

    private static Map<Long, List<CampaignForBlockedMoneyCheck>> walletToCamps = new HashMap<>();

    @BeforeClass
    public static void beforeClass() {
        for (CampaignForBlockedMoneyCheck camp : campsAlwaysTrue) {
            idToExpectedResult.put(camp.getId(), true);
            idToExpectedResultNoWallet.put(camp.getId(), true);
            campsToCheck.add(camp);
        }
        for (CampaignForBlockedMoneyCheck camp : campsFalseOnNoWallet) {
            idToExpectedResult.put(camp.getId(), true);
            idToExpectedResultNoWallet.put(camp.getId(), false);
            campsToCheck.add(camp);
        }
        for (CampaignForBlockedMoneyCheck camp : campsAlwaysFalse) {
            idToExpectedResult.put(camp.getId(), false);
            idToExpectedResultNoWallet.put(camp.getId(), false);
            campsToCheck.add(camp);
        }

        for (CampaignForBlockedMoneyCheck camp : campsToCheck) {
            if (!isValidId(camp.getWalletId())) {
                continue;
            }
            Long walletId = camp.getWalletId();
            if (!walletToCamps.containsKey(walletId)) {
                walletToCamps.put(walletId, new ArrayList<>());
            }
            walletToCamps.get(walletId).add(camp);
        }
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        ShardHelper shardHelper = new ShardHelper(shardSupport);

        campaignService = new CampaignService(campaignRepository, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, shardHelper,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null);

        when(shardSupport.getShards(any(), any())).then(i -> {
            List<Integer> shards = new ArrayList<>();
            for (Object o : (Collection<?>) i.getArgument(1)) {
                shards.add(1);
            }
            return shards;
        });
        when(campaignRepository.getCampaignsUnderWalletsForBlockedMoneyCheck(eq(1), any(), any()))
                .then(this::underWalletsMock);
    }

    @SuppressWarnings("unchecked")
    private List<CampaignForBlockedMoneyCheck> underWalletsMock(InvocationOnMock invocation) {
        List<CampaignForBlockedMoneyCheck> result = new ArrayList<>();
        for (Long walletId : (Collection<Long>) invocation.getArgument(2)) {
            result.addAll(walletToCamps.get(walletId));
        }
        return result;
    }

    private static CampaignForBlockedMoneyCheck camp(Long id, CampaignType type, Long walletId, Long userId,
                                                     CampaignStatusPostmoderate statusPostModerate,
                                                     CampaignStatusModerate statusModerate, BigDecimal sum,
                                                     BigDecimal sumToPay) {
        return new Campaign()
                .withId(id)
                .withType(type)
                .withWalletId(walletId)
                .withUserId(userId)
                .withManagerUserId(0L)
                .withAgencyUserId(0L)
                .withStatusPostModerate(statusPostModerate)
                .withStatusModerate(statusModerate)
                .withSum(sum)
                .withSumToPay(sumToPay);
    }

    @Test(expected = RuntimeException.class)
    public void testCheckBlockedMoneyException() {
        campaignService.moneyOnCampaignIsBlocked(
                camp(1L, null, 0L, 0L, CampaignStatusPostmoderate.NEW, CampaignStatusModerate.YES, BigDecimal.ZERO,
                        BigDecimal.ZERO),
                false, false);
    }

    @Test
    public void testCheckBlockedMoney() {
        Map<Long, Boolean> idToResult = campaignService.moneyOnCampaignsIsBlocked(campsToCheck, false, false);

        verify(shardSupport).getShards(any(), any());
        verify(campaignRepository).getCampaignsUnderWalletsForBlockedMoneyCheck(eq(1), any(), any());

        assertThat("При массовом получении результаты совпали с ожидаемыми", idToResult,
                beanDiffer(idToExpectedResult));
    }

    @Test
    public void testCheckBlockedMoneyNoWallet() {
        Map<Long, Boolean> idToResult = campaignService.moneyOnCampaignsIsBlocked(campsToCheck, true, false);

        verify(shardSupport, never()).getShards(any(), any());
        verify(campaignRepository, never()).getCampaignsUnderWalletsForBlockedMoneyCheck(anyInt(), any(), any());

        assertThat("При массовом получении результаты совпали с ожидаемыми", idToResult,
                beanDiffer(idToExpectedResultNoWallet));
    }
}
