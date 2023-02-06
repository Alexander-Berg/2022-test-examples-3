package ru.yandex.direct.api.v5.entity.adextensions.delegate;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.adextensions.container.GetFieldName;
import ru.yandex.direct.api.v5.entity.adextensions.container.InternalGetResponse;
import ru.yandex.direct.api.v5.entity.adextensions.converter.GetRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.addition.callout.container.CalloutSelection;
import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.service.CalloutService;
import ru.yandex.direct.core.entity.banner.type.banneradditions.BannerAdditionsRepository;
import ru.yandex.direct.core.entity.client.service.checker.ClientAccessCheckerTypeSupportFacade;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.entity.moderationreason.service.ModerationReasonService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.isEmptyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class GetAdExtensionsDelegateGetMethodTest {
    public static final int SHARD = 13;
    private static final ClientId CLIENT_ID = ClientId.fromLong(1111L);
    private static final long CALLOUT_ID = 77L;
    private static final String CALLOUT_FIELD = "callout";
    private static final String ID_FIELD = "id";
    private static final String ASSOCIATED_FIELD = "associated";
    private static final String STATUS_CLARIFICATION_FIELD = "statusClarification";
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    private GetAdExtensionsDelegate getDelegate;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ApiAuthenticationSource authenticationSource;
    @Mock
    private CalloutService calloutService;
    @Mock
    private ShardHelper shardHelper;
    @Mock
    private BannerAdditionsRepository bannerAdditionsRepository;
    @Mock
    private ModerationReasonService moderationReasonService;
    @Mock
    private ClientAccessCheckerTypeSupportFacade clientAccessCheckerTypeSupportFacade;

    @Mock
    private GenericGetRequest<GetFieldName, CalloutSelection> req;

    private LimitOffset limitOffset;
    private CalloutSelection calloutSelection;
    private Set<GetFieldName> fieldNames;

    @Before
    public void setUp() throws Exception {
        when(authenticationSource.getSubclient().getClientId()).thenReturn(CLIENT_ID);
        when(calloutService.getCallouts(eq(CLIENT_ID), any(CalloutSelection.class), any(LimitOffset.class)))
                .thenReturn(singletonList(new Callout().withId(CALLOUT_ID)));

        when(shardHelper.getShardByClientIdStrictly(eq(CLIENT_ID))).thenReturn(SHARD);
        when(bannerAdditionsRepository.getLinkedBannersAdditions(eq(SHARD), any()))
                .thenReturn(singleton(CALLOUT_ID));
        when(clientAccessCheckerTypeSupportFacade.sendToCheck(eq(Map.of(Callout.class, singletonList(CALLOUT_ID))),
                eq(CLIENT_ID)))
                .thenReturn(Map.of(Callout.class, singleton(CALLOUT_ID)));

        when(moderationReasonService
                .getRejectReasonDiags(eq(CLIENT_ID), same(ModerationReasonObjectType.CALLOUT), any()))
                .thenReturn(Collections.emptyMap());
        getDelegate = new GetAdExtensionsDelegate(calloutService,
                shardHelper, bannerAdditionsRepository,
                moderationReasonService, authenticationSource,
                new PropertyFilter(), mock(GetRequestConverter.class), clientAccessCheckerTypeSupportFacade);

        limitOffset = LimitOffset.limited(1);
        calloutSelection = new CalloutSelection();
        fieldNames = new HashSet<>();

        when(req.getRequestedFields()).thenReturn(fieldNames);
        when(req.getSelectionCriteria()).thenReturn(calloutSelection);
        when(req.getLimitOffset()).thenReturn(limitOffset);
    }

    @Test
    public void get_AllFields_CheckCall() throws Exception {
        fieldNames.addAll(EnumSet.allOf(GetFieldName.class));
        getDelegate.get(req);

        verify(calloutService).getCallouts(eq(CLIENT_ID), same(calloutSelection), same(limitOffset));
        verify(bannerAdditionsRepository).getLinkedBannersAdditions(eq(SHARD), eq(singleton(CALLOUT_ID)));
        verify(moderationReasonService).getRejectReasonDiags(eq(CLIENT_ID), same(ModerationReasonObjectType.CALLOUT),
                eq(singleton(CALLOUT_ID)));
    }

    @Test
    public void get_IdField_CheckCall() throws Exception {
        fieldNames.add(GetFieldName.ID);
        getDelegate.get(req);

        verify(calloutService).getCallouts(eq(CLIENT_ID), same(calloutSelection), same(limitOffset));
        verify(bannerAdditionsRepository, never()).getLinkedBannersAdditions(eq(SHARD), eq(singletonList(CALLOUT_ID)));
        verify(moderationReasonService, never())
                .getRejectReasonDiags(eq(CLIENT_ID), same(ModerationReasonObjectType.CALLOUT),
                        anyCollection());
    }

    @Test
    public void get_IdField_CheckFieldValue() throws Exception {
        fieldNames.add(GetFieldName.ID);
        List<InternalGetResponse> result = getDelegate.get(req);
        assertThat(result, contains(hasProperty(CALLOUT_FIELD, hasProperty(ID_FIELD, equalTo(CALLOUT_ID)))));
    }

    @Test
    public void get_AssociatedField_CheckCall() throws Exception {
        fieldNames.add(GetFieldName.ASSOCIATED);
        getDelegate.get(req);

        verify(calloutService).getCallouts(eq(CLIENT_ID), same(calloutSelection), same(limitOffset));
        verify(bannerAdditionsRepository).getLinkedBannersAdditions(eq(SHARD), eq(singleton(CALLOUT_ID)));
        verify(moderationReasonService, never())
                .getRejectReasonDiags(eq(CLIENT_ID), same(ModerationReasonObjectType.CALLOUT),
                        anyCollection());
    }

    @Test
    public void get_AssociatedFieldAndUsed_CheckFieldValue() throws Exception {
        fieldNames.add(GetFieldName.ASSOCIATED);
        List<InternalGetResponse> result = getDelegate.get(req);
        assertThat(result, contains(hasProperty(ASSOCIATED_FIELD, equalTo(true))));
    }

    @Test
    public void get_AssociatedFieldAndUnused_CheckFieldValue() throws Exception {
        fieldNames.add(GetFieldName.ASSOCIATED);
        when(bannerAdditionsRepository.getLinkedBannersAdditions(eq(SHARD), any()))
                .thenReturn(emptySet());
        List<InternalGetResponse> result = getDelegate.get(req);
        assertThat(result, contains(hasProperty(ASSOCIATED_FIELD, equalTo(false))));
    }

    @Test
    public void get_StatusClarificationField_CheckCall() throws Exception {
        fieldNames.add(GetFieldName.STATUS_CLARIFICATION);
        getDelegate.get(req);

        verify(calloutService).getCallouts(eq(CLIENT_ID), same(calloutSelection), same(limitOffset));
        verify(bannerAdditionsRepository, never()).getLinkedBannersAdditions(eq(SHARD), eq(singletonList(CALLOUT_ID)));
        verify(moderationReasonService).getRejectReasonDiags(eq(CLIENT_ID), same(ModerationReasonObjectType.CALLOUT),
                eq(singleton(CALLOUT_ID)));
    }

    @Test
    public void get_StatusClarificationFieldEmpty_CheckFieldValue() throws Exception {
        fieldNames.add(GetFieldName.STATUS_CLARIFICATION);
        List<InternalGetResponse> result = getDelegate.get(req);
        assertThat(result, contains(hasProperty(STATUS_CLARIFICATION_FIELD, isEmptyString())));
    }

    @Test
    public void get_StatusClarificationField_CheckFieldValue() throws Exception {
        fieldNames.add(GetFieldName.STATUS_CLARIFICATION);
        when(moderationReasonService
                .getRejectReasonDiags(eq(CLIENT_ID), same(ModerationReasonObjectType.CALLOUT), any()))
                .thenReturn(ImmutableMap.of(
                        CALLOUT_ID, asList(
                                new ModerationDiag().withDiagText("aaa"),
                                new ModerationDiag().withDiagText("bbb")
                        )
                ));
        List<InternalGetResponse> result = getDelegate.get(req);
        assertThat(result, contains(hasProperty(STATUS_CLARIFICATION_FIELD, equalTo("aaa\nbbb"))));
    }
}
