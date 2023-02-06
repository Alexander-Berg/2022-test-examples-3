package ru.yandex.market.admin.service;

import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.core.passport.PassportService;
import ru.yandex.market.core.passport.model.SessionInfo;
import ru.yandex.market.mbi.environment.TestEnvironmentService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UidExtractorTest extends FunctionalTest {

    public static final String VALID = "VALID";
    private static final String HOST = "admin.market.yandex.ru";
    private static final Long DEV_UID = 10101010L;
    private static final Long BLACKBOX_UID = 10101011L;
    private static final String SESSION_ID_COOKIE_NAME = "Session_id";
    private static final String SESSION_ID = "some_session_id_value";
    @Autowired
    private PassportService passportService;

    @Autowired
    private TestEnvironmentService environmentService;

    @Mock
    private HttpServletRequest request;

    private UidExtractor uidExtractor;

    @BeforeEach
    void setUp() {
        when(request.getServerName()).thenReturn(HOST);
    }

    @Test
    @DisplayName("Запрос без кук")
    void requestWithoutCookies() throws Exception {
        uidExtractor = new UidExtractor(passportService, environmentService);
        uidExtractor.computeEnvironmentType();
        when(request.getCookies()).thenReturn(null);
        Assertions.assertDoesNotThrow(() -> uidExtractor.extractUid(request));
    }

    @Test
    @DisplayName("Запрос на DEV-окружении")
    void requestOnDevelopmentEnvironment() throws Exception {
        // Подготовить uidExtractor как на DEVELOPMENT
        environmentService.setEnvironmentType(EnvironmentType.DEVELOPMENT);
        uidExtractor = new UidExtractor(passportService, environmentService);
        uidExtractor.computeEnvironmentType();
        uidExtractor.setUid(DEV_UID);

        Optional<Long> uid = uidExtractor.extractUid(request);

        // Проверить, что не было запроса в passportService
        verifyZeroInteractions(passportService);

        // Проверить, что установлено правильное значение UID
        Assertions.assertEquals(uid.get(), DEV_UID);
    }

    @Test
    @DisplayName("Запрос не на DEV-окружении")
    void requestOnNonDevelopmentEnvironment() throws Exception {
        // Подготовить uidExtractor как на PRODUCTION
        environmentService.setEnvironmentType(EnvironmentType.PRODUCTION);
        uidExtractor = new UidExtractor(passportService, environmentService);
        uidExtractor.computeEnvironmentType();

        // Подготовить запрос
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie(SESSION_ID_COOKIE_NAME, SESSION_ID)});

        // Подготовить ответ passportService'а
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setStatus(VALID);
        sessionInfo.setUid(BLACKBOX_UID);
        when(passportService.getSessionInfo(SESSION_ID, HOST)).thenReturn(sessionInfo);

        Optional<Long> uid = uidExtractor.extractUid(request);

        // Проверить, что был запрос в passportService
        verify(passportService, times(1)).getSessionInfo(SESSION_ID, HOST);

        // Проверить, что установлено правильное значение UID
        Assertions.assertEquals(uid.get(), BLACKBOX_UID);
    }
}
