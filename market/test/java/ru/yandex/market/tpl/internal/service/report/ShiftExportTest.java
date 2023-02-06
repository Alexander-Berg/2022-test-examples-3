package ru.yandex.market.tpl.internal.service.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.shift.partner.PartnerShiftDto;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerUserShiftParamsDto;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.usershift.partner.PartnerShiftService;
import ru.yandex.market.tpl.internal.controller.TplIntTest;
import ru.yandex.market.tpl.report.core.ReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TplIntTest
@Disabled
public class ShiftExportTest {

    private final ReportService reportService;

    @MockBean
    private PartnerShiftService partnerShiftService;

    private ShiftReportService shiftReportService;


    @BeforeEach
    void init() {

        shiftReportService = new ShiftReportService(
                reportService,
                partnerShiftService
        );

        given(partnerShiftService.findShifts(
                any(PartnerUserShiftParamsDto.class), any(CompanyPermissionsProjection.class)
                )
        )
                .willReturn(getTestDataShift());
    }


    @Test
    @Disabled
    @DisplayName("Тестик для отладки верстки отчета смен")
    void getShiftsReport() throws IOException {
        String path = System.getProperty("user.home") + "/test/shifts" + Instant.now() + ".xlsx";
        var p = Path.of(path);
        if (!Files.exists(p.getParent())) {
            Files.createDirectories(p.getParent());
        }
        if (!Files.exists(p)) {
            Files.createFile(p);
        }

        FileOutputStream fos = new FileOutputStream(path);

        shiftReportService.getShiftsReport(fos, new PartnerUserShiftParamsDto(), CompanyPermissionsProjection.builder().build());

        fos.flush();
        fos.close();
    }

    private List<PartnerShiftDto> getTestDataShift() {
        List<PartnerShiftDto> res  = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
            res.add(createShiftDto());
        }
        return res;
    }

    public PartnerShiftDto createShiftDto() {
        return new PartnerShiftDto(LocalDate.now(),
                123L,
                "сц имя",
                123,
                123,
                100,
                15,
                5,
                3,
                1000,
                200,
                900,
                100,
                10,
                10.0d,
                "fff",
                "www.fff.ru",
                200,
                100,
                60,
                40,
                100,
                30,
                15,
                10,
                5,
                0,
                30,
                15,
                0,
                10,
                5,
                0,
                0,
                0,
                100,
                10,
                90,
                3,
                2,
                0,
                0,
                0,
                1
        );
    }


}
