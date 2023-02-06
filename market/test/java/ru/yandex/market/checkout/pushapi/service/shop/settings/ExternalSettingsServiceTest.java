package ru.yandex.market.checkout.pushapi.service.shop.settings;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.cache.memcached.MemCachedServiceConfig;
import ru.yandex.common.cache.memcached.MemCachingService;
import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsCheckouterService;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.checkout.util.SettingsUtils.sameSettings;

/**
 * @author kl1san
 */
public class ExternalSettingsServiceTest extends AbstractWebTestBase {

    private static final Settings DEFAULT_SETTINGS = Settings.builder()
            .urlPrefix("prefix")
            .authType(AuthType.URL)
            .authToken("token")
            .dataType(DataType.XML)
            .partnerInterface(true)
            .build();
    private static final Long SHOP_ID = 600L;
    @Autowired
    private MemCachedServiceConfig config;
    @Autowired
    private MemCachingService memCachingService;
    @Autowired
    private ExternalSettingsService settingsService;
    @Mock
    private SettingsCheckouterService settingsCheckouterServiceMock;

    @BeforeEach()
    public void init() {
        settingsService = new ExternalSettingsService(settingsCheckouterServiceMock);
    }

    @AfterEach
    public void tearDown() {
        settingsService.deleteSettings(SHOP_ID, true);
    }

    @Test
    public void shouldUpdateSettingsInDB() {
        settingsService.updateSettings(SHOP_ID, DEFAULT_SETTINGS, true);

        verify(settingsCheckouterServiceMock, times(1)).updateSettings(anyLong(), any(), anyBoolean());

        Settings updatedSettings =
                DEFAULT_SETTINGS.toBuilder().authType(AuthType.HEADER).urlPrefix("newPrefix").build();
        settingsService.updateSettings(SHOP_ID, updatedSettings, true);

        ArgumentCaptor<Settings> settingsCaptor = ArgumentCaptor.forClass(Settings.class);
        verify(settingsCheckouterServiceMock, times(2)).updateSettings(anyLong(), settingsCaptor.capture(),
                anyBoolean());
        assertThat(settingsCaptor.getValue(), sameSettings(updatedSettings));
    }

    @Test
    public void shouldDeleteSettingsFromDB() {
        settingsService.updateSettings(SHOP_ID, DEFAULT_SETTINGS, true);

        settingsService.deleteSettings(SHOP_ID, true);

        verify(settingsCheckouterServiceMock, times(1)).deleteSettings(anyLong(), anyBoolean());
    }

}
