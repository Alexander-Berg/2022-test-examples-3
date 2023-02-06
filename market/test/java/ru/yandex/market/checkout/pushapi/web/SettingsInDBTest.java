package ru.yandex.market.checkout.pushapi.web;

import java.sql.Timestamp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.helpers.CheckouterMockConfigurer;
import ru.yandex.market.checkout.pushapi.helpers.PushApiSettingsHelper;
import ru.yandex.market.checkout.pushapi.providers.SettingsProvider;
import ru.yandex.market.checkout.pushapi.settings.Settings;

public class SettingsInDBTest extends AbstractWebTestBase {
    private static final long DEFAULT_SHOP_ID = 221123123L;

    @Autowired
    private SettingsProvider settingsProvider;
    @Autowired
    private CheckouterMockConfigurer checkouterMockConfigurer;
    @Autowired
    private PushApiSettingsHelper pushApiSettingsHelper;

    @Test
    public void shouldGetSettingsFromCheckouter() throws Exception {
        CheckoutDateFormat format = new CheckoutDateFormat();
        Timestamp forceLogResponseUntil =
                new Timestamp(format.createLongDateFormat().parse("12-02-2020 20:25:20").getTime());

        Settings settings = settingsProvider.buildXmlSettings();
        settings.setForceLogResponseUntil(forceLogResponseUntil);

        checkouterMockConfigurer.setSettings(DEFAULT_SHOP_ID, settings);

        Settings saved = pushApiSettingsHelper.getSettings(DEFAULT_SHOP_ID);

        Assertions.assertEquals(
                settings.getForceLogResponseUntil().toLocalDateTime().toLocalDate(),
                saved.getForceLogResponseUntil().toLocalDateTime().toLocalDate()
        );
    }
}
