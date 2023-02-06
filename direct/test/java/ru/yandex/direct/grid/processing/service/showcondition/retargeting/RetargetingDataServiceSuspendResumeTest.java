package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.common.GdResult;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdResumeRetargetings;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdResumeRetargetingsPayloadItem;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdSuspendRetargetings;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdSuspendRetargetingsPayloadItem;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingDataServiceSuspendResumeTest {
    @Autowired
    private Steps steps;

    @Autowired
    private RetargetingService retargetingService;

    @Autowired
    private RetargetingDataService retargetingDataService;

    private Long retargetingId1;
    private Long retargetingId2;
    private UserInfo operatorInfo;
    private SoftAssertions softAssertions = new SoftAssertions();
    private GridGraphQLContext context;
    private AdGroupInfo adGroupInfo;
    private List<Long> retargetingIdList;

    @Before
    public void initTestData() {
        operatorInfo = steps.userSteps().createDefaultUser();

        CampaignInfo campaignInfo =
                steps.campaignSteps().createActiveCampaign(operatorInfo.getClientInfo(), CampaignsPlatform.SEARCH);
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        retargetingId1 = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo).getRetargetingId();
        retargetingId2 = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo).getRetargetingId();

        retargetingIdList = ImmutableList.of(retargetingId1, retargetingId2);

        context = ContextHelper.buildContext(operatorInfo.getUser())
                .withFetchedFieldsReslover(null);
    }

    @Test
    public void suspendRetargetings_allResumed() {
        GdSuspendRetargetings request = new GdSuspendRetargetings().withRetargetingIds(retargetingIdList);
        GdResult<GdSuspendRetargetingsPayloadItem> result =
                retargetingDataService.suspendRetargetings(context, request);

        softAssertions.assertThat(result.getSuccessCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getTotalCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getRowset()).containsOnly(
                new GdSuspendRetargetingsPayloadItem().withRetargetingId(retargetingId1),
                new GdSuspendRetargetingsPayloadItem().withRetargetingId(retargetingId2)
        );

        Map<Long, Boolean> statusMap = getRetargetingStatusMap();

        //noinspection unchecked
        softAssertions.assertThat(statusMap).containsOnly(Pair.of(retargetingId1, true), Pair.of(retargetingId2, true));
        softAssertions.assertAll();
    }

    @Test
    public void suspendRetargetings_partial() {
        retargetingService.suspendRetargetings(Collections.singletonList(retargetingId2),
                operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid());

        GdSuspendRetargetings request = new GdSuspendRetargetings().withRetargetingIds(retargetingIdList);
        GdResult<GdSuspendRetargetingsPayloadItem> result = retargetingDataService.suspendRetargetings(context,
                request);
        softAssertions.assertThat(result.getSuccessCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getTotalCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getRowset()).containsOnly(
                new GdSuspendRetargetingsPayloadItem().withRetargetingId(retargetingId1),
                new GdSuspendRetargetingsPayloadItem().withRetargetingId(retargetingId2)
        );

        Map<Long, Boolean> statusMap = getRetargetingStatusMap();

        //noinspection unchecked
        softAssertions.assertThat(statusMap).containsOnly(Pair.of(retargetingId1, true), Pair.of(retargetingId2, true));
        softAssertions.assertAll();
    }

    @Test
    public void resumeRetargetings_allSuspended() {
        retargetingService.suspendRetargetings(retargetingIdList,
                operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid());
        GdResumeRetargetings request = new GdResumeRetargetings().withRetargetingIds(retargetingIdList);
        GdResult<GdResumeRetargetingsPayloadItem> result = retargetingDataService.resumeRetargetings(context, request);

        softAssertions.assertThat(result.getSuccessCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getTotalCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getRowset()).containsOnly(
                new GdResumeRetargetingsPayloadItem().withRetargetingId(retargetingId1),
                new GdResumeRetargetingsPayloadItem().withRetargetingId(retargetingId2)
        );

        Map<Long, Boolean> statusMap = getRetargetingStatusMap();

        //noinspection unchecked
        softAssertions.assertThat(statusMap).containsOnly(Pair.of(retargetingId1, false), Pair.of(retargetingId2,
                false));
        softAssertions.assertAll();
    }

    @Test
    public void resumeRetargetings_partial() {
        retargetingService.suspendRetargetings(Collections.singletonList(retargetingId2),
                operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid());

        GdResumeRetargetings request = new GdResumeRetargetings().withRetargetingIds(retargetingIdList);
        GdResult<GdResumeRetargetingsPayloadItem> result =
                retargetingDataService.resumeRetargetings(context, request);

        softAssertions.assertThat(result.getSuccessCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getTotalCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getRowset()).containsOnly(
                new GdResumeRetargetingsPayloadItem().withRetargetingId(retargetingId1),
                new GdResumeRetargetingsPayloadItem().withRetargetingId(retargetingId2)
        );

        Map<Long, Boolean> statusMap = getRetargetingStatusMap();

        //noinspection unchecked
        softAssertions.assertThat(statusMap).containsOnly(Pair.of(retargetingId1, false), Pair.of(retargetingId2,
                false));
        softAssertions.assertAll();
    }

    private Map<Long, Boolean> getRetargetingStatusMap() {
        List<TargetInterest> retargetings =
                retargetingService.getRetargetings(new RetargetingSelection().withAdGroupIds(
                        Collections.singletonList(adGroupInfo.getAdGroupId())),
                        operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid(),
                        LimitOffset.maxLimited());
        return StreamEx.of(retargetings)
                .mapToEntry(TargetInterest::getId, TargetInterest::getIsSuspended)
                .toMap();
    }

}
