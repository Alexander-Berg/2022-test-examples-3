package ru.yandex.market.hrms.tms.manager.timex;

import java.time.Instant;
import java.util.Optional;

import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Employee;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.IdentifierGroup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.service.timex.TimexApiFacadeNew;
import ru.yandex.market.hrms.tms.AbstractTmsTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TimexQrBadgeManagerTest extends AbstractTmsTest {

    @SpyBean
    @MockBean
    private TimexApiFacadeNew timexApiFacadeNew;

    @Autowired
    private TimexQrBadgeManager timexQrBadgeManager;

    @Test
    @DbUnitDataSet(before = "TimexQrBadgeManagerTest.before.csv",
                   after = "TimexQrBadgeManagerTest.after.csv")
    public void expiredCodesShouldBeDisabledForOutstaff() {
        mockClock(Instant.parse("2022-05-26T00:00:01Z"));
        //arrange
        var identifierGroup = Mockito.mock(IdentifierGroup.class);
        var employeeMock = Mockito.mock(Employee.class);
        when(employeeMock.getOid()).thenReturn("timex-100");
        when(timexApiFacadeNew.getIdentifierGroup("timex-100")).thenReturn(Optional.of(identifierGroup));
        when(timexApiFacadeNew.getEmployee("timex-100")).thenReturn(Optional.of(employeeMock));
        doNothing().when(timexApiFacadeNew).updateIdentifierGroup(any());

        //act
        timexQrBadgeManager.disableExpiredQrCodes();

        //assert
        verify(identifierGroup).setQrCode(null);
        verify(timexApiFacadeNew).updateIdentifierGroup(identifierGroup);
    }

    @Test
    @DbUnitDataSet(before = "TimexQrBadgeManagerTest.staff.before.csv",
            after = "TimexQrBadgeManagerTest.staff.after.csv")
    public void expiredCodesShouldBeDisabledForStaff() {
        mockClock(Instant.parse("2022-05-26T00:00:01Z"));
        //arrange
        var identifierGroup = Mockito.mock(IdentifierGroup.class);
        var employeeMock = Mockito.mock(Employee.class);
        when(employeeMock.getOid()).thenReturn("timex-100");
        when(timexApiFacadeNew.getIdentifierGroup("timex-100")).thenReturn(Optional.of(identifierGroup));
        when(timexApiFacadeNew.getEmployee("timex-100")).thenReturn(Optional.of(employeeMock));
        when(timexApiFacadeNew.getEmployeeByStaffLogin("timex-100")).thenReturn(Optional.of(employeeMock));
        doNothing().when(timexApiFacadeNew).updateIdentifierGroup(any());

        //act
        timexQrBadgeManager.disableExpiredQrCodes();

        //assert
        verify(identifierGroup).setQrCode(null);
        verify(timexApiFacadeNew).updateIdentifierGroup(identifierGroup);
    }
}
