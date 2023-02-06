package ru.yandex.market.mbi.partner.registration.tasks;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.partner.registration.AbstractFunctionalTest;
import ru.yandex.market.mbi.partner.registration.tasks.model.PartnerParams;
import ru.yandex.market.mbi.partner.registration.tasks.model.TaskContext;
import ru.yandex.mj.generated.client.integration_npd.api.ApplicationApiClient;
import ru.yandex.mj.generated.server.model.PartnerNotificationContact;

public class PartnerNpdRegistrationTaskTest extends AbstractFunctionalTest {

    @Autowired
    private PartnerNpdRegistrationTask partnerNpdRegistrationTask;

    @Autowired
    private ApplicationApiClient applicationApiClient;

    @Test
    void testNoPhoneNoCall() {
        partnerNpdRegistrationTask.executeVoid(
                new TaskContext<>(
                        PartnerParams.newBuilder()
                                .setPartnerId(123L)
                                .setPartnerContact(
                                        new PartnerNotificationContact()
                                                .email("email@ya.ru")
                                                .firstName("Name")
                                                .lastName("Last Name")
                                )
                                .build(),
                        Mockito.mock(DelegateExecution.class)
                )
        );
        Mockito.verifyNoInteractions(applicationApiClient);
    }
}
