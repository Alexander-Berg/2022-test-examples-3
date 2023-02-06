package ru.yandex.market.hrms.tms.ticket;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.hrms.tms.executor.startrek.OpenQueueRef;
import ru.yandex.market.hrms.tms.executor.startrek.OpenStatusRef;
import ru.yandex.market.tpl.common.startrek.listener.TicketStateTransitionProcessor;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;

@DbUnitDataSet(before = "VirvTicketApprovedTransitionHandlerTest.before.csv")
class VirvTicketApprovedTransitionHandlerTest extends AbstractTmsTest {
    @Autowired
    private TicketStateTransitionProcessor ticketStateTransitionProcessor;
    @Autowired
    private Session session;

    private Issue first;
    private Issue second;

    @BeforeEach
    void setUp() {
        first = Mockito.mock(Issue.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(first.getKey()).thenReturn("HRMS-1");
        Mockito.when(first.getStatus()).thenReturn(new OpenStatusRef(1L, null, "approved", "", session));
        Mockito.when(first.getQueue()).thenReturn(new OpenQueueRef(1L, null, "TESTHRMSVIRV", "TESTHRMSVIRV", session));

        second = Mockito.mock(Issue.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(second.getKey()).thenReturn("HRMS-2");
        Mockito.when(second.getStatus()).thenReturn(new OpenStatusRef(1L, null, "approved", "", session));
        Mockito.when(second.getQueue()).thenReturn(new OpenQueueRef(1L, null, "TESTHRMSVIRV", "TESTHRMSVIRV", session));
    }

    @DbUnitDataSet(after = "VirvTicketApprovedTransitionHandlerTest.after.csv")
    @Test
    void shouldUpdateEmployeeAbsenceStatus() {
        List<Issue> issues = List.of(this.first, second);

        Mockito.when(session.issues().find(Mockito.anyString()))
                .thenReturn(DefaultIteratorF.wrap(issues.iterator()));

        ticketStateTransitionProcessor.processAll();
    }
}
