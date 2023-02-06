package ru.yandex.direct.core.entity.adgroup.service.validation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.PerformanceAdGroup;
import ru.yandex.direct.core.entity.adgroup.service.AdGroupService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.PerformanceAdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupServicePerformanceTest {
    @Autowired
    private AdGroupService service;
    @Autowired
    private Steps steps;

    @Test
    public void getAdGroups_success() {
        //Создаём группу
        PerformanceAdGroupInfo performanceAdGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup();
        Long adGroupId = performanceAdGroupInfo.getAdGroupId();
        ClientId clientId = performanceAdGroupInfo.getClientId();
        Long campaignId = performanceAdGroupInfo.getCampaignId();
        Long feedId = performanceAdGroupInfo.getFeedId();

        //Ожидаемое состояние группы
        PerformanceAdGroup defaultGroup = defaultPerformanceAdGroup(campaignId, feedId);

        //Получаем реальное значение из базы
        AdGroup actual = service.getAdGroups(clientId, singletonList(adGroupId))
                .get(0);

        //Сверяем ожидания и реальность
        assertThat(actual)
                .is(matchedBy(beanDiffer(defaultGroup)
                        .useCompareStrategy(onlyExpectedFields())));
    }
}
