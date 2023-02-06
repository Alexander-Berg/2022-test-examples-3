package ru.yandex.market.deliverycalculator.storage.util;

import org.junit.jupiter.api.Test;

import ru.yandex.market.deliverycalculator.storage.config.DeliveryCalculatorStorageConfig;
import ru.yandex.market.deliverycalculator.storage.repository.YaDeliverySenderSettingsRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Тест для {@link PooledIdGenerator}.
 */
class ModifierIdGeneratorTest {

    /**
     * Тест для {@link PooledIdGenerator#generate()}.
     */
    @Test
    public void testGenerate() {
        YaDeliverySenderSettingsRepository senderSettingsRepository = mock(YaDeliverySenderSettingsRepository.class);

        PooledIdGenerator generator = new PooledIdGenerator(
                senderSettingsRepository::getNextModifierId,
                DeliveryCalculatorStorageConfig.MODIFIER_SEQUENCE_INCREMENT
        );

        doReturn(1L, 21L).when(senderSettingsRepository).getNextModifierId();

        for (int i = 1; i <= 22; i++) {
            assertEquals(i, generator.generate());
        }

        verify(senderSettingsRepository, times(2)).getNextModifierId();
    }

}
