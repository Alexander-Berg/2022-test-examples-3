package ru.yandex.direct.core.entity.freelancer.service;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.FreelancerSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.result.ResultState;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

@Ignore("For manual run")
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerAvatarsClientServiceManualTest {
    private static final String IMAGE_PATH = "avatars/YandexDirect.png";

    @Autowired
    public FreelancerClientAvatarService freelancerClientAvatarService;
    @Autowired
    public FreelancerCardService freelancerCardUpdateService;
    @Autowired
    public FreelancerSteps freelancerSteps;
    @Autowired
    public FreelancerService freelancerService;

    private byte[] imageBody;
    private ClientId freelancerId;

    @Before
    public void setup() throws IOException {
        imageBody = getImageBody();
        FreelancerInfo freelancerInfo = freelancerSteps.addDefaultFreelancer();
        freelancerId = freelancerInfo.getClientId();
    }

    private byte[] getImageBody() throws IOException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        try (InputStream is = classloader.getResourceAsStream(IMAGE_PATH)) {
            return IOUtils.toByteArray(is);
        }
    }

    @Test
    public void updateAvatar_success() {
        Long oldAvatarId =
                freelancerService.getFreelancers(singletonList(freelancerId.asLong())).get(0).getCard().getAvatarId();
        Result<Long> updateResult = freelancerClientAvatarService.updateAvatar(freelancerId, imageBody);
        assertThat(updateResult.getState()).isEqualTo(ResultState.SUCCESSFUL);
        Long newAvatarId =
                freelancerService.getFreelancers(singletonList(freelancerId.asLong())).get(0).getCard().getAvatarId();
        assertThat(newAvatarId).isNotEqualTo(oldAvatarId);
    }
}
