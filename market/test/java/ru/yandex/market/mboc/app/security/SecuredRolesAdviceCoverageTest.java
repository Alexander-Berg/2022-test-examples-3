package ru.yandex.market.mboc.app.security;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.fail;

/**
 * This test ensures we don't accidentally miss security annotations on new controller.
 *
 * @author yuramalinov
 * @created 25.09.18
 */
public class SecuredRolesAdviceCoverageTest {
    @Test
    public void testAllControllersAreEitherCoveredOrIgnored() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));

        List<String> fails = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("ru.yandex.market.mboc")) {
            Class<?> cls = Class.forName(bd.getBeanClassName());
            if (cls.getAnnotation(SecuredRoles.class) == null && cls.getAnnotation(SecuredRolesIgnore.class) == null) {
                fails.add("Either @SecuredRoles or @SecuredRolesIgnore is required for controller "
                    + cls.getCanonicalName());
            }
        }

        if (!fails.isEmpty()) {
            fail(String.join("\n", fails));
        }
    }
}
