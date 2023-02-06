package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
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
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdDeleteRetargetings;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdDeleteRetargetingsPayloadItem;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;

import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RetargetingDataServiceDeleteTest {
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
    public void suspendRetargetings_deleteOneOfTwo() {
        List<Long> retargetingIdsToDelete = Collections.singletonList(retargetingId1);
        List<Long> retargetingIdsToKeep = new ArrayList<>(retargetingIdList);
        retargetingIdsToKeep.removeAll(retargetingIdsToDelete);

        GdDeleteRetargetings request =
                new GdDeleteRetargetings().withRetargetingIds(retargetingIdsToDelete);
        GdResult<GdDeleteRetargetingsPayloadItem> result = retargetingDataService.deleteRetargetings(context, request);

        softAssertions.assertThat(result.getSuccessCount()).isEqualTo(retargetingIdsToDelete.size());
        softAssertions.assertThat(result.getTotalCount()).isEqualTo(retargetingIdsToDelete.size());
        softAssertions.assertThat(result.getRowset()).hasSameElementsAs(mapList(retargetingIdsToDelete,
                id -> new GdDeleteRetargetingsPayloadItem().withRetargetingId(id)));

        List<TargetInterest> retargetings =
                retargetingService.getRetargetings(new RetargetingSelection()
                                .withAdGroupIds(Collections.singletonList(adGroupInfo.getAdGroupId())),
                        operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid(),
                        LimitOffset.maxLimited());
        List<Long> actualRetargetingsIds = mapList(retargetings, TargetInterest::getId);

        softAssertions.assertThat(actualRetargetingsIds).hasSameElementsAs(retargetingIdsToKeep);

        softAssertions.assertAll();
    }

    @Test
    public void suspendRetargetings_deleteAll() {
        GdDeleteRetargetings request =
                new GdDeleteRetargetings().withRetargetingIds(retargetingIdList);
        GdResult<GdDeleteRetargetingsPayloadItem> result = retargetingDataService.deleteRetargetings(context, request);

        softAssertions.assertThat(result.getSuccessCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getTotalCount()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getRowset()).hasSameElementsAs(mapList(retargetingIdList,
                id -> new GdDeleteRetargetingsPayloadItem().withRetargetingId(id)));

        List<TargetInterest> retargetings =
                retargetingService.getRetargetings(new RetargetingSelection()
                                .withAdGroupIds(Collections.singletonList(adGroupInfo.getAdGroupId())),
                        operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid(),
                        LimitOffset.maxLimited());

        softAssertions.assertThat(retargetings).isEmpty();
        softAssertions.assertAll();
    }

}
