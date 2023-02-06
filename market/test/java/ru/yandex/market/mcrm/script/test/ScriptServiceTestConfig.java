package ru.yandex.market.mcrm.script.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mcrm.script.ScriptServiceConfiguration;

/**
 * @author apershukov
 */
@Configuration
@Import(ScriptServiceConfiguration.class)
public class ScriptServiceTestConfig {

    @Bean
    public ObjectMapper jsonObjectMapper() {
        return new ObjectMapper();
    }
}
