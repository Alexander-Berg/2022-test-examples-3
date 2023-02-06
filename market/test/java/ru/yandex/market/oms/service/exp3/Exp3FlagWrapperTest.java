package ru.yandex.market.oms.service.exp3;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.checkout.backbone.config.ExperimentsFlagWrapper;
import ru.yandex.market.checkout.backbone.fintech.AccountPaymentFeatureToggle;
import ru.yandex.market.oms.AbstractFunctionalTest;
import ru.yandex.market.oms.config.Exp3TestBeansConfig;

@ActiveProfiles("functionalTest")
@ContextConfiguration(classes = {
        Exp3TestBeansConfig.class
})
public class Exp3FlagWrapperTest extends AbstractFunctionalTest {
    @Autowired
    ExperimentsFlagWrapper experimentsFlagWrapper;

    @Test
    public void getFlags() {
        Assertions.assertTrue(experimentsFlagWrapper.enableCisFullValidation());
        Assertions.assertTrue(experimentsFlagWrapper.enableDbsWithRouteDeliveryFeature());
        Assertions.assertFalse(experimentsFlagWrapper.forbiddenProcessingShippedForShops());
        Assertions.assertTrue(experimentsFlagWrapper.enableDeferredCourierNewDeliverySubstatuses());
        Assertions.assertEquals(AccountPaymentFeatureToggle.LOGGING,
                experimentsFlagWrapper.accountPaymentFeatureToggle());
    }
}
