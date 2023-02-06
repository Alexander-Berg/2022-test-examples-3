package ru.yandex.direct.core.entity.dynamictextadtarget.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FeedInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DynamicFeedAdTargetRepositoryTest {
    @Autowired
    private Steps steps;
    @Autowired
    private DynamicTextAdTargetRepository dynamicTextAdTargetRepository;

    private int shard;
    private ClientId clientId;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        FeedInfo feedInfo = steps.feedSteps().createDefaultFeed(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(feedInfo);
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();
    }

    @Test
    public void getDynamicFeedAdTarget() {
        DynamicFeedAdTarget dynamicFeedAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(adGroupInfo);

        List<DynamicFeedAdTarget> actual = dynamicTextAdTargetRepository.getDynamicFeedAdTargets(
                shard, clientId, List.of(dynamicFeedAdTarget.getDynamicConditionId()));

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath("price"), newPath("priceContext")).useDiffer(new BigDecimalDiffer());

        assertThat(actual, hasSize(1));
        assertThat(actual.get(0), beanDiffer(dynamicFeedAdTarget).useCompareStrategy(compareStrategy));
    }

    @Test
    public void getDynamicFeedAdTargetWithLimitOffset() {
        DynamicFeedAdTarget dynamicFeedAdTarget = steps.dynamicTextAdTargetsSteps()
                .createDefaultDynamicFeedAdTarget(adGroupInfo);

        List<DynamicFeedAdTarget> actual = dynamicTextAdTargetRepository.getDynamicFeedAdTargets(
                shard, clientId, List.of(dynamicFeedAdTarget.getDynamicConditionId()), true, LimitOffset.maxLimited());

        CompareStrategy compareStrategy = DefaultCompareStrategies.onlyExpectedFields()
                .forFields(newPath("price"), newPath("priceContext")).useDiffer(new BigDecimalDiffer());

        assertThat(actual, hasSize(1));
        assertThat(actual.get(0), beanDiffer(dynamicFeedAdTarget).useCompareStrategy(compareStrategy));
    }
}
