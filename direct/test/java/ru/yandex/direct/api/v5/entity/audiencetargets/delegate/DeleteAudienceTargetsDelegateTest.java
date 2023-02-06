package ru.yandex.direct.api.v5.entity.audiencetargets.delegate;

import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.api.v5.converter.ResultConverter;
import ru.yandex.direct.api.v5.entity.audiencetargets.converter.AudienceTargetsDeleteRequestConverter;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.validation.DefectType;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.api.v5.validation.AssertJMatcherAdaptors.hasNoDefects;

public class DeleteAudienceTargetsDelegateTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ApiAuthenticationSource auth;

    @Mock
    private RetargetingService retargetingService;

    @Mock
    private AdGroupService adGroupService;

    private DeleteAudienceTargetsDelegate delegate;

    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final Long ADGROUP_ID = 1L;
    private static final Long TARGET_INTEREST_ID = 1L;
    private static final Long OPERATOR_UID = 2L;

    @Before
    public void before() {
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(CLIENT_ID));
        when(auth.getOperator()).thenReturn(new ApiUser().withUid(OPERATOR_UID));

        when(adGroupService.getAdGroupTypes(eq(CLIENT_ID), eq(ImmutableSet.of(ADGROUP_ID))))
                .thenReturn(ImmutableMap.of(ADGROUP_ID, AdGroupType.CPM_VIDEO));

        when(retargetingService.getRetargetings(any(RetargetingSelection.class), eq(CLIENT_ID), eq(OPERATOR_UID),
                eq(LimitOffset.maxLimited())))
                .thenReturn(singletonList(new TargetInterest().withId(TARGET_INTEREST_ID).withAdGroupId(ADGROUP_ID)));

        delegate = new DeleteAudienceTargetsDelegate(
                auth,
                mock(AudienceTargetsDeleteRequestConverter.class),
                retargetingService,
                mock(ResultConverter.class),
                adGroupService,
                mock(PpcPropertiesSupport.class),
                mock(FeatureService.class)
        );
    }

    @Test
    public void validateInternalRequest_CpmVideoAdGroup_Ok() {
        ValidationResult<List<Long>, DefectType> vr = delegate.validateInternalRequest(
                singletonList(TARGET_INTEREST_ID));

        assertThat(vr).as("Тип cpm_video поддерживается в методе audiencetargets.delete").is(hasNoDefects());
    }
}
