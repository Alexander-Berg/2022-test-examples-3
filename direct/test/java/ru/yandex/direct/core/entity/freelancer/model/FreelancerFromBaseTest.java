package ru.yandex.direct.core.entity.freelancer.model;

import org.junit.Test;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class FreelancerFromBaseTest {

    @Test
    public void fromBaseModel_success() {
        FreelancerBase base = new FreelancerBase()
                .withId(1L)
                .withIsSearchable(false)
                .withStatus(FreelancerStatus.FREE)
                .withRating(5.0)
                .withCertificates(emptyList());

        Freelancer actual = Freelancer.fromBaseModel(base);
        assertThat(actual).is(matchedBy(beanDiffer(base)));
    }

}
