package ru.yandex.market.checkout.checkouter.health;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import ru.yandex.market.checkout.checkouter.controllers.oms.archive.ArchiveEndPoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.ARCHIVED;

public class CheckouterEndPointAnnotationTest {

    private static final String PACKAGE_PREFIX = "ru.yandex.market.checkout.checkouter.controllers";
    private static final List<Class<? extends Annotation>> MAPPING_ANNOTATIONS = Arrays.asList(
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class,
            PatchMapping.class
    );

    @Test
    void methodsWithArchivedParamShouldHaveArchiveEndPointAnnotation() {
        Reflections reflections = new Reflections(
                PACKAGE_PREFIX,
                new MethodAnnotationsScanner()
        );

        List<Method> methods = MAPPING_ANNOTATIONS.stream()
                .map(reflections::getMethodsAnnotatedWith)
                .flatMap(Set::stream)
                .filter(this::hasArchivedParam)
                .collect(Collectors.toList());
        for (Method method : methods) {
            assertNotNull(method.getAnnotation(ArchiveEndPoint.class),
                    "Method with ARCHIVED param should be annotated with ArchiveEndPoint");
        }
    }

    @Test
    void methodsShouldHaveAppropriateArchiveEndPointAnnotation() {
        Reflections reflections = new Reflections(
                PACKAGE_PREFIX,
                new MethodAnnotationsScanner()
        );

        List<Method> methods = MAPPING_ANNOTATIONS.stream()
                .map(reflections::getMethodsAnnotatedWith)
                .flatMap(Set::stream)
                .filter(method -> method.getAnnotation(ArchiveEndPoint.class) != null)
                .sorted(Comparator.comparing(m -> m.getDeclaringClass().getCanonicalName()))
                .collect(Collectors.toList());
        for (Method method : methods) {
            assertNull(method.getAnnotation(PutMapping.class),
                    "PUT method shouldn't be annotated with ArchiveEndPoint!");
            assertNull(method.getAnnotation(DeleteMapping.class),
                    "DELETE method shouldn't be annotated with ArchiveEndPoint!");
            assertNull(method.getAnnotation(PatchMapping.class),
                    "PATCH method shouldn't be annotated with ArchiveEndPoint!");

            String[] realEndPointNames = null;
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                int notGetAndPostMethodsCount = (int) Stream.of(requestMapping.method())
                        .filter(m -> !RequestMethod.GET.equals(m)
                                && !RequestMethod.POST.equals(m)).count();
                assertEquals(0, notGetAndPostMethodsCount,
                        "Only GET and POST methods can be annotated with ArchiveEndPoint");
                realEndPointNames = requestMapping.value();
            }

            GetMapping getMapping = method.getAnnotation(GetMapping.class);
            if (getMapping != null) {
                realEndPointNames = getMapping.value();
            }

            PostMapping postMapping = method.getAnnotation(PostMapping.class);
            if (postMapping != null) {
                realEndPointNames = postMapping.value();
            }

            ArchiveEndPoint archiveEndPoint = method.getAnnotation(ArchiveEndPoint.class);
            String endPointNameInAnnotation = archiveEndPoint.value().getEndPointName();
            boolean appropriateEndPointName = Stream.of(realEndPointNames)
                    .anyMatch(name -> name.equals(endPointNameInAnnotation));
            assertTrue(appropriateEndPointName, "ArchiveEndPoint annotation name doesn't match real endpoint name, " +
                    "annotation name: " + endPointNameInAnnotation +
                    ", real endpoint name: " + Arrays.toString(realEndPointNames));

            assertTrue(hasArchivedParam(method), "Endpoint annotated with ArchiveEndPoint must have ARCHIVED " +
                    "RequestParam");
        }
    }

    private boolean hasArchivedParam(Method method) {
        Annotation[][] argAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < argAnnotations.length; i++) {
            Annotation[] argAnnotation = argAnnotations[i];
            for (int annotationIndex = 0; annotationIndex < argAnnotation.length; annotationIndex++) {
                if (argAnnotation[annotationIndex] instanceof RequestParam) {
                    RequestParam param = (RequestParam) argAnnotation[annotationIndex];
                    if (ARCHIVED.equals(param.value())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
