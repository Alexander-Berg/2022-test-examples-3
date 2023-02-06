package ru.yandex.market.hrms.tms.ticket;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.domain.overtime.ticket.OvertimeApprovalResolution;
import ru.yandex.market.hrms.core.domain.overtime.ticket.OvertimeApprovalTransitionHandler;
import ru.yandex.market.hrms.tms.AbstractTmsTest;
import ru.yandex.market.tpl.common.startrek.ticket.StartrekTicket;

@DbUnitDataSet(before = "OvertimeApprovalTransitionHandlerTest.before.csv")
class OvertimeApprovalTransitionHandlerTest extends AbstractTmsTest {

    @Autowired
    private OvertimeApprovalTransitionHandler handler;
    private StartrekTicket startrekTicket;

    @BeforeEach
    public void init() {
        mockClock(LocalDate.parse("2021-02-01"));

        startrekTicket = Mockito.mock(StartrekTicket.class);
        Mockito.when(startrekTicket.getKey())
                .thenReturn("HRMSTESTOVTAPR-777");
    }

    @Test
    @DbUnitDataSet(after = "OvertimeApprovalTransitionHandlerTest.happyPath.after.csv")
    public void happyPath() {

        Mockito.when(startrekTicket.getResolution(OvertimeApprovalResolution.class))
                .thenReturn(Optional.of(OvertimeApprovalResolution.APPROVED));

        handler.handleTransition(startrekTicket);
    }

    @Test
    @DbUnitDataSet(after = "OvertimeApprovalTransitionHandlerTest.notApproved.after.csv")
    public void notApproved() {
        Mockito.when(startrekTicket.getResolution(OvertimeApprovalResolution.class))
                .thenReturn(Optional.empty());

        handler.handleTransition(startrekTicket);
    }
}