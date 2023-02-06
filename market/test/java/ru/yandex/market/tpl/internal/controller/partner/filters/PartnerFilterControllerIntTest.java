package ru.yandex.market.tpl.internal.controller.partner.filters;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.schedule.UserScheduleMetaType;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleStatus;
import ru.yandex.market.tpl.api.model.schedule.UserScheduleType;
import ru.yandex.market.tpl.api.model.user.CourierVehicleType;
import ru.yandex.market.tpl.api.model.user.partner.PartnerReportCourierToReassignOptionDto;
import ru.yandex.market.tpl.api.model.user.partner.ReassignTaskType;
import ru.yandex.market.tpl.common.util.datetime.RelativeTimeInterval;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleData;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRule;
import ru.yandex.market.tpl.core.service.user.schedule.UserScheduleRuleRepository;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler.COMPANY_HEADER;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class PartnerFilterControllerIntTest extends BaseTplIntWebTest {
    private static final long UID1 = 123456L;
    private static final long UID2 = 234567L;

    private static final long SC_ID = SortingCenter.DEFAULT_SC_ID;

    private final UserScheduleRuleRepository scheduleRuleRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final Clock clock;

    private User userWithDropshipSupport;
    private User userWithoutDropshipSupport;

    @BeforeEach
    void setUp() {
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        userWithDropshipSupport = testUserHelper.createUserWithTransportTags(UID1, List.of("dropship", "dropoff_cargo_return"));

        var company = testUserHelper.findOrCreateSuperCompany();

        LocalDate activeFrom = shift.getShiftDate();
        UserScheduleType scheduleType = UserScheduleType.ALWAYS_WORKS;
        RelativeTimeInterval schedule = RelativeTimeInterval.valueOf("09:00-19:00");

        SortingCenter sc = sortingCenterRepository.findByIdOrThrow(SC_ID);

        UserScheduleRule rule = new UserScheduleRule();
        rule.init(userWithDropshipSupport, scheduleType, sc, activeFrom,
                scheduleType.getMetaType() == UserScheduleMetaType.BASIC ? null : activeFrom,
                activeFrom, new UserScheduleData(CourierVehicleType.NONE, schedule),
                UserScheduleStatus.READY,
                scheduleType.getMaskWorkDays()
        );

        scheduleRuleRepository.save(rule);

        testUserHelper.createEmptyShift(userWithDropshipSupport, shift);

        userWithoutDropshipSupport = testUserHelper.findOrCreateUser(UID2);
        testUserHelper.createEmptyShift(userWithoutDropshipSupport, shift);
    }

    @Test
    void shouldReturnOnlyAvailableForDropshipUsers() throws Exception {
        String response = mockMvc.perform(
                        get("/internal/partner/filters/courierToReassign")
                                .param("sortingCenterId", String.valueOf(SC_ID))
                                .header(COMPANY_HEADER, -1L)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<LocalDate, List<PartnerReportCourierToReassignOptionDto>> responseObj = tplObjectMapper.readValue(
                response,
                new TypeReference<Map<LocalDate, List<PartnerReportCourierToReassignOptionDto>>>() {
                }
        );

        Assertions.assertThat(responseObj.values())
                .allMatch(l -> {
                    PartnerReportCourierToReassignOptionDto dto =
                            StreamEx.of(l).findFirst(p -> p.getCourierUid() == userWithoutDropshipSupport.getUid()).orElseThrow();
                    return dto.getReassignTaskTypes().contains(ReassignTaskType.ORDER)
                            && !dto.getReassignTaskTypes().contains(ReassignTaskType.MOVEMENT);
                })
                .allMatch(l -> {
                    PartnerReportCourierToReassignOptionDto dto =
                            StreamEx.of(l).findFirst(p -> p.getCourierUid() == userWithDropshipSupport.getUid()).orElseThrow();
                    return dto.getReassignTaskTypes().contains(ReassignTaskType.ORDER)
                            && dto.getReassignTaskTypes().contains(ReassignTaskType.MOVEMENT);
                })
        ;

    }
}
