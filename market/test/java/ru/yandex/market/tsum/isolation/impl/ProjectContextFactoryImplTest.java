package ru.yandex.market.tsum.isolation.impl;

import java.util.Collections;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.MapPropertySource;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.notifications.TelegramChatProvider;
import ru.yandex.market.tsum.clients.notifications.telegram.TelegramClient;
import ru.yandex.market.tsum.pipe.engine.isolation.exceptions.PlaceholdersResolveException;
import ru.yandex.market.tsum.pipe.engine.isolation.model.SecretVersion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 03/12/2018
 */
public class ProjectContextFactoryImplTest {
    @Test
    public void createsContext() {
        HashMap<String, Object> jobsProperties = new HashMap<>();
        jobsProperties.put("tsum.telegram.api.url", "https://api.telegram.org");

        HashMap<String, String> vaultProperties = new HashMap<>();
        vaultProperties.put("tsum.telegram-bot.token", "<token>");

        ProjectContextFactoryImpl factory = new ProjectContextFactoryImpl(
            new NettyHttpClientContext(new HttpClientConfig()),
            Collections.singletonList(new MapPropertySource("jobs", jobsProperties)),
            mock(TelegramChatProvider.class));

        SecretVersion secretVersion = mock(SecretVersion.class);
        when(secretVersion.getValues()).thenReturn(vaultProperties);

        ApplicationContext context = factory.create(secretVersion);

        Assert.assertNotNull(context.getBean(TelegramClient.class));
    }

    @Test
    public void failsOnMissingProperty() {
        ProjectContextFactoryImpl factory = new ProjectContextFactoryImpl(
            new NettyHttpClientContext(new HttpClientConfig()), Collections.emptyList(),
            null);

        SecretVersion secretVersion = mock(SecretVersion.class);
        when(secretVersion.getValues()).thenReturn(new HashMap<>());

        ApplicationContext context = factory.create(secretVersion);
        try {
            context.getBean(TelegramClient.class);
        } catch (BeanCreationException exception) {
            Assert.assertEquals(PlaceholdersResolveException.class, exception.getRootCause().getClass());
            return;
        }

        Assert.fail("BeanCreationException not thrown");
    }
}
