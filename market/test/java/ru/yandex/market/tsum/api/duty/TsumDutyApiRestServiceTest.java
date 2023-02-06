package ru.yandex.market.tsum.api.duty;


import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.market.tsum.core.duty.DutyManager;
import ru.yandex.market.tsum.core.duty.exceptions.*;

import java.io.UnsupportedEncodingException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

/**
 * @author David Burnazyan <a href="mailto:dburnazyan@yandex-team.ru"></a>
 * @date 10/08/2017
 */
@RunWith(MockitoJUnitRunner.class)
public class TsumDutyApiRestServiceTest {

    private static final String DUTY_TYPE = "SOME_DUTY";

    @InjectMocks
    private TsumDutyApiRestService tsumDutyApiRestService;

    @Mock
    private DutyManager dutyManager;

    @Test
    public void testSwitchDuty_normalExecution_httpOkIsReturned()
            throws UnsupportedEncodingException {
        String departmentName = "test_department";
        String dutyType = "some_duty";
        String switchTo = "test_duty";

        doNothing().when(dutyManager).switchDuty(eq(departmentName), eq(dutyType), eq(switchTo));

        ResponseEntity<String> result = tsumDutyApiRestService.switchDuty(null, departmentName, dutyType, switchTo);

        Assert.assertEquals(result.getStatusCode(), HttpStatus.OK);
    }

    @Test
    public void testSwitchDuty_accessingCalendarExceptionIsThrown_httpInternalServerErrorIsReturned()
            throws UnsupportedEncodingException {
        String departmentName = "test_department";
        String switchTo = "test_duty";

        doThrow(new AccessingCalendarException(null, null)).when(dutyManager).switchDuty(eq(departmentName),
                eq(DUTY_TYPE),
                eq(switchTo));

        ResponseEntity<String> result = tsumDutyApiRestService.switchDuty(null, departmentName, DUTY_TYPE, switchTo);

        Assert.assertEquals(result.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
        Assert.assertEquals(result.getBody(), "Error while getting data from Calendar");
    }

    @Test
    public void testSwitchDuty_dutyEventNotFoundInCalendarExceptionIsThrown_httpInternalServerErrorIsReturned()
            throws UnsupportedEncodingException {
        String departmentName = "test_department";
        String switchTo = "test_duty";

        doThrow(new DutyEventNotFoundInCalendarException(null)).when(dutyManager).switchDuty(eq(departmentName),
                eq(DUTY_TYPE),
                eq(switchTo));

        ResponseEntity<String> result = tsumDutyApiRestService.switchDuty(null, departmentName, DUTY_TYPE, switchTo);

        Assert.assertEquals(result.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
        Assert.assertEquals(result.getBody(), "Failed to find duty event in calendar");
    }

    @Test
    public void testSwitchDuty_dutyNotFoundInStaffExceptionIsThrown_httpBadRequestIsReturned()
            throws UnsupportedEncodingException {
        String departmentName = "test_department";
        String switchTo = "test_duty";

        doThrow(new DutyNotFoundInStaffException(null)).when(dutyManager).switchDuty(eq(departmentName),
                eq(DUTY_TYPE),
                eq(switchTo));

        ResponseEntity<String> result = tsumDutyApiRestService.switchDuty(null, departmentName, DUTY_TYPE, switchTo);

        Assert.assertEquals(result.getStatusCode(), HttpStatus.BAD_REQUEST);
        Assert.assertEquals(result.getBody(), "Duty login " + switchTo + " not found");
    }

    @Test
    public void testSwitchDuty_updatingDataInTelegraphExceptionIsThrown_httpInternalServerErrorIsReturned()
            throws UnsupportedEncodingException {
        String departmentName = "test_department";
        String switchTo = "test_duty";

        doThrow(new UpdatingDataInTelegraphException(null, null)).when(dutyManager).switchDuty(eq(departmentName),
                eq(DUTY_TYPE),
                eq(switchTo));

        ResponseEntity<String> result = tsumDutyApiRestService.switchDuty(null, departmentName, DUTY_TYPE, switchTo);

        Assert.assertEquals(result.getStatusCode(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testSwitchDuty_departmentNotFoundExceptionIsThrown_httpBadRequestIsReturned()
            throws UnsupportedEncodingException {
        String departmentName = "test_department";
        String switchTo = "test_duty";

        doThrow(new DepartmentNotFoundException(null)).when(dutyManager).switchDuty(eq(departmentName),
                eq(DUTY_TYPE),
                eq(switchTo));

        ResponseEntity<String> result = tsumDutyApiRestService.switchDuty(null, departmentName, DUTY_TYPE, switchTo);

        Assert.assertEquals(result.getStatusCode(), HttpStatus.BAD_REQUEST);
        Assert.assertEquals(result.getBody(), "Department not found in TSUM config");
    }
}
