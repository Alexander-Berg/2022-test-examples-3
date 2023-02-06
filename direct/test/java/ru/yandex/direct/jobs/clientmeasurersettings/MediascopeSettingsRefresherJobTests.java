package ru.yandex.direct.jobs.clientmeasurersettings;

import java.time.Instant;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.client.model.ClientMeasurerSettings;
import ru.yandex.direct.core.entity.client.model.ClientMeasurerSystem;
import ru.yandex.direct.core.entity.client.model.MediascopeClientMeasurerSettings;
import ru.yandex.direct.core.entity.client.service.ClientMeasurerSettingsService;
import ru.yandex.direct.mediascope.MediascopeClient;
import ru.yandex.direct.mediascope.model.response.MediascopePrefixResponse;
import ru.yandex.direct.mediascope.model.response.MediascopeTokensResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.utils.JsonUtils.toJson;

@ParametersAreNonnullByDefault
public class MediascopeSettingsRefresherJobTests {

    private MediascopeSettingsRefresherJob mediascopeSettingsRefresherJob;

    @Mock
    private MediascopeClient mediascopeClient;

    @Mock
    private ClientMeasurerSettingsService clientMeasurerSettingsService;

    private final int shardId = 1;
    private final Long clientId = 5L;

    @BeforeEach
    public void before() {
        initMocks(this);
        mediascopeSettingsRefresherJob =
                new MediascopeSettingsRefresherJob(shardId, mediascopeClient, clientMeasurerSettingsService);
    }

    @Test
    public void execute_FreshToken_UpdatePrefix() {
        long expiresAt = Instant.now().plusSeconds(24 * 60 * 60).getEpochSecond();

        var settings = new MediascopeClientMeasurerSettings()
                .withAccessToken("1")
                .withRefreshToken("2")
                .withExpiresAt(expiresAt);

        List<ClientMeasurerSettings> clientMeasurerSettingsList = List.of(
                new ClientMeasurerSettings()
                        .withClientId(clientId)
                        .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                        .withSettings(toJson(settings)));

        when(clientMeasurerSettingsService.getByMeasurerSystem(shardId, ClientMeasurerSystem.MEDIASCOPE))
                .thenReturn(clientMeasurerSettingsList);
        when(mediascopeClient.getPrefix(any())).thenReturn(new MediascopePrefixResponse().withPrefix("ggg"));

        mediascopeSettingsRefresherJob.execute();

        verify(mediascopeClient, never()).getFreshTokens(any());
        verify(clientMeasurerSettingsService, times(1)).update(eq(shardId), any());
    }

    @Test
    public void execute_OldToken_UpdateSettings() {
        long expiresAt = Instant.now().plusSeconds(60).getEpochSecond();

        var settings = new MediascopeClientMeasurerSettings()
                .withAccessToken("1")
                .withRefreshToken("2")
                .withExpiresAt(expiresAt);

        List<ClientMeasurerSettings> clientMeasurerSettingsList = List.of(
                new ClientMeasurerSettings()
                        .withClientId(clientId)
                        .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                        .withSettings(toJson(settings)));

        when(clientMeasurerSettingsService.getByMeasurerSystem(shardId, ClientMeasurerSystem.MEDIASCOPE))
                .thenReturn(clientMeasurerSettingsList);

        var tokensResponse =
                new MediascopeTokensResponse()
                        .withAccessToken("xxx")
                        .withRefreshToken("yyy")
                        .withExpiresIn(1000L);
        when(mediascopeClient.getFreshTokens(any())).thenReturn(tokensResponse);
        when(mediascopeClient.getPrefix(any())).thenReturn(new MediascopePrefixResponse().withPrefix("ggg"));

        mediascopeSettingsRefresherJob.execute();

        verify(clientMeasurerSettingsService, times(1)).update(eq(shardId), any());
    }

    @Test
    public void execute_NoPrefix_UpdateOnlyTokens() {
        long expiresAt = Instant.now().plusSeconds(60).getEpochSecond();

        var settings = new MediascopeClientMeasurerSettings()
                .withAccessToken("1")
                .withRefreshToken("2")
                .withExpiresAt(expiresAt);

        List<ClientMeasurerSettings> clientMeasurerSettingsList = List.of(
                new ClientMeasurerSettings()
                        .withClientId(clientId)
                        .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                        .withSettings(toJson(settings)));

        when(clientMeasurerSettingsService.getByMeasurerSystem(shardId, ClientMeasurerSystem.MEDIASCOPE))
                .thenReturn(clientMeasurerSettingsList);

        var tokensResponse =
                new MediascopeTokensResponse()
                        .withAccessToken("xxx")
                        .withRefreshToken("yyy")
                        .withExpiresIn(1000L);
        when(mediascopeClient.getFreshTokens(any())).thenReturn(tokensResponse);
        when(mediascopeClient.getPrefix(any())).thenReturn(null);

        mediascopeSettingsRefresherJob.execute();

        verify(clientMeasurerSettingsService, times(1)).update(eq(shardId), any());
    }

    @Test
    public void execute_NoTokens_UpdatePrefix() {
        long expiresAt = Instant.now().plusSeconds(60).getEpochSecond();

        var settings = new MediascopeClientMeasurerSettings()
                .withAccessToken("1")
                .withRefreshToken("2")
                .withExpiresAt(expiresAt);

        List<ClientMeasurerSettings> clientMeasurerSettingsList = List.of(
                new ClientMeasurerSettings()
                        .withClientId(clientId)
                        .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                        .withSettings(toJson(settings)));

        when(clientMeasurerSettingsService.getByMeasurerSystem(shardId, ClientMeasurerSystem.MEDIASCOPE))
                .thenReturn(clientMeasurerSettingsList);
        when(mediascopeClient.getFreshTokens(any())).thenReturn(null);
        when(mediascopeClient.getPrefix(any())).thenReturn(new MediascopePrefixResponse().withPrefix("ggg"));

        mediascopeSettingsRefresherJob.execute();

        verify(clientMeasurerSettingsService, times(1)).update(eq(shardId), any());
    }

    @Test
    public void execute_Exception_NoUpdate() {
        String invalidJson = "xxxx1111:2344";
        List<ClientMeasurerSettings> clientMeasurerSettingsList = List.of(
                new ClientMeasurerSettings()
                        .withClientId(clientId)
                        .withClientMeasurerSystem(ClientMeasurerSystem.MEDIASCOPE)
                        .withSettings(invalidJson));

        when(clientMeasurerSettingsService.getByMeasurerSystem(shardId, ClientMeasurerSystem.MEDIASCOPE))
                .thenReturn(clientMeasurerSettingsList);

        mediascopeSettingsRefresherJob.execute();

        verify(mediascopeClient, never()).getFreshTokens(any());
        verify(mediascopeClient, never()).getPrefix(any());
        verify(clientMeasurerSettingsService, never()).update(eq(shardId), any());
    }
}
