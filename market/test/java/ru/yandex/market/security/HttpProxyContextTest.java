package ru.yandex.market.security;

import java.util.HashSet;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.security.config.FunctionalTest;
import ru.yandex.market.security.util.heater.CacheHeater;
import ru.yandex.market.util.MbiMatchers;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@DbUnitDataSet(before = "classpath:domain.csv")
public class HttpProxyContextTest extends FunctionalTest {

    private static final Set<String> IGNORED = new HashSet<>() {{
        add("host");
        add("executing-time");
        add("version");
        add("servant");
    }};

    private static final String EVERYTHING_IS_OK = "0;OK\n";

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    public String baseUrl;

    @Autowired
    CacheHeater cacheHeater;


    /**
     * Тест принудительно инициализирует все lazy-init bean
     */
    @Test
    public void test() {
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition definition = beanFactory.getBeanDefinition(name);
            if (!definition.isAbstract()) {
                beanFactory.getBean(name);
            }
        }
    }

    @Test
    public void pingTest() {
        final String response = FunctionalTestHelper.get(baseUrl + "/ping").getBody();
        assertThat(response, equalTo(EVERYTHING_IS_OK));
    }

    @Test
    public void loadStaticDomainAuthoritiesTest() {
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<data " +
                "actions=\"[loadStaticDomainAuthorities]\"  >" +
                "<static-domain-authorities " +
                "domain=\"test2\">\n" +
                "<authority " +
                "name=\"auth5\" " +
                "uid=\"12345\"/>\n" +
                "</static-domain-authorities>\n" +
                "</data>\n";
        final String actual = FunctionalTestHelper.get(baseUrl + "/loadStaticDomainAuthorities?domain=test2").getBody();
        MatcherAssert.assertThat(actual, MbiMatchers.xmlEquals(expected, IGNORED));
    }

    @Test
    public void checkStaticAuthorityTest() {
        String expected =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<data " +
                "actions=\"[checkStaticAuthority]\" >" +
                "<result value=\"true\"/>\n" +
                "</data>";
        final String actual = FunctionalTestHelper.get(
                baseUrl + "/checkStaticAuthority?uid=1234&authority-name=auth5").getBody();
        MatcherAssert.assertThat(actual, MbiMatchers.xmlEquals(expected, IGNORED));
    }

    @Test
    public void checkStaticDomainAuthorityTest() {
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<data " +
                "actions=\"[checkStaticDomainAuthority]\" >" +
                "<result value=\"true\"/>\n" +
                "</data>";
        final String actual = FunctionalTestHelper.get(
                baseUrl + "/checkStaticDomainAuthority?domain=test2&uid=12345&authority-name=auth5").getBody();
        MatcherAssert.assertThat(actual, MbiMatchers.xmlEquals(expected, IGNORED));
    }

}
