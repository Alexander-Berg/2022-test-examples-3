package ru.yandex.direct.api.v5.entity.bidmodifiers.delegate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierLevelEnum;
import com.yandex.direct.api.v5.bidmodifiers.BidModifierTypeEnum;
import com.yandex.direct.api.v5.bidmodifiers.BidModifiersSelectionCriteria;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.bidmodifiers.validation.GetBidModifiersValidationService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_AD_GROUP_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_CAMPAIGN_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_ID;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_LEVEL;
import static ru.yandex.direct.api.v5.entity.bidmodifiers.delegate.BidModifierAnyFieldEnum.BID_MODIFIER_TYPE;
import static ru.yandex.direct.core.entity.bidmodifiers.Constants.ALL_LEVELS;

public class GetBidModifiersDelegateTest {

    private static final long CAMPAIGN_ID = 11L;
    private static final long AD_GROUP_ID = 22L;

    public static final long EXTERNAL_ID = 1033L;
    public static final long INTERNAL_ID = 33L;

    private static final ClientId CHIEF_CLIENT_ID = ClientId.fromLong(44L);

    private static final long OPERATOR_UID = 55L;
    private static final LimitOffset LIMIT_OFFSET = new LimitOffset(5, 100);

    private static final HashSet<BidModifierTypeEnum> BID_MODIFIER_TYPE_ENUMS =
            new HashSet<>(asList(BidModifierTypeEnum.values()));
    private static final HashSet<BidModifierLevelEnum> BID_MODIFIER_LEVEL_ENUMS =
            new HashSet<>(asList(BidModifierLevelEnum.values()));

    private static final long BID_MODIFIER_MOBILE_ADJUSTMENT_ID = 1L;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiAuthenticationSource apiAuthenticationSource;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BidModifierService bidModifierService;

    @Mock
    private AdGroupService adGroupService;

    private GetBidModifiersDelegate getBidModifiersDelegate;

    private GenericGetRequest<BidModifierAnyFieldEnum, BidModifiersSelectionCriteria> request;

    private BidModifiersSelectionCriteria criteria;

    private Multimap<BidModifierType, Long> idsByType;

    @Before
    public void before() {
        when(apiAuthenticationSource.getOperator().getUid()).thenReturn(OPERATOR_UID);
        when(apiAuthenticationSource.getChiefSubclient().getClientId()).thenReturn(CHIEF_CLIENT_ID);

        List<BidModifier> bidModifiers = new ArrayList<>();
        bidModifiers.add(new BidModifierMobile()
                .withId(INTERNAL_ID)
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withEnabled(true)
                .withMobileAdjustment(new BidModifierMobileAdjustment().withId(INTERNAL_ID)));

        when(bidModifierService.getByIds(any(ClientId.class), any(), any(), any(), any(), any(), anyLong()))
                .thenReturn(bidModifiers);

        when(bidModifierService.getByAdGroupIds(any(ClientId.class), any(), any(), any(), any(), anyLong()))
                .thenReturn(bidModifiers);

        when(bidModifierService.getByCampaignIds(any(ClientId.class), any(), any(), any(), anyLong()))
                .thenReturn(bidModifiers);

        getBidModifiersDelegate = new GetBidModifiersDelegate(
                apiAuthenticationSource,
                mock(GetBidModifiersValidationService.class),
                bidModifierService,
                new PropertyFilter(),
                adGroupService);

        ImmutableSet<BidModifierAnyFieldEnum> fields = ImmutableSet.of(BID_MODIFIER_ID, BID_MODIFIER_CAMPAIGN_ID,
                BID_MODIFIER_AD_GROUP_ID, BID_MODIFIER_LEVEL, BID_MODIFIER_TYPE);

        criteria = new BidModifiersSelectionCriteria()
                .withCampaignIds(CAMPAIGN_ID)
                .withAdGroupIds(AD_GROUP_ID)
                .withIds(EXTERNAL_ID)
                .withTypes(BID_MODIFIER_TYPE_ENUMS)
                .withLevels(BID_MODIFIER_LEVEL_ENUMS);

        idsByType = ArrayListMultimap.create();
        idsByType.put(BidModifierType.MOBILE_MULTIPLIER, INTERNAL_ID);

        request = new GenericGetRequest<>(fields, criteria, LIMIT_OFFSET);
    }

    @Test
    public void get_ByIds() {
        getBidModifiersDelegate.get(request);

        Set<BidModifierType> allowedBidModifierTypes = getBidModifiersDelegate.getAllowedBidModifierTypes();
        verify(bidModifierService).getByIds(eq(CHIEF_CLIENT_ID), eq(idsByType),
                eq(new HashSet<>(criteria.getCampaignIds())),
                eq(new HashSet<>(criteria.getAdGroupIds())), eq(allowedBidModifierTypes),
                eq(ALL_LEVELS), eq(OPERATOR_UID));
    }

    @Test
    public void get_ByIdsWithEmptyTypes() {
        request.getSelectionCriteria().setTypes(null);
        getBidModifiersDelegate.get(request);

        Set<BidModifierType> allowedBidModifierTypes = getBidModifiersDelegate.getAllowedBidModifierTypes();
        verify(bidModifierService).getByIds(eq(CHIEF_CLIENT_ID), eq(idsByType),
                eq(new HashSet<>(criteria.getCampaignIds())),
                eq(new HashSet<>(criteria.getAdGroupIds())), eq(allowedBidModifierTypes),
                eq(ALL_LEVELS), eq(OPERATOR_UID));
    }

    @Test
    public void get_ByAdGroupIds() {
        criteria.setIds(null);
        getBidModifiersDelegate.get(request);

        Set<BidModifierType> allowedBidModifierTypes = getBidModifiersDelegate.getAllowedBidModifierTypes();
        verify(bidModifierService).getByAdGroupIds(eq(CHIEF_CLIENT_ID),
                eq(new HashSet<>(criteria.getAdGroupIds())), eq(new HashSet<>(criteria.getCampaignIds())),
                eq(allowedBidModifierTypes), eq(ALL_LEVELS), eq(OPERATOR_UID));
    }

    @Test
    public void get_ByCampaignIds() {
        criteria.setIds(null);
        criteria.setAdGroupIds(null);
        getBidModifiersDelegate.get(request);

        Set<BidModifierType> allowedBidModifierTypes = getBidModifiersDelegate.getAllowedBidModifierTypes();
        verify(bidModifierService).getByCampaignIds(eq(CHIEF_CLIENT_ID), eq(new HashSet<>(criteria.getCampaignIds())),
                eq(allowedBidModifierTypes), eq(ALL_LEVELS), eq(OPERATOR_UID));
    }

    @Test
    public void get_CpmVideoAdGroup_AdGroupId_ReturnResult() {
        criteria.setIds(null);
        criteria.setCampaignIds(null);

        List<BidModifier> bidModifiers = new ArrayList<>();
        bidModifiers.add(new BidModifierMobile()
                .withAdGroupId(AD_GROUP_ID)
                .withId(INTERNAL_ID)
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withEnabled(true)
                .withMobileAdjustment(new BidModifierMobileAdjustment().withId(BID_MODIFIER_MOBILE_ADJUSTMENT_ID)));

        when(bidModifierService.getByAdGroupIds(eq(CHIEF_CLIENT_ID), eq(ImmutableSet.of(AD_GROUP_ID)),
                eq(ImmutableSet.of()), any(), any(), eq(OPERATOR_UID)))
                .thenReturn(bidModifiers);

        when(adGroupService.getAdGroupTypes(eq(CHIEF_CLIENT_ID), eq(ImmutableList.of(AD_GROUP_ID))))
                .thenReturn(ImmutableMap.of(AD_GROUP_ID, AdGroupType.CPM_VIDEO));

        request = new GenericGetRequest<>(request.getRequestedFields(), request.getSelectionCriteria(),
                new LimitOffset(1, 0));

        assertThat(getBidModifiersDelegate.get(request)).hasSize(1);
    }

    @Test
    public void get_CpmVideoAdGroup_BidModifierId_ReturnResult() {
        criteria.setAdGroupIds(null);
        criteria.setCampaignIds(null);

        List<BidModifier> bidModifiers = new ArrayList<>();
        bidModifiers.add(new BidModifierMobile()
                .withAdGroupId(AD_GROUP_ID)
                .withId(INTERNAL_ID)
                .withType(BidModifierType.MOBILE_MULTIPLIER)
                .withEnabled(true)
                .withMobileAdjustment(new BidModifierMobileAdjustment().withId(BID_MODIFIER_MOBILE_ADJUSTMENT_ID)));

        when(bidModifierService.getByIds(eq(CHIEF_CLIENT_ID), any(), eq(ImmutableSet.of()), eq(ImmutableSet.of()),
                any(), any(), eq(OPERATOR_UID)))
                .thenReturn(bidModifiers);

        when(adGroupService.getAdGroupTypes(eq(CHIEF_CLIENT_ID), eq(ImmutableList.of(AD_GROUP_ID))))
                .thenReturn(ImmutableMap.of(AD_GROUP_ID, AdGroupType.CPM_VIDEO));

        request = new GenericGetRequest<>(request.getRequestedFields(), request.getSelectionCriteria(),
                new LimitOffset(1, 0));

        assertThat(getBidModifiersDelegate.get(request)).hasSize(1);
    }
}
