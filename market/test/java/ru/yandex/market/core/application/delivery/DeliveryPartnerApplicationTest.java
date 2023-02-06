package ru.yandex.market.core.application.delivery;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.Test;
import org.unitils.reflectionassert.ReflectionAssert;

import ru.yandex.market.core.application.delivery.model.DeliveryPartnerApplication;

public class DeliveryPartnerApplicationTest {

    /**
     * Тест убеждается, что метод для копирования заявки {@link DeliveryPartnerApplication.Builder(DeliveryPartnerApplication)}
     * копирует все поля.
     */
    @Test
    public void testBuildNewModel() {
        DeliveryPartnerApplication origApplication = EnhancedRandom.random(DeliveryPartnerApplication.class);
        DeliveryPartnerApplication newApplication = new DeliveryPartnerApplication.Builder(origApplication).build();

        ReflectionAssert.assertReflectionEquals(origApplication, newApplication);
    }

}
