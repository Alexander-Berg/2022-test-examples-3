package ru.yandex.market.tpl.internal.controller.partner;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.core.domain.partner.PartnerkaCommandEvent;
import ru.yandex.market.tpl.core.domain.partner.PartnerkaCommandRepository;
import ru.yandex.market.tpl.core.service.company.CompanyAuthenticationService;
import ru.yandex.market.tpl.core.service.company.PartnerCompanyRoleService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommandService;
import ru.yandex.market.tpl.core.service.user.schedule.CommonSlotService;
import ru.yandex.market.tpl.core.service.user.schedule.ShiftSettingsDto;
import ru.yandex.market.tpl.core.service.user.schedule.SlotDistributionService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleService;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleTestHelper;
import ru.yandex.market.tpl.internal.BaseShallowTest;
import ru.yandex.market.tpl.internal.WebLayerTest;
import ru.yandex.market.tpl.internal.service.report.csv.ScheduleReportService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;
import static ru.yandex.market.tpl.core.service.user.schedule.ShiftDurationSettingsUtil.SHIFT_DURATION_HOURS;
import static ru.yandex.market.tpl.core.service.user.schedule.ShiftDurationSettingsUtil.SHIFT_END_TIME;
import static ru.yandex.market.tpl.internal.config.filter.PartnerRequestsFilter.UID_HEADER;

/**
 * @author kukabara
 */
@WebLayerTest(PartnerUserScheduleController.class)
class PartnerUserScheduleControllerTest extends BaseShallowTest {

    private static final ShiftSettingsDto DEFAULT_SHIFT_SETTINGS_DTO =
            new ShiftSettingsDto(SHIFT_DURATION_HOURS, SHIFT_END_TIME);
    private static final long USER_ID = 1L;

    @SpyBean
    private PartnerkaCommandService commandService;

    @MockBean
    private UserScheduleService userScheduleService;
    @MockBean
    private PartnerCompanyRoleService partnerCompanyRoleService;
    @MockBean
    private PartnerkaCommandRepository commandRepository;
    @MockBean
    private ScheduleReportService scheduleReportService;
    @MockBean
    private SlotDistributionService slotDistributionService;
    @MockBean
    private CommonSlotService commonSlotService;
    @MockBean
    private CompanyAuthenticationService companyAuthenticationService;

    @Test
    void shouldBeValidRequestWhenOverrideSkip() throws Exception {
        mockRuleResult(UserScheduleType.OVERRIDE_SKIP);
        mockMvc.perform(post("/internal/partner/users/{userId}/schedule-rules", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1)
                        .content("{\n" +
                                "  \"scheduleType\": \"OVERRIDE_SKIP\",\n" +
                                "  \"activeFrom\": \"2019-10-28\"\n" +
                                "}")
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\n" +
                                "  \"scheduleType\": \"OVERRIDE_SKIP\",\n" +
                                "  \"scheduleMetaType\": \"OVERRIDE\",\n" +
                                "  \"activeFrom\": \"2019-10-28\",\n" +
                                "  \"activeTo\": \"2019-10-28\",\n" +
                                "  \"applyFrom\": \"2019-10-28\",\n" +
                                "  \"maskWorkDays\": [false]\n" +
                                "}",
                        true));
    }

    @Test
    void shouldBeNotValidRequestWhenOther() throws Exception {
        mockMvc.perform(post("/internal/partner/users/{userId}/schedule-rules", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(UID_HEADER, 1)
                        .content("{\n" +
                                "  \"scheduleType\": \"OVERRIDE_WORK\",\n" +
                                "  \"activeFrom\": \"2019-10-28\"\n" +
                                "}")
                )
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldBeValidRequestWhenOther() throws Exception {
        mockRuleResult(UserScheduleType.OVERRIDE_WORK);
        mockMvc.perform(post("/internal/partner/users/{userId}/schedule-rules", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1)
                        .content("{\n" +
                                "  \"scheduleType\": \"OVERRIDE_WORK\",\n" +
                                "  \"shiftStart\": \"09:00:00\",\n" +
                                "  \"shiftEnd\": \"22:00:00\",\n" +
                                "  \"vehicleType\": \"CAR\",\n" +
                                "  \"sortingCenterId\": 1,\n" +
                                "  \"activeFrom\": \"2019-10-28\",\n" +
                                "  \"applyFrom\": \"2019-10-28\"\n" +
                                "}")
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json("{\n" +
                                "  \"scheduleType\": \"OVERRIDE_WORK\",\n" +
                                "  \"scheduleMetaType\": \"OVERRIDE\",\n" +
                                "  \"shiftStart\": \"10:00:00\",\n" +
                                "  \"shiftEnd\": \"18:00:00\",\n" +
                                "  \"vehicleType\": \"CAR\",\n" +
                                "  \"sortingCenterId\": 1,\n" +
                                "  \"activeFrom\": \"2019-10-28\",\n" +
                                "  \"activeTo\": \"2019-10-28\",\n" +
                                "  \"applyFrom\": \"2019-10-28\",\n" +
                                "  \"shiftDuration\":\"PT8H\",\n" +
                                "  \"maskWorkDays\": [true]\n" +
                                "}",
                        true));
    }

    @Test
    void shouldVeValidDefaultShiftSettings() throws Exception {
        when(userScheduleService.getShiftDurationSettings()).thenReturn(DEFAULT_SHIFT_SETTINGS_DTO);

        mockMvc.perform(get("/internal/partner/users/schedules/shift-duration-settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(COMPANY_HEADER, 1)
                )
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("partner/response_shift_duration_settings.json")));
    }

    private void mockRuleResult(UserScheduleType type) {
        LocalDate ruleDate = LocalDate.parse("2019-10-28");
        when(userScheduleService.createRule(eq(USER_ID), any(), anyBoolean()))
                .thenReturn(UserScheduleTestHelper.ruleDto(type, ruleDate, ruleDate));
        when(commandRepository.save(any())).thenReturn(new PartnerkaCommandEvent());
    }

}
