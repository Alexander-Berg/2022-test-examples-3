package ru.yandex.market.mbi.environment;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(locations = "classpath:ru/yandex/market/mbi/environment/test.xml")
class MultiEnvironmentServiceTest {
    private final EnvironmentService mockEnvironmentService = mock(EnvironmentService.class);
    private MultiEnvironmentService multiEnvironmentService;

    @Autowired
    AbstractBeanFactory beanFactory;

    @BeforeEach
    void setUp() {
        multiEnvironmentService = new MultiEnvironmentService(List.of(
                mockEnvironmentService,
                new ApplicationContextEnvironmentService(beanFactory)
        ));
    }

    @Test
    void testDefault() {
        String result = multiEnvironmentService.getValue("propName1", "defaultPropValue");
        assertThat(result).isEqualTo("defaultPropValue");
    }

    @Test
    void testValue() {
        String result = multiEnvironmentService.getValue("propName2", "defaultPropValue");
        assertThat(result).isEqualTo("propValue1");
    }

    @Test
    void testComposedValue() {
        String result = multiEnvironmentService.getValue("test1", "defaultPropValue");
        assertThat(result).isEqualTo("abcde");
    }

    @Test
    void testDbValue() {
        when(mockEnvironmentService.getValues(anyString(), anyList())).thenReturn(List.of("db1", "db2"));
        String result = multiEnvironmentService.getValue("propName2", "defaultPropValue");
        assertThat(result).isEqualTo("db1");
    }

    @Test
    void testDouble() {
        double testDouble = multiEnvironmentService.getDoubleValue("testDouble");
        assertThat(testDouble).isCloseTo(0.75D, offset(0.001D));
    }

    @Test
    void testDoubleDefault() {
        double testDouble = multiEnvironmentService.getDoubleValue("testDouble", 0.1D);
        assertThat(testDouble).isCloseTo(0.75D, offset(0.001D));
        testDouble = multiEnvironmentService.getDoubleValue("testDouble2", 0.6D);
        assertThat(testDouble).isCloseTo(10.0D, offset(0.001D));
        testDouble = multiEnvironmentService.getDoubleValue("testDouble3", 0.6D);
        assertThat(testDouble).isCloseTo(0.6D, offset(0.001D));
    }

    @Test
    void testDate() {
        Instant testDate = multiEnvironmentService.getInstant("testDate");
        assertThat(testDate).isEqualTo("2019-07-11T01:01:01Z");
    }

    @Test
    void removeAllValuesFound() {
        when(mockEnvironmentService.removeAllValues(anyString())).thenReturn(true);
        boolean removed = multiEnvironmentService.removeAllValues("testDouble");
        assertThat(removed).isTrue();
    }

    @Test
    void removeAllValuesNotFound() {
        when(mockEnvironmentService.removeAllValues(anyString())).thenReturn(false);
        boolean removed = multiEnvironmentService.removeAllValues("testDouble");
        assertThat(removed).isFalse();
    }
}
