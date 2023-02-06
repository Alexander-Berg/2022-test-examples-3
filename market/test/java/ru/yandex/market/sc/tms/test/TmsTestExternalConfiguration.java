package ru.yandex.market.sc.tms.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistic.api.utils.UniqService;
import ru.yandex.market.tpl.common.logbroker.config.LogbrokerTestExternalConfig;
import ru.yandex.startrek.client.Session;

import static org.mockito.Mockito.mock;

/**
 * @author valter
 */
@Configuration
@Import({
        LogbrokerTestExternalConfig.class
})
public class TmsTestExternalConfiguration {

    private static final int HASH_LEN = 32;

    @Bean
    Session trackerSession() {
        return mock(Session.class);
    }

    @Bean
    XmlMapper xmlMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return xmlMapper;
    }

    @Bean
    public UniqService uniqService() {
        return () -> RandomStringUtils.randomAlphanumeric(HASH_LEN);
    }

}
