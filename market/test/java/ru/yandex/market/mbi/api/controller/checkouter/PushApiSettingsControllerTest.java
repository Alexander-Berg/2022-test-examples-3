package ru.yandex.market.mbi.api.controller.checkouter;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.mbi.api.client.entity.checkouter.LogResponseStatusDto;
import ru.yandex.market.mbi.api.config.FunctionalTest;

/**
 * Тестирует функциональность {@link ru.yandex.market.mbi.api.controller.PushApiSettingsController}.
 */
public class PushApiSettingsControllerTest extends FunctionalTest {

    private static final Settings SETTINGS = Settings.builder()
            .urlPrefix("localhost")
            .authToken("auth")
            .authType(AuthType.URL)
            .dataType(DataType.JSON)
            .partnerInterface(false)
            .build();
    private static final long SHOP_ID = 123;
    private static final int DAYS = 31;
    private static final Instant EXPECTED_DATE = Instant.EPOCH.plus(DAYS, ChronoUnit.DAYS);
    private static final boolean SANDBOX = false;

    @Autowired
    private MbiApiClient mbiApiClient;

    @Autowired
    private PushApi pushApi;

    @Autowired
    private TestableClock clock;

    @BeforeEach
    public void init() {
        clock.setFixed(Instant.EPOCH, DateTimeUtils.MOSCOW_ZONE);
        Mockito.when(pushApi.getSettings(SHOP_ID, SANDBOX)).thenReturn(SETTINGS);
    }

    @Test
    @DbUnitDataSet(before = "CheckouterSettingsControllerTest.before.csv")
    public void getLogResponseStatus() {
        Settings newSettings = SETTINGS;
        newSettings.setForceLogResponseUntil(Timestamp.from(EXPECTED_DATE));
        Mockito.when(pushApi.getSettings(SHOP_ID, SANDBOX)).thenReturn(newSettings);

        LogResponseStatusDto status = mbiApiClient.getLogResponseStatus(SHOP_ID, SANDBOX);
        Assertions.assertTrue(status.isEnabled());
        Assertions.assertEquals(EXPECTED_DATE, status.getForceEnabledUntil());
    }

    @Test
    @DbUnitDataSet(before = "CheckouterSettingsControllerTest.before.csv")
    public void getLogResponseStatusForDisabledLogResponse() {
        Mockito.when(pushApi.getSettings(SHOP_ID, SANDBOX)).thenReturn(SETTINGS);

        LogResponseStatusDto status = mbiApiClient.getLogResponseStatus(SHOP_ID, SANDBOX);
        Assertions.assertFalse(status.isEnabled());
        Assertions.assertNull(status.getForceEnabledUntil());
    }

    @Test
    public void getLogResponseStatusForUnknownUser() {
        Assertions.assertThrows(HttpClientErrorException.NotFound.class,
                () -> mbiApiClient.getLogResponseStatus(222, false));
    }

    @Test
    @DbUnitDataSet(before = "CheckouterSettingsControllerTest.before.csv")
    public void forceLogResponseUntil() {
        LogResponseStatusDto status = mbiApiClient.enableLogResponse(SHOP_ID, DAYS, SANDBOX);
        Assertions.assertTrue(status.isEnabled());
        Assertions.assertEquals(EXPECTED_DATE, status.getForceEnabledUntil());
    }

    @Test
    public void forceLogResponseUntilForUnknownUser() {
        Assertions.assertThrows(HttpClientErrorException.NotFound.class,
                () -> mbiApiClient.enableLogResponse(222, DAYS, false));
    }

    @Test
    public void forceLogResponseUntilForLongPeriod() {
        Assertions.assertThrows(HttpClientErrorException.BadRequest.class,
                () -> mbiApiClient.enableLogResponse(SHOP_ID, 32, false));
    }
}
