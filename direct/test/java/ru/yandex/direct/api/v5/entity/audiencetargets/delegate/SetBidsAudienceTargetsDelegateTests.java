package ru.yandex.direct.api.v5.entity.audiencetargets.delegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.audiencetargets.AudienceTargetSetBidsItem;
import com.yandex.direct.api.v5.audiencetargets.SetBidsRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.audiencetargets.converter.AudienceTargetsHelperConverter;
import ru.yandex.direct.api.v5.result.ApiResult;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bids.container.SetBidItem;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.nCopies;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.audiencetargets.Constants.MAX_BID_ADGROUPIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.audiencetargets.Constants.MAX_BID_CAMPAIGNIDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.entity.audiencetargets.Constants.MAX_BID_IDS_PER_REQUEST;
import static ru.yandex.direct.api.v5.validation.Matchers.hasNoDefects;
import static ru.yandex.direct.api.v5.validation.Matchers.validationError;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(SpringRunner.class)
@Api5Test
public class SetBidsAudienceTargetsDelegateTests {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private SetBidsAudienceTargetsDelegate delegate;

    @Mock
    private AudienceTargetsHelperConverter convertService;

    @Mock
    private RetargetingService retargetingService;

    @Mock
    private ApiAuthenticationSource auth;

    @Mock
    private AdGroupService adGroupService;

    @Mock
    private ApiUser simpleUser;

    @Autowired
    private ResultConverter resultConverter;

    private static final Long ADGROUP_ID = 1L;
    private static final Long TARGET_INTEREST_ID = 1L;
    private static final Long OPERATOR_UID = 1L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(2L);

    @Before
    public void setup() {
        when(simpleUser.getUid()).thenReturn(OPERATOR_UID);
        when(simpleUser.getClientId()).thenReturn(CLIENT_ID);

        when(retargetingService.setBidsApi(any(), any(), any(), anySet()))
                .thenReturn(
                        MassResult.successfulMassAction(new ArrayList<>(), new ValidationResult<>(new ArrayList<>())));

        when(auth.getOperator()).thenReturn(simpleUser);
        when(auth.getChiefSubclient()).thenReturn(simpleUser);

        delegate = new SetBidsAudienceTargetsDelegate(
                convertService,
                auth,
                resultConverter,
                retargetingService,
                adGroupService,
                mock(PpcPropertiesSupport.class),
                mock(FeatureService.class)
        );
    }

    @Test
    public void convertRequestDelegatesToSpecializedService() {
        SetBidsRequest request = new SetBidsRequest();
        List<SetBidItem> setBidItemList = new ArrayList<>();
        when(convertService.convertSetBidsRequest(request)).thenReturn(setBidItemList);

        delegate.convertRequest(request);

        verify(convertService).convertSetBidsRequest(request);
    }

    @Test
    public void convertResultDelegatesToSpecializedService() {
        delegate.convertResponse(ApiResult.successful(new ArrayList<>()));

        verify(convertService).convertToSetBidsResponse(any(), any());
    }

    @Test
    public void processList_DelegatedToDomainService() {
        List<SetBidItem> request = new ArrayList<>();

        delegate.processList(request);
        Set<AdGroupType> allowedAdGroupTypes = delegate.getAllowedAdGroupTypes();
        verify(retargetingService).setBidsApi(request, simpleUser.getClientId(), simpleUser.getUid(),
                allowedAdGroupTypes);
    }

    @Test
    public void validateRequest_idsAtTheLimit_noError() {
        SetBidsRequest request = createRequest(MAX_BID_IDS_PER_REQUEST, item -> item.setId(1L));

        ValidationResult<SetBidsRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validateRequest_idsOverlowTheLimit_errorIsGenerated() {
        SetBidsRequest request = createRequest(MAX_BID_IDS_PER_REQUEST + 1, item -> item.setId(1L));

        ValidationResult<SetBidsRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(9300))));
    }

    @Test
    public void validateRequest_adGroupIdsAtTheLimit_noError() {
        SetBidsRequest request = createRequest(MAX_BID_ADGROUPIDS_PER_REQUEST, item -> item.setAdGroupId(1L));

        ValidationResult<SetBidsRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validateRequest_adGroupIdsOverlowTheLimit_errorIsGenerated() {
        SetBidsRequest request =
                createRequest(MAX_BID_ADGROUPIDS_PER_REQUEST + 1, item -> item.setAdGroupId(1L));
        ValidationResult<SetBidsRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(9300))));
    }

    @Test
    public void validateRequest_campaignIdsAtTheLimit_noError() {
        SetBidsRequest request = createRequest(MAX_BID_CAMPAIGNIDS_PER_REQUEST, item -> item.setCampaignId(1L));

        ValidationResult<SetBidsRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.hasAnyErrors()).isFalse();
    }

    @Test
    public void validateRequest_campaignIdsOverlowTheLimit_errorIsGenerated() {
        SetBidsRequest request =
                createRequest(MAX_BID_CAMPAIGNIDS_PER_REQUEST + 1, item -> item.setCampaignId(1L));
        ValidationResult<SetBidsRequest, DefectType> result = delegate.validateRequest(request);

        assertThat(result.flattenErrors()).is(matchedBy(contains(validationError(9300))));
    }

    @Test
    public void validateInternalRequest_CpmVideoAdGroup_Ok() {
        List<SetBidItem> request = singletonList(new SetBidItem().withAdGroupId(ADGROUP_ID));

        when(retargetingService.getRetargetings(
                any(RetargetingSelection.class),
                eq(CLIENT_ID),
                eq(OPERATOR_UID), eq(LimitOffset.maxLimited())))
                .thenReturn(singletonList(new TargetInterest().withId(TARGET_INTEREST_ID).withAdGroupId(ADGROUP_ID)));

        when(adGroupService.getAdGroupTypes(eq(CLIENT_ID), eq(ImmutableSet.of(ADGROUP_ID))))
                .thenReturn(ImmutableMap.of(ADGROUP_ID, AdGroupType.CPM_VIDEO));

        ValidationResult<List<SetBidItem>, DefectType> vr = delegate.validateInternalRequest(request);
        Assert.assertThat("Тип cpm_video поддерживается в методе audiencetargets.setBids", vr, hasNoDefects());
    }

    private static SetBidsRequest createRequest(int numberOfIds, Consumer<AudienceTargetSetBidsItem> setId) {
        AudienceTargetSetBidsItem item = new AudienceTargetSetBidsItem();
        setId.accept(item);
        return new SetBidsRequest().withBids(nCopies(numberOfIds, item));
    }
}
