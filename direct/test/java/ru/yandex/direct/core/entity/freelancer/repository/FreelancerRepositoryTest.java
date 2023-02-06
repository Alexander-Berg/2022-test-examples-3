package ru.yandex.direct.core.entity.freelancer.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFreelancers;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerRepositoryTest {

    @Autowired
    Steps steps;
    @Autowired
    FreelancerRepository freelancerRepository;

    private ClientInfo clientInfo;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void add_success() {
        Long freelancerId = clientInfo.getClientId().asLong();
        Freelancer freelancer =
                TestFreelancers.defaultFreelancer(freelancerId);
        int shard = clientInfo.getShard();
        freelancerRepository.addFreelancers(shard, singletonList(freelancer));
        List<Freelancer> freelancers = freelancerRepository.getByIds(shard, singleton(freelancerId));
        assertThat(freelancers).first().isEqualToComparingFieldByFieldRecursively(freelancer);
    }
}
