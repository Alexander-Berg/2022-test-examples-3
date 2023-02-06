package ru.yandex.market.core.delivery.tariff.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.delivery.RegionGroupPaymentType;
import ru.yandex.market.core.test.utils.UniquenessTestUtils;

/**
 * Unit-тесты для {@link RegionGroupPaymentType}.
 *
 * @author Vladislav Bauer
 */
class RegionGroupPaymentTypeTest {

    @Test
    void testIdUniqueness() {
        UniquenessTestUtils.checkUniqueness(RegionGroupPaymentType.class);
    }

}
