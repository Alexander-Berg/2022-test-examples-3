package ru.yandex.market.tpl.carrier.driver.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.version.UpdateRequired;
import ru.yandex.market.tpl.carrier.core.domain.version.UserAppVersion;
import ru.yandex.market.tpl.carrier.core.domain.version.UserVersionRepository;
import ru.yandex.market.tpl.carrier.core.domain.version.Version;
import ru.yandex.market.tpl.carrier.core.domain.version.VersionRepository;
import ru.yandex.market.tpl.carrier.driver.BaseDriverApiIntTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class VersionControllerIntTest extends BaseDriverApiIntTest {

    private static final String GOOGLE_URL = "https://example.org/google";
    private static final String HUAWEI_URL = "https://example.org/huawei";

    private final TestUserHelper testUserHelper;
    private final VersionRepository versionRepository;
    private final UserVersionRepository userVersionRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);

        Version version = new Version();
        version.setVersion("1.23");
        version.setLatest(true);
        version.setDescription("Новая крутая версия");
        version.setUrl(GOOGLE_URL);
        version.setHuaweiUrl(HUAWEI_URL);
        version.setUpdateRequired(UpdateRequired.REQUIRED);

        versionRepository.save(version);
    }

    @SneakyThrows
    @Test
    void shouldReturnLatestVersion() {
        mockMvc.perform(
                get("/api/app/versions/latest")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .header("App-Version", "1.22")
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.url").value("https://example.org/google"),
                        jsonPath("$.updateRequired").value(UpdateRequired.REQUIRED.name())
                ));
    }

    @SneakyThrows
    @Test
    void shouldReturnNoUpdateRequired() {
        mockMvc.perform(
                get("/api/app/versions/latest")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header("App-Version", "1.23")
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.url").value("https://example.org/google"),
                        jsonPath("$.updateRequired").value(ru.yandex.market.tpl.carrier.driver.api.model.app.UpdateRequired.UPDATE_NOT_NEEDED.name())
                ));
    }

    @SneakyThrows
    @Test
    void shouldReturnHuaweiUrl() {
        mockMvc.perform(
                get("/api/app/versions/latest")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header("App-Version", "1.22")
                        .header("Device-Type", "HUAWEI")
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.url").value("https://example.org/huawei"),
                        jsonPath("$.updateRequired").value(ru.yandex.market.tpl.carrier.driver.api.model.app.UpdateRequired.REQUIRED.name())
                ));
    }

    @SneakyThrows
    @Test
    void shouldReturnCustomVersionForUser() {
        Version version = new Version();
        version.setVersion("1.24");
        version.setLatest(false);
        version.setDescription("Новая крутая версия");
        version.setUrl(GOOGLE_URL + "2");
        version.setHuaweiUrl(HUAWEI_URL + "2");
        version.setUpdateRequired(UpdateRequired.REQUIRED);

        versionRepository.save(version);

        UserAppVersion userAppVersion = new UserAppVersion();
        userAppVersion.setUser(user);
        userAppVersion.setAppVersion(version);

        userVersionRepository.save(userAppVersion);

        mockMvc.perform(
                get("/api/app/versions/latest")
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header("App-Version", "1.22")
        )
                .andExpect(ResultMatcher.matchAll(
                        status().isOk(),
                        jsonPath("$.version").value("1.24")
                ));
    }

}
