package ru.yandex.direct.web.core.security.authentication;

import java.util.Arrays;
import java.util.Collection;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.web.method.HandlerMethod;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.annotations.AllowedBlockedOperatorOrUser;
import ru.yandex.direct.web.annotations.AllowedOperatorRoles;
import ru.yandex.direct.web.annotations.OperatorHasFeatures;

import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.web.core.security.authentication.DirectWebAuthenticationInterceptorTestUtils.notOk;
import static ru.yandex.direct.web.core.security.authentication.DirectWebAuthenticationInterceptorTestUtils.ok;

@RunWith(JUnitParamsRunner.class)
public class DirectWebAuthenticationInterceptorOperatorTest {

    private static final String METHOD_NAME = "testMethod";

    public static Collection<Object[]> parametrizedTestData() {
        return Arrays.asList(new Object[][]{
                {operatorWithRole(RbacRole.SUPER), ClassWithoutMethodWithout.class, false, ok()},
                {operatorWithRole(RbacRole.AGENCY), ClassWithoutMethodWithout.class, false, ok()},
                {operatorWithRole(RbacRole.CLIENT), ClassWithoutMethodWithout.class, false, ok()},

                {operatorWithRole(RbacRole.SUPER), ClassDefault.class, false, ok()},
                {operatorWithRole(RbacRole.AGENCY), ClassDefault.class, false, ok()},
                {operatorWithRole(RbacRole.CLIENT), ClassDefault.class, false, ok()},

                {operatorWithRole(RbacRole.SUPER), ClassAgency.class, false, notOk()},
                {operatorWithRole(RbacRole.AGENCY), ClassAgency.class, false, ok()},
                {operatorWithRole(RbacRole.CLIENT), ClassAgency.class, false, notOk()},

                {operatorWithRole(RbacRole.SUPER), MethodSuper.class, false, ok()},
                {operatorWithRole(RbacRole.AGENCY), MethodSuper.class, false, notOk()},
                {operatorWithRole(RbacRole.CLIENT), MethodSuper.class, false, notOk()},

                {operatorWithRole(RbacRole.SUPER), ClassAgencyMethodSuper.class, false, ok()},
                {operatorWithRole(RbacRole.AGENCY), ClassAgencyMethodSuper.class, false, notOk()},
                {operatorWithRole(RbacRole.CLIENT), ClassAgencyMethodSuper.class, false, notOk()},

                {operatorWithRole(RbacRole.SUPER), ClassDefaultWithFeature.class, true, ok()},
                {operatorWithRole(RbacRole.AGENCY), ClassDefaultWithFeature.class, true, ok()},
                {operatorWithRole(RbacRole.CLIENT), ClassDefaultWithFeature.class, false, notOk()},

                {operatorWithRole(RbacRole.SUPER), ClassDefaultWithMethodFeature.class, true, ok()},
                {operatorWithRole(RbacRole.AGENCY), ClassDefaultWithMethodFeature.class, true, ok()},
                {operatorWithRole(RbacRole.CLIENT), ClassDefaultWithMethodFeature.class, false, notOk()},

                {operatorWithRole(RbacRole.SUPER), MethodWithFeature.class, true, ok()},
                {operatorWithRole(RbacRole.SUPER), MethodWithFeature.class, false, notOk()},
                {operatorWithRole(RbacRole.AGENCY), MethodWithFeature.class, true, notOk()},

                {operatorWithRole(RbacRole.SUPER), MethodWithClassFeature.class, true, ok()},
                {operatorWithRole(RbacRole.SUPER), MethodWithClassFeature.class, false, notOk()},
                {operatorWithRole(RbacRole.AGENCY), MethodWithClassFeature.class, true, notOk()},

                {operatorWithRole(RbacRole.SUPER), ClassDefault.class, false, ok()},
                {operatorWithRole(RbacRole.AGENCY), ClassDefault.class, false, ok()},
                {operatorWithRole(RbacRole.CLIENT), ClassDefault.class, false, ok()},

                {blockedOperatorWithRole(RbacRole.SUPER), ClassDefault.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.LIMITED_SUPPORT), ClassDefault.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.AGENCY), ClassDefault.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.CLIENT), ClassDefault.class, false, ok()},

                {blockedOperatorWithRole(RbacRole.SUPER), ClassDefault.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.LIMITED_SUPPORT), ClassDefault.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.AGENCY), ClassDefault.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.CLIENT), ClassDefault.class, false, ok()},

                {blockedOperatorWithRole(RbacRole.SUPER), ClassWithAllowedBlockedOperator.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.LIMITED_SUPPORT),
                        ClassWithAllowedBlockedOperator.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.AGENCY), ClassWithAllowedBlockedOperator.class, false, ok()},
                {blockedOperatorWithRole(RbacRole.CLIENT), ClassWithAllowedBlockedOperator.class, false, ok()},

                {blockedOperatorWithRole(RbacRole.SUPER), ClassWithMethodAllowedBlockedOperator.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.LIMITED_SUPPORT),
                        ClassWithMethodAllowedBlockedOperator.class, false, notOk()},
                {blockedOperatorWithRole(RbacRole.AGENCY), ClassWithMethodAllowedBlockedOperator.class, false, ok()},
                {blockedOperatorWithRole(RbacRole.CLIENT), ClassWithMethodAllowedBlockedOperator.class, false, ok()},
        });
    }

    private HandlerMethod handlerMethod;
    private DirectAuthentication authentication;
    private FeatureService featureService;
    private DirectWebAuthenticationInterceptor interceptor;

    public void setUp(User operator, Class<?> aClass) throws Exception {
        authentication = new DirectAuthentication(operator,
                new User().withRole(RbacRole.CLIENT).withStatusBlocked(false));
        handlerMethod = new HandlerMethod(aClass.newInstance(), aClass.getMethod(METHOD_NAME));
        featureService = mock(FeatureService.class);
        interceptor = new DirectWebAuthenticationInterceptor(featureService,
                new DirectWebAuthenticationSourceStub(authentication));
    }


    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{index}")
    public void checkOperator(User operator, Class<?> aClass, boolean hasFeature,
                              Condition<? super Throwable> exceptionCondition) throws Exception {
        setUp(operator, aClass);
        doReturn(hasFeature)
                .when(featureService)
                .isEnabledForUid((Long) any(), anyCollection());
        Throwable thrown = catchThrowable(() -> interceptor.checkOperatorAndSubjectUser(handlerMethod, authentication));

        Assertions.assertThat(thrown).is(exceptionCondition);
    }

    private static User operatorWithRole(RbacRole role) {
        return new User()
                .withRole(role)
                .withStatusBlocked(false);
    }

    private static User blockedOperatorWithRole(RbacRole role) {
        return operatorWithRole(role)
                .withStatusBlocked(true);
    }

    public static class ClassWithoutMethodWithout {
        public void testMethod() {
        }
    }

    @AllowedOperatorRoles
    public static class ClassDefault {
        public void testMethod() {
        }
    }

    @AllowedOperatorRoles({RbacRole.AGENCY})
    public static class ClassAgency {
        public void testMethod() {
        }
    }

    public static class MethodSuper {
        @AllowedOperatorRoles({RbacRole.SUPER})
        public void testMethod() {
        }
    }

    @AllowedOperatorRoles({RbacRole.AGENCY})
    public static class ClassAgencyMethodSuper {
        @AllowedOperatorRoles({RbacRole.SUPER})
        public void testMethod() {
        }
    }

    @AllowedOperatorRoles
    @OperatorHasFeatures({FeatureName.CPM_BANNER})
    public static class ClassDefaultWithFeature {
        public void testMethod() {
        }
    }

    @AllowedOperatorRoles
    public static class ClassDefaultWithMethodFeature {
        @OperatorHasFeatures({FeatureName.CPM_BANNER})
        public void testMethod() {
        }
    }

    public static class MethodWithFeature {
        @AllowedOperatorRoles({RbacRole.SUPER})
        @OperatorHasFeatures({FeatureName.CPM_BANNER})
        public void testMethod() {
        }
    }

    @OperatorHasFeatures({FeatureName.CPM_BANNER})
    public static class MethodWithClassFeature {
        @AllowedOperatorRoles({RbacRole.SUPER})
        public void testMethod() {
        }
    }

    @AllowedBlockedOperatorOrUser
    public static class ClassWithAllowedBlockedOperator {
        public void testMethod() {
        }
    }

    public static class ClassWithMethodAllowedBlockedOperator {
        @AllowedBlockedOperatorOrUser
        public void testMethod() {
        }
    }

}
