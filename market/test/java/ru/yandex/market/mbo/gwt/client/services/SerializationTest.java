package ru.yandex.market.mbo.gwt.client.services;

import com.google.gwt.user.client.rpc.RemoteService;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import ru.yandex.market.mbo.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 22.12.2017
 */
@SuppressWarnings({"NonJREEmulationClassesInClientCode", "checkstyle:HideUtilityClassConstructor"})
@RunWith(Enclosed.class)
public class SerializationTest {

    private static final int EXPECTED_SERVICES_COUNT = 50;

    private static List<Class<?>> services;

    @RunWith(Parameterized.class)
    public static class ServicesSerialization {
        private final Class<?> serviceClass;

        public ServicesSerialization(ServiceClassDescription serviceClass) {
            this.serviceClass = serviceClass.getServiceClass();
        }

        @Test
        public void checkService() {
            assertThat(serviceClass.getSimpleName() + " doesn't use objects with final fields",
                    getFinalFields(serviceClass), is(empty()));
        }

        @Parameterized.Parameters(name = "{0}")
        public static List<ServiceClassDescription> getServiceClasses() throws ClassNotFoundException {
            // use wrapper for control parameterized runner test names
            // overriding parameter's toString
            return SerializationTest.getServiceClasses().stream()
                    .map(ServiceClassDescription::new)
                    .collect(Collectors.toList());
        }

        private Set<Field> getFinalFields(Class<?> serviceClass) {
            Set<Class> usedClasses = Stream.of(serviceClass.getMethods())
                    .flatMap(m -> ReflectionUtils.getUsedClasses(m).stream())
                    .collect(Collectors.toSet());

            Set<Class> seen = new HashSet<>();
            Set<Field> wrongFields = new HashSet<>();
            for (Class usedClass : usedClasses) {
                wrongFields.addAll(getFinalFieldsRecursive(usedClass, seen));
            }

            return wrongFields;
        }

        private List<Field> getFinalFieldsRecursive(Class clazz, Set<Class> seen) {
            Set<Field> fields = ReflectionUtils.getAllFields(clazz);
            if (clazz.isPrimitive() || clazz.getCanonicalName().startsWith("java.")) {
                // don't check jre classes and primitives
                return Collections.emptyList();
            }

            List<Field> finalFields = new ArrayList<>();
            for (Field field : fields) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    // static final fields are good
                    continue;
                }
                if (Modifier.isTransient(modifiers)) {
                    // allow mark final fields as transient to skip checking
                    continue;
                }
                if (clazz.isEnum()) {
                    // final fields in enums are ok, they are initialize at start
                    continue;
                }
                if (Modifier.isFinal(modifiers)) {
                    finalFields.add(field);
                    continue;
                }

                // get field type and check all generic parameters same way
                // it's used to check fields like List<Some> and be sure that Some is Serializable well
                Set<Class> fieldClasses = ReflectionUtils.getGenericClasses(field.getGenericType());
                for (Class fieldClass : fieldClasses) {
                    if (seen.add(fieldClass)) {
                        finalFields.addAll(getFinalFieldsRecursive(fieldClass, seen));
                    }
                }
            }

            return finalFields;
        }

        // simple toString
        private static class ServiceClassDescription {
            private final Class<?> serviceClass;

            private ServiceClassDescription(Class<?> serviceClass) {
                this.serviceClass = serviceClass;
            }

            public Class<?> getServiceClass() {
                return serviceClass;
            }

            @Override
            public String toString() {
                return serviceClass.getSimpleName();
            }
        }
    }

    public static class ServiceScan {

        @Test
        public void checkAllServicesFound() throws ClassNotFoundException {
            // if you create new service make EXPECTED_SERVICES_COUNT be relevant
            assertThat("found all services", getServiceClasses(), hasSize(EXPECTED_SERVICES_COUNT));
        }

    }

    /**
     * Scan gwt services package for Remote interfaces.
     */
    private static List<Class<?>> scanForServices() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface();
            }
        };
        provider.addIncludeFilter(new AssignableTypeFilter(RemoteService.class));

        List<Class<?>> serviceClasses = new ArrayList<>();
        for (BeanDefinition definition : provider.findCandidateComponents("ru.yandex.market.mbo.gwt.client.services")) {
            String className = definition.getBeanClassName();
            Class<?> serviceClass = SerializationTest.class.getClassLoader().loadClass(className);
            serviceClasses.add(serviceClass);
        }
        return serviceClasses;
    }

    public static List<Class<?>> getServiceClasses() throws ClassNotFoundException {
        if (services == null) {
            synchronized (SerializationTest.class) {
                if (services != null) {
                    return services;
                }
                services = scanForServices();
            }
        }
        return services;
    }


}
