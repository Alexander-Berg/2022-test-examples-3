package ru.yandex.market.hrms.core.service.outstaff.document;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.domain.repo.Domain;
import ru.yandex.market.hrms.core.domain.outstaff.OutstaffCompanyEntity;
import ru.yandex.market.hrms.core.service.outstaff_document.OutstaffDailyTimesheetService;
import ru.yandex.market.hrms.core.service.outstaff_document.excel.OutstaffTimesheetExcelLoaderService;
import ru.yandex.market.hrms.core.service.s3.S3Service;
import ru.yandex.market.hrms.model.outstaff.OutStaffShiftType;
import ru.yandex.market.hrms.model.outstaff.timesheet.OutstaffTimesheetExcel;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitDataSet(schema = "public", before = "OutstaffDailyTimesheetServiceTest.before.csv")
public class OutstaffDailyTimesheetServiceTest extends AbstractCoreTest {

    private final Domain DEFAULT_DOMAIN = createDefaultDomain();
    private final OutstaffCompanyEntity DEFAULT_COMPANY = createDefaultCompany();

    @Autowired
    private OutstaffDailyTimesheetService sut;

    @MockBean
    private OutstaffTimesheetExcelLoaderService outstaffTimesheetExcelLoaderService;

    @MockBean
    private S3Service s3Service;

    @AfterEach
    public void release() {
        Mockito.reset(s3Service, outstaffTimesheetExcelLoaderService);
    }

    @Test
    public void shouldCreateTimesheetWhenExistOperations() {
        OutstaffTimesheetExcel excelModel = OutstaffTimesheetExcel.builder().build();

        sut.createTimesheet(DEFAULT_DOMAIN, DEFAULT_COMPANY,
                OutStaffShiftType.FIRST_SHIFT, LocalDate.of(2022, 1, 1), "batman", excelModel);

        verify(s3Service, times(1)).putObject(any(), any(), any());
    }

    @Test
    public void shouldNotCreateTimesheetWhenSaveS3Fail() {
        doThrow(RuntimeException.class).when(s3Service).putObject(any(), any(), any());
        OutstaffTimesheetExcel excelModel = OutstaffTimesheetExcel.builder().build();

        assertThatThrownBy(() -> sut.createTimesheet(DEFAULT_DOMAIN, DEFAULT_COMPANY,
                OutStaffShiftType.FIRST_SHIFT, LocalDate.of(2022, 1, 1), "mr_freeze", excelModel))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void shouldUpdateFileLinkWhenTimesheetExists() {
        doNothing().when(s3Service).putObject(any(), any(), any());
        OutstaffTimesheetExcel excelModel = OutstaffTimesheetExcel.builder().build();

        sut.createTimesheet(DEFAULT_DOMAIN, DEFAULT_COMPANY,
                OutStaffShiftType.FIRST_SHIFT, LocalDate.of(2022, 1, 1), "mr_freeze", excelModel);
        verify(s3Service, times(1)).putObject(any(), any(), any());
    }

    private Domain createDefaultDomain() {
        return Domain.builder()
                .id(1L)
                .name("Голливуд")
                .timezone(ZoneId.of("America/Los_Angeles"))
                .build();
    }

    private OutstaffCompanyEntity createDefaultCompany() {
        return OutstaffCompanyEntity.builder()
                .id(1L)
                .name("Warner Bros")
                .build();
    }
}
