package ru.yandex.market.ocrm.module.yadelivery;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.ocrm.module.yadelivery.test.YaDeliveryTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ModuleYaDeliveryTestConfiguration.class)
public class YaDeliveryOrderTest {

    @Inject
    private YaDeliveryTestUtils testUtils;

    /**
     * Проверяем, что не развалились при загрузке конфигурации модуля
     */
    @Test
    public void checkConfiguration() {
        // do nothing
    }

    @Test
    @Transactional
    public void createDeliveryOrder() {
        Assertions.assertNotNull(testUtils.createYaDeliveryOrder());
    }
}
