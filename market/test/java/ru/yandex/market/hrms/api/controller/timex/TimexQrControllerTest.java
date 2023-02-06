package ru.yandex.market.hrms.api.controller.timex;

import java.time.Instant;
import java.util.Optional;

import javax.servlet.http.Cookie;

import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Employee;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.EmployeeDetailInfo;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.IdentifierGroup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.service.timex.TimexApiFacadeNew;
import ru.yandex.market.hrms.core.service.timex.client.TimexSessionUpdater;
import ru.yandex.market.hrms.core.service.wms.PasswordGenerator;
import ru.yandex.market.logistics.management.plugin.hrms.HrmsPlugin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TimexQrControllerTest extends AbstractApiTest {

    @MockBean
    private PasswordGenerator passwordGenerator;

    @SpyBean
    @MockBean
    private TimexApiFacadeNew timexApiFacadeNew;

    @Autowired
    @MockBean
    private TimexSessionUpdater timexSessionUpdater;

    @Test
    @DbUnitDataSet(before = "TimexQrControllerTest.before.csv",
                   after = "TimexQrControllerTest.after.csv")
    public void shouldCreateQrCode() throws Exception {
        //arrange
        mockClock(Instant.parse("2022-05-25T00:00:00Z"));
        var identifierGroup = Mockito.mock(IdentifierGroup.class);
        var employeeMock = Mockito.mock(Employee.class);
        var employeeDetailMock = Mockito.mock(EmployeeDetailInfo.class);
        doNothing().when(timexSessionUpdater).updateSession();
        when(employeeDetailMock.getIdentifierGroups()).thenReturn(new IdentifierGroup[]{identifierGroup});
        when(employeeMock.getOid()).thenReturn("timex-100");
        when(employeeDetailMock.getOid()).thenReturn("timex-100");
        when(timexApiFacadeNew.getEmployeeDetailInfo("timex-100")).thenReturn(Optional.of(employeeDetailMock));
        when(timexApiFacadeNew.getIdentifierGroup("timex-100")).thenReturn(Optional.of(identifierGroup));
        when(timexApiFacadeNew.getEmployee("timex-100")).thenReturn(Optional.of(employeeMock));
        doNothing().when(timexApiFacadeNew).updateIdentifierGroup(any());
        when(passwordGenerator.generate()).thenReturn("password");

        //act
        mockMvc.perform(get("/lms/timex/print")
                        .cookie(new Cookie("yandex_login", "magomedovgh"))
                        .queryParam("outstaffIds", "1")
                        .queryParam("domainId", "4")
                        .header("X-Admin-Roles", String.join(",",
                                HrmsPlugin.getHrmsRolesMap()
                                        .get(HrmsPlugin.HrmsRoles.BIOMETRY_SUPPORT_MANAGER)
                                        .getAuthorities()
                        ))
                )
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename*=UTF-8''timex_qr_badge_20220525_030000.xlsx"))
                .andReturn();

        //assert
        verify(identifierGroup).setQrCode("PASSWORD");
        verify(timexApiFacadeNew).updateIdentifierGroup(identifierGroup);
    }
}
