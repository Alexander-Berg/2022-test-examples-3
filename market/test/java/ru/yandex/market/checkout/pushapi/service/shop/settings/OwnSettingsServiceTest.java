package ru.yandex.market.checkout.pushapi.service.shop.settings;

import java.sql.Timestamp;
import java.text.ParseException;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.mybatis.mappers.EnvironmentMapper;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Features;
import ru.yandex.market.checkout.pushapi.settings.Settings;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnSettingsServiceTest extends AbstractWebTestBase {

    @Autowired
    OwnSettingsService ownService;

    @Autowired
    EnvironmentMapper environmentMapper;

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @AfterEach
    public void trunkTable() {
        jdbcTemplate.getJdbcTemplate().execute("truncate push_api.settings");
    }

    @BeforeEach
    public void setWritePercentTo100() {
        environmentMapper.upsertEnvironmentEntity("write_requests_percent", "100");
    }

    @Test
    public void shouldStoreSettingsProperly() throws ParseException {
        var in = anySettings();
        ownService.updateSettings(123L, in, true);
        var out = ownService.getSettings(123L, true);
        assertTrue(objEquals(in, out));
    }

    @Test
    public void shouldUpdateIfExists() throws ParseException {
        var id = 1243L;
        ownService.updateSettings(id, anySettings(), true);
        ownService.updateSettings(id, anySettings(), true);
        var rows = JdbcTestUtils.countRowsInTable(
                jdbcTemplate.getJdbcTemplate(), "push_api.settings");
        Assertions.assertEquals(1, rows);
    }

    @Test
    public void shouldSeparateBySandboxType() throws ParseException {
        var id = 123L;
        ownService.updateSettings(id, anySettings(), true);
        ownService.updateSettings(id, anySettings(), false);
        var rows = JdbcTestUtils.countRowsInTable(
                jdbcTemplate.getJdbcTemplate(), "push_api.settings");
        Assertions.assertEquals(2, rows);
    }

    @Test
    public void shouldProperlyStoreNullableProperties() {
        Settings settings = Settings.builder()
                .partnerInterface(true)
                .build();

        ownService.updateSettings(123L, settings, true);
        Assertions.assertTrue(objEquals(settings, ownService.getSettings(123L, true)));
    }


    private Settings anySettings() throws ParseException {
        CheckoutDateFormat format = new CheckoutDateFormat();
        Timestamp time =
                new Timestamp(format.createLongDateFormat().parse("12-02-2020 20:25:20").getTime());

        return Settings.builder()
                .forceLogResponseUntil(time)
                .changerAddress("address")
                .features(new Features(true))
                .changerId("123245")
                .changeTimestamp(time.getTime())
                .urlPrefix("url")
                .authType(AuthType.URL)
                .authToken("token")
                .dataType(DataType.XML)
                .fingerprint("byte".getBytes())
                .partnerInterface(true)
                .build();
    }

    private boolean objEquals(Object left, Object right) {
        return EqualsBuilder.reflectionEquals(left, right);
    }
}
