package ru.yandex.market.supportwizard.service.alert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.supportwizard.base.supplier.SupplierOnboardingStepType;
import ru.yandex.market.supportwizard.config.BaseFunctionalTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OnboardingAlertConfigurationCacheTest extends BaseFunctionalTest {

    @Autowired
    private OnboardingAlertConfigurationCache tested;

    @BeforeEach
    public void configure() {
        //необходимо, чтобы кэш рефрэшнулся после загрузки данных дб юнитом
        tested.invalidateAll();
    }

    @Test
    @DbUnitDataSet(before = "alertConfigurationCache.before.csv")
    void testConfigCacheBehaviour() {
        assertEquals(2, tested.getMaxHoursOnStep(SupplierOnboardingStepType.REGISTRATION));
        assertEquals(10, tested.getMaxHoursOnStep(SupplierOnboardingStepType.REQUEST_PROCESSING));
        assertEquals(500, tested.getMaxHoursOnStep(SupplierOnboardingStepType.FEED_CREATION));
    }
}
