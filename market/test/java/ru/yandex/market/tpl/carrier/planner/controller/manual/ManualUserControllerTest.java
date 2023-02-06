package ru.yandex.market.tpl.carrier.planner.controller.manual;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserFacade;
import ru.yandex.market.tpl.carrier.core.domain.user.UserRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.UserRole;
import ru.yandex.market.tpl.carrier.core.domain.user.UserSource;
import ru.yandex.market.tpl.carrier.core.domain.user.UserStatus;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.NewDriverData;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.core.domain.user.data.UserData;
import ru.yandex.market.tpl.carrier.core.domain.user.util.UsersUtil;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.manual.draft.ManualUserDraftRequestDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ManualUserControllerTest extends BasePlannerWebTest {

    private final TestUserHelper testUserHelper;
    private final UserFacade userFacade;
    private final UserCommandService userCommandService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Test
    @SneakyThrows
    void shouldPromoteDraft() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        var userData = UserData.builder()
                .phone("+79295585060")
                .firstName(UserUtil.FIRST_NAME)
                .lastName(UserUtil.LAST_NAME)
                .source(UserSource.CARRIER)
                .build();

        User user = userFacade.createUser(userData, company);

        ManualUserDraftRequestDto dto = new ManualUserDraftRequestDto();
        dto.setPhone(userData.getPhone());
        dto.setUid(user.getUid());

        Assertions.assertEquals(UserStatus.DRAFT, user.getStatus());

        mockMvc.perform(post("/manual/user-drafts/promote/")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsString(dto))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("id").value(user.getId()));
        user = userRepository.findById(user.getId()).orElseThrow();

        Assertions.assertEquals(UserStatus.NOT_ACTIVE, user.getStatus());
    }

    @Test
    @SneakyThrows
    void shouldDeleteUser() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);


        User user = executeInTransaction(tc -> {
            Long uid = userRepository.getNextFakeUid();
            var userData = NewDriverData.builder()
                    .dsmId(UserUtil.DEFAULT_DSM_ID_1)
                    .role(UserRole.COURIER)
                    .uid(uid)
                    .email(UsersUtil.fakeEmail(uid))
                    .lastName("lastName")
                    .firstName("fistName")
                    .patronymic("patronymic")
                    .phone("+79998887722")
                    .company(company)
                    .source(UserSource.CARRIER)
                    .passport(null)
                    .build();

            return userCommandService.create(new UserCommand.CreateDriver(userData));
        });


        mockMvc.perform(delete("/manual/users/{id}", user.getId()))
                .andExpect(status().isNoContent());

        Assertions.assertTrue(userRepository.findById(user.getId()).isEmpty());

        // Повторный вызов вернёт 404, так как сущность удалена
        mockMvc.perform(delete("/manual/users/{id}", user.getId()))
                .andExpect(status().isNotFound());
    }

    @Disabled("Ручка работает отлично, но не при запусках тестов в CI, даже ya make нормально запускает тест")
    @SneakyThrows
    @Test
    void userListForEducationIsWorking() {
        mockMvc.perform(get("/manual/user-draft/export/userListForEducation"))
                .andExpect(status().isOk());
    }

}
