package ru.yandex.market.deliverycalculator.storage.repository;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.yadelivery.YaDeliverySenderSettingsData;
import ru.yandex.market.deliverycalculator.storage.model.yadelivery.YaDeliverySenderSettingsEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.deliverycalculator.storage.util.PostgresJsonDataType.OBJECT_MAPPER;

/**
 * Тест для {@link YaDeliverySenderSettingsRepository}.
 */
public class YaDeliverySenderSettingsRepositoryTest extends FunctionalTest {

    @Autowired
    private YaDeliverySenderSettingsRepository tested;

    /**
     * Тест на {@link YaDeliverySenderSettingsRepository#save(Object)}.
     * Случай: создание новых настроек сендера
     */
    @DbUnitDataSet(
            before = "database/storeSenderSettings.before.csv",
            after = "database/storeSenderSettings.after.csv"
    )
    @Test
    void testSave_newSender() {
        YaDeliverySenderSettingsEntity toBeStored = new YaDeliverySenderSettingsEntity();
        toBeStored.setSenderId(1L);
        toBeStored.setGeneration(3L);

        tested.save(toBeStored);
    }

    /**
     * Тест на {@link YaDeliverySenderSettingsRepository#findById(Object)}.
     * Случай: поиск по идентификатору сендера
     */
    @DbUnitDataSet(before = "database/findNotExportedSenderSettings.before.csv")
    @Test
    void testGetById() throws IOException {
        //проверяем на оффере с среднеофферными габаритами
        Optional<YaDeliverySenderSettingsEntity> senderEntity = tested.findById(3L);
        assertTrue(senderEntity.isPresent());
        assertNotNull(OBJECT_MAPPER.readValue(senderEntity.get().getData(), YaDeliverySenderSettingsData.class));
        //проверяем на оффере без среднеофферных габаритов
        senderEntity = tested.findById(1L);
        assertTrue(senderEntity.isPresent());
        assertNotNull(OBJECT_MAPPER.readValue(senderEntity.get().getData(), YaDeliverySenderSettingsData.class));
    }

    /**
     * Тест на {@link YaDeliverySenderSettingsRepository#findSendersWithNotExportedSettings(Pageable)}.
     * Случай: В БД 2 записи, подходящие по критериям поиска. Мы ищем только 1.
     */
    @DbUnitDataSet(before = "database/findNotExportedSenderSettings.before.csv")
    @Test
    void testFindSendersWithNotExportedSettings_oneRecordExport() {
        List<Long> actual = tested.findSendersWithNotExportedSettings(PageRequest.of(0, 1));

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(1L, actual.get(0));
    }

    /**
     * Тест на {@link YaDeliverySenderSettingsRepository#findSendersWithNotExportedSettings(Pageable)}.
     * Случай: В БД 2 записи, подходящие по критериям поиска. Мы ищем 3.
     */
    @DbUnitDataSet(before = "database/findNotExportedSenderSettings.before.csv")
    @Test
    void testFindSendersWithNotExportedSettings_RecordExport() {
        List<Long> actual = tested.findSendersWithNotExportedSettings(PageRequest.of(0, 3));

        assertNotNull(actual);
        assertEquals(2, actual.size());
        assertEquals(1L, actual.get(0));
        assertEquals(3L, actual.get(1));
    }

    /**
     * Тест для {@link YaDeliverySenderSettingsRepository#getNextModifierId()}.
     */
    @Test
    void testGenerateId() {
        long firstId = tested.getNextModifierId();
        long secondId = tested.getNextModifierId();

        //Должно быть больше 20, так как 20 - это инкремент сиквенса
        Assert.assertTrue(secondId - firstId >= 20);
    }
}
