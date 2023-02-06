package ru.yandex.market.tsum.spok.validation.validator;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.registry.proto.model.AppType;
import ru.yandex.market.tsum.spok.validation.model.SpokValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.spok.validation.SpokValidationMessages.MISSING_REQUIRED_PARAMETER_MESSAGE;
import static ru.yandex.market.tsum.spok.validation.validator.SpokResourceTypeNameValidator.FIELD_NAME;
import static ru.yandex.market.tsum.spok.validation.validator.SpokResourceTypeNameValidator.RESOURCE_TYPE_ALREADY_EXISTS;

@ParametersAreNonnullByDefault
@RunWith(MockitoJUnitRunner.class)
public class SpokResourceTypeNameValidatorTest {
    private static final String EXISTING_RESOURCE = "EXISTING_RESOURCE";
    private static final String NON_EXISTING_RESOURCE = "NON_EXISTING_RESOURCE";

    @Mock
    private SandboxClient sandboxClient;

    @InjectMocks
    private SpokResourceTypeNameValidator validator;

    @Before
    public void setUp() {
        when(sandboxClient.isResourceExistsSafe(EXISTING_RESOURCE)).thenReturn(true);
        when(sandboxClient.isResourceExistsSafe(NON_EXISTING_RESOURCE)).thenReturn(false);
    }

    @Test
    public void nullResource() {
        assertThat(validator.validate(params(null)))
                .isEqualTo(SpokValidationResult.error(FIELD_NAME,
                        String.format(MISSING_REQUIRED_PARAMETER_MESSAGE, FIELD_NAME)));
    }

    @Test
    public void emptyResource() {
        assertThat(validator.validate(params("")))
                .isEqualTo(SpokValidationResult.error(FIELD_NAME,
                        String.format(MISSING_REQUIRED_PARAMETER_MESSAGE, FIELD_NAME)));
    }

    @Test
    public void existingResource() {
        assertThat(validator.validate(params(EXISTING_RESOURCE)))
                .isEqualTo(SpokValidationResult.error(FIELD_NAME,
                        String.format(RESOURCE_TYPE_ALREADY_EXISTS, EXISTING_RESOURCE)));
    }

    @Test
    public void nonExistingResource() {
        assertThat(validator.validate(params(NON_EXISTING_RESOURCE))).isEqualTo(SpokValidationResult.ok());
    }

    private ServiceParams params(@Nullable String type) {
        ServiceParams result = new ServiceParams();
        result.setResourceTypeName(type);
        result.setApplicationType(AppType.JAVA.toString());
        return result;
    }
}
