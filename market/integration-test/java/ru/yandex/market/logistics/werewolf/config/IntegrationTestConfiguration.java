package ru.yandex.market.logistics.werewolf.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import ru.yandex.market.common.ping.PingChecker;
import ru.yandex.market.logistics.werewolf.util.ExternalProcessBuilder;
import ru.yandex.market.logistics.werewolf.util.HtmlToPdfConverter;
import ru.yandex.market.logistics.werewolf.util.ResponseWriterFactory;
import ru.yandex.passport.tvmauth.TvmClient;

@Configuration
@MockBean({
    TvmClient.class,
    ExternalProcessBuilder.class,
    PingChecker.class,
})
@SpyBean({
    HtmlToPdfConverter.class,
    ResponseWriterFactory.class,
})
@Import({
    JacksonConfiguration.class,
    TemplateEngineConfig.class,
    WebMvcConfiguration.class,
    ResponseWriterConfiguration.class,
})
@EnableWebMvc
@PropertySource({
    "classpath:application.properties",
    "classpath:application-template-engine.properties",
})
public class IntegrationTestConfiguration {
    @Autowired
    protected ExternalProcessBuilder wkhtmltopdfProcess;

    @Bean
    @Primary
    @ConfigurationProperties("response")
    public ResponseWriterConfiguration.ResponseConfiguration responseConfigurationTest() {
        return new ResponseWriterConfiguration.ResponseConfiguration();
    }

    @ConfigurationProperties("pdf-converter")
    @Primary
    @Bean
    public PdfConverterConfiguration.WktohtmlConfig wktohtmlConfigTest() {
        return new PdfConverterConfiguration.WktohtmlConfig();
    }
}
