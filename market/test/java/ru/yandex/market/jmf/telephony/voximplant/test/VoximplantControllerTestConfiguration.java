package ru.yandex.market.jmf.telephony.voximplant.test;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({VoximplantTestConfiguration.class})
@ComponentScan("ru.yandex.market.jmf.telephony.voximplant.controller")
public class VoximplantControllerTestConfiguration {
}
