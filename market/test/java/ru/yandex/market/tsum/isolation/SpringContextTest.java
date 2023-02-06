package ru.yandex.market.tsum.isolation;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.MapPropertySource;

import ru.yandex.market.request.netty.HttpClientConfig;
import ru.yandex.market.request.netty.NettyHttpClientContext;
import ru.yandex.market.tsum.clients.arcadia.review.ArcadiaReviewsClient;

/**
 * This test is just a proof of concept test bed, so we can ignore them.
 *
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 28/11/2018
 */
@Ignore
public class SpringContextTest {
    public static class ClientTestConfig {
        @Lazy
        @Bean
        public NettyHttpClientContext context() {
            return new NettyHttpClientContext(new HttpClientConfig());
        }

        @Lazy
        @Bean
        public ArcadiaReviewsClient arcadiaReviewsClient(NettyHttpClientContext context) {
            return new ArcadiaReviewsClient("", "", context);
        }
    }

    public static class PropertyTestConfig {
        @Value("${test.property}")
        String value;

        @Bean
        public ArcadiaReviewsClient arcadiaReviewsClient() {
            return new ArcadiaReviewsClient(value, "", null);
        }
    }

    @Test
    public void overwritesNettyContextBean() {
        NettyHttpClientContext context = new NettyHttpClientContext(new HttpClientConfig());

        createClientContext(context).getBean(ArcadiaReviewsClient.class);
        createClientContext(context).getBean(ArcadiaReviewsClient.class);
        createClientContext(context).getBean(ArcadiaReviewsClient.class);

        int numberOfThreads = Thread.getAllStackTraces().keySet().size();

        Assert.assertTrue(numberOfThreads < 9);
    }

    private AnnotationConfigApplicationContext createClientContext(NettyHttpClientContext nettyContext) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        context.getBeanFactory().registerResolvableDependency(NettyHttpClientContext.class, nettyContext);
        context.register(ClientTestConfig.class);

        context.refresh();

        return context;
    }

    @Test
    public void resolvesCustomPropertySource() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        HashMap<String, Object> map = new HashMap<>();
        map.put("test.property", "http://arcadia.ru");

        MapPropertySource propertySource = new MapPropertySource("global", map);
        context.getEnvironment().getPropertySources().addFirst(propertySource);

        context.register(PropertyTestConfig.class);
        context.refresh();

        context.getBean(ArcadiaReviewsClient.class);
    }
}
