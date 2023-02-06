package ru.yandex.direct.internaltools.tools.feature.tool;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.internaltools.tools.feature.container.InternalToolsClientIdLogin;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;

public class GetClientsFeaturesToolTest {

    private GetClientsFeaturesTool getClientsFeaturesTool;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        getClientsFeaturesTool = new GetClientsFeaturesTool();
    }

    @Test
    public void only_client_id() {
        InternalToolsClientIdLogin parameters = new InternalToolsClientIdLogin();
        parameters
                .withClientId(1L)
                .withLogin("");

        ValidationResult<InternalToolsClientIdLogin, Defect> validationResult = getClientsFeaturesTool.validate(parameters);

        assertThat(validationResult.getErrors()).isEmpty();
    }

    @Test
    public void only_login() {
        InternalToolsClientIdLogin parameters = new InternalToolsClientIdLogin();
        parameters
                .withClientId(null)
                .withLogin("login");

        ValidationResult<InternalToolsClientIdLogin, Defect> validationResult = getClientsFeaturesTool.validate(parameters);

        assertThat(validationResult.getErrors()).isEmpty();
    }

    @Test
    public void client_id_and_login() {
        InternalToolsClientIdLogin parameters = new InternalToolsClientIdLogin();
        parameters
                .withClientId(1L)
                .withLogin("login");

        ValidationResult<InternalToolsClientIdLogin, Defect> validationResult = getClientsFeaturesTool.validate(parameters);

        assertThat(validationResult.getErrors().size()).isEqualTo(1);
        assertThat(validationResult.getErrors().get(0).defectId()).isEqualTo(DefectIds.MUST_BE_EMPTY);
    }

    @Test
    public void no_client_id_no_login() {
        InternalToolsClientIdLogin parameters = new InternalToolsClientIdLogin();
        parameters
                .withClientId(null)
                .withLogin(null);

        ValidationResult<InternalToolsClientIdLogin, Defect> validationResult = getClientsFeaturesTool.validate(parameters);

        assertThat(validationResult.getErrors().size()).isEqualTo(1);
        assertThat(validationResult.getErrors().get(0).defectId()).isEqualTo(DefectIds.CANNOT_BE_NULL);
    }
}
