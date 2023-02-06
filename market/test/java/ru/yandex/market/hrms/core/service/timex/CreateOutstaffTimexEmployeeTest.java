package ru.yandex.market.hrms.core.service.timex;

import java.time.Instant;
import java.util.Optional;

import org.datacontract.schemas._2004._07.ArmoSystems_Timex_SDKService_SDKClasses.Employee;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.service.outstaff.timex.CreateOutstaffTimexEmployeeConsumer;
import ru.yandex.market.hrms.core.service.outstaff.timex.CreateOutstaffTimexEmployeePayload;
import ru.yandex.market.hrms.core.service.s3.S3Service;
import ru.yandex.market.hrms.core.service.timex.client.model.employee.identifiers.TimexIdentifierGroup;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CreateOutstaffTimexEmployeeTest extends AbstractCoreTest {

    @Autowired
    @SpyBean
    @MockBean
    private TimexApiFacadeNew timexApiFacadeNew;

    @Autowired
    private CreateOutstaffTimexEmployeeConsumer createOutstaffTimexEmployeeConsumer;

    @Autowired
    @MockBean
    private S3Service s3Service;

    @Value("${market.hrms.aws.s3.bucket:}")
    private String s3Bucket;

    @Captor
    private ArgumentCaptor<Employee> captor;

    @BeforeEach
    public void up() throws Exception {
        mockClock(Instant.parse("2022-06-23T12:00:00Z"));
        var employeeMock = new Employee();
        employeeMock.setOid("TEST-1");
        when(timexApiFacadeNew.createEmptyEmployee()).thenReturn(employeeMock);
        when(timexApiFacadeNew.getEmployee("TEST-1")).thenReturn(Optional.of(employeeMock));
        doNothing().when(timexApiFacadeNew).updateEmployee(any());
    }

    @AfterEach
    public void down() {
        clearInvocations(timexApiFacadeNew, s3Service);
        reset(timexApiFacadeNew, s3Service);
    }

    @Test
    @DbUnitDataSet(
            before = "CreateOutstaffTimexEmployeeTest.before.csv",
            after = "CreateOutstaffTimexEmployeeTest.exception_on_create.after.csv")
    @DisplayName("Запись в outstaff_timex должна остаться даже если были ошибки при заполнении")
    public void shouldCreateMappingAnyway() {
        when(timexApiFacadeNew.getEmployee("TEST-1")).thenThrow(TplIllegalStateException.class);
        Assertions.assertThrows(TplIllegalStateException.class, () -> createOutstaffTimexEmployeeConsumer
                .processPayload(new CreateOutstaffTimexEmployeePayload(1L)));
    }

    @Test
    @DbUnitDataSet(
            before = "CreateOutstaffTimexEmployeeTest.before.csv",
            after = "CreateOutstaffTimexEmployeeTest.success.after.csv")
    @DisplayName("Должен создаться и заполниться сотрудник в Timex")
    public void shouldCreateTimexEmployee() throws Exception {
        when(s3Service.getObject(s3Bucket, "www.url.com")).thenReturn(Optional.of(new byte[0]));
        var identifierGroupMock = new TimexIdentifierGroup();
        identifierGroupMock.setOid("ID-GROUP-1");
        when(timexApiFacadeNew.getIdentifierGroups("TEST-1")).thenReturn(identifierGroupMock);
        doNothing().when(timexApiFacadeNew).setPhotoToEmployee(any(), any(), eq(false));
        when(timexApiFacadeNew.sendPhotoToTimexBiometry(any())).thenReturn(true);
        createOutstaffTimexEmployeeConsumer.processPayload(
                new CreateOutstaffTimexEmployeePayload(1L));
        verify(timexApiFacadeNew).updateEmployee(captor.capture());
        verify(timexApiFacadeNew).setPhotoToEmployee(eq("TEST-1"), notNull(), eq(false));
        verify(timexApiFacadeNew).sendPhotoToTimexBiometry(eq("ID-GROUP-1"));
        var arg = captor.getValue();
        Assertions.assertEquals("OUT-WORKING-AREA-1", arg.getAccessArea().getOid());
        Assertions.assertEquals("COMPANY-1", arg.getCompany().getOid());
        Assertions.assertEquals("жора", arg.getLastName());
        Assertions.assertEquals("жоревич", arg.getMidName());
        Assertions.assertEquals("жорочкин", arg.getName());
        Assertions.assertEquals("POSITION-1", arg.getPost().getOid());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateOutstaffTimexEmployeeTest.before.csv",
            after = "CreateOutstaffTimexEmployeeTest.filled_fields.after.csv")
    @DisplayName("Должно сохраниться состояние FILLED_FIELDS")
    public void shouldSaveFilledState() throws Exception {
        when(s3Service.getObject(s3Bucket, "www.url.com")).thenReturn(Optional.of(new byte[0]));
        var identifierGroupMock = new TimexIdentifierGroup();
        identifierGroupMock.setOid("ID-GROUP-1");
        when(timexApiFacadeNew.getIdentifierGroups("TEST-1")).thenReturn(identifierGroupMock);
        doNothing().when(timexApiFacadeNew).setPhotoToEmployee(any(), any(), eq(false));
        doThrow(TplIllegalStateException.class).when(timexApiFacadeNew).setAccessLevels(any(), any());
        Assertions.assertThrows(TplIllegalStateException.class ,() -> createOutstaffTimexEmployeeConsumer
                .processPayload(new CreateOutstaffTimexEmployeePayload(1L)));
        verify(timexApiFacadeNew).updateEmployee(captor.capture());
        var arg = captor.getValue();
        Assertions.assertEquals("OUT-WORKING-AREA-1", arg.getAccessArea().getOid());
        Assertions.assertEquals("COMPANY-1", arg.getCompany().getOid());
        Assertions.assertEquals("жора", arg.getLastName());
        Assertions.assertEquals("жоревич", arg.getMidName());
        Assertions.assertEquals("жорочкин", arg.getName());
        Assertions.assertEquals("POSITION-1", arg.getPost().getOid());
    }

    @Test
    @DbUnitDataSet(
            before = "CreateOutstaffTimexEmployeeTest.before.csv",
            after = "CreateOutstaffTimexEmployeeTest.has_access_levels.after.csv")
    @DisplayName("Должно сохраниться состояние HAS_ACCESS_LEVELS")
    public void shouldSaveHasAcceessLevelsState() throws Exception {
        when(s3Service.getObject(s3Bucket, "www.url.com")).thenReturn(Optional.of(new byte[0]));
        var identifierGroupMock = new TimexIdentifierGroup();
        identifierGroupMock.setOid("ID-GROUP-1");
        when(timexApiFacadeNew.getIdentifierGroups("TEST-1")).thenReturn(identifierGroupMock);
        doNothing().when(timexApiFacadeNew).setPhotoToEmployee(any(), any(), eq(false));
        when(timexApiFacadeNew.sendPhotoToTimexBiometry(any())).thenThrow(TplIllegalStateException.class);
        Assertions.assertThrows(TplIllegalStateException.class ,() -> createOutstaffTimexEmployeeConsumer
                .processPayload(new CreateOutstaffTimexEmployeePayload(1L)));
        verify(timexApiFacadeNew).updateEmployee(captor.capture());
        var arg = captor.getValue();
        Assertions.assertEquals("OUT-WORKING-AREA-1", arg.getAccessArea().getOid());
        Assertions.assertEquals("COMPANY-1", arg.getCompany().getOid());
        Assertions.assertEquals("жора", arg.getLastName());
        Assertions.assertEquals("жоревич", arg.getMidName());
        Assertions.assertEquals("жорочкин", arg.getName());
        Assertions.assertEquals("POSITION-1", arg.getPost().getOid());
    }
}
