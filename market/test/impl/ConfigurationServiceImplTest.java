package ru.yandex.market.jmf.configuration.test.impl;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.configuration.api.Property;
import ru.yandex.market.jmf.configuration.api.PropertyTypes;
import ru.yandex.market.jmf.configuration.impl.ConfigurationServiceImpl;

@Transactional
@SpringJUnitConfig(classes = InternalConfigurationTestConfiguration.class)
@TestPropertySource(value = "classpath:/configuration-example.properties")
public class ConfigurationServiceImplTest {

    private static final String INTEGER_PROPERTY_NAME = "integerProperty";
    private static final String DECIMAL_PROPERTY_NAME = "decimalProperty";
    private static final String STRING_PROPERTY_NAME = "stringProperty";
    private static final String BOOLEAN_PROPERTY_NAME = "booleanProperty";

    private static final Property<Boolean> BOOLEAN_PROPERTY = Property.of(BOOLEAN_PROPERTY_NAME, PropertyTypes.BOOLEAN);
    private static final Property<Long> INTEGER_PROPERTY = Property.of(INTEGER_PROPERTY_NAME, PropertyTypes.INTEGER);
    private static final Property<BigDecimal> DECIMAL_PROPERTY = Property.of(DECIMAL_PROPERTY_NAME,
            PropertyTypes.DECIMAL);
    private static final Property<String> STRING_PROPERTY = Property.of(STRING_PROPERTY_NAME, PropertyTypes.STRING);
    private static final Property<List<String>> LIST_STRING_PROPERTY = Property.of(STRING_PROPERTY_NAME,
            PropertyTypes.LIST_OF_STRINGS);
    private static final Property<List<Long>> LIST_INTEGERS_PROPERTY = Property.of(STRING_PROPERTY_NAME,
            PropertyTypes.LIST_OF_INTEGERS);

    @Inject
    ConfigurationServiceImpl configurationService;

    @Test
    public void saveIntegerPropertyValue() {
        long initialValue = Randoms.longValue();

        // вызов системы
        configurationService.setValue(INTEGER_PROPERTY_NAME, initialValue);
        long result = configurationService.getValue(INTEGER_PROPERTY_NAME);

        // проверка утверждений
        Assertions.assertEquals(initialValue, result);
    }

    @Test
    public void saveNull() {
        // вызов системы
        configurationService.setValue(INTEGER_PROPERTY_NAME, null);
        Long result = configurationService.getValue(INTEGER_PROPERTY_NAME);

        // проверка утверждений
        Assertions.assertNull(result);
    }

    @Test
    public void getIntegerProperty() {
        Long initialValue = Randoms.longValue();
        configurationService.setValue(INTEGER_PROPERTY_NAME, initialValue);

        // вызов системы
        Long result = configurationService.getValue(INTEGER_PROPERTY);

        // проверка утверждений
        Assertions.assertEquals(initialValue, result);
    }

    @Test
    public void getIntegerProperty_null() {
        configurationService.setValue(INTEGER_PROPERTY_NAME, null);

        // вызов системы
        Long result = configurationService.getValue(INTEGER_PROPERTY);

        // проверка утверждений
        Assertions.assertNull(result);
    }

    @Test
    public void saveDecimalPropertyValue() {
        BigDecimal initialValue = Randoms.bigDecimal();

        // вызов системы
        configurationService.setValue(DECIMAL_PROPERTY_NAME, initialValue);
        BigDecimal result = configurationService.getValue(DECIMAL_PROPERTY_NAME);

        // проверка утверждений
        Assertions.assertEquals(initialValue, result);
    }

    @Test
    public void saveNullDecimalPropertyValue() {
        // вызов системы
        configurationService.setValue(DECIMAL_PROPERTY_NAME, null);
        BigDecimal result = configurationService.getValue(DECIMAL_PROPERTY_NAME);

        // проверка утверждений
        Assertions.assertNull(result);
    }

    @Test
    public void getDecimalProperty() {
        BigDecimal initialValue = Randoms.bigDecimal();
        configurationService.setValue(DECIMAL_PROPERTY_NAME, initialValue);

        // вызов системы
        BigDecimal result = configurationService.getValue(DECIMAL_PROPERTY);

        // проверка утверждений
        Assertions.assertEquals(initialValue, result);
    }

    @Test
    public void getDecimalProperty_null() {
        configurationService.setValue(DECIMAL_PROPERTY_NAME, null);

        // вызов системы
        BigDecimal result = configurationService.getValue(DECIMAL_PROPERTY);

        // проверка утверждений
        Assertions.assertNull(result);
    }

    @Test
    public void getStringProperty() {
        String initialValue = Randoms.string();
        configurationService.setValue(STRING_PROPERTY_NAME, initialValue);

        // вызов системы
        String result = configurationService.getValue(STRING_PROPERTY);

        // проверка утверждений
        Assertions.assertEquals(initialValue, result);
    }

    @Test
    public void getBooleanProperty_null() {
        configurationService.setValue(BOOLEAN_PROPERTY_NAME, null);

        // вызов системы
        Boolean result = configurationService.getValue(BOOLEAN_PROPERTY);

        // проверка утверждений
        Assertions.assertFalse(result);
    }

    @Test
    public void getBooleanProperty_true() {
        configurationService.setValue(BOOLEAN_PROPERTY_NAME, true);

        // вызов системы
        Boolean result = configurationService.getValue(BOOLEAN_PROPERTY);

        // проверка утверждений
        Assertions.assertTrue(result);
    }

    @Test
    public void getBooleanProperty_false() {
        configurationService.setValue(BOOLEAN_PROPERTY_NAME, false);

        // вызов системы
        Boolean result = configurationService.getValue(BOOLEAN_PROPERTY);

        // проверка утверждений
        Assertions.assertFalse(result);
    }

    @Test
    public void getListOfStringsProperty() {
        String initialValue = "a,b,,c";
        configurationService.setValue(STRING_PROPERTY_NAME, initialValue);

        // вызов системы
        List<String> result = configurationService.getValue(LIST_STRING_PROPERTY);

        // проверка утверждений
        Assertions.assertEquals(List.of("a", "b", "c"), result);
    }

    @Test
    public void getListOfStringsProperty_null() {
        configurationService.setValue(STRING_PROPERTY_NAME, null);

        // вызов системы
        List<String> result = configurationService.getValue(LIST_STRING_PROPERTY);

        // проверка утверждений
        Assertions.assertEquals(List.of(), result);
    }

    @Test
    public void getListOfIntegersProperty() {
        String initialValue = "3,5,,11";
        configurationService.setValue(STRING_PROPERTY_NAME, initialValue);

        // вызов системы
        List<Long> result = configurationService.getValue(LIST_INTEGERS_PROPERTY);

        // проверка утверждений
        Assertions.assertEquals(List.of(3L, 5L, 11L), result);
    }

    @Test
    public void getListOfIntegersProperty_null() {
        configurationService.setValue(STRING_PROPERTY_NAME, null);

        // вызов системы
        List<Long> result = configurationService.getValue(LIST_INTEGERS_PROPERTY);

        // проверка утверждений
        Assertions.assertEquals(List.of(), result);
    }

    @Test
    public void getCachedProperty() throws Exception {
        Long initialValue = Randoms.longValue();
        configurationService.setValue(INTEGER_PROPERTY_NAME, initialValue);

        // Получаем значение первый раз. В кеше оно еще отсутствует и должны получить актуальное значение и
        // закешировать его
        Long result = configurationService.getCached(INTEGER_PROPERTY_NAME, Duration.ofMillis(50));
        Assertions.assertEquals(initialValue, result);

        // Устанавливаем новое значение конфигурации
        Long newValue = Randoms.longValue();
        configurationService.setValue(INTEGER_PROPERTY_NAME, newValue);

        // Получаем значение второй раз. Значение закешировано первым вызовом и оно еще не протухло.
        Long result1 = configurationService.getCached(INTEGER_PROPERTY_NAME, Duration.ofMillis(50));
        Assertions.assertEquals(initialValue, result1);

        Thread.sleep(55);

        // Получаем значение после задержки. Значение закешировано, но оно уже протухло.
        Long result2 = configurationService.getCached(INTEGER_PROPERTY_NAME, Duration.ofMillis(50));
        Assertions.assertEquals(newValue, result2);
    }
}
