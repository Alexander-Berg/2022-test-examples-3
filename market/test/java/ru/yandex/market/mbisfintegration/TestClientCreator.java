package ru.yandex.market.mbisfintegration;

import java.util.function.Function;

import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;

import ru.yandex.market.javaframework.clients.client.ApiClient;
import ru.yandex.market.javaframework.clients.client.ApiClientFactory;
import ru.yandex.market.javaframework.clients.client.ApiClientSettings;
import ru.yandex.mj.generated.client.self_client.api.AccountApiClient;
import ru.yandex.mj.generated.client.self_client.api.LeadApiClient;
import ru.yandex.mj.generated.client.self_client.api.SearchApiClient;

/**
 * Затычка, чтобы генерировать тестовые клиенты ручек
 * (пока не выкатится https://st.yandex-team.ru/MARKETDX-634)
 */
public class TestClientCreator implements ApplicationListener<ServletWebServerInitializedEvent> {

    private final ApiClientFactory apiClientFactory;

    public TestClientCreator(ApiClientFactory apiClientFactory) {
        this.apiClientFactory = apiClientFactory;
    }

    @Override
    public void onApplicationEvent(final ServletWebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        var context = event.getApplicationContext();
        replaceBean(context, LeadApiClient.class, LeadApiClient::new, port);
        replaceBean(context, SearchApiClient.class, SearchApiClient::new, port);
        replaceBean(context, AccountApiClient.class, AccountApiClient::new, port);
    }

    private <T> void replaceBean(
            GenericApplicationContext context,
            Class<T> clientClass,
            Function<ApiClient, T> constructor,
            int port
    ) {
        String[] beansForRemove = context.getBeanNamesForType(clientClass);
        for (String beanName : beansForRemove) {
            context.removeBeanDefinition(beanName);
        }
        context.registerBean(
                "get" + clientClass.getSimpleName(),
                clientClass,
                () -> constructor.apply(
                        apiClientFactory.apply(
                                ApiClientSettings.forClient("self_client")
                                        .setBaseUrl("http://localhost:" + port)
                                        .build()
                        ))
        );
    }

}