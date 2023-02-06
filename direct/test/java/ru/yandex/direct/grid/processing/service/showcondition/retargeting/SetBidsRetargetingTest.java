package ru.yandex.direct.grid.processing.service.showcondition.retargeting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignsPlatform;
import ru.yandex.direct.core.entity.retargeting.container.RetargetingSelection;
import ru.yandex.direct.core.entity.retargeting.model.TargetInterest;
import ru.yandex.direct.core.entity.retargeting.service.RetargetingService;
import ru.yandex.direct.core.testing.data.TestRetargetings;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSetBids;
import ru.yandex.direct.grid.processing.model.showcondition.mutation.GdSetBidsPayload;
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider;
import ru.yandex.direct.grid.processing.service.showcondition.bids.BidsDataService;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SetBidsRetargetingTest {
    private static final BigDecimal PRICE_CONTEXT = TestRetargetings.PRICE_CONTEXT.add(TestRetargetings.PRICE_CONTEXT);
    private static final int AUTOBUDGET_PRIORITY = TestRetargetings.AUTOBUDGET_PRIORITY == 3 ? 1 : 3;
    private static final Long UNEXISTING_RETARGETING_ID = Long.MAX_VALUE - 1;

    @Autowired
    private Steps steps;

    @Autowired
    private RetargetingService retargetingService;

    @Autowired
    private BidsDataService bidsDataService;

    @Autowired
    private GridContextProvider gridContextProvider;

    private Long retargetingId1;
    private Long retargetingId2;
    private UserInfo operatorInfo;
    private SoftAssertions softAssertions = new SoftAssertions();
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

        gridContextProvider.setGridContext(ContextHelper.buildContext(operatorInfo.getUser())
                .withFetchedFieldsReslover(null));
    }

    @Test
    public void setBidsRetargeting_twoExisting() {
        List<TargetInterest> expectedRetargetings = retargetingService.getRetargetings(new RetargetingSelection()
                        .withAdGroupIds(Collections.singletonList(adGroupInfo.getAdGroupId()))
                        .withIds(retargetingIdList),
                operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid(),
                LimitOffset.maxLimited());

        GdSetBids gdSetBids = new GdSetBids().withShowConditionIds(retargetingIdList)
                .withExactPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(AUTOBUDGET_PRIORITY);

        GdSetBidsPayload result = bidsDataService.setBidsRetargeting(gdSetBids);

        softAssertions.assertThat(result.getShowConditionIds().size()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getShowConditionIds()).containsOnly(retargetingId1, retargetingId2);

        List<TargetInterest> retargetings =
                retargetingService.getRetargetings(new RetargetingSelection()
                                .withAdGroupIds(Collections.singletonList(adGroupInfo.getAdGroupId()))
                                .withIds(retargetingIdList),
                        operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid(),
                        LimitOffset.maxLimited());

        expectedRetargetings.forEach(targetInterest -> targetInterest
                .withPriceContext(PRICE_CONTEXT.setScale(2, RoundingMode.HALF_UP))
                .withAutobudgetPriority(AUTOBUDGET_PRIORITY)
                .withStatusBsSynced(StatusBsSynced.NO));

        softAssertions.assertThat(retargetings)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("lastChangeTime")
                .containsExactlyInAnyOrderElementsOf(expectedRetargetings);
        softAssertions.assertAll();
    }

    @Test
    public void setBidsRetargeting_oneExistsAndOneNot() {
        retargetingIdList = Collections.singletonList(retargetingId1);
        List<TargetInterest> expectedRetargetings = retargetingService.getRetargetings(new RetargetingSelection()
                        .withAdGroupIds(Collections.singletonList(adGroupInfo.getAdGroupId()))
                        .withIds(retargetingIdList),
                operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid(),
                LimitOffset.maxLimited());

        GdSetBids gdSetBids = new GdSetBids().withShowConditionIds(ImmutableList.of(retargetingId1,
                        UNEXISTING_RETARGETING_ID))
                .withExactPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(AUTOBUDGET_PRIORITY);

        GdSetBidsPayload result = bidsDataService.setBidsRetargeting(gdSetBids);

        softAssertions.assertThat(result.getShowConditionIds().size()).isEqualTo(retargetingIdList.size());
        softAssertions.assertThat(result.getShowConditionIds()).containsOnly(retargetingId1);

        List<TargetInterest> retargetings =
                retargetingService.getRetargetings(new RetargetingSelection()
                                .withAdGroupIds(Collections.singletonList(adGroupInfo.getAdGroupId()))
                                .withIds(retargetingIdList),
                        operatorInfo.getClientInfo().getClientId(), operatorInfo.getUid(),
                        LimitOffset.maxLimited());

        expectedRetargetings.forEach(targetInterest -> targetInterest
                .withPriceContext(PRICE_CONTEXT.setScale(2, RoundingMode.HALF_UP))
                .withAutobudgetPriority(AUTOBUDGET_PRIORITY)
                .withStatusBsSynced(StatusBsSynced.NO));

        softAssertions.assertThat(retargetings)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("lastChangeTime")
                .containsExactlyInAnyOrderElementsOf(expectedRetargetings);
        softAssertions.assertAll();
    }

}
