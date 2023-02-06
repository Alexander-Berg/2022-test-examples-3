package ru.yandex.market.tsum.tms.isolation;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.ResourcePropertySource;
import ru.yandex.passport.tvmauth.TvmClient;
import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.notifications.TelegramChatProvider;
import ru.yandex.market.tsum.config.ClientExtensionsConfig;
import ru.yandex.market.tsum.config.JobIsolationConfig;
import ru.yandex.market.tsum.isolation.impl.ProjectContextFactoryImpl;
import ru.yandex.market.tsum.pipe.engine.isolation.model.SecretVersion;
import ru.yandex.market.tsum.tms.isolation.model.NamedBean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 04/12/2018
 */
public class JobServicesIsolationTest {

    private List<Class<?>> ignore = Collections.singletonList(TvmClient.class);

    @Test
    public void autowireAllServices() throws IOException {
        NettyHttpClientContext nettyContext = new NettyHttpClientContext(new HttpClientConfig());

        ArrayList<PropertySource> sources = new ArrayList<>(JobIsolationConfig.jobsPropertySources());
        sources.add(new ResourcePropertySource("classpath:/vault-test-secrets.properties"));

        ProjectContextFactoryImpl factory = new ProjectContextFactoryImpl(nettyContext, sources, telegramChatProvider());

        ApplicationContext context = factory.create(SecretVersion.builder().withVersion("").build());

        Set<NamedBean> declaredServices = IsolationHelper.loadBeansHierarchy(ClientExtensionsConfig.class)
            .stream().filter(bean -> !ignore.contains(bean.getClazz()))
            .collect(Collectors.toSet());

        Set<String> errors = new HashSet<>();
        declaredServices.forEach(requiredBean -> {
            try {
                context.getBean(requiredBean.getName(), requiredBean.getClazz());
            } catch (BeanCreationException e) {
                errors.add(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
        });

        Assert.assertTrue(errors.stream().sorted().collect(Collectors.joining("\n")), errors.isEmpty());
    }

    private static TelegramChatProvider telegramChatProvider() {
        return new TelegramChatProvider() {
            @Override
            public String getChatIdByStaffName(String name) {
                return null;
            }

            @Override
            public String getChatIdByTelegramUserName(String name) {
                return null;
            }
        };
    }
}
