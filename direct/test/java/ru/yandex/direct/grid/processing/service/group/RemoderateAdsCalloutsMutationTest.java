package ru.yandex.direct.grid.processing.service.group;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.addition.callout.model.Callout;
import ru.yandex.direct.core.entity.addition.callout.model.CalloutsStatusModerate;
import ru.yandex.direct.core.entity.addition.callout.repository.CalloutRepository;
import ru.yandex.direct.core.entity.addition.converter.AdditionConverter;
import ru.yandex.direct.core.entity.addition.model.ModerateAdditions;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.moderation.repository.ModerationRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdRemoderateAdsCallouts;
import ru.yandex.direct.grid.processing.model.group.mutation.GdRemoderateAdsCalloutsPayload;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestBanners.defaultTextBanner;
import static ru.yandex.direct.grid.processing.service.group.AdGroupGraphQlService.REMODERATE_ADS_CALLOUTS_MUTATION_NAME;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class RemoderateAdsCalloutsMutationTest {

    private static final String REMODERETA_ADS_CALLOUTS_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "    validationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    processedAdGroupIds\n"
            + "    skippedAdGroupIds\n"
            + "  }\n"
            + "}";

    private static final TemplateMutation<GdRemoderateAdsCallouts, GdRemoderateAdsCalloutsPayload> REMODERATE_MUTATION =
            new TemplateMutation<>(REMODERATE_ADS_CALLOUTS_MUTATION_NAME, REMODERETA_ADS_CALLOUTS_MUTATION_TEMPLATE,
                    GdRemoderateAdsCallouts.class, GdRemoderateAdsCalloutsPayload.class);
    private static final CalloutsStatusModerate CALLOUT_STATUS_MODERATE_BEFORE_MUTATION = CalloutsStatusModerate.NO;

    private Integer shard;
    private User operator;
    private AdGroupInfo adGroup;
    private AdGroupInfo adGroupWithoutCalloutsInfo;
    private Long calloutId;
    private GdRemoderateAdsCallouts input;

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    private Steps steps;

    @Autowired
    private CalloutRepository calloutRepository;

    @Autowired
    private ModerationRepository moderationRepository;

    @Before
    public void before() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        shard = userInfo.getShard();
        operator = userInfo.getUser();
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveCampaign(userInfo.getClientInfo());
        adGroup = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        //add banner with callout
        Callout callout = steps.calloutSteps().createDefaultCallout(userInfo.getClientInfo());
        calloutId = callout.getId();
        calloutRepository.setStatusModerate(shard, singleton(calloutId), CALLOUT_STATUS_MODERATE_BEFORE_MUTATION);
        OldTextBanner textBanner = defaultTextBanner(adGroup.getCampaignId(), adGroup.getAdGroupId());
        textBanner.setCalloutIds(singletonList(calloutId));
        steps.bannerSteps().createBanner(textBanner, adGroup);

        adGroupWithoutCalloutsInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);
        input = new GdRemoderateAdsCallouts()
                .withAdGroupIds(ImmutableSet.of(adGroup.getAdGroupId(), adGroupWithoutCalloutsInfo.getAdGroupId()))
                .withModerateAccept(false);
    }


    @Test
    public void remoderateAdsCallouts_Success() {
        operator.setRole(RbacRole.SUPPORT);

        GdRemoderateAdsCalloutsPayload payload =
                graphQlTestExecutor.doMutationAndGetPayload(REMODERATE_MUTATION, input, operator);

        Callout callout = steps.calloutSteps().getCallout(shard, calloutId);
        List<ModerateAdditions> moderateAdditions =
                moderationRepository.getModerateAdditions(shard, singleton(calloutId));
        List<ModerateAdditions> expectedModerateAdditions =
                AdditionConverter.toModerateAdditions(input.getModerateAccept(), singleton(calloutId));
        GdRemoderateAdsCalloutsPayload expectedPayload = new GdRemoderateAdsCalloutsPayload()
                .withProcessedAdGroupIds(singleton(adGroup.getAdGroupId()))
                .withSkippedAdGroupIds(singleton(adGroupWithoutCalloutsInfo.getAdGroupId()))
                .withValidationResult(null);

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(payload)
                    .is(matchedBy(beanDiffer(expectedPayload)));
            softAssertions.assertThat(callout.getStatusModerate())
                    .isSameAs(CalloutsStatusModerate.READY);
            softAssertions.assertThat(moderateAdditions)
                    .is(matchedBy(beanDiffer(expectedModerateAdditions)));
        });
    }

    @Test
    public void remoderateAdsCallouts_OperatorAccessDenied() {
        operator.setRole(RbacRole.CLIENT);

        ExecutionResult executionResult = graphQlTestExecutor.doMutation(REMODERATE_MUTATION, input, operator);

        Callout callout = steps.calloutSteps().getCallout(shard, calloutId);
        List<ModerateAdditions> moderateAdditions =
                moderationRepository.getModerateAdditions(shard, singleton(calloutId));

        assertThat(executionResult.getErrors())
                .hasSize(1);

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(callout.getStatusModerate())
                    .isSameAs(CALLOUT_STATUS_MODERATE_BEFORE_MUTATION);
            softAssertions.assertThat(moderateAdditions)
                    .isEmpty();
        });
    }

}
