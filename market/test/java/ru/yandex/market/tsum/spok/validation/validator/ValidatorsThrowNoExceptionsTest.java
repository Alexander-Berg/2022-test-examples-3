package ru.yandex.market.tsum.spok.validation.validator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.market.tsum.clients.abc.AbcApiClient;
import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.clients.mdb.MdbClient;
import ru.yandex.market.tsum.clients.sandbox.SandboxClient;
import ru.yandex.market.tsum.clients.solomon.SolomonAccessChecker;
import ru.yandex.market.tsum.clients.solomon.models.SolomonAccessCheckResult;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.registry.v2.dao.ComponentsDao;
import ru.yandex.market.tsum.registry.v2.dao.ServicesDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Service;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class ValidatorsThrowNoExceptionsTest {
    private static final Gson GSON = new Gson();

    private final String fileName;
    private final SpokValidator validator;
    private ServiceParams serviceParams;

    private static final List<String> FILE_NAMES = List.of(
            "mbo-t2m-monitor.json",
            "mobile-validator.json",
            "wms-radiator.json"
    );

    private static final List<SpokValidator> VALIDATORS = List.of(
            getSpokAbcServiceValidator(),
            getArcadiaPathValidator(),
            getComponentNameValidator(),
            new SpokPostgresConnectionLimitValidator(),
            getSpokPostgresDbNamesValidator(),
            getResourceTypeNameValidator(),
            getSolomonProjectNameValidator()
    );

    public ValidatorsThrowNoExceptionsTest(String fileName,
                                           @SuppressWarnings("unused") String validatorName,
                                           SpokValidator validator) {
        this.fileName = fileName;
        this.validator = validator;
    }

    private static SpokValidator getArcadiaPathValidator() {
        return new SpokArcadiaPathValidator(mock(RootArcadiaClient.class));
    }

    private static SpokValidator getSpokAbcServiceValidator() {
        MdbClient mdbClient = mock(MdbClient.class);
        when(mdbClient.folderExists(anyString())).thenReturn(true);
        return new SpokAbcServiceIdValidator(mock(AbcApiClient.class), mdbClient);
    }

    private static SpokValidator getComponentNameValidator() {
        return new SpokComponentNameValidator(mock(ComponentsDao.class));
    }

    private static SpokPostgresDbNamesValidator getSpokPostgresDbNamesValidator() {
        return new SpokPostgresDbNamesValidator(mock(MdbClient.class));
    }

    private static SpokValidator getResourceTypeNameValidator() {
        return new SpokResourceTypeNameValidator(mock(SandboxClient.class));
    }

    private static SpokValidator getSolomonProjectNameValidator() {
        ServicesDao servicesDao = mock(ServicesDao.class);
        Service service = new Service();
        service.setProjectId("projectId");
        when(servicesDao.get(anyString())).thenReturn(service);
        String solomonProjectId = "market-projectId";
        SolomonAccessChecker accessChecker = mock(SolomonAccessChecker.class);
        when(accessChecker.checkProjectAccess(eq(solomonProjectId)))
                .thenReturn(SolomonAccessCheckResult.accessible(solomonProjectId));

        return new SpokSolomonProjectNameValidator(
                servicesDao,
                accessChecker);
    }

    @Parameterized.Parameters(name = "{0} using {1}")
    public static Collection<Object[]> parameters() {
        return FILE_NAMES.stream()
                .flatMap(
                        fileName -> VALIDATORS.stream()
                                .map(validator -> new Object[]{fileName, validator.getClass().getSimpleName(),
                                        validator})
                )
                .collect(Collectors.toList());
    }

    @Before
    public void setUp() throws URISyntaxException, IOException {
        String fn = "spok-validation-testcases/" + fileName;
        serviceParams = GSON.fromJson(FileUtils.readFileToString(getFile(fn), StandardCharsets.UTF_8),
                ServiceParams.class);
    }

    private static File getFile(String name) throws URISyntaxException {
        URL resource = ValidatorsThrowNoExceptionsTest.class.getClassLoader().getResource(name);
        checkNotNull(resource, "file not found: %s", name);
        return new File(resource.toURI());
    }

    @Test
    public void validate() {
        validator.validate(serviceParams);
    }
}
