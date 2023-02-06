package ru.yandex.market.supportwizard.job;

import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.base.supplier.SupplierOnboardingStepType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;
import ru.yandex.market.supportwizard.service.StartrekSessionBuilder;
import ru.yandex.market.supportwizard.service.alert.ticket.SupplierOnboardingTicketAlertService;
import ru.yandex.market.supportwizard.storage.SupplierOnboardingAlertTicketEntity;
import ru.yandex.market.supportwizard.storage.SupplierOnboardingAlertTicketRepository;
import ru.yandex.startrek.client.Issues;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

import static java.util.stream.Collectors.toMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class AlertStuckOnOnboardingPartnersJobTest extends BaseFunctionalTest {

    private Session session = mock(Session.class);
    private Issues issues = mock(Issues.class);
    private Issue createdIssue = mock(Issue.class);
    private Issue updatedIssue = mock(Issue.class);

    @Autowired
    private StartrekSessionBuilder startrekSessionBuilder;
    @Autowired
    private SupplierOnboardingTicketAlertService alertService;
    @Autowired
    private SupplierOnboardingAlertTicketRepository ticketRepository;

    @Autowired
    private AlertStuckOnOnboardingPartnersJob tested;

    @BeforeEach
    void init() {
        doReturn(session).when(startrekSessionBuilder).buildSession();
        doReturn(issues).when(session).issues();
        doReturn(updatedIssue).when(issues).get(anyString());
        doReturn(updatedIssue).when(updatedIssue).update(any());

        doReturn(createdIssue).when(issues).create(any());
        doReturn("ONBERU-1").when(createdIssue).getId();
        doReturn("ONBERU-2").when(updatedIssue).getId();

        alertService.afterPropertiesSet();
    }

    @Test
    @DbUnitDataSet(before = "alertStuckOnOnboardingPartnersJob.before.csv")
    void testAlert() {
        tested.doJob(null);

        Map<SupplierOnboardingStepType, SupplierOnboardingAlertTicketEntity> tickets = ticketRepository.findAll()
                .stream()
                .collect(toMap(SupplierOnboardingAlertTicketEntity::getStep, Function.identity()));

        assertEquals(2, tickets.size());

        assertNotNull(tickets.get(SupplierOnboardingStepType.REGISTRATION));
        assertEquals(1, tickets.get(SupplierOnboardingStepType.REGISTRATION).getTicketData().getStuckOnStep().size());
        assertEquals(1, tickets.get(SupplierOnboardingStepType.REGISTRATION).getTicketData().getFinishedStep().size());

        assertNotNull(tickets.get(SupplierOnboardingStepType.REQUEST_PROCESSING));
        assertEquals(1, tickets.get(SupplierOnboardingStepType.REQUEST_PROCESSING).getTicketData().getStuckOnStep().size());
        assertEquals(0, tickets.get(SupplierOnboardingStepType.REQUEST_PROCESSING).getTicketData().getFinishedStep().size());
    }
}
