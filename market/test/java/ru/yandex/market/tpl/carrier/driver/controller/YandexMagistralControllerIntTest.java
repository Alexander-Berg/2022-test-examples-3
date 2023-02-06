package ru.yandex.market.tpl.carrier.driver.controller;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.Base64Utils;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.company_property.CompanyProperty;
import ru.yandex.market.tpl.carrier.core.domain.company_property.CompanyPropertyRepository;
import ru.yandex.market.tpl.carrier.core.domain.company_property.CompanyPropertyType;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.mj.generated.server.model.CreateYandexMagistralUserDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class YandexMagistralControllerIntTest extends BaseDriverApiIntTest {

    private final ObjectMapper tplObjectMapper;
    private final TestUserHelper testUserHelper;
    private final CompanyPropertyRepository companyPropertyRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private String username;
    private String password;

    private Company company;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);
        CompanyProperty<Object> property = new CompanyProperty<>();
        property.init(company, CompanyPropertyType.CompanyPropertyName.IS_YANDEX_MAGISTRAL, Boolean.toString(true));
        companyPropertyRepository.save(property);

        username = "login";
        configurationServiceAdapter.mergeValue(ConfigurationProperties.YANDEX_MAGISTRAL_USER, username);
        password = "password";
        configurationServiceAdapter.mergeValue(ConfigurationProperties.YANDEX_MAGISTRAL_PASSWORD_BCRYPT, new BCryptPasswordEncoder().encode(password));
    }

    @SneakyThrows
    @Test
    void shouldCreateUser() {
        mockMvc.perform(post("/yandex-magistral/users")
                .header(HttpHeaders.AUTHORIZATION,
                        "Basic " + Base64Utils.encodeToString(
                                (username + ":" + password).getBytes(StandardCharsets.UTF_8)
                        ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(tplObjectMapper.writeValueAsString(new CreateYandexMagistralUserDto()
                        .lastName("Иванов")
                        .firstName("Петр")
                        .patronymic("Сидровоич")
                        .phone("+79272403522")
                        .companyId(company.getId())
                ))
        )
                .andExpect(status().isOk());

    }
}
