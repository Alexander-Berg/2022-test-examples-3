package ru.yandex.market.wms.autostart.autostartlogic.service.aoswaveserviceimpl.config;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.common.spring.IntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestConfigurations.class})
public abstract class ParentTestConfiguration extends IntegrationTest {
}
