package ru.yandex.market.tsum.spok.validation.validator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.tsum.clients.solomon.SolomonAccessChecker;
import ru.yandex.market.tsum.clients.solomon.models.SolomonAccessCheckResult;
import ru.yandex.market.tsum.core.registry.v2.model.spok.ServiceParams;
import ru.yandex.market.tsum.registry.proto.model.AppType;
import ru.yandex.market.tsum.registry.v2.dao.ServicesDao;
import ru.yandex.market.tsum.registry.v2.dao.model.Service;
import ru.yandex.market.tsum.spok.validation.model.SpokValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tsum.spok.validation.validator.SpokSolomonProjectNameValidator.NOT_FOUND_SERVICE_WITH_ABC_SLUG;
import static ru.yandex.market.tsum.spok.validation.validator.SpokSolomonProjectNameValidator.SERVICE_ID_FIELD_NAME;
import static ru.yandex.market.tsum.spok.validation.validator.SpokSolomonProjectNameValidator.SOLOMON_PROJECT_FIELD_NAME;
import static ru.yandex.market.tsum.spok.validation.validator.SpokSolomonProjectNameValidator.SOLOMON_PROJECT_NOT_FOUND;

@RunWith(MockitoJUnitRunner.class)
public class SpokSolomonProjectNameValidatorTest {
    private static final String ERROR_MSG = "error";
    private static final String ABC_SLUG = "abc-slug";
    private static final String NOT_EXISTING_SERVICE_ABC_SLUG = "not-existing-service-abc-slug";
    private static final String ABC_GROUP = "abc-group";
    private static final String ACCESSIBLE_PROJECT = "accessible-project";
    private static final String NOT_ACCESSIBLE_PROJECT = "not-accessible-project";
    private static final String NOT_EXISTING_PROJECT = "not-existing-project";
    @Mock
    private ServicesDao servicesDao;
    @Mock
    private SolomonAccessChecker solomonAccessChecker;

    @InjectMocks
    private SpokSolomonProjectNameValidator validator;

    @Before
    public void setUp() {
        when(servicesDao.getBySlug(ABC_SLUG)).thenReturn(new Service());

        when(solomonAccessChecker.checkProjectAccess(ACCESSIBLE_PROJECT))
                .thenReturn(SolomonAccessCheckResult.accessible(ACCESSIBLE_PROJECT));
        when(solomonAccessChecker.checkProjectAccess(NOT_ACCESSIBLE_PROJECT))
                .thenReturn(SolomonAccessCheckResult.accessDenied(NOT_ACCESSIBLE_PROJECT, ERROR_MSG));
        when(solomonAccessChecker.checkProjectAccess(NOT_EXISTING_PROJECT))
                .thenReturn(SolomonAccessCheckResult.notFound(NOT_EXISTING_PROJECT));
    }

    @Test
    public void accessibleProjectForExistingJavaApp() {
        assertThat(validator.validate(params(ABC_GROUP)))
                .isEqualTo(SpokValidationResult.ok());
    }

    @Test
    public void notExistingServiceForAbcSlug() {
        assertThat(validator.check(NOT_EXISTING_SERVICE_ABC_SLUG, ACCESSIBLE_PROJECT, false))
                .isEqualTo(SpokValidationResult.error(
                        SERVICE_ID_FIELD_NAME,
                        String.format(NOT_FOUND_SERVICE_WITH_ABC_SLUG, NOT_EXISTING_SERVICE_ABC_SLUG)
                ));
    }

    @Test
    public void accessibleProject() {
        assertThat(validator.check(ABC_SLUG, ACCESSIBLE_PROJECT, false))
                .isEqualTo(SpokValidationResult.ok());
    }

    @Test
    public void notAccessProject() {
        assertThat(validator.check(ABC_SLUG, NOT_ACCESSIBLE_PROJECT, false))
                .isEqualTo(SpokValidationResult.error(SOLOMON_PROJECT_FIELD_NAME, ERROR_MSG));
    }

    @Test
    public void notExistingProject() {
        assertThat(validator.check(ABC_SLUG, NOT_EXISTING_PROJECT, false))
                .isEqualTo(SpokValidationResult.error(
                        SOLOMON_PROJECT_FIELD_NAME,
                        String.format(SOLOMON_PROJECT_NOT_FOUND, NOT_EXISTING_PROJECT)
                ));
    }

    @Test
    public void notExistingProjectOnCreateNewProject() {
        assertThat(validator.check(ABC_SLUG, NOT_EXISTING_PROJECT, true))
                .isEqualTo(SpokValidationResult.ok());
    }

    private ServiceParams params(String abcGroup) {
        ServiceParams result = new ServiceParams();
        result.setApplicationType(AppType.JAVA_EXISTING.toString());
        result.setAbcSlug(abcGroup);
        result.setParentAbcSlug(ABC_SLUG);
        result.setSolomonProjectId(ACCESSIBLE_PROJECT);
        return result;
    }
}
