package ru.yandex.market.tpl.carrier.planner.controller.api.user;

import java.time.LocalDate;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserQueryService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserSource;
import ru.yandex.market.tpl.carrier.core.domain.user.UserStatus;
import ru.yandex.market.tpl.carrier.core.domain.user.data.PassportData;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.PersonalDataDto;
import ru.yandex.mj.generated.server.model.UserCompanyDto;
import ru.yandex.mj.generated.server.model.UserCreateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserControllerTest extends BasePlannerWebTest {

    public static final long ANOTHER_DS_ID = 124L;
    private final TestUserHelper testUserHelper;
    private final UserQueryService userQueryService;

    private Company firstCompany;
    private Company secondCompany;
    private User firstUser;
    private User secondUser;

    @BeforeEach
    void setup() {
        firstCompany = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        secondCompany = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName("Company2")
                .campaignId(23L)
                .deliveryServiceIds(Set.of(ANOTHER_DS_ID))
                .login("login")
                .build());

        firstUser = testUserHelper.findOrCreateUser(
                5L,
                Company.DEFAULT_COMPANY_NAME,
                "+78005553535",
                "Иванов", "Василий", "Маркетович",
                UserSource.LOGISTICS_COORDINATOR
        );

        secondUser = testUserHelper.findOrCreateUser(6L, "Company2", "+79773452341");
    }

    @Test
    void create() throws Exception {
        UserCreateDto dto = new UserCreateDto()
                .companyId(firstCompany.getId())
                .name("Павел")
                .surname("Павлов")
                .patronymic("Павлович")
                .phone("89778234512");

        mockMvc.perform(post("/internal/users/create")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(toJson(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("+79778234512"))
                .andExpect(jsonPath("$.firstName").value("Павел"))
                .andExpect(jsonPath("$.lastName").value("Павлов"))
                .andExpect(jsonPath("$.source").value("LOGISTICS_COORDINATOR"))
                .andExpect(jsonPath("$.companies").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.companies[0].id").value(firstCompany.getId()))
                .andExpect(jsonPath("$.companies[0].name").value(firstCompany.getName()))
                .andExpect(jsonPath("$.patronymic").value("Павлович"));

        transactionTemplate.execute(tx -> {
            assertThat(userQueryService.findByPhoneAndDeletedFalse("+79778234512").get())
                    .extracting(
                            User::getFirstName,
                            User::getLastName,
                            User::getPatronymic,
                            User::getStatus,
                            User::getSource,
                            User::getCompanies,
                            User::isDeleted
                    )
                    .containsExactly(
                            "Павел",
                            "Павлов",
                            "Павлович",
                            UserStatus.DRAFT,
                            UserSource.LOGISTICS_COORDINATOR,
                            Set.of(testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME)),
                            false
                    );
            return null;
        });

        // user already exists - при совпадении ФИО и телефона ошибки не будет
        dto.setName(dto.getName() + ")");
        mockMvc.perform(post("/internal/users/create")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(toJson(dto)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @SneakyThrows
    void testGet() {

        mockMvc.perform(get("/internal/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));

        mockMvc.perform(get("/internal/users")
                          .param("id", firstUser.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
            .andExpect(jsonPath("$.content[0].id").value(firstUser.getId()));

        mockMvc.perform(get("/internal/users")
                        .param("phone", "79773452341"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].phone").value("+79773452341"))
                .andExpect(jsonPath("$.content[0].id").value(secondUser.getId()));

        mockMvc.perform(get("/internal/users")
                        .param("phone", "79773452341")
                        .param("companyId", firstCompany.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(0)));

        mockMvc.perform(get("/internal/users")
                        .param("phone", "8(800)555-35-35")
                        .param("companyId", firstCompany.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].phone").value("+78005553535"))
                .andExpect(jsonPath("$.content[0].id").value(firstUser.getId()));


        mockMvc.perform(get("/internal/users")
                        .param("fullName", "Васил"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(firstUser.getId()));
    }

    @Test
    void testAddCompany() throws Exception {
        mockMvc.perform(post("/internal/users/add-company")
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(toJson(
                                new UserCompanyDto().companyId(secondCompany.getId()).userId(firstUser.getId())))
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstUser.getId()))
                .andExpect(jsonPath("$.companies").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.companies[0].id").value(firstCompany.getId()))
                .andExpect(jsonPath("$.companies[0].name").value(firstCompany.getName()))
                .andExpect(jsonPath("$.companies[1].id").value(secondCompany.getId()))
                .andExpect(jsonPath("$.companies[1].name").value(secondCompany.getName()));

        // can add company to carrier user
        mockMvc.perform(post("/internal/users/add-company")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(toJson(
                        new UserCompanyDto().companyId(firstCompany.getId()).userId(secondUser.getId())))
        ).andExpect(status().isOk());

    }

    @SneakyThrows
    @Test
    void testFindByDeliveryServiceId() {
        mockMvc.perform(get("/internal/users")
                        .param("deliveryServiceId", String.valueOf(DeliveryService.DEFAULT_DS_ID))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));

        mockMvc.perform(get("/internal/users")
                        .param("deliveryServiceId", String.valueOf(124L))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @Test
    void getUserWithBirthDate() {
        LocalDate birthDate = LocalDate.of(2000, 1, 1);

        var passport = PassportData.builder()
                .citizenship("RUS")
                .serialNumber("1234123456")
                .birthDate(birthDate)
                .issueDate(LocalDate.of(2014, 1, 1))
                .issuer("ТП УФМС Гондора в Минас Тирит")
                .build();

        testUserHelper.addPassport(secondUser.getUid(), passport);

        mockMvc.perform(get("/internal/users")
                        .param("phone", "+79773452341")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].phone").value("+79773452341"))
                .andExpect(jsonPath("$.content[0].birthDate").value(birthDate.toString()));
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(strings = {
            "+79998881122",
            "79998881122",
            "89998881122",
            "8(999)888-11-22",
            "1122",
            "988811",
            "+7 999 888 11 22",
            "Крутоу",
            "Крутоумов    Ива +7 999 888 11 22",
            "+7 999 888 11 22 умов  Ива",
            "2000-01-02",
            "2000.01.02",
            "2000/01/02",
            "02-01-2000",
            "02.01.2000",
            "02/01/2000",
            "Крутоумов Иван 2000-01-02"
    })
    void search(String search) {
        String expectedPhone = "+79998881122";
        var user = testUserHelper.findOrCreateUser(10L, expectedPhone, "Крутоумов", "Иван", "Андреевич");

        LocalDate birthDate = LocalDate.of(2000, 1, 2);
        var passport = PassportData.builder()
                .citizenship("RUS")
                .serialNumber("1234123456")
                .birthDate(birthDate)
                .issueDate(LocalDate.of(2014, 1, 1))
                .issuer("ТП УФМС Гондора в Минас Тирит")
                .build();

        testUserHelper.addPassport(user.getUid(), passport);

        mockMvc.perform(get("/internal/users")
                        .param("search", search)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].phone").value(expectedPhone));
    }

    @SneakyThrows
    @Test
    void search() {
        testUserHelper.findOrCreateUser(10L, "+79998881122", "Крутоумов", "Иван", "Андреевич");
        testUserHelper.findOrCreateUser(15L, "+79998881123", "Крутоумов", "Иван", "Андреевич");

        mockMvc.perform(get("/internal/users")
                        .param("search", "Крутоумов    Ива 1123")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].phone").value("+79998881123"));
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(strings = {
            "Крутоумов    Ива +7 999 888 11",
            "+7 999 888 11 умов  Ива",
    })
    void searchAndMoreThanTwoResult(String search) {
        testUserHelper.findOrCreateUser(10L, "+79998881122", "Крутоумов", "Иван", "Андреевич");
        testUserHelper.findOrCreateUser(15L, "+79998881123", "Крутоумов", "Иван", "Андреевич");

        mockMvc.perform(get("/internal/users")
                        .param("search", search)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(Matchers.hasSize(2)));
    }

    @SneakyThrows
    @Test
    void getUserPersonalData() {
        var expected = new PersonalDataDto()
                        .firstName("Иван")
                        .lastName("Крутоумов")
                        .patronymic("Андреевич")
                        .phone("+79998881122");

        var user = testUserHelper.findOrCreateUser(
                10L,
                expected.getPhone(),
                expected.getLastName(),
                expected.getFirstName(),
                expected.getPatronymic()
        );

        expected.id(user.getId()).dsmId(user.getDsmId());

        mockMvc.perform(get("/internal/users/{userId}/personal", expected.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(toJson(expected)));
    }

}
