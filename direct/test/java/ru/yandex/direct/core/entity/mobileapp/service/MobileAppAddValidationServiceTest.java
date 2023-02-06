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
public class MobileAppAddValidationServiceTest {
    private static final String SOME_DOMAIN = "ya.ru";

    private MobileAppAddValidationService mobileAppAddValidationService;

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public MobileApp addingMobileApp;

    @Parameterized.Parameter(2)
    public RbacRole operatorRole;

    @Parameterized.Parameter(3)
    public Boolean expectError;

    private User operator;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"Клиент, без домена, без ошибок", new MobileApp(), RbacRole.CLIENT, false},
                {"Клиент, с доменом, ожидаем ошибку", new MobileApp().withDomain(SOME_DOMAIN), RbacRole.CLIENT, true},
                {"Супер, без домена, без ошибок", new MobileApp(), RbacRole.SUPER, false},
                {"Супер, с доменом, без ошибок", new MobileApp().withDomain(SOME_DOMAIN), RbacRole.SUPER, false},
                {"Менеджер, без домена, без ошибок", new MobileApp(), RbacRole.MANAGER, false},
                {"Менеджер, с доменом, без ошибок", new MobileApp().withDomain(SOME_DOMAIN), RbacRole.MANAGER, false},
                {"null, с доменом, без ошибок", new MobileApp().withDomain(SOME_DOMAIN), null, false},
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
        mobileAppAddValidationService = new MobileAppAddValidationService(mobileAppValidationService);

        if (operatorRole != null) {
            operator = mock(User.class);
            when(operator.getRole()).thenReturn(operatorRole);
        } else {
            operator = null;
        }
    }

    @Test
    public void validateMobileApp() {
        ClientId clientId = mock(ClientId.class);
        ValidationResult<MobileApp, Defect> result =
                mobileAppAddValidationService.validateMobileApp(operator, addingMobileApp, clientId);
        if (expectError) {
            assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                    validationError(path(field("domain")), noAccessToChangeDomain()))));
        } else {
            assertThat(result).is(matchedBy(hasNoErrorsAndWarnings()));
        }
    }
}
