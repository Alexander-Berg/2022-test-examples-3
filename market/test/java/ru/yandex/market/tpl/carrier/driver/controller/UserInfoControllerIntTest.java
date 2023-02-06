package ru.yandex.market.tpl.carrier.driver.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserCommandService;
import ru.yandex.market.tpl.carrier.core.domain.user.UserUtil;
import ru.yandex.market.tpl.carrier.core.domain.user.commands.UserCommand;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.carrier.driver.api.model.company.CompanyTypeDto;
import ru.yandex.market.tpl.carrier.driver.web.auth.ApiParams;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RequiredArgsConstructor(onConstructor_=@Autowired)
public class UserInfoControllerIntTest extends BaseDriverApiIntTest {

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TestUserHelper testUserHelper;
    private final UserCommandService commandService;

    private Company company;
    private Company company2;
    private User user;

    @BeforeEach
    void setUp() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.USER_DRAFT_ENABLED, true);

        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        company2 = testUserHelper.findOrCreateCompany(TestUserHelper.CompanyGenerateParam.builder()
                .companyName(Company.DEFAULT_COMPANY_NAME + ".2")
                .login("def@yandex.ru")
                .campaignId(456789L)
                .build());
        user = testUserHelper.findOrCreateUser(UserUtil.TAXI_ID, UID);
        commandService.addCompanyIfAbsent(new UserCommand.AddCompanyIfAbsent(user.getId(), company2.getId()));
    }

    @SneakyThrows
    @Test
    void shouldSelectCompanyFromList() {
        getUserInfoForActions()
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.selectedCompany").value(Matchers.nullValue()));

        getCompaniesForActions()
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$").value(Matchers.hasSize(2)))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").value(company.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].name").value(Company.DEFAULT_COMPANY_NAME))
                .andExpect(MockMvcResultMatchers.jsonPath("$[0].type").value(CompanyTypeDto.MARKET_MAGISTRAL.name()));

        chooseCompanyForActions()
                .andExpect(status().isOk());

        getUserInfoForActions()
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.selectedCompany.id").value(company.getId()));
    }

    private ResultActions getCompaniesForActions() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(ApiParams.BASE_PATH + "/companies")
                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
        );
    }

    private ResultActions chooseCompanyForActions() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(ApiParams.BASE_PATH + "/companies/choice")
                .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                .param("companyId", String.valueOf(company.getId())));
    }

    private ResultActions getUserInfoForActions() throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(ApiParams.BASE_PATH + "/user-info")
                        .header(ApiParams.TAXI_PARK_ID_HEADER, TAXI_PARK_ID_HEADER_VALUE)
                        .header(ApiParams.TAXI_PROFILE_ID_HEADER, TAXI_PROFILE_ID_HEADER_VALUE)
                );
    }
}
