package ru.yandex.market.tpl.carrier.lms.controller;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.audit.CarrierAuditTracer;
import ru.yandex.market.tpl.carrier.core.audit.CarrierSource;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.planner.lms.IdDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsUserShiftControllerTest extends LmsControllerTest {

    private final TestUserHelper testUserHelper;
    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final ObjectMapper tplObjectMapper;

    private final UserShiftRepository userShiftRepository;

    private User user;
    private Transport transport;
    private Run run;
    private UserShift userShift;
    private LocalDate runDate;

    static {
        CarrierAuditTracer.putSource(CarrierSource.SYSTEM);
    }

    @BeforeEach
    void setUp() {
        testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        user = testUserHelper.findOrCreateUser(1L);
        transport = testUserHelper.findOrCreateTransport();

        runDate = LocalDate.of(2021, 11, 21);
        run = runGenerator.generate(r -> {
            return r.runDate(runDate)
                    .clearItems()
                    .item(RunGenerator.RunItemGenerateParam.builder()
                            .movement(MovementCommand.Create.builder()
                                    .deliveryIntervalFrom(runDate.atTime(9, 0)
                                            .atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .deliveryIntervalTo(runDate.atTime(18, 0)
                                            .atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .build())
                            .orderNumber(1)
                            .build());
        });
        userShift = runHelper.assignUserAndTransport(run, user, transport);
    }

    @SneakyThrows
    @Test
    void shouldGetUserShifts() {
        mockMvc.perform(get("/LMS/carrier/user-shifts")
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @Test
    void shouldFilterByRun() {
        Run run2 = runGenerator.generate();
        runHelper.assignUserAndTransport(run2, user, transport);
        mockMvc.perform(get("/LMS/carrier/user-shifts")
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(2)));
        mockMvc.perform(get("/LMS/carrier/user-shifts")
                .param("run", String.valueOf(run.getId()))
                .accept(MediaType.APPLICATION_JSON_UTF8)
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @Test
    void shouldFilterByStartDate() {
        LocalDate runDate2 = runDate.plusDays(1);
        Run run2 = runGenerator.generate(r -> {
            return r.runDate(runDate2)
                    .clearItems()
                    .item(RunGenerator.RunItemGenerateParam.builder()
                            .movement(MovementCommand.Create.builder()
                                    .inboundArrivalTime(runDate2.atTime(9, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .deliveryIntervalFrom(runDate2.atTime(9, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .deliveryIntervalTo(runDate2.atTime(18, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .outboundArrivalTime(runDate2.atTime(18, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .build())
                            .orderNumber(1)
                            .build());
        });
        var userShift2 = runHelper.assignUserAndTransport(run2, user, transport);

        mockMvc.perform(get("/LMS/carrier/user-shifts")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .param("shiftDate", runDate2.toString())
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(userShift2.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldSortByStartDate() {
        Run run2 = runGenerator.generate(r -> {
            return r.runDate(runDate)
                    .clearItems()
                    .item(RunGenerator.RunItemGenerateParam.builder()
                            .movement(MovementCommand.Create.builder()
                                    .inboundArrivalTime(runDate.atTime(10, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .deliveryIntervalFrom(runDate.atTime(10, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .outboundArrivalTime(runDate.atTime(19, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .deliveryIntervalTo(runDate.atTime(19, 0).atZone(DateTimeUtil.DEFAULT_ZONE_ID).toInstant())
                                    .build())
                            .orderNumber(1)
                            .build());
        });
        var userShift2 = runHelper.assignUserAndTransport(run2, user, transport);

        mockMvc.perform(get("/LMS/carrier/user-shifts")
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .param("sort", "shiftDate,desc")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(userShift2.getId()))
                .andExpect(jsonPath("$.items[1].id").value(userShift.getId()))
        ;
    }

    @SneakyThrows
    @Test
    void shouldGetUserShift() {
        mockMvc.perform(get("/LMS/carrier/user-shifts/{id}", userShift.getId()))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldActivateUserShift() {

        mockMvc.perform(post("/LMS/carrier/user-shifts/activate")
                        .content(tplObjectMapper.writeValueAsString(new IdDto(userShift.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        Assertions.assertThat(userShift.isActive()).isTrue();
        Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        Assertions.assertThat(userShift.getCurrentRoutePoint()).isNotNull();

        Run run2 = runGenerator.generate();
        var userShift2 = runHelper.assignUserAndTransport(run2, user, transport);

        Assertions.assertThat(userShift2.isActive()).isFalse();

        mockMvc.perform(post("/LMS/carrier/user-shifts/activate")
                .content(tplObjectMapper.writeValueAsString(new IdDto(userShift2.getId())))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        userShift2 = userShiftRepository.findByIdOrThrow(userShift2.getId());

        Assertions.assertThat(userShift.isActive()).isFalse();
        Assertions.assertThat(userShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        Assertions.assertThat(userShift.getCurrentRoutePoint()).isNotNull();

        Assertions.assertThat(userShift2.isActive()).isTrue();
        Assertions.assertThat(userShift2.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        Assertions.assertThat(userShift2.getCurrentRoutePoint()).isNotNull();
    }

    @SneakyThrows
    @Test
    void shouldDeactivateUserShift() {

        mockMvc.perform(post("/LMS/carrier/user-shifts/activate")
                        .content(tplObjectMapper.writeValueAsString(new IdDto(userShift.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        Assertions.assertThat(userShift.isActive()).isTrue();

        mockMvc.perform(post("/LMS/carrier/user-shifts/deactivate")
                        .content(tplObjectMapper.writeValueAsString(new IdDto(userShift.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        Assertions.assertThat(userShift.isActive()).isFalse();
    }

    @SneakyThrows
    @Test
    void shouldDeactivateStartedUserShift() {
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.arriveAtRoutePoint(userShift.streamRoutePoints().findFirst().get());
        mockMvc.perform(post("/LMS/carrier/user-shifts/deactivate")
                        .content(tplObjectMapper.writeValueAsString(new IdDto(userShift.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        Assertions.assertThat(userShift.isActive()).isFalse();
    }


    @SneakyThrows
    @Test
    void shouldForceClose() {

        mockMvc.perform(post("/LMS/carrier/user-shifts/forceClose")
                        .content(tplObjectMapper.writeValueAsString(new IdDto(userShift.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        userShift = userShiftRepository.findByIdOrThrow(userShift.getId());
        Assertions.assertThat(userShift.isClosed()).isTrue();

        Run run2 = runGenerator.generate();
        var userShift2 = runHelper.assignUserAndTransport(run2, user, transport);
        testUserHelper.openShift(user, userShift2.getId());

        mockMvc.perform(post("/LMS/carrier/user-shifts/forceClose")
                        .content(tplObjectMapper.writeValueAsString(new IdDto(userShift2.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());

        userShift2 = userShiftRepository.findByIdOrThrow(userShift2.getId());
        Assertions.assertThat(userShift2.isClosed()).isTrue();
    }

    @SneakyThrows
    @Test
    void shouldNotCloseIfHasNotFinishedTasks() {

        mockMvc.perform(post("/LMS/carrier/user-shifts/close")
                        .content(tplObjectMapper.writeValueAsString(new IdDto(userShift.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andDo(print())
                .andExpect(status().isBadRequest());
    }

    @SneakyThrows
    @Test
    void shouldCloseIfAllTasksFinished() {

        testUserHelper.openShift(user, userShift.getId());

        mockMvc.perform(post("/LMS/carrier/user-shifts/switchToNext")
                        .content(tplObjectMapper.writeValueAsString(new IdDto(userShift.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andDo(print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/LMS/carrier/user-shifts/close")
                        .content(tplObjectMapper.writeValueAsString(new IdDto(userShift.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                ).andDo(print())
                .andExpect(status().isOk());
    }

}
