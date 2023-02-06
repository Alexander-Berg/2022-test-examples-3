package ru.yandex.market.wms.autostart;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.autostart.service.OrderFlowService;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles;

@ActiveProfiles(Profiles.TEST)
@AutoConfigureMockMvc
public class AutostartIntegrationTest extends IntegrationTest {
    @Autowired
    private OrderFlowService orderFlowService;

    @BeforeEach
    public void reset() {
        orderFlowService.clearCache();
    }
}
