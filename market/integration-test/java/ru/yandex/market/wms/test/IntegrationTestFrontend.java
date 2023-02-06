package ru.yandex.market.wms.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = IntegrationTestFrontendConfiguration.class)
@ComponentScan({
        "ru.yandex.market.wms.radiator.core.web",
        "ru.yandex.market.wms.radiator.api",
        "ru.yandex.market.wms.radiator.controller",
        "ru.yandex.market.wms.radiator.core.config.xml",
})
public abstract class IntegrationTestFrontend extends IntegrationTestBackend {

    @Autowired
    protected MockMvc mockMvc;
}
