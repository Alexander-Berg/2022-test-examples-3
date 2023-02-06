package ru.yandex.market.jmf.module.http.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.jmf.module.secret.ModuleSecretTestConfiguration;
import ru.yandex.market.jmf.tvm.support.test.TvmSupportTestConfiguration;
import ru.yandex.market.jmf.utils.AbstractModuleConfiguration;

import static org.mockito.Mockito.mock;
import static ru.yandex.market.jmf.module.http.support.ModuleHttpSupportConfiguration.HTTP_SUPPORT_REST_TEMPLATE;
import static ru.yandex.market.jmf.module.http.support.ModuleHttpSupportConfiguration.HTTP_SUPPORT_REST_TEMPLATE_WITH_PROCESS_REDIRECT;

@Configuration
@Import({
        ModuleHttpSupportConfiguration.class,
        TvmSupportTestConfiguration.class,
        ModuleSecretTestConfiguration.class,
})
public class ModuleHttpSupportTestConfiguration extends AbstractModuleConfiguration {

    protected ModuleHttpSupportTestConfiguration() {
        super("test/jmf/module/http/support");
    }


    @Bean(HTTP_SUPPORT_REST_TEMPLATE)
    public RestTemplate mockRestTemplate() {

        return mock(RestTemplate.class);
    }

    @Bean(HTTP_SUPPORT_REST_TEMPLATE_WITH_PROCESS_REDIRECT)
    public RestTemplate mockRestTemplateWithProcessRedirect() {
        return mock(RestTemplate.class);
    }
}
