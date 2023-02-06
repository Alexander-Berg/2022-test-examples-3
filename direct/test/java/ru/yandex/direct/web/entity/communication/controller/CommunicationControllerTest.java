package ru.yandex.direct.web.entity.communication.controller;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.ads.bsyeti.libs.communications.EMessageStatus;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DirectWebTest
@RunWith(SpringRunner.class)
public class CommunicationControllerTest {

    @Autowired
    private CommunicationController communicationController;

    @Test
    public void buildCookieValue_emptyCookieSingleStatus(){
        var actualCookie = communicationController.buildCookieValue(null, 111L,
                Set.of(EMessageStatus.NEW));
        assertThat("111-NEW").isEqualTo(actualCookie);
    }

    @Test
    public void buildCookieValue_emptyCookieMultiStatus(){
        var actualCookie = communicationController.buildCookieValue(null, 111L,
                Set.of(EMessageStatus.NEW, EMessageStatus.DELIVERED));
        assertThat("111-DELIVERED+NEW").isEqualTo(actualCookie);
    }

    @Test
    public void buildCookieValue_existingCookieNewMessage(){
        var existingCookie = "111-DELIVERED+NEW|222-DELIVERED+REJECT";
        var actualCookie = communicationController.buildCookieValue(existingCookie, 333L,
                Set.of(EMessageStatus.DELIVERED, EMessageStatus.APPLY));
        assertThat("111-DELIVERED+NEW|222-DELIVERED+REJECT|333-APPLY+DELIVERED").isEqualTo(actualCookie);
    }

    @Test
    public void buildCookieValue_existingCookieExistingMessage(){
        var existingCookie = "111-DELIVERED+NEW|222-NEW";
        var actualCookie = communicationController.buildCookieValue(existingCookie, 222L,
                Set.of(EMessageStatus.DELIVERED, EMessageStatus.APPLY));
        assertThat("111-DELIVERED+NEW|222-APPLY+DELIVERED+NEW").isEqualTo(actualCookie);
    }

    @Test
    public void buildCookieValue_corruptedCookie(){
        var existingCookie = "111-DELIVERED+NEW|222-| 333 - NEW || |HELLO-world|555-UNKNOWN";
        var actualCookie = communicationController.buildCookieValue(existingCookie, 333L,
                Set.of(EMessageStatus.DELIVERED, EMessageStatus.APPLY));
        assertThat("111-DELIVERED+NEW|333-APPLY+DELIVERED+NEW").isEqualTo(actualCookie);
    }

    @Test
    public void buildCookieValue_existingCookieDuplicateMessage(){
        var existingCookie = "111-DELIVERED+NEW|222-NEW|222-DELIVERED+APPLY";
        var actualCookie = communicationController.buildCookieValue(existingCookie, 222L,
                Set.of(EMessageStatus.CANCEL));
        assertThat("111-DELIVERED+NEW|222-APPLY+CANCEL+DELIVERED").isEqualTo(actualCookie);
    }
}
