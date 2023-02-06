package ru.yandex.direct.api.v5.entity.dynamictextadtargets.delegate;

import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.yandex.direct.api.v5.dynamictextadtargets.WebpageFieldEnum;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.GenericGetRequest;
import ru.yandex.direct.api.v5.entity.dynamictextadtargets.container.DynamicTextAdTargetGetContainer;
import ru.yandex.direct.api.v5.entity.dynamictextadtargets.converter.GetResponseConverter;
import ru.yandex.direct.api.v5.entity.dynamictextadtargets.validation.DynamicTextAdTargetsGetRequestValidator;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.util.PropertyFilter;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.dynamictextadtarget.container.DynamicTextAdTargetSelectionCriteria;
import ru.yandex.direct.core.entity.dynamictextadtarget.service.DynamicTextAdTargetService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.DynamicTextAdTargetInfo;
import ru.yandex.direct.core.testing.steps.DynamicTextAdTargetSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.multitype.entity.LimitOffset.maxLimited;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@Api5Test
@RunWith(SpringRunner.class)
public class GetDynamicTextAdTargetsDelegateTest {

    @Autowired
    private Steps steps;

    @Autowired
    private DynamicTextAdTargetSteps dynamicTextAdTargetSteps;

    @Autowired
    private DynamicTextAdTargetsGetRequestValidator dynamicTextAdTargetsGetRequestValidator;

    @Autowired
    private DynamicTextAdTargetService dynamicTextAdTargetService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private PropertyFilter propertyFilter;

    @Autowired
    private GetResponseConverter getResponseConverter;

    @InjectMocks
    private GetDynamicTextAdTargetsDelegate delegate;

    @Mock
    private ApiAuthenticationSource apiAuthenticationSource;

    private AdGroupInfo adGroupInfo;
    private Long operatorUid;
    private ClientId clientId;
    private DynamicTextAdTargetInfo dynamicTextAdTargetInfo;

    @Before
    public void setUp() {
        initMocks(this);

        adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup();
        operatorUid = adGroupInfo.getUid();
        clientId = adGroupInfo.getClientId();
        dynamicTextAdTargetInfo = dynamicTextAdTargetSteps.createDefaultDynamicTextAdTarget(adGroupInfo);

        apiAuthenticationSource = mock(ApiAuthenticationSource.class);
        when(apiAuthenticationSource.getOperator()).thenReturn(new ApiUser().withUid(operatorUid));
        when(apiAuthenticationSource.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientId));

        delegate =
                new GetDynamicTextAdTargetsDelegate(apiAuthenticationSource, dynamicTextAdTargetService,
                        campaignService,
                        propertyFilter, getResponseConverter, dynamicTextAdTargetsGetRequestValidator);
    }

    @Test
    public void getDynamicRetargetingWithId() {
        Long dynamicTextAdTargetId = dynamicTextAdTargetInfo.getDynamicConditionId();
        DynamicTextAdTargetSelectionCriteria selectionCriteria =
                new DynamicTextAdTargetSelectionCriteria().withConditionIds(dynamicTextAdTargetId);
        List<DynamicTextAdTargetGetContainer> dynamicTextAdTargets = delegate.get(
                new GenericGetRequest<>(ImmutableSet.of(WebpageFieldEnum.ID), selectionCriteria, maxLimited()));

        assumeThat("вернулось ожидаемое число условий", dynamicTextAdTargets.size(), Matchers.is(1));
        List<Long> dynamicTextAdTargetIds =
                dynamicTextAdTargets.stream().map(c -> c.getDynamicTextAdTarget().getDynamicConditionId())
                        .collect(toList());

        assertThat(dynamicTextAdTargetIds).contains(dynamicTextAdTargetId);
    }
}
