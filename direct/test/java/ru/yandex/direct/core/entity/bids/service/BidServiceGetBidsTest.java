package ru.yandex.direct.core.entity.bids.service;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.bids.container.ShowConditionSelectionCriteria;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.core.entity.bids.repository.BidRepository;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessChecker;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessCheckerFactory;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectAccessValidator;
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignAccessType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.campaignTypeNotSupported;

@CoreTest
@RunWith(MockitoJUnitRunner.class)
public class BidServiceGetBidsTest {
    private static final long operatorUid = 22L;
    private static final long adGroupIds = 42L;
    private static final long campaignId = 333;
    private static final ClientId clientId = ClientId.fromLong(10L);
    private static final LimitOffset limitOffset = new LimitOffset(10, 0);

    @Mock
    ShardHelper shardHelper;
    @Mock
    BidRepository bidRepository;
    @Mock
    CampaignSubObjectAccessCheckerFactory campaignSubObjectAccessCheckerFactory;
    @Mock
    CampaignSubObjectAccessChecker accessChecker;
    @Mock
    CampaignSubObjectAccessValidator validator;
    @InjectMocks
    BidService bidService;

    private Bid bid;
    private ShowConditionSelectionCriteria selectionCriteria;

    @Before
    public void setUp() {
        bid = new Bid();
        selectionCriteria = new ShowConditionSelectionCriteria().withAdGroupIds(Collections.singleton(adGroupIds));

        when(shardHelper.getShardByClientIdStrictly(any(ClientId.class))).thenReturn(1);
        when(bidRepository.getCampaignIdsForBids(anyInt(), anyCollection(), anyCollection()))
                .thenReturn(Collections.singletonList(campaignId));
        when(bidRepository.getBids(anyInt(), any(), any())).thenReturn(Collections.singletonList(bid));
        when(campaignSubObjectAccessCheckerFactory.newCampaignChecker(anyLong(), any(ClientId.class), anyCollection()))
                .thenReturn(accessChecker);
        when(accessChecker.createValidator(CampaignAccessType.READ))
                .thenReturn(validator);
    }

    @Test
    public void getBids_success() throws Exception {
        when(validator.apply(anyLong())).thenReturn(ValidationResult.success(campaignId));
        initMocks(this);
        List<Bid> result = bidService.getBids(clientId, operatorUid, selectionCriteria, limitOffset);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(bid, result.get(0));
    }

    @Test
    public void getBids_filterFailed() throws Exception {
        when(validator.apply(anyLong())).thenReturn(ValidationResult.failed(campaignId, campaignTypeNotSupported()));
        initMocks(this);
        List<Bid> result = bidService.getBids(clientId, operatorUid, selectionCriteria, limitOffset);
        Assert.assertEquals(0, result.size());
    }
}
