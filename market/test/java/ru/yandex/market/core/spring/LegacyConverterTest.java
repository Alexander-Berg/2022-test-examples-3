package ru.yandex.market.core.spring;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import ru.yandex.common.framework.core.ServantInfo;
import ru.yandex.market.common.test.SerializationChecker;
import ru.yandex.market.common.test.spring.MVCSerializationTest;
import ru.yandex.market.core.xml.impl.HybridMarshaller;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@SpringJUnitConfig(LegacyConverterTest.SpringConfig.class)
class LegacyConverterTest extends MVCSerializationTest {

    private HttpMessageConverter xml;
    private SerializationChecker checker;

    @Autowired
    private HybridMarshaller marshaller;

    @BeforeEach
    void setUp() {
        xml = new LegacyXmlHttpMessageConverter(marshaller);
        checker = new SerializationChecker(
                obj -> "{}",
                null,
                obj -> out(xml, obj),
                null
        );
    }

    @Test
    void testReportLegacySerialize() throws JSONException {
        checker.testSerialization(
                "test",
                "{}",
                "<data servant='test' version='0' host='localhost'><string>test</string></data>"
        );

    }

    @Configuration
    @ImportResource("classpath:common/common-marshalling.xml")
    public static class SpringConfig {

        @Bean
        public ServantInfo servantInfo() {
            return new ServantInfo() {
                @Override
                public String getName() {
                    return "test";
                }

                @Override
                public String getVersion() {
                    return "0";
                }

                @Override
                public String getHostName() {
                    return "localhost";
                }
            };
        }
    }

}
