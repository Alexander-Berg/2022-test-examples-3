package ru.yandex.direct.web.entity.adgroup.controller;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupRetargeting;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestRetargetings.defaultRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.adGroupRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerUpdateRetargetingTest extends TextAdGroupControllerTestBase {

    private RetConditionInfo retConditionInfo;
    private long retConditionId;

    private AdGroupInfo adGroupInfo;
    private long adGroupId;

    @Before
    public void before() {
        super.before();

        retConditionInfo = steps.retConditionSteps().createDefaultRetCondition(campaignInfo.getClientInfo());
        retConditionId = retConditionInfo.getRetConditionId();

        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup(campaignInfo);
        adGroupId = adGroupInfo.getAdGroupId();
    }

    @Test
    public void update_AdGroupWithAddedRetargeting_AdGroupAndRetargetingAreUpdated() {
        WebAdGroupRetargeting requestRetargeting = adGroupRetargeting(retConditionId);
        updateAdGroupWithRetargeting(requestRetargeting);

        Retargeting expectedRetargeting = new Retargeting().withRetargetingConditionId(retConditionId);
        List<Retargeting> actualRetargetings = findRetargetings(adGroupId);
        assertThat("данные добавленного ретаргетинга не соответствуют ожидаемым",
                actualRetargetings,
                contains(beanDiffer(expectedRetargeting).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void update_AdGroupWithUpdatedRetargeting_AdGroupIsUpdatedAndRetargetingIsUntouched() {
        RetargetingInfo retargetingInfo = createRetargeting(retConditionInfo);
        WebAdGroupRetargeting requestRetargeting =
                adGroupRetargeting(retargetingInfo.getRetargetingId(), null);
        updateAdGroupWithRetargeting(requestRetargeting);

        Retargeting expectedRetargeting = new Retargeting().withRetargetingConditionId(retConditionId);
        List<Retargeting> actualRetargetings = findRetargetings(adGroupId);
        assertThat("данные нетронутого ретаргетинга не соответствуют ожидаемым",
                actualRetargetings,
                contains(beanDiffer(expectedRetargeting).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void update_AdGroupWithDeletedRetargeting_AdGroupIsUpdatedAndRetargetingIsDeleted() {
        createRetargeting(retConditionInfo);

        updateAdGroupWithRetargetings(null);

        List<Retargeting> actualRetargetings = findRetargetings(adGroupId);
        assertThat("ретаргетинг должен быть удален", actualRetargetings, emptyIterable());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void update_AdGroupWithAddedAndUpdatedAndDeletedRetargeting_WorksFine() {
        RetConditionInfo retConditionInfo2 = steps.retConditionSteps()
                .createDefaultRetCondition(campaignInfo.getClientInfo());
        long retConditionId2 = retConditionInfo2.getRetConditionId();

        RetargetingInfo retargetingInfo1 = createRetargeting(retConditionInfo);
        createRetargeting(retConditionInfo2);

        WebAdGroupRetargeting requestRetargeting1 =
                adGroupRetargeting(retargetingInfo1.getRetargetingId(), null);
        WebAdGroupRetargeting requestRetargeting2 =
                adGroupRetargeting(null, retConditionId2);
        updateAdGroupWithRetargetings(asList(requestRetargeting1, requestRetargeting2));

        Retargeting expectedRetargeting1 = new Retargeting().withRetargetingConditionId(retConditionId);
        Retargeting expectedRetargeting2 = new Retargeting().withRetargetingConditionId(retConditionId2);
        List<Retargeting> actualRetargetings = findRetargetings(adGroupId);
        assertThat("данные ретаргетингов не соответствуют ожидаемым",
                actualRetargetings,
                containsInAnyOrder(
                        beanDiffer(expectedRetargeting1).useCompareStrategy(onlyExpectedFields()),
                        beanDiffer(expectedRetargeting2).useCompareStrategy(onlyExpectedFields())
                ));
    }

    private void updateAdGroupWithRetargeting(WebAdGroupRetargeting retargeting) {
        updateAdGroupWithRetargetings(singletonList(retargeting));
    }

    private void updateAdGroupWithRetargetings(List<WebAdGroupRetargeting> retargetings) {
        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, null)
                .withRetargetings(retargetings);

        updateAndCheckResult(requestAdGroup);

        List<AdGroup> actualAdGroups = findAdGroups();
        assertThat("группа не обновлена",
                actualAdGroups.get(0).getName(),
                equalTo(requestAdGroup.getName()));
    }

    private RetargetingInfo createRetargeting(RetConditionInfo retConditionInfo) {
        Retargeting retargeting = defaultRetargeting();
        RetargetingInfo retargetingInfo = new RetargetingInfo()
                .withRetargeting(retargeting)
                .withRetConditionInfo(retConditionInfo)
                .withAdGroupInfo(adGroupInfo);
        return steps.retargetingSteps().createRetargeting(retargetingInfo);
    }
}
