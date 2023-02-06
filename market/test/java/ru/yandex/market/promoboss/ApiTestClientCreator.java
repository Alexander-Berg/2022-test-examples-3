package ru.yandex.market.promoboss;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

import ru.yandex.market.javaframework.clients.client.ApiClient;
import ru.yandex.market.javaframework.clients.client.ApiClientFactory;
import ru.yandex.market.javaframework.clients.client.ApiClientSettings;
import ru.yandex.market.javaframework.clients.retry.MJAbstractRetrofitService;
import ru.yandex.market.request.trace.Module;
import ru.yandex.mj.generated.client.self_client.api.CreatePromoApiClient;
import ru.yandex.mj.generated.client.self_client.api.GetPromoApiClient;
import ru.yandex.mj.generated.client.self_client.api.UpdatePromoApiClient;

/**
 * This class is needed to recreate ApiClients during run SpringBoot Web tests on the random http port
 * Can be deleted after completion of <a href=https://st.yandex-team.ru/MARKETDX-634>MARKETDX-634</a>
 */
@Service
public class ApiTestClientCreator implements ApplicationListener<ServletWebServerInitializedEvent> {

    private static final Set<Class<? extends MJAbstractRetrofitService<Object>>> CLIENT_CLASSES = Set.of(
            GetPromoApiClient.class,
            CreatePromoApiClient.class,
            UpdatePromoApiClient.class);

    @Autowired
    private Module traceModule;

    @Autowired
    private ApiClientFactory apiClientFactory;

    @SneakyThrows
    @Override
    public void onApplicationEvent(ServletWebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        ServletWebServerApplicationContext applicationContext = event.getApplicationContext();

        for (Class<? extends MJAbstractRetrofitService<Object>> aClass : CLIENT_CLASSES) {
            recreateApiClient(port, applicationContext, aClass);
        }
    }

    public <T extends MJAbstractRetrofitService<Object>> void recreateApiClient(
            int port,
            ServletWebServerApplicationContext applicationContext,
            Class<T> aClass) throws InvocationTargetException, InstantiationException, IllegalAccessException {

        String[] beanNamesForType = applicationContext.getBeanNamesForType(aClass);

        for (String beanName : beanNamesForType) {
            applicationContext.removeBeanDefinition(beanName);
        }

        Constructor<T> clientConstructor = ConstructorUtils.getMatchingAccessibleConstructor(aClass, ApiClient.class);

        T client = clientConstructor
                .newInstance(apiClientFactory.apply(
                        ApiClientSettings
                                .forClient("self_client")
                                .setBaseUrl("http://localhost:" + port)
                                .setSourceTraceModule(traceModule)
                                .setTargetTraceModule(traceModule)
                                .build()));

        applicationContext.registerBean(decapitalizeClassName(aClass), aClass, () -> client);
    }

    private static String decapitalizeClassName(Class<?> aClass) {
        char[] chars = aClass.getSimpleName().toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }
}
