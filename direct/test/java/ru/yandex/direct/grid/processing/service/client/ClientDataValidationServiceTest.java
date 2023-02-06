package ru.yandex.direct.grid.processing.service.client;

import java.time.Instant;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.client.GdClientMeasurerSystem;
import ru.yandex.direct.grid.processing.model.cliententity.mutation.GdAddClientMeasurerAccount;
import ru.yandex.direct.grid.processing.service.validation.GridValidationResultConversionService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class ClientDataValidationServiceTest {

    @InjectMocks
    private GridValidationResultConversionService validationService;

    private GridValidationService gridValidationService;

    @Before
    public void before() {
        gridValidationService = new GridValidationService(validationService);
    }

    @Test
    public void validateClientMeasurerAccount_Valid_NoException() {
        var gdAddClientMeasurerAccount = new GdAddClientMeasurerAccount()
                .withMeasurerSystem(GdClientMeasurerSystem.MEDIASCOPE)
                .withSettings(String.format("{\"tmsec_prefix\":\"xxx\", \"access_token\": \"1\"," +
                                " \"refresh_token\": \"2\", \"expires_at\": %s}",
                        Instant.now().plusSeconds(100).getEpochSecond()));
        gridValidationService.validateClientMeasurerAccount(gdAddClientMeasurerAccount);
    }

    @Test(expected = GridValidationException.class)
    public void validateClientMeasurerAccount_ExpiredToken_ThrowException() {
        var gdAddClientMeasurerAccount = new GdAddClientMeasurerAccount()
                .withMeasurerSystem(GdClientMeasurerSystem.MEDIASCOPE)
                .withSettings(String.format("{\"tmsec_prefix\":\"xxx\", \"access_token\": \"1\"," +
                                " \"refresh_token\": \"2\", \"expires_at\": %s}",
                        Instant.now().minusSeconds(100).getEpochSecond()));
        gridValidationService.validateClientMeasurerAccount(gdAddClientMeasurerAccount);
    }

    @Test(expected = GridValidationException.class)
    public void validateClientMeasurerAccount_NullSettings_ThrowException() {
        var gdAddClientMeasurerAccount = new GdAddClientMeasurerAccount()
                .withMeasurerSystem(GdClientMeasurerSystem.MEDIASCOPE)
                .withSettings(null);
        gridValidationService.validateClientMeasurerAccount(gdAddClientMeasurerAccount);
    }

    @Test(expected = GridValidationException.class)
    public void validateClientMeasurerAccount_EmptySettings_ThrowException() {
        var gdAddClientMeasurerAccount = new GdAddClientMeasurerAccount()
                .withMeasurerSystem(GdClientMeasurerSystem.MEDIASCOPE)
                .withSettings("");
        gridValidationService.validateClientMeasurerAccount(gdAddClientMeasurerAccount);
    }

    @Test(expected = GridValidationException.class)
    public void validateClientMeasurerAccount_NoAccessToken_ThrowException() {
        var gdAddClientMeasurerAccount = new GdAddClientMeasurerAccount()
                .withMeasurerSystem(GdClientMeasurerSystem.MEDIASCOPE)
                .withSettings(String.format("{\"tmsec_prefix\":\"xxx\", \"access_token\": \"\"," +
                                " \"refresh_token\": \"2\", \"expires_at\": %s}",
                        Instant.now().plusSeconds(100).getEpochSecond()));
        gridValidationService.validateClientMeasurerAccount(gdAddClientMeasurerAccount);
    }

    @Test(expected = GridValidationException.class)
    public void validateClientMeasurerAccount_NoRefreshToken_ThrowException() {
        var gdAddClientMeasurerAccount = new GdAddClientMeasurerAccount()
                .withMeasurerSystem(GdClientMeasurerSystem.MEDIASCOPE)
                .withSettings(String.format("{\"tmsec_prefix\":\"xxx\", \"access_token\": \"1\"," +
                                " \"refresh_token\": \"\", \"expires_at\": %s}",
                        Instant.now().plusSeconds(100).getEpochSecond()));
        gridValidationService.validateClientMeasurerAccount(gdAddClientMeasurerAccount);
    }

    @Test(expected = GridValidationException.class)
    public void validateClientMeasurerAccount_NoPrefix_ThrowException() {
        var gdAddClientMeasurerAccount = new GdAddClientMeasurerAccount()
                .withMeasurerSystem(GdClientMeasurerSystem.MEDIASCOPE)
                .withSettings(String.format("{\"tmsec_prefix\":\"\", \"access_token\": \"1\"," +
                                " \"refresh_token\": \"\", \"expires_at\": %s}",
                        Instant.now().plusSeconds(100).getEpochSecond()));
        gridValidationService.validateClientMeasurerAccount(gdAddClientMeasurerAccount);
    }

    @Test(expected = GridValidationException.class)
    public void validateClientMeasurerAccount_InvalidJson_ThrowException() {
        var gdAddClientMeasurerAccount = new GdAddClientMeasurerAccount()
                .withMeasurerSystem(GdClientMeasurerSystem.MEDIASCOPE)
                .withSettings(String.format("{\"tmsec_prefix\":\"xxx\", \"access_token\": \"1\"," +
                                " \"refresh\": \"2\", \"expires_at\": %s}",
                        Instant.now().plusSeconds(100).getEpochSecond()));
        gridValidationService.validateClientMeasurerAccount(gdAddClientMeasurerAccount);
    }
}
