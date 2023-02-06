package ru.yandex.market.tsum.tms.resource;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.annotation.PersistenceConstructor;

import ru.yandex.common.util.StringUtils;
import ru.yandex.market.tsum.pipe.engine.definition.resources.Resource;
import ru.yandex.market.tsum.pipe.engine.source_code.SourceCodeService;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.ReflectionsSourceCodeProvider;
import ru.yandex.market.tsum.pipe.engine.source_code.impl.SourceCodeServiceImpl;
import ru.yandex.market.tsum.pipe.engine.source_code.model.ResourceObject;
import ru.yandex.market.tsum.pipe.engine.source_code.model.SourceCodeObject;
import ru.yandex.market.tsum.pipe.engine.source_code.model.forms.FieldControlType;
import ru.yandex.market.tsum.pipe.engine.source_code.model.forms.IncorrectResourceFieldException;
import ru.yandex.misc.test.Assert;

/**
 * For more information about this test visit:
 * https://wiki.yandex-team.ru/market/development/infra/tsum/relizy/resource/#trebovanijakresursu
 *
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 06/12/2018
 */
public class ResourceTest {

    public static final String GETTERS_CHECK_MESSAGE =
        "Getters does not match fields in %s\n" +
            "If you have transient method, rename it to calculate<Name> or add @JsonIgnore.\n" +
            "If you have transient field, mark it transient, @JsonIgnore and @IgnoreResourceField.\n" +
            "For more info visit: " +
            "https://wiki.yandex-team.ru/market/development/infra/tsum/relizy/resource/#trebovanijakresursu\n";

    // If you have problems with FAIL_ON_EMPTY_BEANS, you should add getters to your resource.
    private static ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
        .enable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private SourceCodeService sourceCodeService;

    @Before
    public void setup() {
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        try {
            sourceCodeService = new SourceCodeServiceImpl(
                new ReflectionsSourceCodeProvider(ReflectionsSourceCodeProvider.SOURCE_CODE_PACKAGE)
            );
        } catch (IncorrectResourceFieldException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void resourceValidationTest() {
        List<Class<? extends Resource>> resources = getResources().stream()
            .map(SourceCodeObject::getClazz)
            .collect(Collectors.toList());

        resources.forEach(
            resource -> Assert.assertTrue(
                "Resource can not be abstract " + resource,
                !Modifier.isAbstract(resource.getModifiers()))
        );

        Set<String> instantiationErrors = new HashSet<>();
        Set<String> serializationErrors = new HashSet<>();
        Set<String> noEmptyConstructor = new HashSet<>();
        Set<String> finalFieldsWithEmptyConstructor = new HashSet<>();

        classes:
        for (Class<? extends Resource> resource : resources) {
            boolean hasFinalFields = Arrays.stream(resource.getDeclaredFields())
                .anyMatch(f -> !Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()));

            List<Constructor<?>> constructors = Arrays.stream(resource.getDeclaredConstructors())
                .filter(
                    c -> c.getParameterCount() == 0 ||
                        c.getAnnotation(PersistenceConstructor.class) != null &&
                            c.getAnnotation(JsonCreator.class) != null
                )
                .collect(Collectors.toList());

            if (constructors.isEmpty()) {
                noEmptyConstructor.add(resource.getName());
                continue;
            }

            if (hasFinalFields) {
                if (Arrays.stream(resource.getDeclaredConstructors()).anyMatch(c -> c.getParameterCount() == 0)) {
                    finalFieldsWithEmptyConstructor.add(resource.getName());
                    continue;
                }
            }

            for (Constructor<?> constructor : constructors) {
                constructor.setAccessible(true);
                Resource instance;

                try {
                    instance =
                        (Resource) constructor.newInstance(
                            IntStream.range(0, constructor.getParameterCount())
                                .mapToObj(i -> null)
                                .toArray()
                        );
                } catch (
                    InstantiationException | IllegalAccessException | InvocationTargetException | RuntimeException e
                ) {
                    instantiationErrors.add(String.format(
                        "%s using %s: %s", resource.getName(), constructor.toString(), e));
                    continue classes;
                }

                try {
                    mapper.writeValueAsString(instance);
                } catch (JsonProcessingException e) {
                    serializationErrors.add("Can not serialize resource: " + e.getMessage());
                }

                String getters = Arrays.stream(resource.getMethods())
                    .filter(
                        method -> !method.getDeclaringClass().equals(Object.class) &&
                            !Modifier.isStatic(method.getModifiers()) &&
                            (method.getName().startsWith("get") || method.getName().startsWith("is")) &&
                            !method.getName().equals("getSourceCodeId") &&
                            method.getAnnotation(JsonIgnore.class) == null
                    )
                    .map(Method::getName)
                    .sorted()
                    .collect(Collectors.joining("\n"));

                String fields = sourceCodeService.getResource(resource).getFields()
                    .stream()
                    .map(field -> {
                        String name = field.getFieldName().substring(0, 1).toUpperCase() + field.getFieldName().substring(1);
                        String getter = "get" + name;
                        if (!field.getControl().getType().equals(FieldControlType.CHECKBOX) ||
                            StringUtils.matchesPattern(getters, createMatchingPattern(getter))) {
                            return getter;
                        } else {
                            return "is" + name;
                        }

                    })
                    .sorted()
                    .collect(Collectors.joining("\n"));

                Assert.equals(fields, getters, String.format(GETTERS_CHECK_MESSAGE, resource));

                Assert.notNull(instance.getSourceCodeId(), "Resource has not source code id " + resource.getName());

                Assert.equals(
                    instance.getSourceCodeId(),
                    instance.getSourceCodeId(),
                    String.format("Not persistent job id, class: %s", resource.getName())
                );
            }
        }

        Assert.assertTrue(
            String.format("Failed to instantiate resources:\n%s", String.join("\n", instantiationErrors)),
            instantiationErrors.isEmpty()
        );

        Assert.assertTrue(
            String.format(
                "Resource must have empty constructor or constructor with @JsonCreator and " +
                    "@PersistenceConstructor annotations, invalid " +
                    "resources:\n%s",
                String.join("\n", noEmptyConstructor)
            ),
            noEmptyConstructor.isEmpty()
        );

        Assert.assertTrue(
            String.format(
                "Resource can not have empty constructor and final fields. " +
                    "Invalid resources:\n%s",
                String.join("\n", finalFieldsWithEmptyConstructor)
            ),
            finalFieldsWithEmptyConstructor.isEmpty()
        );

        Assert.assertTrue(
            String.format("Failed to serialize resources:\n%s", String.join("\n", serializationErrors)),
            serializationErrors.isEmpty()
        );
    }

    private List<ResourceObject> getResources() {
        return sourceCodeService.getResources().stream()
            .sorted(Comparator.comparing(SourceCodeObject::getId))
            .collect(Collectors.toList());
    }

    private Pattern createMatchingPattern(String getter) {
        return Pattern.compile(String.format(".*^%s$.*", getter), Pattern.MULTILINE | Pattern.DOTALL);
    }
}
