package ru.yandex.market.supercontroller.mbologs.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
    MboLogsTestXmlConfig.class
})
public class MboLogsTestConfig {
}
