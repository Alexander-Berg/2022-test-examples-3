package ru.yandex.market.javaframework.yamlproperties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import ru.yandex.market.application.properties.utils.Environments;
import ru.yandex.market.javaframework.internal.environment.test.EnvironmentExtension;
import ru.yandex.market.javaframework.internal.environment.test.TestEnvironment;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EnvironmentExtension.class)
public class PropertySourceEnvironmentResolverTest {

    @Test
    @TestEnvironment(Environments.TESTING)
    public void commonPropTest() {
        String prop = "test.property";
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            prop, val1,
            "test.env." + Environments.PRODUCTION + ".property", val2
        );
        final MapPropertySource propertySource = new MapPropertySource("test", propMap);

        assertEquals(val1, PropertySourceEnvironmentResolver.resolve(propertySource).getProperty(prop));
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void envPropOverridingTest() {
        String prop = "test.property";
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            prop, val1,
            "test.env." + Environments.PRODUCTION + ".property", val2
        );
        final MapPropertySource propertySource = new MapPropertySource("test", propMap);

        assertEquals(val2, PropertySourceEnvironmentResolver.resolve(propertySource).getProperty(prop));
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void multipleEnvPropOverridingTest() {
        String prop = "test.property";
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            prop, val1,
            "test.env." + Environments.PRODUCTION + ".property", val2,
            "test.env." + Environments.TESTING + ".property", "rtfhyy"
        );
        final MapPropertySource propertySource = new MapPropertySource("test", propMap);

        assertEquals(val2, PropertySourceEnvironmentResolver.resolve(propertySource).getProperty(prop));
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void envAtStartDoesNotSupportedTest() {
        String prop = "test.property";
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            prop, val1,
            "env." + Environments.PRODUCTION + ".test.property", val2
        );
        final MapPropertySource propertySource = new MapPropertySource("test", propMap);

        assertEquals(val1, PropertySourceEnvironmentResolver.resolve(propertySource).getProperty(prop));
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void envAtTheEndTest() {
        String prop = "test.property";
        final String envProp = "test.property.env." + Environments.PRODUCTION;
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            prop, val1,
            envProp, val2
        );
        final MapPropertySource propertySource = new MapPropertySource("test", propMap);

        final PropertySource<?> resolved = PropertySourceEnvironmentResolver.resolve(propertySource);
        assertEquals(val1, resolved.getProperty(prop));
        assertEquals(val2, resolved.getProperty(envProp));
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void targetEnvPropOverridingTest() {
        String prop = "test.property";
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            prop, val1,
            "test.env." + Environments.PRODUCTION + ".property", "fsfsdht",
            "test.env." + Environments.TESTING + ".property", val2
        );
        final MapPropertySource propertySource = new MapPropertySource("test", propMap);

        assertEquals(val2,
            PropertySourceEnvironmentResolver.resolve(propertySource, Environments.TESTING).getProperty(prop));
    }

    @Test
    @TestEnvironment(Environments.TESTING)
    public void commonListPropTest() {
        String prop = "test.property";
        final List<String> list = List.of("elem1", "elem2");
        final List<String> envList = List.of("elem3", "elem4");
        final Map<String, Object> propMap = Map.of(
            prop+"[0]", list.get(0),
            prop+"[1]", list.get(1),
            "test.env." + Environments.PRODUCTION + ".property[0]", envList.get(0),
            "test.env." + Environments.PRODUCTION + ".property[1]", envList.get(1)
        );
        final MapPropertySource propertySource = new MapPropertySource("test", propMap);
        final PropertySource<?> resolvedPropertySource = PropertySourceEnvironmentResolver.resolve(propertySource);
        final Binder binder = new Binder(ConfigurationPropertySources.from(resolvedPropertySource));
        final Set<?> resolvedSet = binder.bind(prop, Set.class).get();
        assertEquals(new HashSet<>(list), resolvedSet);
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void envListPropOverridingTest() {
        String prop = "test.property";
        final List<String> list = List.of("elem1", "elem2");
        final List<String> envList = List.of("elem3", "elem4");
        final Map<String, Object> propMap = Map.of(
            prop+"[0]", list.get(0),
            prop+"[1]", list.get(1),
            "test.env." + Environments.PRODUCTION + ".property[0]", envList.get(0),
            "test.env." + Environments.PRODUCTION + ".property[1]", envList.get(1)
        );
        final MapPropertySource propertySource = new MapPropertySource("test", propMap);
        final PropertySource<?> resolvedPropertySource = PropertySourceEnvironmentResolver.resolve(propertySource);
        final Binder binder = new Binder(ConfigurationPropertySources.from(resolvedPropertySource));
        final Set<?> resolvedSet = binder.bind(prop, Set.class).get();
        assertEquals(new HashSet<>(envList), resolvedSet);
    }

    @Test
    @TestEnvironment(Environments.PRODUCTION)
    public void envPropDashSeparatedTest() {
        String prop = "test.service-property";
        String val1 = "qwerty";
        String val2 = "asdfg";
        final Map<String, Object> propMap = Map.of(
            prop, val1,
            "test.env." + Environments.PRODUCTION + ".service-property", val2
        );
        final MapPropertySource propertySource = new MapPropertySource("test", propMap);

        assertEquals(val2, PropertySourceEnvironmentResolver.resolve(propertySource).getProperty(prop));
    }
}
