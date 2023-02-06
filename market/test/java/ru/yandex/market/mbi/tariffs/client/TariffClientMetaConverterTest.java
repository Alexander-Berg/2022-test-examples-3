package ru.yandex.market.mbi.tariffs.client;

import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;

import static org.junit.Assert.assertNotNull;

/**
 * Тесты для {@link TariffClientMetaConverter}
 */
@ParametersAreNonnullByDefault
class TariffClientMetaConverterTest {
    private TariffClientMetaConverter tariffClientMetaConverter;

    @BeforeEach
    void setUp() {
        tariffClientMetaConverter = new TariffClientMetaConverter(new ObjectMapper()
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
        );
    }

    @Test
    void testAllServiceTypesHaveConverterClass() {
        for (ServiceTypeEnum serviceType : ServiceTypeEnum.values()) {
            Class<?> metaClass = tariffClientMetaConverter.getMetaClass(serviceType);
            assertNotNull("ServiceType [" + serviceType + "] doesn't have meta json class", metaClass);
        }
    }
}
