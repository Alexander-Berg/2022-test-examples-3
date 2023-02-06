package ru.yandex.direct.api.v5.entity.audiencetargets.delegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.audiencetargets.AudienceTargetFieldEnum;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.audiencetargets.converter.AudienceTargetsGetRequestConverter;
import ru.yandex.direct.api.v5.entity.audiencetargets.converter.TargetInterestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.client.service.ClientService;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GetAudienceTargetsDelegateTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final ClientId CLIENT_ID = ClientId.fromLong(33L);
    private static final long UID = 444L;
    private static final long CAMPAIGN_ID = 222L;
    private static final long ADGROUP_ID = 1L;

    private static final GenericGetRequest<AudienceTargetFieldEnum, RetargetingSelection> GET_REQUEST =
            new GenericGetRequest<>(
                    singleton(AudienceTargetFieldEnum.CONTEXT_BID),
                    new RetargetingSelection(),
                    LimitOffset.maxLimited());


    private GetAudienceTargetsDelegate getAudienceTargetsDelegate;
    private RetargetingService retargetingService;

    @Mock
    private AdGroupService adGroupService;

    @Before
    public void setUp() {
        ApiUser operatorAndSubclient = mock(ApiUser.class);
        when(operatorAndSubclient.getClientId()).thenReturn(CLIENT_ID);
        when(operatorAndSubclient.getUid()).thenReturn(UID);

        ApiAuthenticationSource apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        when(apiAuthenticationSource.getOperator()).thenReturn(operatorAndSubclient);
        when(apiAuthenticationSource.getChiefSubclient()).thenReturn(operatorAndSubclient);

        ClientService clientService = mock(ClientService.class);
        when(clientService.getWorkCurrency(CLIENT_ID)).thenReturn(CurrencyCode.RUB.getCurrency());

        retargetingService = mock(RetargetingService.class);
        getAudienceTargetsDelegate = new GetAudienceTargetsDelegate(
                apiAuthenticationSource,
                mock(AudienceTargetsGetRequestConverter.class),
                retargetingService,
                clientService,
                mock(AdGroupRepository.class),
                mock(ShardHelper.class),
                mock(PropertyFilter.class),
                mock(TargetInterestConverter.class),
                adGroupService
        );
    }

    @Test
    public void getDoesNotReturnNullPriceContext() {
        List<TargetInterest> targetInterests = new ArrayList<>();
        targetInterests.add(new TargetInterest().withCampaignId(CAMPAIGN_ID));
        when(
                retargetingService.getRetargetings(
                        same(GET_REQUEST.getSelectionCriteria()),
                        eq(CLIENT_ID), eq(UID),
                        same(GET_REQUEST.getLimitOffset()))
        ).thenReturn(
                targetInterests
        );

        assertThat(getAudienceTargetsDelegate.get(GET_REQUEST))
                .extracting(Retargeting::getPriceContext)
                .allMatch(Objects::nonNull);
    }

    @Test
    public void get_CpmVideoAdGroup_ReturnResult() {
        List<TargetInterest> targetInterests = new ArrayList<>();
        targetInterests.add(new TargetInterest().withAdGroupId(ADGROUP_ID));
        when(
                retargetingService.getRetargetings(
                        same(GET_REQUEST.getSelectionCriteria()),
                        eq(CLIENT_ID), eq(UID),
                        same(GET_REQUEST.getLimitOffset()))
        ).thenReturn(
                targetInterests
        );

        when(adGroupService.getAdGroupTypes(eq(CLIENT_ID), eq(ImmutableSet.of(ADGROUP_ID))))
                .thenReturn(ImmutableMap.of(ADGROUP_ID, AdGroupType.CPM_VIDEO));

        assertThat(getAudienceTargetsDelegate.get(GET_REQUEST))
                .withFailMessage("Тип cpm_video поддерживается в методе audiencetargets.get")
                .isNotEmpty();
    }
}
