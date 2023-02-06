package ru.yandex.market.abo;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ru.yandex.market.abo.core.assessor.AboRole;
import ru.yandex.market.abo.web.controller.MonitoringController;
import ru.yandex.market.abo.web.controller.PageMatcherController;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 13.11.2019.
 */
class DefaultSecurityTest {
    private static final String PACKAGE = "ru.yandex.market.abo.web.controller";

    @Test
    void testPreAuthorizeAnnotationAndRole() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        for (BeanDefinition controllerBean : scanner.findCandidateComponents(PACKAGE)) {
            checkPreAuthorizeAnnotationForController(Class.forName(controllerBean.getBeanClassName()));
        }
    }

    private static void checkPreAuthorizeAnnotationForController(Class<?> controllerClass) {
        if (MonitoringController.class.equals(controllerClass) || PageMatcherController.class.equals(controllerClass)) {
            return;
        }
        PreAuthorize annotation = controllerClass.getAnnotation(PreAuthorize.class);
        if (annotation == null) {
            checkPreAuthorizeAnnotationForMethods(controllerClass.getMethods());
        } else {
            checkAboRole(annotation.value());
        }
    }

    private static void checkPreAuthorizeAnnotationForMethods(Method[] methods) {
        for (Method method : methods) {
            if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) != null) {
                PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
                assertNotNull(annotation, "PreAuthorize annotation for method "
                        + method.toString() + " not found");
                checkAboRole(annotation.value());
            }
        }
    }

    private static void checkAboRole(String authorizeExpression) {
        if (!authorizeExpression.contains("hasAnyRole") && !authorizeExpression.contains("hasRole")) {
            fail("PreAuthorize annotation does not contain necessary argument(hasAnyRole/hasRole)");
        }
        authorizeExpression = StringUtils.substringBetween(authorizeExpression, "(", ")");
        Arrays.stream(authorizeExpression.split(","))
                .map(a -> a.trim().replace("'", ""))
                .forEach(DefaultSecurityTest::tryParseRole);
    }

    private static void tryParseRole(String roleName) {
        try {
            AboRole.valueOf(roleName);
        } catch (Exception e) {
            fail("Could not parse role with name: " + roleName);
        }
    }
}
