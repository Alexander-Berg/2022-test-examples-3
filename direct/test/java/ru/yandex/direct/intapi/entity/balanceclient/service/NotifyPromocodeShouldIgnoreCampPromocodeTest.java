package ru.yandex.direct.intapi.entity.balanceclient.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Collections.singletonList;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@RunWith(Parameterized.class)
public class NotifyPromocodeShouldIgnoreCampPromocodeTest {
    @Parameterized.Parameter
    public String campDescription;
    @Parameterized.Parameter(1)
    public Campaign campaignUnderTest;
    @Parameterized.Parameter(2)
    public boolean ignoreReason;

    @Parameterized.Parameters(name = "{0}: ignore reason = {2}")
    public static Object[] params() {
        return new Object[][]{
                {"camp without wallet", new Campaign().withType(CampaignType.TEXT).withWalletId(0L), false},
                {"wallet", new Campaign().withType(CampaignType.WALLET).withWalletId(0L), false},
                {"camp with wallet", new Campaign().withType(CampaignType.CPM_BANNER).withWalletId(124L), true
                },
                {"billing aggregate with wallet",
                        new Campaign().withType(CampaignType.BILLING_AGGREGATE).withWalletId(124L), true
                },
        };
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testShouldIgnoreCampPromocode() {
        long campaignId = 123L;
        int shard = 12;

        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByCampaignId(campaignId)).thenAnswer(invocation -> shard);
        CampaignRepository campaignRepository = mock(CampaignRepository.class);
        when(campaignRepository.getCampaigns(shard, singletonList(campaignId)))
                .thenAnswer(invocation -> singletonList(campaignUnderTest));
        NotifyPromocodeService svc = new NotifyPromocodeService(null, null,
                null, campaignRepository, shardHelper, null, null
        );

        assumeThat(svc.shouldIgnoreCampPromocode(123), is(ignoreReason));
    }
}
