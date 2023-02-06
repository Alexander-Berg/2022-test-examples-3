package ru.yandex.market.tsum.spok.validation.validator;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.registry.proto.model.AppType;
import ru.yandex.market.tsum.spok.validation.model.SpokValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.spok.validation.validator.SpokArcadiaPathValidator.FIELD_NAME;
import static ru.yandex.market.tsum.spok.validation.validator.SpokArcadiaPathValidator.INVALID_PATH_MESSAGE;
import static ru.yandex.market.tsum.spok.validation.validator.SpokArcadiaPathValidator.PATH_ALREADY_EXISTS_MESSAGE;

@ParametersAreNonnullByDefault
@RunWith(MockitoJUnitRunner.class)
public class SpokArcadiaPathValidatorTest {
    private static final String EXISTING_PATH = "existing";
    private static final String NON_EXISTING_PATH = "non-existing";
    private static final String EXISTING_PATH_FULL = "/trunk/arcadia/" + EXISTING_PATH;
    private static final String NON_EXISTING_PATH_FULL = "/trunk/arcadia/" + NON_EXISTING_PATH;
    private static final String INVALID_PATH = "https://a.yandex-team.ru/";

    @Mock
    private RootArcadiaClient arcadiaClient;

    @InjectMocks
    private SpokArcadiaPathValidator validator;

    @Before
    public void setUp() {
        when(arcadiaClient.isPathPresentSafe(EXISTING_PATH_FULL)).thenReturn(true);
        when(arcadiaClient.isPathPresentSafe(NON_EXISTING_PATH_FULL)).thenReturn(false);
        when(arcadiaClient.getFullPathInTrunk(anyString())).thenCallRealMethod();
    }

    @Test
    public void javaExisting() {
        assertThat(validator.validate(javaExistingParams())).isEqualTo(SpokValidationResult.ok());
        verifyZeroInteractions(arcadiaClient);
    }

    @Test
    public void validPath() {
        assertThat(validator.validate(params(NON_EXISTING_PATH))).isEqualTo(SpokValidationResult.ok());
        verify(arcadiaClient).getFullPathInTrunk(NON_EXISTING_PATH);
        verify(arcadiaClient).isPathPresentSafe(NON_EXISTING_PATH_FULL);
        verifyNoMoreInteractions(arcadiaClient);
    }

    @Test
    public void invalidPath() {
        assertThat(validator.validate(params(INVALID_PATH)))
                .isEqualTo(SpokValidationResult.error(FIELD_NAME,
                        String.format(INVALID_PATH_MESSAGE, INVALID_PATH)));
        verifyZeroInteractions(arcadiaClient);
    }

    @Test
    public void existingPath() {
        assertThat(validator.validate(params(EXISTING_PATH)))
                .isEqualTo(SpokValidationResult.error(FIELD_NAME,
                        String.format(PATH_ALREADY_EXISTS_MESSAGE, EXISTING_PATH)));
        verify(arcadiaClient).getFullPathInTrunk(EXISTING_PATH);
        verify(arcadiaClient).isPathPresentSafe(EXISTING_PATH_FULL);
        verifyNoMoreInteractions(arcadiaClient);
    }

    private static ServiceParams javaExistingParams() {
        ServiceParams result = new ServiceParams();
        result.setApplicationType(AppType.JAVA_EXISTING.toString());
        return result;
    }

    private static ServiceParams params(String path) {
        ServiceParams result = new ServiceParams();
        result.setApplicationType(AppType.JAVA.toString());
        result.setArcadiaPath(path);
        return result;
    }
}
