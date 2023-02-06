package ru.yandex.direct.jobs.freelancers.bsratingimport;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerBase;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerService;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;


@JobsTest
@ExtendWith(SpringExtension.class)
class FreelancerUpdateRatingServiceTest {

    @Autowired
    private Steps steps;

    @Autowired
    private FreelancerService freelancerService;

    @Autowired
    private FreelancerUpdateRatingService testedService;

    @Test
    void updateRatingForAll_success() {
        List<FreelancerBase> changes = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
            Long freelancerId = freelancerInfo.getFreelancerId();
            FreelancerBase change = new FreelancerBase()
                    .withId(freelancerId)
                    .withAdvQualityRank((long) i + 1)
                    .withAdvQualityRating(BigDecimal.valueOf(1000 - i * 10));
            changes.add(change);
        }

        testedService.updateRatingForAll(changes);

        List<Freelancer> actualFreelancers = freelancerService.getFreelancers(mapList(changes, FreelancerBase::getId));
        actualFreelancers.sort(Comparator.comparing(FreelancerBase::getId));

        assertThat(actualFreelancers)
                .is(matchedBy(beanDiffer(changes).useCompareStrategy(onlyExpectedFields())));
    }

    private DefaultCompareStrategy onlyExpectedFields() {
        return DefaultCompareStrategies.onlyExpectedFields()
                .forClasses(BigDecimal.class).useDiffer(new BigDecimalDiffer());
    }
}
