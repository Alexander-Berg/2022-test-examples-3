package ru.yandex.market.tpl.partner.carrier.config;

import org.mockito.Answers;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;

@MockBean(classes = {
        BlackboxClient.class
}, answer = Answers.RETURNS_DEEP_STUBS)
@Configuration
public class MockPartnerCarrierTestsConfig {

}
