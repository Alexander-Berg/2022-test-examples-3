package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDesktop;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierRetargeting;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierVideo;
import ru.yandex.direct.core.entity.bidmodifier.OsType;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupBidModifiers;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierMobile;
import static ru.yandex.direct.core.testing.data.TestBidModifiers.createDefaultBidModifierRetargeting;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.randomPercentDesktopBidModifier;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.randomPercentIosMobileBidModifier;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.randomPercentMobileBidModifier;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.randomPercentVideoBidModifier;
import static ru.yandex.direct.web.testing.data.TestBidModifiers.singleRandomPercentRetargetingBidModifier;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateBidModifierTest extends TextAdGroupControllerTestBase {

    private AdGroupInfo adGroupInfo;
    private long adGroupId;

    @Before
    public void before() {
        super.before();

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        steps.retargetingSteps().createRetargeting(defaultRetargeting(), adGroupInfo, retConditionInfo);

        adGroupId = adGroupInfo.getAdGroupId();
    }

    @Test
    public void addedRetargetingBidModifier() {
        WebAdGroupBidModifiers requestBidModifiers = new WebAdGroupBidModifiers()
                .withRetargetingBidModifier(singleRandomPercentRetargetingBidModifier(retCondId));
        updateAdGroupWithBidModifiers(requestBidModifiers);

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должна быть 1 корректировка", actualBidModifiers, hasSize(1));

        BidModifierRetargeting bidModifier =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierRetargeting.class);
        assertThat("добавленная корректировка должна быть привязана к группе",
                bidModifier.getAdGroupId(), equalTo(adGroupId));
        Integer expectedPercent = requestBidModifiers.getRetargetingBidModifier()
                .getAdjustments().get(String.valueOf(retCondId)).getPercent();
        assertThat("данные корректировки не соответствуют ожидаемым",
                bidModifier.getRetargetingAdjustments().get(0).getPercent(),
                equalTo(expectedPercent));
    }

    @Test
    public void updatedRetargetingBidModifier() {
        createRetargetingBidModifier();

        WebAdGroupBidModifiers requestBidModifiers = new WebAdGroupBidModifiers()
                .withRetargetingBidModifier(singleRandomPercentRetargetingBidModifier(retCondId));
        updateAdGroupWithBidModifiers(requestBidModifiers);

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должна быть 1 корректировка", actualBidModifiers, hasSize(1));

        BidModifierRetargeting bidModifier =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierRetargeting.class);
        assertThat("обновленная корректировка должна быть привязана к группе",
                bidModifier.getAdGroupId(), equalTo(adGroupId));
        Integer expectedPercent = requestBidModifiers.getRetargetingBidModifier()
                .getAdjustments().get(String.valueOf(retCondId)).getPercent();
        assertThat("данные корректировки не соответствуют ожидаемым",
                bidModifier.getRetargetingAdjustments().get(0).getPercent(),
                equalTo(expectedPercent));
    }

    @Test
    public void deletedRetargetingBidModifier() {
        createRetargetingBidModifier();

        updateAdGroupWithBidModifiers(null);

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("корректировок быть не должно", actualBidModifiers, emptyIterable());
    }

    @Test
    public void addedRetargetingBidModifierAndUpdatedMobileBidModifier() {
        createMobileBidModifier();

        WebAdGroupBidModifiers requestBidModifiers = new WebAdGroupBidModifiers()
                .withRetargetingBidModifier(singleRandomPercentRetargetingBidModifier(retCondId))
                .withMobileBidModifier(randomPercentMobileBidModifier());
        updateAdGroupWithBidModifiers(requestBidModifiers);

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должно быть 2 корректировки", actualBidModifiers, hasSize(2));

        BidModifierRetargeting addedBidModifier =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierRetargeting.class);
        assertThat("добавленная корректировка должна быть привязана к группе",
                addedBidModifier.getAdGroupId(), equalTo(adGroupId));
        Integer expectedPercentForAdded = requestBidModifiers.getRetargetingBidModifier()
                .getAdjustments().get(String.valueOf(retCondId)).getPercent();
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                addedBidModifier.getRetargetingAdjustments().get(0).getPercent(),
                equalTo(expectedPercentForAdded));

        BidModifierMobile updatedBidModifier =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierMobile.class);
        assertThat("обновленная корректировка должна быть привязана к группе",
                updatedBidModifier.getAdGroupId(), equalTo(adGroupId));
        Integer expectedPercentForUpdated = requestBidModifiers.getMobileBidModifier().getPercent();
        assertThat("данные корректировки не соответствуют ожидаемым",
                updatedBidModifier.getMobileAdjustment().getPercent(),
                equalTo(expectedPercentForUpdated));
    }

    @Test
    public void addedMobileBidModifierAndDeletedRetargetingBidModifier() {
        createRetargetingBidModifier();

        WebAdGroupBidModifiers requestBidModifiers = new WebAdGroupBidModifiers()
                .withMobileBidModifier(randomPercentMobileBidModifier());
        updateAdGroupWithBidModifiers(requestBidModifiers);

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должна быть 1 корректировка", actualBidModifiers, hasSize(1));

        BidModifierMobile bidModifier =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierMobile.class);
        assertThat("обновленная корректировка должна быть привязана к группе",
                bidModifier.getAdGroupId(), equalTo(adGroupId));
        Integer expectedPercent = requestBidModifiers.getMobileBidModifier().getPercent();
        assertThat("данные корректировки не соответствуют ожидаемым",
                bidModifier.getMobileAdjustment().getPercent(),
                equalTo(expectedPercent));
    }

    @Test
    public void updatedMobileBidModifierAndDeletedRetargetingBidModifier() {
        createRetargetingBidModifier();
        createMobileBidModifier(1);

        WebAdGroupBidModifiers requestBidModifiers = new WebAdGroupBidModifiers()
                .withMobileBidModifier(randomPercentMobileBidModifier());
        updateAdGroupWithBidModifiers(requestBidModifiers);

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должна быть 1 корректировка", actualBidModifiers, hasSize(1));

        BidModifierMobile bidModifier =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierMobile.class);
        assertThat("обновленная корректировка должна быть привязана к группе",
                bidModifier.getAdGroupId(), equalTo(adGroupId));
        Integer expectedPercent = requestBidModifiers.getMobileBidModifier().getPercent();
        assertThat("данные корректировки не соответствуют ожидаемым",
                bidModifier.getMobileAdjustment().getPercent(),
                equalTo(expectedPercent));
    }

    @Test
    public void updatedMobileBidModifierDeletedRetargetingBidModifierAndAddDesktop() {

        createRetargetingBidModifier();
        createMobileBidModifier(1);

        WebAdGroupBidModifiers requestBidModifiers = new WebAdGroupBidModifiers()
                .withMobileBidModifier(randomPercentIosMobileBidModifier())
                .withDesktopBidModifier(randomPercentDesktopBidModifier());
        updateAdGroupWithBidModifiers(requestBidModifiers);

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должна быть 2 корректировки", actualBidModifiers, hasSize(2));

        BidModifierMobile bidModifier =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierMobile.class);
        assertThat("обновленная корректировка должна быть привязана к группе",
                bidModifier.getAdGroupId(), equalTo(adGroupId));
        Integer expectedPercent = requestBidModifiers.getMobileBidModifier().getPercent();
        assertThat("OsType корректировки должны соответствовать ожидаемым",
                bidModifier.getMobileAdjustment().getOsType(),
                equalTo(OsType.IOS));
        assertThat("Процент корректировки должны соответствовать ожидаемым",
                bidModifier.getMobileAdjustment().getPercent(),
                equalTo(expectedPercent));

        BidModifierDesktop modifierDesktop =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierDesktop.class);
        assertThat("обновленная корректировка должна быть привязана к группе",
                modifierDesktop.getAdGroupId(), equalTo(adGroupId));
        assertThat("Процент корректировки должны соответствовать ожидаемым",
                modifierDesktop.getDesktopAdjustment().getPercent(),
                equalTo(requestBidModifiers.getDesktopBidModifier().getPercent()));
    }

    @Test
    public void addedVideoBidModifierAndUpdatedMobileBidModifierAndDeletedRetargetingBidModifier() {
        createRetargetingBidModifier();
        createMobileBidModifier(1);

        WebAdGroupBidModifiers requestBidModifiers = new WebAdGroupBidModifiers()
                .withMobileBidModifier(randomPercentMobileBidModifier())
                .withVideoBidModifier(randomPercentVideoBidModifier());
        updateAdGroupWithBidModifiers(requestBidModifiers);

        List<BidModifier> actualBidModifiers = findBidModifiers();
        assertThat("должно быть 2 корректировки", actualBidModifiers, hasSize(2));

        BidModifierMobile updatedBidModifier =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierMobile.class);
        assertThat("обновленная корректировка должна быть привязана к группе",
                updatedBidModifier.getAdGroupId(), equalTo(adGroupId));
        Integer expectedPercentForUpdated = requestBidModifiers.getMobileBidModifier().getPercent();
        assertThat("данные обновленной корректировки не соответствуют ожидаемым",
                updatedBidModifier.getMobileAdjustment().getPercent(),
                equalTo(expectedPercentForUpdated));

        BidModifierVideo addedBidModifier =
                extractOnlyOneBidModifierOfType(actualBidModifiers, BidModifierVideo.class);
        assertThat("добавленная корректировка должна быть привязана к группе",
                addedBidModifier.getAdGroupId(), equalTo(adGroupId));
        Integer expectedPercentForAdded = requestBidModifiers.getVideoBidModifier().getPercent();
        assertThat("данные добавленной корректировки не соответствуют ожидаемым",
                addedBidModifier.getVideoAdjustment().getPercent(),
                equalTo(expectedPercentForAdded));
    }

    private void updateAdGroupWithBidModifiers(WebAdGroupBidModifiers bidModifiers) {
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null)
                .withBidModifiers(bidModifiers);

        updateAndCheckResult(requestAdGroup);

        List<AdGroup> actualAdGroups = findAdGroups();
        assertThat("группа не обновлена",
                actualAdGroups.get(0).getName(),
                equalTo(requestAdGroup.getName()));
    }

    private void createRetargetingBidModifier() {
        BidModifierRetargeting bidModifierRetargeting =
                createDefaultBidModifierRetargeting(campaignInfo.getCampaignId(), adGroupId, retCondId);
        steps.bidModifierSteps().createAdGroupBidModifier(bidModifierRetargeting, adGroupInfo);

        List<BidModifier> initialBidModifiers = findBidModifiers();
        assumeThat("количество исходных корректировок перед тестов не соответствует ожидаемому",
                initialBidModifiers, hasSize(1));

        BidModifierRetargeting bidModifier =
                extractOnlyOneBidModifierOfType(initialBidModifiers, BidModifierRetargeting.class);
        assumeThat("перед тестом корректировка должна быть привязана к группе",
                bidModifier.getAdGroupId(), equalTo(adGroupId));
    }

    private void createMobileBidModifier() {
        createMobileBidModifier(0);
    }

    private void createMobileBidModifier(int alreadyPresentBidModifiers) {
        BidModifierMobile bidModifierMobile =
                createDefaultBidModifierMobile(campaignInfo.getCampaignId());
        steps.bidModifierSteps().createAdGroupBidModifier(bidModifierMobile, adGroupInfo);

        List<BidModifier> initialBidModifiers = findBidModifiers();
        assumeThat("количество исходных корректировок перед тестов не соответствует ожидаемому",
                initialBidModifiers, hasSize(alreadyPresentBidModifiers + 1));

        BidModifierMobile bidModifier =
                extractOnlyOneBidModifierOfType(initialBidModifiers, BidModifierMobile.class);
        assumeThat("перед тестом корректировка должна быть привязана к группе",
                bidModifier.getAdGroupId(), equalTo(adGroupId));
    }
}
