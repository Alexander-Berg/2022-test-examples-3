package ru.yandex.market.wms.auth.service;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.dao.DevicesAuditDao;
import ru.yandex.market.wms.common.spring.exception.LoginException;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DevicesAuditServiceIntegrationTest extends AuthIntegrationTest {

    private static final String MAIN_USERNAME = "AD2";
    private static final String OTHER_USERNAME = "AD1";
    private static final String CORRECT_DEVICE_ID = "123456789";
    private static final String OTHER_DEVICE_ID = "098765432";
    private static final String CORRECT_RETURN_REASON = "OK";

    @Autowired
    private DevicesAuditService devicesAuditService;

    @Autowired
    @SpyBean
    private DevicesAuditDao devicesAuditDao;

    @BeforeEach
    public void setUp() {
        Mockito.reset(devicesAuditDao);
        ReflectionTestUtils.setField(devicesAuditService, "checkingUserSessionEnabled", true);
    }

    /**
     * Некоректные либо пустые значения ID устройства или логина пользователя,
     * состояние - проверка активности сессии не производится
     */
    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceNotCheckedForEmptyDeviceId() {
        devicesAuditService.createUserSessionOnDevice(MAIN_USERNAME, "");
        checkMocksForActivateUserSession(0, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceNotCheckedForNullableDeviceId() {
        devicesAuditService.createUserSessionOnDevice(MAIN_USERNAME, null);
        checkMocksForActivateUserSession(0, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceNotCheckedForIncorrectDeviceId() {
        devicesAuditService.createUserSessionOnDevice(MAIN_USERNAME, "777");
        checkMocksForActivateUserSession(0, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceNotCheckedForEmptyUsername() {
        devicesAuditService.createUserSessionOnDevice("", CORRECT_DEVICE_ID);
        checkMocksForActivateUserSession(0, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceNotCheckedForNullableUsername() {
        devicesAuditService.createUserSessionOnDevice(null, CORRECT_DEVICE_ID);
        checkMocksForActivateUserSession(0, 0, 0);
    }

    /**
     * Отсутствуют данные аудита для пользователя,
     * состояние - нет активных сессий (в том числе, после логаута), возможен логин с любого устройства
     */
    @Test
    @DatabaseSetup(value = "/db/service/device-audit/empty-audit-data.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/after-main-user-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceSuccessfulForEmptyAuditData() {
        devicesAuditService.createUserSessionOnDevice(MAIN_USERNAME, CORRECT_DEVICE_ID);
        checkMocksForActivateUserSession(1, 1, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-some-users-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/after-some-users-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceSuccessfulForAuditDataOfOtherUser() {
        devicesAuditService.createUserSessionOnDevice(MAIN_USERNAME, CORRECT_DEVICE_ID);
        checkMocksForActivateUserSession(1, 1, 0);
    }

    /**
     * Последнее событие аудита - логин на устройстве с CORRECT_DEVICE_ID,
     * состояние - есть активная сессия на устройстве с CORRECT_DEVICE_ID, возможен логин только с него
     */
    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceSuccessfulForLoginBySameDevice() {
        devicesAuditService.createUserSessionOnDevice(MAIN_USERNAME, CORRECT_DEVICE_ID);
        checkMocksForActivateUserSession(1, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceFailedForOtherDevice() {
        assertThrows(LoginException.class,
                () -> devicesAuditService.createUserSessionOnDevice(MAIN_USERNAME, OTHER_DEVICE_ID));
        checkMocksForActivateUserSession(1, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceFailedForOtherUsername() {
        assertThrows(LoginException.class,
                () -> devicesAuditService.createUserSessionOnDevice(OTHER_USERNAME, CORRECT_DEVICE_ID));
        checkMocksForActivateUserSession(1, 0, 0);
    }

    /**
     * Последнее событие аудита - логин на устройстве с CORRECT_DEVICE_ID,
     * состояние - есть активная сессия, делаем логаут на устройстве с CORRECT_DEVICE_ID
     */
    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void endUserSessionOnDeviceUnsuccessfulForEmptyUsername() {
        devicesAuditService.deleteUserSessionOnDevice("", CORRECT_RETURN_REASON);
        checkMocksForActivateUserSession(0, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void endUserSessionOnDeviceUnsuccessfulForNullableUsername() {
        String nullableDeviceId = null;
        devicesAuditService.deleteUserSessionOnDevice(nullableDeviceId, CORRECT_RETURN_REASON);
        checkMocksForActivateUserSession(0, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void endUserSessionOnDeviceUnsuccessfulForOtherDevice() {
        devicesAuditService.deleteUserSessionOnDevice("777", CORRECT_RETURN_REASON);
        checkMocksForActivateUserSession(0, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/empty-audit-data.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void endUserSessionOnDeviceSuccessfulForLastLogout() {
        devicesAuditService.deleteUserSessionOnDevice(CORRECT_DEVICE_ID, CORRECT_RETURN_REASON);
        checkMocksForActivateUserSession(0, 0, 1);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/before-last-login.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/before-last-login.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void endUserSessionOnDeviceUnsuccessfulForOtherDeviceAndLastLogout() {
        devicesAuditService.deleteUserSessionOnDevice(OTHER_DEVICE_ID, CORRECT_RETURN_REASON);
        checkMocksForActivateUserSession(0, 0, 1);
    }

    /**
     * Последнее событие аудита отсутствует,
     * делаем логаут на устройстве с CORRECT_DEVICE_ID
     */
    @Test
    @DatabaseSetup(value = "/db/service/device-audit/empty-audit-data.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/empty-audit-data.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void endUserSessionOnDeviceSuccessfulForEmptyAudit() {
        devicesAuditService.deleteUserSessionOnDevice(CORRECT_DEVICE_ID, CORRECT_RETURN_REASON);
        checkMocksForActivateUserSession(0, 0, 1);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/empty-audit-data.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/empty-audit-data.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void endUserSessionOnDeviceUnsuccessfulForOtherDeviceAndEmptyAudit() {
        devicesAuditService.deleteUserSessionOnDevice(OTHER_DEVICE_ID, CORRECT_RETURN_REASON);
        checkMocksForActivateUserSession(0, 0, 1);
    }

    /**
     * Отсутствуют данные аудита для пользователя, состояние - нет активных сессий, возможен логин с любого устройства,
     * однако запрещена проверка активности сессий пользователей
     */
    @Test
    @DatabaseSetup(value = "/db/service/device-audit/empty-audit-data.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/empty-audit-data.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void createUserSessionOnDeviceSuccessfulForСheckingUserSessionDisabled() {
        ReflectionTestUtils.setField(devicesAuditService, "checkingUserSessionEnabled", false);
        devicesAuditService.createUserSessionOnDevice(MAIN_USERNAME, CORRECT_DEVICE_ID);
        checkMocksForActivateUserSession(0, 0, 0);
    }

    @Test
    @DatabaseSetup(value = "/db/service/device-audit/empty-audit-data.xml", connection = "wmwhseConnection")
    @ExpectedDatabase(
            value = "/db/service/device-audit/empty-audit-data.xml",
            connection = "wmwhseConnection",
            assertionMode = NON_STRICT_UNORDERED
    )
    public void endUserSessionOnDeviceSuccessfulForСheckingUserSessionDisabled() {
        ReflectionTestUtils.setField(devicesAuditService, "checkingUserSessionEnabled", false);
        devicesAuditService.deleteUserSessionOnDevice(CORRECT_DEVICE_ID, CORRECT_RETURN_REASON);
        checkMocksForActivateUserSession(0, 0, 0);
    }

    private void checkMocksForActivateUserSession(
            int findSessionOnDeviceTimes,
            int createUserSessionOnDeviceTimes,
            int deleteUserSessionOnDeviceTimes
    ) {
        verify(devicesAuditDao, times(findSessionOnDeviceTimes)).findUserAndDeviceSessions(anyString(), anyString());
        verify(devicesAuditDao, times(createUserSessionOnDeviceTimes))
                .createUserSessionOnDevice(anyString(), any(), anyString());
        verify(devicesAuditDao, times(deleteUserSessionOnDeviceTimes)).deleteUserSessionOnDevice(anyString());
    }
}
