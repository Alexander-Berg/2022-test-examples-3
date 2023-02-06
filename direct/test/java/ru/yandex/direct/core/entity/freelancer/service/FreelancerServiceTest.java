package ru.yandex.direct.core.entity.freelancer.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringRunner.class)
public class FreelancerServiceTest {

    @Autowired
    Steps steps;

    @Autowired
    FreelancerService testedService;

    @Test
    public void isFreelancer_true_forFreelancer() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();

        boolean actual = testedService.isFreelancer(freelancerInfo.getClientId());
        assertThat(actual).isTrue();
    }

    @Test
    public void isFreelancer_false_forClientNonFreelancer() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        boolean actual = testedService.isFreelancer(clientInfo.getClientId());
        assertThat(actual).isFalse();
    }
}
