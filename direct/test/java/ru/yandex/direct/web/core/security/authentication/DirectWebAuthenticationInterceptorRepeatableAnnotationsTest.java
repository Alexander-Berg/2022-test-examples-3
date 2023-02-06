package ru.yandex.direct.web.core.security.authentication;

import java.util.Arrays;
import java.util.Collection;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatcher;
import org.springframework.web.method.HandlerMethod;

import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.security.DirectAuthentication;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.annotations.OperatorHasFeatures;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isEqualCollection;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.web.core.security.authentication.DirectWebAuthenticationInterceptorTestUtils.notOk;
import static ru.yandex.direct.web.core.security.authentication.DirectWebAuthenticationInterceptorTestUtils.ok;

@RunWith(Parameterized.class)
public class DirectWebAuthenticationInterceptorRepeatableAnnotationsTest {
    private static final String METHOD_NAME = "testMethod";

    private static final ArgumentMatcher<Collection<FeatureName>> BANNER_AIMING_ALLOWED_FEATURE_COLLECTION_MATCHER =
            getCollectionMatcher(singletonList(FeatureName.BANNER_AIMING_ALLOWED));

    private static final ArgumentMatcher<Collection<FeatureName>> BANNER_AIMING_ALLOWED_CPM_FEATURE_COLLECTION_MATCHER =
            getCollectionMatcher(singletonList(FeatureName.BANNER_AIMING_CPM_ALLOWED));

    @Parameterized.Parameter
    public Class<?> paramClass;

    @Parameterized.Parameter(1)
    public boolean hasBannerAimingAllowedFeature;

    @Parameterized.Parameter(2)
    public boolean hasBannerAimingCpmAllowedFeature;

    @Parameterized.Parameter(3)
    public Condition<? super Throwable> exceptionCondition;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {ClassWithMultipleMethodAnnotations.class, true, true, ok()},
                {ClassWithMultipleMethodAnnotations.class, true, false, ok()},
                {ClassWithMultipleMethodAnnotations.class, false, true, ok()},
                {ClassWithMultipleMethodAnnotations.class, false, false, notOk()},

                {ClassWithMethodAndClassAnnotation.class, true, false, notOk()},
                {ClassWithMethodAndClassAnnotation.class, false, true, ok()},
        });
    }

    private HandlerMethod handlerMethod;
    private DirectAuthentication authentication;
    private FeatureService featureService;
    private DirectWebAuthenticationInterceptor interceptor;

    @Before
    public void setUp() throws Exception {
        authentication =
                new DirectAuthentication(new User().withRole(RbacRole.SUPER).withStatusBlocked(false),
                        new User().withRole(RbacRole.CLIENT).withStatusBlocked(false));
        handlerMethod = new HandlerMethod(paramClass.newInstance(), paramClass.getMethod(METHOD_NAME));
        featureService = mock(FeatureService.class);
        interceptor = new DirectWebAuthenticationInterceptor(featureService,
                new DirectWebAuthenticationSourceStub(authentication));
    }


    @Test
    public void checkRepeatableAnnotations() {
        doReturn(hasBannerAimingAllowedFeature).when(featureService).isEnabledForUid((Long) any(),
                argThat(BANNER_AIMING_ALLOWED_FEATURE_COLLECTION_MATCHER));
        doReturn(hasBannerAimingCpmAllowedFeature).when(featureService).isEnabledForUid((Long) any(),
                argThat(BANNER_AIMING_ALLOWED_CPM_FEATURE_COLLECTION_MATCHER));

        Throwable thrown = catchThrowable(() -> interceptor.checkOperatorAndSubjectUser(handlerMethod, authentication));
        Assertions.assertThat(thrown).is(exceptionCondition);
    }

    /**
     * Для проверки, что если у метода 2 раза указана аннотация OperatorHasFeatures,
     * то доступ к методу разрешен, если хотя бы одна из фичей есть у оператора
     */

    public static class ClassWithMultipleMethodAnnotations {
        @OperatorHasFeatures({FeatureName.BANNER_AIMING_CPM_ALLOWED})
        @OperatorHasFeatures({FeatureName.BANNER_AIMING_ALLOWED})
        public void testMethod() {
        }
    }

    /**
     * Для проверки, что если у класса и у метода укзана одинаковая аннотация OperatorHasFeatures,
     * то предпочтнеие отдастся аннотации над методом
     */

    @OperatorHasFeatures({FeatureName.BANNER_AIMING_ALLOWED})
    public static class ClassWithMethodAndClassAnnotation {
        @OperatorHasFeatures({FeatureName.BANNER_AIMING_CPM_ALLOWED})
        public void testMethod() {
        }
    }

    private static <T> ArgumentMatcher<Collection<T>> getCollectionMatcher(Collection<T> collection) {
        return v -> isEqualCollection(collection, v);
    }
}
