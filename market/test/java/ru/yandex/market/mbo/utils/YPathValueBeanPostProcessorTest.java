package ru.yandex.market.mbo.utils;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import ru.yandex.inside.yt.kosher.cypress.YPath;

/**
 * @author amaslak
 */
public class YPathValueBeanPostProcessorTest {

    private static final String TEST_PROP = "test.prop";

    private static final String TEST_PROP_SPEL = "${" + TEST_PROP + "}";

    private static final YPath TEST_YPATH = YPath.simple("//tmp/mbo/whatever");

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(Conf.class);

    @Test
    public void testYpathValueAutowire() {
        TestBean testBean = ctx.getBean(TestBean.class);
        Assert.assertEquals(TEST_YPATH.toString(), testBean.valueProperty);
        Assert.assertEquals(TEST_YPATH, testBean.ypathValueProperty);
    }

    public static class TestBean {

        @Value(YPathValueBeanPostProcessorTest.TEST_PROP_SPEL)
        String valueProperty;

        @YPathValue(YPathValueBeanPostProcessorTest.TEST_PROP_SPEL)
        YPath ypathValueProperty;
    }

    @Configuration
    public static class Conf {

        @Bean
        public static PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
            PropertySourcesPlaceholderConfigurer placeholderConfigurer = new PropertySourcesPlaceholderConfigurer();
            MutablePropertySources propertySources = new MutablePropertySources();
            propertySources.addFirst(new MapPropertySource(
                "src", ImmutableMap.of(TEST_PROP, TEST_YPATH.toString())
            ));

            placeholderConfigurer.setPropertySources(propertySources);
            return placeholderConfigurer;
        }

        @Bean
        public YPathValueBeanPostProcessor yPathValueBeanPostProcessor() {
            return new YPathValueBeanPostProcessor();
        }

        @Bean
        public AutowiredAnnotationBeanPostProcessor autowiredAnnotationBeanPostProcessor() {
            return new AutowiredAnnotationBeanPostProcessor();
        }

        @Bean
        public TestBean testBean() {
            return new TestBean();
        }

    }
}
