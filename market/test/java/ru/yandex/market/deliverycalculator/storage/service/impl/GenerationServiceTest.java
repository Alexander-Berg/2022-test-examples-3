package ru.yandex.market.deliverycalculator.storage.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.model.DeliveryTariffProgramType;
import ru.yandex.market.deliverycalculator.model.Generation;
import ru.yandex.market.deliverycalculator.model.YaDeliveryTariffType;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.service.GenerationService;

@DbUnitDataSet
public class GenerationServiceTest extends FunctionalTest {

    @Autowired
    private GenerationService generationService;

    @Test
    @DbUnitDataSet(before = "getLastDaasCourierGenerationByTariffId.before.csv")
    void getLastDaasCourierGenerationByTariffId_shouldReturnNull() {
        // Given, When
        final Generation generation = generationService.findLastTariffGeneration(0,
                DeliveryTariffProgramType.DAAS, YaDeliveryTariffType.COURIER);

        // Then
        Assertions.assertNull(generation);
    }

    @Test
    @DbUnitDataSet(before = "getLastDaasCourierGenerationByTariffId.before.csv")
    void getLastDaasCourierGenerationByTariffId_shouldReturnNonNull() {
        // Given, When
        final Generation generation = generationService.findLastTariffGeneration(1,
                DeliveryTariffProgramType.DAAS, YaDeliveryTariffType.COURIER);
        // Then
        Assertions.assertNotNull(generation);

        Assertions.assertEquals(2, generation.getId());
    }
}
