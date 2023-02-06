package ru.yandex.market.hrms.tms.ticket;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultIteratorF;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.employee.absence.ticket.VirvWorkflowResolutionEnum;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.tpl.common.startrek.listener.TicketStateTransitionProcessor;
import ru.yandex.startrek.client.Session;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.ResolutionRef;

@DbUnitDataSet(before = "VirvTicketClosedTransitionHandlerTest.before.csv")
public class VirvTicketClosedTransitionHandlerTest extends AbstractTmsTest {
    @Autowired
    private TicketStateTransitionProcessor ticketStateTransitionProcessor;
    @Autowired
    private Session session;

    private Issue first;
    private Issue second;

    @BeforeEach
    void setUp() {

        ResolutionRef resolutionRef = Mockito.mock(ResolutionRef.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(resolutionRef.getKey()).thenReturn(VirvWorkflowResolutionEnum.WONT_FIX.getResolutionKey());

        first = Mockito.mock(Issue.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(first.getKey()).thenReturn("HRMS-1");
        Mockito.when(first.getStatus().getId()).thenReturn(1L);
        Mockito.when(first.getStatus().getSelf()).thenReturn(null);
        Mockito.when(first.getStatus().getKey()).thenReturn("closed");
        Mockito.when(first.getStatus().getDisplay()).thenReturn("");
        Mockito.when(first.getQueue().getId()).thenReturn(1L);
        Mockito.when(first.getQueue().getSelf()).thenReturn(null);
        Mockito.when(first.getQueue().getKey()).thenReturn("TESTHRMSVIRV");
        Mockito.when(first.getQueue().getDisplay()).thenReturn("TESTHRMSVIRV");
        Mockito.when(first.getResolution()).thenReturn(Option.of(resolutionRef));

        second = Mockito.mock(Issue.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(second.getKey()).thenReturn("HRMS-2");
        Mockito.when(second.getStatus().getId()).thenReturn(1L);
        Mockito.when(second.getStatus().getSelf()).thenReturn(null);
        Mockito.when(second.getStatus().getKey()).thenReturn("closed");
        Mockito.when(second.getStatus().getDisplay()).thenReturn("");
        Mockito.when(second.getQueue().getId()).thenReturn(1L);
        Mockito.when(second.getQueue().getSelf()).thenReturn(null);
        Mockito.when(second.getQueue().getKey()).thenReturn("TESTHRMSVIRV");
        Mockito.when(second.getQueue().getDisplay()).thenReturn("TESTHRMSVIRV");
        Mockito.when(second.getResolution()).thenReturn(Option.of(resolutionRef));
    }

    @DbUnitDataSet(after = "VirvTicketClosedTransitionHandlerTest.after.csv")
    @Test
    void shouldUpdateEmployeeAbsenceStatus() {
        List<Issue> issues = List.of(this.first, second);

        Mockito.when(session.issues().find(Mockito.anyString()))
                .thenReturn(DefaultIteratorF.wrap(issues.iterator()));

        ticketStateTransitionProcessor.processAll();
    }
}
