package ru.yandex.direct.core.entity.banner.service;

import java.util.Arrays;
import java.util.Collection;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@CoreTest
@RunWith(Parameterized.class)
public class SetDomainTest {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    public BannersUrlHelper bannersUrlHelper;

    @Parameterized.Parameter(0)
    public String href;

    @Parameterized.Parameter(1)
    public String expDomain;


    @Parameterized.Parameters(name = "href = {0}, expDomain = {1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][]{
                {"http://www.ginekology.ru/sos/", "www.ginekology.ru"},
                {"http://oho.ru/?good=252&from=direct&kw=numizmatika/", "oho.ru"},
                {"http://www.medcentr.ru:8080", "www.medcentr.ru"},
                {"http://www.tvo.net.ru", "www.tvo.net.ru"},

                {"https://yandex.ru/maps/?orgid=1234", "maps.yandex.ru"},
                {"https://m.yandex.ru/maps/?orgid=1234", "maps.yandex.ru"},
                {"http://www.yandex.ru/maps/?orgid=1234", "maps.yandex.ru"},
                {"http://www.yandex.ru/maps", "maps.yandex.ru"},
                {"http://maps.yandex.ru/maps", "maps.yandex.ru"},
                {"https://yandex.ru/web-maps/org/105372541245", "maps.yandex.ru"},

                {"https://yandex.ru/sprav/main?utm_medium", "sprav.yandex.ru"},
                {"https://yandex.ru/profile/7927076942", "sprav.yandex.ru"},

                {"https://yandex.ru/collections/user/opt-darya/khalaty-torgovoi-marki-daria/", "collections.yandex.ru"},

                {"http://yandex.ru/auto/?orgid=1234", "yandex.ru"},
                {"http://m.yandex.ru/auto/?orgid=1234", "m.yandex.ru"},
                {null, null},
                {"yandex.ru/sprav/main?utm_medium", "sprav.yandex.ru"}, // href с потерянным протоколом
                {"https://user@#ancor", null}, // пустой домен
                {"user@#ancor", null}, // пустой домен
                {"http;%@#&%#", null}, // тоже пустой домен
        });
    }

    @Test
    public void getDomainWithWWW() {
        String domain = bannersUrlHelper.extractHostFromHrefWithWwwOrNull(href);
        assertThat("Получили ожидаемый домен из href", domain, equalTo(expDomain));
    }
}
