package ru.yandex.direct.web.entity.creative;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.CreativeSteps;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.configuration.mock.auth.TestAuthHelper;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.direct.web.entity.creative.service.CreativeWebService;

import static org.assertj.core.api.Assertions.assertThat;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CreativeWebServiceTest {
    @Autowired
    public CreativeWebService creativeWebService;

    @Autowired
    private TestAuthHelper testAuthHelper;

    @Autowired
    private CreativeSteps creativeSteps;

    private UserInfo defaultUser;

    @Before
    public void before() {
        defaultUser = testAuthHelper.createDefaultUser();
    }

    @Test
    public void getVideoCreatives() {
        Long creativeId = creativeSteps.getNextCreativeId();
        creativeSteps.addDefaultVideoAdditionCreative(defaultUser.getClientInfo(), creativeId);
        WebResponse webResponse = creativeWebService.searchVideoCreatives(Collections.singletonList(creativeId));
        assertThat(webResponse.isSuccessful()).isTrue();
    }
}
