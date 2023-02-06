package ru.yandex.market.tsum.spok.validation.validator;

import java.util.Map;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.market.tsum.clients.mdb.MdbClient;
import ru.yandex.market.tsum.core.environment.Environment;
import ru.yandex.market.tsum.core.registry.v2.model.spok.PostgreSqlParams;
import ru.yandex.market.tsum.core.registry.v2.model.spok.RtcEnvironmentSpec;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.spok.validation.model.SpokValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.internal.stubbing.answers.AnswerFunctionalInterfaces.toAnswer;
import static ru.yandex.market.tsum.spok.validation.validator.SpokPostgresDbNamesValidator.POSTGRES_DB_NAME_OCCUPIED;

@ParametersAreNonnullByDefault
public class SpokPostgresDbNamesValidatorTest {
    private static final String ABC_SERVICE = "abc_service";
    private static final String FOLDER_ID = "folder_id";
    private static final String EXISTING_DB_NAME = "market_infra_graphite_pg";
    private static final String NON_EXISTING_DB_NAME = "bogus_name";
    private static final MdbClient MDB_CLIENT = mock(MdbClient.class);
    private static final SpokValidator VALIDATOR = new SpokPostgresDbNamesValidator(MDB_CLIENT);

    static {
        when(MDB_CLIENT.getFolderIdByAbcSlugOptional(ABC_SERVICE)).thenReturn(Optional.of(FOLDER_ID));
        when(MDB_CLIENT.dbNameExists(eq(FOLDER_ID), anyString()))
                .thenAnswer(toAnswer((String folderId, String dbName) -> dbName.equals(EXISTING_DB_NAME)));
    }

    @Test
    public void existingDbName() {
        ServiceParams params = params(ABC_SERVICE, Map.of(Environment.PRODUCTION, EXISTING_DB_NAME));
        params.setPgaasEnabled(true);
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.error(
                "environments.PRODUCTION.postgreSql.databaseName",
                String.format(POSTGRES_DB_NAME_OCCUPIED, EXISTING_DB_NAME)));
    }

    @Test
    public void nonExistingDbName() {
        ServiceParams params = params(ABC_SERVICE, Map.of(Environment.PRODUCTION, NON_EXISTING_DB_NAME));
        SpokValidationResult result = VALIDATOR.validate(params);
        assertThat(result).isEqualTo(SpokValidationResult.ok());
    }

    private static ServiceParams params(String componentName, Map<Environment, String> dbNames) {
        ServiceParams result = new ServiceParams();
        result.setParentAbcSlug(ABC_SERVICE);
        result.setName(componentName);

        for (Map.Entry<Environment, String> entry : dbNames.entrySet()) {
            RtcEnvironmentSpec spec = new RtcEnvironmentSpec();
            spec.setPostgreSql(new PostgreSqlParams(entry.getValue(), null, 0L, 0));
//            result.getEnvironments().put(entry.getKey(), spec);
            result.addInstallation(entry.getKey(), result.getName(), spec);
        }

        return result;
    }
}
