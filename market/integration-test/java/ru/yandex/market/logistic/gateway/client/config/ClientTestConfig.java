package ru.yandex.market.logistic.gateway.client.config;

import javax.annotation.Nonnull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.logistics.util.client.ExternalServiceProperties;
import ru.yandex.market.logistics.util.client.HttpTemplate;
import ru.yandex.market.logistics.util.client.HttpTemplateBuilder;
import ru.yandex.market.logistics.util.client.StatelessTvmTicketProvider;
import ru.yandex.market.request.trace.Module;

@Configuration
public class ClientTestConfig {

    public static final String TVM_SERVICE_TICKET = "3:serv:CPEXEMubptUFIgYI8gEQ4AE";
    public static final String TVM_USER_TICKET = "3:user:COsXEOzPo9UFGhwKBAjhpgQQ4aYEGgtiYjpwYXNzd29yZCDyASgB";
    @Value("${lgw.api.host}")
    private String lgwUrl;
    @Value("${lgw.tvm.id}")
    private Integer tvmId;

    @Bean
    public HttpTemplate httpTemplate() {
        ExternalServiceProperties lgwProperties = new ExternalServiceProperties();
        lgwProperties.setUrl(lgwUrl);
        lgwProperties.setTvmServiceId(tvmId);
        return HttpTemplateBuilder.create(lgwProperties, Module.LOGISTIC_GATEWAY)
            .withTicketProvider(new StatelessTvmTicketProvider() {

                @Override
                public String provideServiceTicket(@Nonnull Integer tvmServiceId) {
                    return TVM_SERVICE_TICKET;
                }

                @Override
                public String provideUserTicket(Integer tvmServiceId) {
                    return TVM_USER_TICKET;
                }
            })
            .build();
    }
}
