package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.List;

import com.yandex.direct.api.v5.bidmodifiers.BidModifierGetItem;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierLevelEnum;
import com.yandex.direct.api.v5.bidmodifiers.BidModifiersSelectionCriteria;
import com.yandex.direct.api.v5.bidmodifiers.GetRequest;
import com.yandex.direct.api.v5.bidmodifiers.SmartAdAdjustmentFieldEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierPerformanceTgoAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultPerformanceTgo;

@Api5Test
@RunWith(SpringRunner.class)
public class GetSmartBidModifiersDelegateTest {

    @Autowired
    private Steps steps;
    @Autowired
    private BidModifierService bidModifierService;
    @Autowired
    private GetBidModifiersValidationService validationService;
    @Autowired
    private PropertyFilter propertyFilter;
    @Autowired
    private AdGroupService adGroupService;
    @Mock
    private ApiAuthenticationSource auth;

    private GetBidModifiersDelegate delegate;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        clientInfo = steps.clientSteps().createDefaultClient();
        when(auth.getSubclient()).thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(clientInfo.getUid()));
        delegate = new GetBidModifiersDelegate(auth,
                validationService,
                bidModifierService,
                propertyFilter,
                adGroupService);
    }

    @Test
    public void get_SmartBidModifier_success() {
        PerformanceAdGroupInfo adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo);
        var campaignId = adGroupInfo.getCampaignId();
        var adGroupId = adGroupInfo.getAdGroupId();

        var adjustment = new BidModifierPerformanceTgoAdjustment().withPercent(20);
        var bidModifier = createDefaultPerformanceTgo(campaignId, adGroupId)
                .withPerformanceTgoAdjustment(adjustment);
        var bidModifierInfo = steps.bidModifierSteps().createAdGroupBidModifier(bidModifier, adGroupInfo);
        var bidModifierRealId = bidModifierInfo.getBidModifierId();
        var bidModifierExternalId = BidModifierService.getExternalId(bidModifierRealId,
                BidModifierType.PERFORMANCE_TGO_MULTIPLIER);

        var criteria = new BidModifiersSelectionCriteria()
                .withIds(bidModifierExternalId)
                .withLevels(BidModifierLevelEnum.AD_GROUP);
        var externalRequest = new GetRequest().withSelectionCriteria(criteria)
                .withSmartAdAdjustmentFieldNames(SmartAdAdjustmentFieldEnum.BID_MODIFIER);

        GenericGetRequest<BidModifierAnyFieldEnum, BidModifiersSelectionCriteria> request =
                delegate.convertRequest(externalRequest);
        List<BidModifierGetItem> bidModifierGetItems = delegate.get(request);
        assertThat(bidModifierGetItems).hasSize(1);
        var smartAdAdjustment = bidModifierGetItems.get(0).getSmartAdAdjustment();
        assertThat(smartAdAdjustment.getBidModifier()).isEqualTo(adjustment.getPercent());
    }

}
