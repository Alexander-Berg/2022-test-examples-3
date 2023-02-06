package ru.yandex.direct.core.entity.banner.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.banner.type.href.BannersUrlHelper;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.YANDEX_URL_TO_SERVICE_MAPPINGS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SetYandexDomainTest {
    @Autowired
    public BannersUrlHelper bannersUrlHelper;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Test
    public void overrideFromPpcProperty() {
        ppcPropertiesSupport.set(YANDEX_URL_TO_SERVICE_MAPPINGS.getName(), "maps=yamaps");
        String domain = bannersUrlHelper.extractHostFromHrefWithWwwOrNull("http://yandex.ru/maps/?orgid=1234");
        assertThat("Получили ожидаемый домен из href", domain, equalTo("yamaps.yandex.ru"));
    }

    @Test
    public void newService() {
        ppcPropertiesSupport.set(YANDEX_URL_TO_SERVICE_MAPPINGS.getName(), "yamaps=maps");
        String domain = bannersUrlHelper.extractHostFromHrefWithWwwOrNull("http://yandex.ru/yamaps/?orgid=1234");
        assertThat("Получили ожидаемый домен из href", domain, equalTo("maps.yandex.ru"));
    }
}
