package ru.yandex.market.partner.mvc.controller.moderation;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.testing.TestingStatus;
import ru.yandex.market.core.testing.TestingType;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ModerationDtoMapperTest {

    /**
     * Фактически тест проверят что все значения {@link TestingStatus}
     * могут транформироваться без ошибок в дто.
     */
    @Test
    void testTestingStatusMapping() {
        for (TestingStatus value : TestingStatus.values()) {
            assertNotNull(ModerationDtoMapper.toStatus(value));
        }
    }

    /**
     * Фактически тест проверят что все значения {@link TestingType}
     * могут транформироваться без ошибок в дто.
     */
    @Test
    void testTestingTypeMapping() {
        for (TestingType value : TestingType.values()) {
            assertNotNull(ModerationDtoMapper.toType(value));
        }
    }
}
