package ru.yandex.direct.web.core.security.authentication;

import java.util.Arrays;
import java.util.Collection;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.apache.commons.lang3.tuple.Triple;
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
import ru.yandex.direct.web.annotations.CheckSubjectAndUserFeaturesSeparately;
import ru.yandex.direct.web.annotations.OperatorHasFeatures;
import ru.yandex.direct.web.annotations.SubjectHasFeatures;

import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.web.core.security.authentication.DirectWebAuthenticationInterceptorTestUtils.notOk;
import static ru.yandex.direct.web.core.security.authentication.DirectWebAuthenticationInterceptorTestUtils.ok;

@RunWith(JUnitParamsRunner.class)
public class DirectWebAuthenticationInterceptorCheckFeaturesSeparatelyTest {
    private static final Long CLIENT_UID = 1L;
    private static final Long OPERATOR_UID = 2L;

    private static final String METHOD_NAME = "testMethod";

    public static Collection<Object[]> parametrizedTestData() {
        return Arrays.asList(new Object[][]{
                {ClassCheckSeparatelyOperatorClientFeature.class, false, false, notOk()},
                {ClassCheckSeparatelyOperatorClientFeature.class, false, true, ok()},
                {ClassCheckSeparatelyOperatorClientFeature.class, true, false, ok()},
                {ClassCheckSeparatelyOperatorClientFeature.class, true, true, ok()},

                {ClassCheckSeparatelyOperatorFeature.class, false, false, notOk()},
                {ClassCheckSeparatelyOperatorFeature.class, false, true, notOk()},
                {ClassCheckSeparatelyOperatorFeature.class, true, false, ok()},
                {ClassCheckSeparatelyOperatorFeature.class, true, true, ok()},

                {ClassCheckSeparatelyClientFeature.class, false, false, notOk()},
                {ClassCheckSeparatelyClientFeature.class, false, true, ok()},
                {ClassCheckSeparatelyClientFeature.class, true, false, notOk()},
                {ClassCheckSeparatelyClientFeature.class, true, true, ok()},

                {ClassCheckNonSeparatelyOperatorClientFeature.class, false, false, notOk()},
                {ClassCheckNonSeparatelyOperatorClientFeature.class, false, true, notOk()},
                {ClassCheckNonSeparatelyOperatorClientFeature.class, true, false, notOk()},
                {ClassCheckNonSeparatelyOperatorClientFeature.class, true, true, ok()},

                {ClassCheckNonSeparatelyOperatorFeature.class, false, false, notOk()},
                {ClassCheckNonSeparatelyOperatorFeature.class, false, true, notOk()},
                {ClassCheckNonSeparatelyOperatorFeature.class, true, false, ok()},
                {ClassCheckNonSeparatelyOperatorFeature.class, true, true, ok()},

                {ClassCheckNonSeparatelyClientFeature.class, false, false, notOk()},
                {ClassCheckNonSeparatelyClientFeature.class, false, true, ok()},
                {ClassCheckNonSeparatelyClientFeature.class, true, false, notOk()},
                {ClassCheckNonSeparatelyClientFeature.class, true, true, ok()}
        });
    }

    private HandlerMethod handlerMethod;
    private DirectAuthentication authentication;
    private FeatureService featureService;
    private DirectWebAuthenticationInterceptor interceptor;


    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{index}")
    public void checkSubjectUser(Class<?> aClass,
                                 boolean operatorHasFeature, boolean subjectHasFeature,
                                 Condition<? super Throwable> exceptionCondition) throws Exception {
        authentication = new DirectAuthentication(
                new User().withUid(OPERATOR_UID).withRole(RbacRole.SUPER).withStatusBlocked(false),
                new User().withUid(CLIENT_UID).withRole(RbacRole.CLIENT).withStatusBlocked(false));
        handlerMethod = new HandlerMethod(aClass.newInstance(), aClass.getMethod(METHOD_NAME));
        featureService = mock(FeatureService.class);
        interceptor = new DirectWebAuthenticationInterceptor(featureService,
                new DirectWebAuthenticationSourceStub(authentication));
        doReturn(subjectHasFeature)
                .when(featureService)
                .isEnabledForUid(eq(CLIENT_UID), anyCollection());
        doReturn(operatorHasFeature)
                .when(featureService)
                .isEnabledForUid(eq(OPERATOR_UID), anyCollection());
        Throwable thrown = catchThrowable(() -> interceptor.checkOperatorAndSubjectUser(handlerMethod, authentication));

        Assertions.assertThat(thrown)
                .describedAs(Triple.of(operatorHasFeature, subjectHasFeature, aClass).toString())
                .is(exceptionCondition);
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

    @CheckSubjectAndUserFeaturesSeparately
    @OperatorHasFeatures({FeatureName.CPM_DEALS})
    @SubjectHasFeatures({FeatureName.CPM_BANNER})
    public static class ClassCheckSeparatelyOperatorClientFeature {
        public void testMethod() {
        }
    }

    @CheckSubjectAndUserFeaturesSeparately
    @OperatorHasFeatures({FeatureName.CPM_DEALS})
    public static class ClassCheckSeparatelyOperatorFeature {
        public void testMethod() {
        }
    }

    @CheckSubjectAndUserFeaturesSeparately
    @SubjectHasFeatures({FeatureName.CPM_BANNER})
    public static class ClassCheckSeparatelyClientFeature {
        public void testMethod() {
        }
    }

    @OperatorHasFeatures({FeatureName.CPM_DEALS})
    @SubjectHasFeatures({FeatureName.CPM_BANNER})
    public static class ClassCheckNonSeparatelyOperatorClientFeature {
        public void testMethod() {
        }
    }

    @OperatorHasFeatures({FeatureName.CPM_DEALS})
    public static class ClassCheckNonSeparatelyOperatorFeature {
        public void testMethod() {
        }
    }

    @SubjectHasFeatures({FeatureName.CPM_BANNER})
    public static class ClassCheckNonSeparatelyClientFeature {
        public void testMethod() {
        }
    }
}
