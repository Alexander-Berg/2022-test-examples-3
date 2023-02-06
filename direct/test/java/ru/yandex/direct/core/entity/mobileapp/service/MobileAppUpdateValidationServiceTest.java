package ru.yandex.direct.core.entity.mobileapp.service;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.mobileapp.MobileAppDefects.noAccessToChangeDomain;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(value = Parameterized.class)
public class MobileAppUpdateValidationServiceTest {
    private static final String SOME_DOMAIN = "ya.ru";

    private MobileAppUpdateValidationService mobileAppUpdateValidationService;

    @Parameterized.Parameter()
    public String description;

    @Parameterized.Parameter(1)
    public ModelChanges<MobileApp> modelChanges;

    @Parameterized.Parameter(2)
    public RbacRole operatorRole;

    @Parameterized.Parameter(3)
    public Boolean expectError;

    private User operator;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Клиент, без домена, без ошибок", createMobileAppChanges(), RbacRole.CLIENT, false},
                {"Клиент, с доменом, ожидаем ошибку", createMobileAppChanges(SOME_DOMAIN), RbacRole.CLIENT, true},
                {"Супер, без домена, без ошибок", createMobileAppChanges(), RbacRole.SUPER, false},
                {"Супер, с доменом, без ошибок", createMobileAppChanges(SOME_DOMAIN), RbacRole.SUPER, false},
                {"null, с доменом, без ошибок", createMobileAppChanges(SOME_DOMAIN), null, false},
        });
    }

    @Before
    public void before() {
        MobileAppValidationService mobileAppValidationService = mock(MobileAppValidationService.class);
        when(mobileAppValidationService.validateMobileApp(any(MobileApp.class), any(ClientId.class)))
                .thenAnswer(answer -> {
                    Object mobileApp = answer.getArgument(0);
                    return ValidationResult.success(mobileApp);
                });

        mobileAppUpdateValidationService = new MobileAppUpdateValidationService(mobileAppValidationService);

        if (operatorRole != null) {
            operator = mock(User.class);
            when(operator.getRole()).thenReturn(operatorRole);
        } else {
            operator = null;
        }
    }

    @Test
    public void validateMobileApp() {
        ValidationResult<ModelChanges<MobileApp>, Defect> result =
                mobileAppUpdateValidationService.validateModelChanges(operator, modelChanges);
        if (expectError) {
            assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                    validationError(path(field("domain")), noAccessToChangeDomain()))));
        } else {
            assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static ModelChanges<MobileApp> createMobileAppChanges(String domain) {
        ModelChanges<MobileApp> mc = createMobileAppChanges();
        mc.process(domain, MobileApp.DOMAIN);
        return mc;
    }

    private static ModelChanges<MobileApp> createMobileAppChanges() {
        return new ModelChanges<>(1L, MobileApp.class);
    }
}
