package ru.yandex.market.tpl.api.confg;

import org.mockito.Answers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.core.external.avatarnica.AvatarnicaClient;
import ru.yandex.market.tpl.core.external.boxbot.LockerApi;
import ru.yandex.market.tpl.core.external.cms.CmsTemplatorClient;
import ru.yandex.market.tpl.core.external.cms.MboCmsApiClient;
import ru.yandex.market.tpl.core.external.lifepos.LifePosFacade;

@MockBean(classes = {
        AvatarnicaClient.class,
        LifePosFacade.class,
        CmsTemplatorClient.class,
        MboCmsApiClient.class,
        LockerApi.class,
        BlackboxClient.class
}, answer = Answers.RETURNS_DEEP_STUBS)
@Configuration
public class MockApiIntegrationTestsConfig {
}
