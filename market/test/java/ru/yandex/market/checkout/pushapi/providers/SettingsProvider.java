package ru.yandex.market.checkout.pushapi.providers;

import org.springframework.beans.factory.annotation.Value;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;

public class SettingsProvider {
    public static final String DEFAULT_TOKEN = "auth-token";
    private final String prefix;

    public SettingsProvider(@Value("http://localhost:#{shopadminStubMock.port()}") String prefix) {
        this.prefix = prefix;
    }

    public Settings buildXmlSettings() {
        return buildXmlSettings(false, DataType.XML);
    }

    public Settings buildXmlSettings(boolean partnerInterface, DataType dataType) {
        return new Settings(
                prefix,
                DEFAULT_TOKEN,
                dataType,
                AuthType.URL,
                partnerInterface
        );
    }
}
