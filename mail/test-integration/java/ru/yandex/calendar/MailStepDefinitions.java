package ru.yandex.calendar;

import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import lombok.val;
import ru.yandex.calendar.logic.sending.real.MailSenderMock;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

public class MailStepDefinitions {
    @Inject
    MailSenderMock mailSender;

    @Before
    public void clearEmailMessages() {
        mailSender.clear();
    }

    @Then("email message for {string} should not contains an invitation")
    public void emailMessageShouldBeWithoutInvitation(String email) {
        val parametersList = mailSender.getLayerInvitationMessageParameterss();
        assertThat(parametersList).hasSize(1);
        val parameters = parametersList.single();
        assertThat(parameters.getRecipient().getEmail().getEmail())
                .isEqualTo(email);
        assertThat(parameters.isAutoAccept()).isTrue();
    }
}
