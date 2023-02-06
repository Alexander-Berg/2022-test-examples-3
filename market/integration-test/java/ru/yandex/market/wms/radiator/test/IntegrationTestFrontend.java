package ru.yandex.market.wms.radiator.test;

import net.javacrumbs.shedlock.core.LockProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.wms.radiator.cache.ExpirationItemsPumper;
import ru.yandex.market.wms.radiator.cache.ReferenceItemsPumper;
import ru.yandex.market.wms.radiator.cache.WarehouseLocker;
import ru.yandex.market.wms.radiator.controller.StocksController;

@SpringBootTest(classes = IntegrationTestFrontendConfiguration.class)
@ComponentScan(value = {
        "ru.yandex.market.wms.radiator.core.config.client",
        "ru.yandex.market.wms.radiator.core.web",
        "ru.yandex.market.wms.radiator.api",
        "ru.yandex.market.wms.radiator.controller",
        "ru.yandex.market.wms.radiator.core.config.xml",
        "ru.yandex.market.wms.radiator.service.stocks.push",
        "ru.yandex.market.wms.radiator.service.pack",
}, excludeFilters={
                @ComponentScan.Filter(type= FilterType.ASSIGNABLE_TYPE, value= StocksController.class)})
public abstract class IntegrationTestFrontend extends IntegrationTestBackend {

    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected WarehouseLocker locker;

    @MockBean
    protected ExpirationItemsPumper expirationItemsPumper;

    @MockBean
    protected ReferenceItemsPumper referenceItemsPumper;

    @MockBean
    protected LockProvider lockProvider;
}
