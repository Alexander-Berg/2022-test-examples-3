package ru.yandex.market.core.protocol.model;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.test.utils.UniquenessTestUtils;

/**
 * Тесты для {@link ActionType}.
 */
class ActionTypeTest {

    @Test
    void testUniqueness() {
        UniquenessTestUtils.checkUniqueness(ActionType.class);
    }

}
