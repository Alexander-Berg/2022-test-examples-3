package ru.yandex.market.tpl.carrier.driver.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.UserFacade;
import ru.yandex.market.tpl.carrier.core.domain.user.UserRepository;
import ru.yandex.market.tpl.carrier.core.domain.user.UserSource;
import ru.yandex.market.tpl.carrier.core.domain.user.UserStatus;
import ru.yandex.market.tpl.carrier.core.domain.user.data.UserData;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;
import ru.yandex.market.tpl.common.web.blackbox.OAuthUser;
import ru.yandex.market.tpl.common.web.blackbox.OAuthUserPhone;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class PromoteUserFromDraftTest extends BaseDriverApiIntTest {

    private static final long UID = 2234562L;
    private static final long UID2 = 2234563L;
    private static final String AUTH_HEADER_VALUE = "OAuth USER_DRAFT_OAUTH";
    private static final String PHONE = "+7495223462";

    private final TestUserHelper testUserHelper;
    private final UserFacade userFacade;
    private final UserRepository userRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private User user;

    @BeforeEach
    void setUp() {
        Company company = testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME);

        var userData = UserData.builder()
                .phone(PHONE)
                .firstName("Курьер")
                .lastName("Драфтов")
                .source(UserSource.CARRIER)
                .build();

        user = userFacade.createUser(userData, company);

        OAuthUserPhone phone = new OAuthUserPhone();
        phone.setAttributes(Map.of(OAuthUserPhone.ATTRIBUTE_PHONE_E164, PHONE));
        mockBlackboxClient(new OAuthUser(UID, Set.of(), null, List.of(phone)));

        configurationServiceAdapter.mergeValue(ConfigurationProperties.USER_DRAFT_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_ENABLED, true);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_AHEAD_MINUTES, 12 * 60);
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DRIVER_NEXT_SHIFTS_IN_INTERVAL_LOOK_BEHIND_MINUTES, -12 * 60);
    }

    @Test
    void shouldPromoteUserFromDraft() throws Exception {
        mockMvc.perform(
                get("/api/shifts/current")
                    .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk());

        user = userRepository.findByUidOrThrow(UID);
        Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.NOT_ACTIVE);
    }

    @Test
    void shouldUpdateUserId() throws Exception {
        mockMvc.perform(
                get("/api/shifts/current")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk());

        user = userRepository.findByUidOrThrow(UID);
        Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.NOT_ACTIVE);

        OAuthUserPhone phone = new OAuthUserPhone();
        phone.setAttributes(Map.of(OAuthUserPhone.ATTRIBUTE_PHONE_E164, PHONE));
        mockBlackboxClient(new OAuthUser(UID2, Set.of(), null, List.of(phone)));

        mockMvc.perform(
                get("/api/shifts/current")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk());

        user = userRepository.findByUidOrThrow(UID2);
        Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.NOT_ACTIVE);
    }

    @Test
    void shouldNotPromoteIfUserAlreadyExists() throws Exception {
        testUserHelper.findOrCreateUser(UID);

        mockMvc.perform(
                get("/api/shifts/current")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk());

        user = userRepository.findById(user.getId()).orElseThrow();
        Assertions.assertThat(user.getStatus()).isEqualTo(UserStatus.DRAFT);
    }

}
