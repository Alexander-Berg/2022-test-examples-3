package ru.yandex.market.tpl.internal.service.report;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.company.PartnerUserCompanyDto;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.transport.PartnerTransportTypeDto;
import ru.yandex.market.tpl.api.model.user.UserRole;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserParamsDto;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserReportDto;
import ru.yandex.market.tpl.api.model.user.partner.PartnerUserRoutingPropertiesDto;
import ru.yandex.market.tpl.api.model.usershift.location.RegionInfoDto;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.service.user.partner.PartnerUserService;
import ru.yandex.market.tpl.internal.controller.TplIntTest;
import ru.yandex.market.tpl.report.core.ReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@TplIntTest
class CourierExportTest {

    private final ReportService reportService;

    @MockBean
    private PartnerUserService userService;

    private UserReportService userReportService;

    @BeforeEach
    void init() {
        userReportService = new UserReportService(
                reportService,
                userService
        );

        given(userService.findAll(
                any(PartnerUserParamsDto.class),
                any(CompanyPermissionsProjection.class)
                )
        )
                .willReturn(getTestData());
    }

    @Test
    @Disabled
    @DisplayName("Тестик для отладки верстки отчета")
    void getUsersReport() throws IOException {
        String path = System.getProperty("user.home") +  "/couriers" + Instant.now() + ".xlsx";
        FileOutputStream fos = new FileOutputStream(path);

        userReportService.getUsersReport(fos, new PartnerUserParamsDto(), CompanyPermissionsProjection.builder().build());

        fos.flush();
        fos.close();
    }

    private List<PartnerUserReportDto> getTestData() {
        return List.of(createCourier(), createCourier());
    }

    private PartnerUserReportDto createCourier() {
        PartnerUserReportDto userDto = new PartnerUserReportDto();
        userDto.setId(123L);
        userDto.setUid(456L);
        userDto.setName("Иванов Иванов");
        userDto.setRole(UserRole.COURIER);
        userDto.setPhone("8-999-999-99-99");
        userDto.setScheduleType(UserScheduleType.FIVE_TWO);
        userDto.setSortingCenterName("СЦ МаркетКурьер");

        PartnerTransportTypeDto transportTypeDto = PartnerTransportTypeDto.builder()
                .id(3L)
                .name("BMW")
                .capacity(1.2)
                .build();
        userDto.setTransportType(transportTypeDto);

        PartnerUserCompanyDto companyDto = PartnerUserCompanyDto.builder()
                .id(1L)
                .name("Курьерская компания")
                .build();
        userDto.setCompany(companyDto);
        RegionInfoDto dto = new RegionInfoDto();
        dto.setId(1);
        dto.setName("Бутырский район");
        userDto.setRegionNameReport("Арбат");


        var routingProperties = new PartnerUserRoutingPropertiesDto();
        routingProperties.setCarMultiplier(BigDecimal.valueOf(1.1));
        routingProperties.setCarSharedServiceMultiplier(BigDecimal.valueOf(1.0));
        routingProperties.setCarServiceMultiplier(BigDecimal.valueOf(0.9));
        routingProperties.setCarEnabled(true);
        routingProperties.setFootEnabled(false);

        userDto.setRoutingProperties(routingProperties);
        return userDto;
    }
}
