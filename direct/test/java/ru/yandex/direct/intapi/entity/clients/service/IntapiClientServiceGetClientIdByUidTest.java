package ru.yandex.direct.intapi.entity.clients.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.PassportClientStub;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.clients.model.ClientIdResponse;
import ru.yandex.direct.web.core.model.WebResponse;

import static org.assertj.core.api.Assertions.assertThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class IntapiClientServiceGetClientIdByUidTest {

    @Autowired
    private Steps steps;

    @Autowired
    private PassportClientStub passportClientStub;

    @Autowired
    private IntapiClientService intapiClientService;

    @Test
    public void getClientIdByUid_existingUser() {
        UserInfo userInfo = steps.userSteps().createDefaultUser();
        Long uid = userInfo.getUid();
        WebResponse webResponse = intapiClientService.getClientIdByUid(uid);
        assertThat(webResponse.isSuccessful()).isTrue();
        ClientIdResponse clientIdResponse = (ClientIdResponse) webResponse;
        long clientId = userInfo.getClientId().asLong();
        assertThat(clientIdResponse.getClientId()).isEqualTo(clientId);
    }

    @Test
    public void getClientIdByUid_nonExistingUser() {
        long uid = passportClientStub.generateNewUserUid();
        WebResponse webResponse = intapiClientService.getClientIdByUid(uid);
        assertThat(webResponse.isSuccessful()).isFalse();
    }
}
