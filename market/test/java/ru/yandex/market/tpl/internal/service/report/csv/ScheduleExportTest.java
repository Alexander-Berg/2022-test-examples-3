package ru.yandex.market.tpl.internal.service.report.csv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.core.service.user.schedule.ScheduleReportDto;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.service.user.schedule.UserSchedulesSearchRequest;
import ru.yandex.market.tpl.internal.controller.TplIntTest;
import ru.yandex.market.tpl.report.core.ReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TplIntTest
@Disabled
public class ScheduleExportTest {
    private final ReportService reportService;
    private final int daysCount = 30;
    @MockBean
    private UserScheduleService userScheduleService;
    private ScheduleReportService scheduleReportService;

    @BeforeEach
    void init() {
        scheduleReportService = new ScheduleReportService(
                reportService,
                userScheduleService
        );

        given(userScheduleService.findScheduleReportDto(
                any(UserSchedulesSearchRequest.class)
                )
        )
                .willReturn(getTestData());
    }

    @Test
    @Disabled
    void getUsersReport() throws IOException {

        String path = System.getProperty("user.home") + "/Расписание" + LocalDate.now().toString()
                + "_" + LocalDate.now().plusDays(daysCount).toString() + ".xlsx";
        FileOutputStream fos = new FileOutputStream(path);

        scheduleReportService.getScheduleCsvReport(fos, new UserSchedulesSearchRequest(null, null, null,
                null, null, null, null, null, null));

        fos.flush();
        fos.close();
    }

    private List<ScheduleReportDto> getTestData() {
        return List.of(createScheduleReportDto());
    }

    private ScheduleReportDto createScheduleReportDto() {
        Map<String, String> scheduleRules = new HashMap<>();
        Map<String, String> slots = new HashMap<>();
        List<String> dateList = new ArrayList<>();
        for (int i = 0; i < daysCount; i++) {
            scheduleRules.put(java.time.format.DateTimeFormatter.ofPattern("E dd.MM", new Locale("ru")).format(LocalDate.now().plusDays(i)),
                    LocalTime.of(0, 0).toString() + " - " +  LocalTime.of(2, 22).toString());
            slots.put(java.time.format.DateTimeFormatter.ofPattern("E dd.MM", new Locale("ru")).format(LocalDate.now().plusDays(i)),
                    LocalTime.of(9, 0).toString());
            dateList.add(java.time.format.DateTimeFormatter.ofPattern("E dd.MM", new Locale("ru")).format(LocalDate.now().plusDays(i)));
        }
        ScheduleReportDto dto = ScheduleReportDto.builder()
                .carNumber("123AA")
                .scName("СЦ Восток")
                .companyName("Компания ООО")
                .courierName("Вася Петя")
                .transportType("CAR")
                .transportCapacity(new BigDecimal(1000))
                .scheduleRules(scheduleRules)
                .slots(slots)
                .dateList(dateList)
                .build();
        return dto;
    }
}
