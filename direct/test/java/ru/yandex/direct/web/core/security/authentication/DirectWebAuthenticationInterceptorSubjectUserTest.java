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
import ru.yandex.direct.web.annotations.AllowedSubjectRoles;
import ru.yandex.direct.web.annotations.SubjectHasFeatures;

import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.web.core.security.authentication.DirectWebAuthenticationInterceptorTestUtils.notOk;
import static ru.yandex.direct.web.core.security.authentication.DirectWebAuthenticationInterceptorTestUtils.ok;

@RunWith(JUnitParamsRunner.class)
public class DirectWebAuthenticationInterceptorSubjectUserTest {

    private static final String METHOD_NAME = "testMethod";

    public static Collection<Object[]> parametrizedTestData() {
        return Arrays.asList(new Object[][]{
                {userWithRole(RbacRole.SUPER), ClassWithoutMethodWithout.class, RbacRole.SUPER, false, notOk()},
                {userWithRole(RbacRole.AGENCY), ClassWithoutMethodWithout.class, RbacRole.SUPER, false, notOk()},
                {userWithRole(RbacRole.CLIENT), ClassWithoutMethodWithout.class, RbacRole.SUPER, false, ok()},

                {userWithRole(RbacRole.SUPER), ClassDefault.class, RbacRole.SUPER, false, notOk()},
                {userWithRole(RbacRole.AGENCY), ClassDefault.class, RbacRole.SUPER, false, notOk()},
                {userWithRole(RbacRole.CLIENT), ClassDefault.class, RbacRole.SUPER, false, ok()},

                {userWithRole(RbacRole.SUPER), ClassAgency.class, RbacRole.SUPER, false, notOk()},
                {userWithRole(RbacRole.AGENCY), ClassAgency.class, RbacRole.SUPER, false, ok()},
                {userWithRole(RbacRole.CLIENT), ClassAgency.class, RbacRole.SUPER, false, notOk()},

                {userWithRole(RbacRole.SUPER), MethodSuper.class, RbacRole.SUPER, false, ok()},
                {userWithRole(RbacRole.AGENCY), MethodSuper.class, RbacRole.SUPER, false, notOk()},
                {userWithRole(RbacRole.CLIENT), MethodSuper.class, RbacRole.SUPER, false, notOk()},

                {userWithRole(RbacRole.SUPER), ClassAgencyMethodSuper.class, RbacRole.SUPER, false, ok()},
                {userWithRole(RbacRole.AGENCY), ClassAgencyMethodSuper.class, RbacRole.SUPER, false, notOk()},
                {userWithRole(RbacRole.CLIENT), ClassAgencyMethodSuper.class, RbacRole.SUPER, false, notOk()},

                {userWithRole(RbacRole.CLIENT), ClassDefaultWithFeature.class, RbacRole.SUPER, true, ok()},
                {userWithRole(RbacRole.CLIENT), ClassDefaultWithFeature.class, RbacRole.SUPER, false, notOk()},
                {userWithRole(RbacRole.AGENCY), ClassDefaultWithFeature.class, RbacRole.SUPER, true, notOk()},

                {userWithRole(RbacRole.CLIENT), ClassDefaultWithMethodFeature.class, RbacRole.SUPER, true, ok()},
                {userWithRole(RbacRole.CLIENT), ClassDefaultWithMethodFeature.class, RbacRole.SUPER, false, notOk()},

                {userWithRole(RbacRole.SUPER), MethodWithFeature.class, RbacRole.SUPER, true, ok()},
                {userWithRole(RbacRole.SUPER), MethodWithFeature.class, RbacRole.SUPER, false, notOk()},
                {userWithRole(RbacRole.CLIENT), MethodWithFeature.class, RbacRole.SUPER, true, notOk()},

                {userWithRole(RbacRole.SUPER), MethodWithClassFeature.class, RbacRole.SUPER, true, ok()},
                {userWithRole(RbacRole.SUPER), MethodWithClassFeature.class, RbacRole.SUPER, false, notOk()},

                {blockedUserWithRole(RbacRole.AGENCY), ClassAgency.class, RbacRole.MANAGER, false, notOk()},
                {blockedUserWithRole(RbacRole.CLIENT), ClassWithoutMethodWithout.class, RbacRole.AGENCY, false,
                        notOk()},
                {blockedUserWithRole(RbacRole.CLIENT), ClassWithoutMethodWithout.class, RbacRole.CLIENT, false,
                        notOk()},

                {blockedUserWithRole(RbacRole.AGENCY), ClassWithAllowedBlockedOperator.class,
                        RbacRole.MANAGER, false, ok()},
                {blockedUserWithRole(RbacRole.CLIENT), ClassWithAllowedBlockedOperator.class,
                        RbacRole.CLIENT, false, ok()},
                {blockedUserWithRole(RbacRole.CLIENT), ClassWithAllowedBlockedOperator.class,
                        RbacRole.AGENCY, false, ok()},
                {blockedUserWithRole(RbacRole.EMPTY), ClassWithAllowedBlockedOperator.class,
                        RbacRole.EMPTY, false, ok()},

                {blockedUserWithRole(RbacRole.AGENCY), ClassWithMethodAllowedBlockedOperator.class,
                        RbacRole.MANAGER, false, ok()},
                {blockedUserWithRole(RbacRole.CLIENT), ClassWithMethodAllowedBlockedOperator.class,
                        RbacRole.CLIENT, false, ok()},
                {blockedUserWithRole(RbacRole.CLIENT), ClassWithMethodAllowedBlockedOperator.class,
                        RbacRole.AGENCY, false, ok()},
                {blockedUserWithRole(RbacRole.EMPTY), ClassWithMethodAllowedBlockedOperator.class,
                        RbacRole.EMPTY, false, ok()},
        });
    }

    private HandlerMethod handlerMethod;
    private DirectAuthentication authentication;
    private FeatureService featureService;
    private DirectWebAuthenticationInterceptor interceptor;

    public void setUp(User subjectUser, Class<?> aClass, RbacRole operatorRole) throws Exception {
        authentication =
                new DirectAuthentication(new User().withRole(operatorRole).withStatusBlocked(false), subjectUser);
        handlerMethod = new HandlerMethod(aClass.newInstance(), aClass.getMethod(METHOD_NAME));
        featureService = mock(FeatureService.class);
        interceptor = new DirectWebAuthenticationInterceptor(featureService,
                new DirectWebAuthenticationSourceStub(authentication));
    }


    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{index}")
    public void checkSubjectUser(User subjectUser, Class<?> aClass, RbacRole operatorRole, boolean hasFeature,
                                 Condition<? super Throwable> exceptionCondition) throws Exception {
        setUp(subjectUser, aClass, operatorRole);
        doReturn(hasFeature)
                .when(featureService)
                .isEnabledForUid((Long) any(), anyCollection());
        Throwable thrown = catchThrowable(() -> interceptor.checkOperatorAndSubjectUser(handlerMethod, authentication));

        Assertions.assertThat(thrown).describedAs(
                "subjectUser role: %s; annotations: %s; expected: %s", subjectUser.getRole(), aClass.getSimpleName(),
                exceptionCondition.description()
        ).is(exceptionCondition);
    }

    private static User userWithRole(RbacRole role) {
        return new User()
                .withRole(role)
                .withStatusBlocked(false);
    }

    private static User blockedUserWithRole(RbacRole role) {
        return userWithRole(role)
                .withStatusBlocked(true);
    }

    public static class ClassWithoutMethodWithout {
        public void testMethod() {
        }
    }

    @AllowedSubjectRoles
    public static class ClassDefault {
        public void testMethod() {
        }
    }

    @AllowedSubjectRoles({RbacRole.AGENCY})
    public static class ClassAgency {
        public void testMethod() {
        }
    }

    public static class MethodSuper {
        @AllowedSubjectRoles({RbacRole.SUPER})
        public void testMethod() {
        }
    }

    @AllowedSubjectRoles({RbacRole.AGENCY})
    public static class ClassAgencyMethodSuper {
        @AllowedSubjectRoles({RbacRole.SUPER})
        public void testMethod() {
        }
    }

    @AllowedSubjectRoles
    @SubjectHasFeatures({FeatureName.CPM_BANNER})
    public static class ClassDefaultWithFeature {
        public void testMethod() {
        }
    }

    @AllowedSubjectRoles
    public static class ClassDefaultWithMethodFeature {
        @SubjectHasFeatures({FeatureName.CPM_BANNER})
        public void testMethod() {
        }
    }

    public static class MethodWithFeature {
        @AllowedSubjectRoles({RbacRole.SUPER})
        @SubjectHasFeatures({FeatureName.CPM_BANNER})
        public void testMethod() {
        }
    }

    @SubjectHasFeatures({FeatureName.CPM_BANNER})
    public static class MethodWithClassFeature {
        @AllowedSubjectRoles({RbacRole.SUPER})
        public void testMethod() {
        }
    }

    @AllowedSubjectRoles({RbacRole.AGENCY, RbacRole.CLIENT, RbacRole.EMPTY})
    @AllowedBlockedOperatorOrUser
    public static class ClassWithAllowedBlockedOperator {
        public void testMethod() {
        }
    }

    @AllowedSubjectRoles({RbacRole.AGENCY, RbacRole.CLIENT, RbacRole.EMPTY})
    public static class ClassWithMethodAllowedBlockedOperator {
        @AllowedBlockedOperatorOrUser
        public void testMethod() {
        }
    }

}
