package ru.yandex.market.tpl.carrier.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.planner.lms.user.LmsUserAddCompanyDto;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class LmsUserControllerCompaniesTest extends LmsControllerTest {

    private static final long CAMPAIGN_ID_2 = 1234L;

    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    private Company company;
    private Company company2;
    private User user;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        company2 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME + ".2")
                .login("def@yandex.ru")
                .campaignId(CAMPAIGN_ID_2)
                .build());
        user = testUserHelper.findOrCreateUser(UserUtil.UID);
    }

    @SneakyThrows
    @Test
    void shouldGetCompaniesByUser() {
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/carrier/users/{id}/companies", user.getId()))
                .andExpect(jsonPath("$.items").value(Matchers.hasSize(1)));
    }

    @SneakyThrows
    @Test
    void shouldGetAddCompanyToUserPage() {
        mockMvc.perform(MockMvcRequestBuilders.get("/LMS/carrier/users/{id}/companies/new", user.getId()))
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldAddCompanyToUser() {
        mockMvc.perform(MockMvcRequestBuilders.post("/LMS/carrier/users/companies", user.getId())
                        .content(tplObjectMapper.writeValueAsString(
                                new LmsUserAddCompanyDto(user.getId(), company2.getId())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        transactionTemplate.execute(tc -> {
            User userFromDB = userRepository.findByIdOrThrow(user.getId());
            Assertions.assertThat(userFromDB.getCompanies())
                    .hasSize(2);
            return null;
        });
    }
}
