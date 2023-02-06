package ru.yandex.market.hrms.core.service.timex;

import java.util.List;
import java.util.Optional;

import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.AccessLevel;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.EmployeeDetailInfo;
import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.IdentifierGroup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.outstaff.timex.DeleteOutstaffTimexEmployeeConsumer;
import ru.yandex.market.hrms.core.service.outstaff.timex.DeleteOutstaffTimexEmployeePayload;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeleteOutstaffTimexEmployeeTest extends AbstractCoreTest {

    @Autowired
    @SpyBean
    @MockBean
    private TimexApiFacadeNew timexApiFacadeNew;

    @Autowired
    private DeleteOutstaffTimexEmployeeConsumer deleteOutstaffTimexEmployeeConsumer;

    @Test
    @DbUnitDataSet(before = "DeleteOutstaffTimexEmployeeTest.before.csv")
    public void shouldRemoveAccessLevels() throws Exception {
        var accessLevels1 = new AccessLevel();
        accessLevels1.setOid("AL-1");

        var accessLevel2 = new AccessLevel();
        accessLevel2.setOid("AL-2");

        var identifierGroup1 = new IdentifierGroup();
        identifierGroup1.setOid("IDG-1");
        identifierGroup1.setAccessLevels(new AccessLevel[]{ accessLevels1, accessLevel2 });

        var identifierGroup2 = new IdentifierGroup();
        identifierGroup2.setOid("IDG-2");
        identifierGroup2.setIsDeleted(true);
        identifierGroup2.setAccessLevels(new AccessLevel[]{ accessLevels1, accessLevel2 });

        var identifierGroup3 = new IdentifierGroup();
        identifierGroup3.setOid("IDG-3");
        identifierGroup3.setAccessLevels(new AccessLevel[]{ accessLevels1, accessLevel2 });

        var employeeDetailInfo1 = new EmployeeDetailInfo();
        employeeDetailInfo1.setIdentifierGroups(new IdentifierGroup[] { identifierGroup1, identifierGroup2 });

        var employeeDetailInfo2 = new EmployeeDetailInfo();
        employeeDetailInfo2.setIdentifierGroups(new IdentifierGroup[]{ identifierGroup3 });

        when(timexApiFacadeNew.getEmployeeDetailInfo("OLD-OUT-1")).thenReturn(Optional.of(employeeDetailInfo2));
        when(timexApiFacadeNew.getEmployeeDetailInfo("NEW-OUT-1")).thenReturn(Optional.of(employeeDetailInfo1));
        doNothing().when(timexApiFacadeNew).setAccessLevels(any(), any());

        deleteOutstaffTimexEmployeeConsumer
                .processPayload(new DeleteOutstaffTimexEmployeePayload(1L, List.of(1L)));

        verify(timexApiFacadeNew).setAccessLevels("IDG-1", "AL-2");
        verify(timexApiFacadeNew).setAccessLevels("IDG-3", "AL-2");
    }
}
