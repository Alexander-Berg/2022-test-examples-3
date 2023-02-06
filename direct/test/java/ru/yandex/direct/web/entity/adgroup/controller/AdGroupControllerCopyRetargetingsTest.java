package ru.yandex.direct.web.entity.adgroup.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.retargeting.model.Retargeting;
import ru.yandex.direct.core.testing.data.TestRetargetings;
import ru.yandex.direct.core.testing.info.RetConditionInfo;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.web.testing.data.TestAdGroups.adGroupRetargeting;
import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;

@DirectWebTest
@RunWith(SpringRunner.class)
public class AdGroupControllerCopyRetargetingsTest extends AdGroupControllerCopyTestBase {

    @Test
    public void retargetingPriceIsCopied() {
        Long retargetingId = createAdGroupWithRetargeting();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withRetargetings(singletonList(adGroupRetargeting(retargetingId, retCondId)));

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Retargeting> retargetingsCopies =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroupCopiesIds.get(0)));
        assumeThat("количество скопированных ретаргетингов не соответствует ожидаемому",
                retargetingsCopies, hasSize(1));

        Retargeting expectedRetargeting = new Retargeting()
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(PRIORITY);
        Retargeting retargetingCopy = retargetingsCopies.get(0);
        assertThat(retargetingCopy, beanDiffer(expectedRetargeting).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void oldRetargetingPriceIsCopiedAndNewRetargetingPriceIsCopiedFromOldRetargeting() {
        Long retargetingId = createAdGroupWithRetargeting();

        Long retCondId2 = createRetCondition().getRetConditionId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withRetargetings(Arrays.asList(
                        adGroupRetargeting(retargetingId, retCondId),
                        adGroupRetargeting(null, retCondId2)));

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Retargeting> retargetingsCopies =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroupCopiesIds.get(0)));
        assumeThat("количество скопированных ретаргетингов не соответствует ожидаемому",
                retargetingsCopies, hasSize(2));

        Retargeting expectedRetargeting = new Retargeting()
                .withPriceContext(PRICE_CONTEXT)
                .withAutobudgetPriority(PRIORITY);
        assertThat(retargetingsCopies.get(0),
                beanDiffer(expectedRetargeting).useCompareStrategy(onlyExpectedFields()));
        assertThat(retargetingsCopies.get(1),
                beanDiffer(expectedRetargeting).useCompareStrategy(onlyExpectedFields()));
    }

    @Test
    public void generalPriceIsSetForCopiedAndNewRetargetingsAndPriorityIsSetWell() {
        Long retargetingId = createAdGroupWithRetargeting();

        Long retCondId2 = createRetCondition().getRetConditionId();

        WebTextAdGroup requestAdGroup = randomNameWebAdGroup(adGroupId, campaignInfo.getCampaignId())
                .withRetargetings(Arrays.asList(
                        adGroupRetargeting(retargetingId, retCondId),
                        adGroupRetargeting(null, retCondId2)))
                .withGeneralPrice(GENERAL_PRICE_DOUBLE);

        List<Long> adGroupCopiesIds = copyAndCheckResult(singletonList(requestAdGroup));

        List<Retargeting> retargetingsCopies =
                retargetingRepository.getRetargetingsByAdGroups(shard, singletonList(adGroupCopiesIds.get(0)));
        assumeThat("количество скопированных ретаргетингов не соответствует ожидаемому",
                retargetingsCopies, hasSize(2));

        Retargeting expectedRetargeting = new Retargeting()
                .withPriceContext(GENERAL_PRICE)
                .withAutobudgetPriority(PRIORITY);
        assertThat(retargetingsCopies.get(0),
                beanDiffer(expectedRetargeting).useCompareStrategy(onlyExpectedFields()));
        assertThat(retargetingsCopies.get(1),
                beanDiffer(expectedRetargeting).useCompareStrategy(onlyExpectedFields()));
    }

    private Long createAdGroupWithRetargeting() {
        createAdGroup();
        return addRetargetingToAdGroup(retConditionInfo, PRICE_CONTEXT, PRIORITY);
    }

    private Long addRetargetingToAdGroup(RetConditionInfo retConditionInfo, BigDecimal priceContext, int priority) {
        Retargeting retargeting = TestRetargetings
                .defaultRetargeting()
                .withPriceContext(priceContext)
                .withAutobudgetPriority(priority);
        return steps.retargetingSteps().createRetargeting(retargeting, adGroupInfo, retConditionInfo)
                .getRetargetingId();
    }

    private RetConditionInfo createRetCondition() {
        return steps.retConditionSteps().createBigRetCondition(campaignInfo.getClientInfo());
    }
}
