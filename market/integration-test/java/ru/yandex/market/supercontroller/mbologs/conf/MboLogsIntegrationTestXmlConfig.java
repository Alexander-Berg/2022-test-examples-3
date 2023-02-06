package ru.yandex.market.supercontroller.mbologs.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("classpath:mbo-logs/integration/mbologs.xml")
public class MboLogsIntegrationTestXmlConfig {
}
