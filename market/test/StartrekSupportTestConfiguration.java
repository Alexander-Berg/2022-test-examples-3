package ru.yandex.market.jmf.startrek.support.test;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import ru.yandex.market.jmf.script.test.ScriptSupportTestConfiguration;
import ru.yandex.market.jmf.startrek.support.StartrekService;
import ru.yandex.market.jmf.startrek.support.StartrekSupportConfiguration;

@Configuration
@PropertySource(
        name = "testMockStartrekProperties",
        value = "classpath:/test/startrek/support/test_startrek.properties"
)
@Import({
        StartrekSupportConfiguration.class,
        ScriptSupportTestConfiguration.class
})
public class StartrekSupportTestConfiguration {
    @Bean
    public StartrekService startrekServiceMock() {
        return Mockito.mock(StartrekService.class);
    }
}
