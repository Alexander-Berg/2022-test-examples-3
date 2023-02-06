package ru.yandex.market.tpl.carrier.planner.controller.manual;

import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ManualUserShiftControllerTest extends BasePlannerWebTest {

    private final TestUserHelper testUserHelper;
    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final UserShiftRepository userShiftRepository;

    @Test
    @SneakyThrows
    void shouldCloseUserShift() {

        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        User user = testUserHelper.findOrCreateUser(123);
        Transport transport = testUserHelper.findOrCreateTransport();
        Run run = runGenerator.generate();
        UserShift userShift = runHelper.assignUserAndTransport(run, user, transport);

        Assertions.assertEquals(UserShiftStatus.SHIFT_CREATED, userShift.getStatus());

        mockMvc.perform(post("/manual/user-shifts/close")
                .param("userShiftId", userShift.getId().toString()))
                .andExpect(status().isOk());

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());

        Assertions.assertEquals(UserShiftStatus.SHIFT_FINISHED, userShift.getStatus());

    }

}
