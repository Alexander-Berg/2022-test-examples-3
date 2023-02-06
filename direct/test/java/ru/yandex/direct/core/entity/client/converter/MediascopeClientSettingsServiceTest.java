package ru.yandex.direct.core.entity.client.converter;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.client.model.MediascopeClientMeasurerSettings;
import ru.yandex.direct.core.entity.client.service.MediascopeClientSettingsService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.mediascope.MediascopeClient;
import ru.yandex.direct.utils.crypt.Encrypter;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class MediascopeClientSettingsServiceTest {

    private MediascopeClientSettingsService mediascopeClientSettingsService;

    @Before
    public void before() {
        mediascopeClientSettingsService =
                new MediascopeClientSettingsService(new Encrypter("test"), mock(MediascopeClient.class));
    }

    @Test
    public void encrypt_Decrypt_GetSameValue() {
        var settings = toJson(new MediascopeClientMeasurerSettings()
                .withAccessToken("access")
                .withRefreshToken("refresh")
                .withExpiresAt(12345678L)
                .withTmsecprefix("prefix"));

        var encrypted = mediascopeClientSettingsService.encryptSettings(settings);
        var decrypted = mediascopeClientSettingsService.decryptSettings(encrypted);

        assertThat(settings, equalTo(decrypted));
    }
}
