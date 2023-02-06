package ru.yandex.market.tpl.carrier.lms.controller;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserFacade;
import ru.yandex.market.tpl.carrier.core.domain.user.UserSource;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.citizenship.CitizenshipRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.data.PassportData;
import ru.yandex.market.tpl.carrier.core.domain.user.data.UserData;
import ru.yandex.market.tpl.carrier.planner.lms.IdDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsUserControllerTest extends LmsControllerTest {

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final CitizenshipRepository citizenshipRepository;
    private final UserFacade userFacade;

    private Company company;
    private Company company2;
    private User user;
    private User user2;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        company2 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder().companyName("Другая")
                .campaignId(228L)
                .login("another-login@yandex.ru")
                .build());

        user = testUserHelper.findOrCreateUser(1L);
        user2 = testUserHelper.findOrCreateUser(UserUtil.ANOTHER_UID, company2.getName(), UserUtil.ANOTHER_PHONE);

    }

    @SneakyThrows
    @Test
    void shouldGetUsers() {
        mockMvc.perform(
                get("/LMS/carrier/users")
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void shouldFilterByCompany() {
        mockMvc.perform(
                get("/LMS/carrier/users")
                        .param("company", String.valueOf(company.getId()))
                        .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)));
    }
    @SneakyThrows
    @Test
    void shouldFilterByPhone() {
        mockMvc.perform(
                        get("/LMS/carrier/users")
                                .param("phone", UserUtil.PHONE)
                                .accept(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @Test
    void shouldGetUser() {
        mockMvc.perform(get("/LMS/carrier/users/{userId}", user.getId())
                .accept(MediaType.APPLICATION_JSON_UTF8)
        )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldGetUserFromDsm() {
        var citizenship = citizenshipRepository.findAll().stream().findFirst().orElseThrow();

        var passportData = PassportData.builder()
                .citizenship(citizenship.getId())
                .serialNumber("1234123456")
                .birthDate(LocalDate.of(2000, 1, 1))
                .issueDate(LocalDate.of(2014, 1, 1))
                .issuer("ТП УФМС Гондора в Минас Тирит")
                .build();

        var userData = UserData.builder()
                .phone("+79272403522")
                .firstName("Ололош")
                .lastName("Ололоев")
                .patronymic("Ололоевич")
                .passport(passportData)
                .source(UserSource.CARRIER)
                .build();

        User created = userFacade.createUser(userData, company);
        mockMvc.perform(get("/LMS/carrier/users/{userId}", created.getId())
                        .accept(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldMarkBlackListedAndUnmark() {
        assertThat(user.isBlackListed()).isFalse();
        mockMvc.perform(post("/LMS/carrier/users/addToBlackList", user.getId())
                        .content(tplObjectMapper.writeValueAsString(new IdDto(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());
        user = testUserHelper.findOrCreateUser(1L);
        assertThat(user.isBlackListed()).isTrue();

        mockMvc.perform(post("/LMS/carrier/users/removeFromBlackList", user.getId())
                        .content(tplObjectMapper.writeValueAsString(new IdDto(user.getId())))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                )
                .andExpect(status().isOk());
        user = testUserHelper.findOrCreateUser(1L);
        assertThat(user.isBlackListed()).isFalse();
    }
}
