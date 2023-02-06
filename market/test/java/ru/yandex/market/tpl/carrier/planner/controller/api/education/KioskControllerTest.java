package ru.yandex.market.tpl.carrier.planner.controller.api.education;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.education.program.Program;
import ru.yandex.market.tpl.carrier.core.domain.education.session.SessionRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserSource;
import ru.yandex.market.tpl.carrier.core.external.kiosk.KioskApiClient;
import ru.yandex.market.tpl.carrier.core.external.kiosk.model.FioDto;
import ru.yandex.market.tpl.carrier.core.external.kiosk.model.RegistrationInfoDto;
import ru.yandex.market.tpl.carrier.core.external.kiosk.model.SessionDto;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.CampusSessionPassedDto;
import ru.yandex.mj.generated.server.model.CampusSessionPayloadDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class KioskControllerTest extends BasePlannerWebTest {

    private final String SESSION_UID = "ses-uid-1";
    private final String PROGRAM_UID = "program-uid-1";

    private final TestUserHelper testUserHelper;
    private final SessionRepository sessionRepository;
    private final KioskApiClient testKioskApiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Company firstCompany;
    private User firstUser;
    private Program program;
    private SessionDto sessionDto;

    @BeforeEach
    void setup() {
        String firstPhoneNumber = "78005553535";
        firstCompany = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        firstUser = testUserHelper.findOrCreateUser(
                5L,
                Company.DEFAULT_COMPANY_NAME,
                "+" + firstPhoneNumber,
                "Иванов", "Василий", "Маркетович",
                UserSource.LOGISTICS_COORDINATOR
        );

        sessionDto = new SessionDto(SESSION_UID, "Яндекс Маркет", "completed", true, firstPhoneNumber,
                new RegistrationInfoDto(new FioDto(firstUser.getFullName(), firstUser.getFullName())));

        Mockito.when(testKioskApiClient.apiGetSession(Mockito.any())).thenReturn(sessionDto);

        program = testUserHelper.findOrCreateProgram(PROGRAM_UID);

    }


    @SneakyThrows
    @Test
    void postSession() {

        var dto = new CampusSessionPassedDto()
                .payload(new CampusSessionPayloadDto().sessionUuid(SESSION_UID).passed(true)
                        .completedAt(Instant.now().getEpochSecond())
                        .educationCompletedAt(Instant.now().getEpochSecond())
                        .programUuid(program.getUuid())
                );

        mockMvc.perform(post("/external/education/session")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        var session = sessionRepository.findByUuid(SESSION_UID).orElseThrow();

        Assertions.assertThat(session.getUser()).isEqualTo(firstUser);
        Assertions.assertThat(session.getProgram()).isEqualTo(program);
    }

    @SneakyThrows
    @Test
    void postSessionUpdate() {

        var dto = new CampusSessionPassedDto()
                .payload(new CampusSessionPayloadDto().sessionUuid(SESSION_UID).passed(false)
                        .completedAt(Instant.now().getEpochSecond())
                        .educationCompletedAt(Instant.now().getEpochSecond())
                        .programUuid(program.getUuid())
                );

        mockMvc.perform(post("/external/education/session")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());

        var session = sessionRepository.findByUuid(SESSION_UID).orElseThrow();

        Assertions.assertThat(session.getUser()).isEqualTo(firstUser);
        Assertions.assertThat(session.getProgram()).isEqualTo(program);
        Assertions.assertThat(session.isPassed()).isEqualTo(false);

        mockMvc.perform(post("/external/education/session")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());


    }

    @AfterEach
    void cleanUp() {
        sessionRepository.deleteAll();
    }
}
