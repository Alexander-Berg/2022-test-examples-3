package ru.yandex.market.deliverycalculator.storage.repository;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.deliverycalculator.storage.FunctionalTest;
import ru.yandex.market.deliverycalculator.storage.model.ShopSettingsMeta;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.ShopModifiersGenerationEntity;
import ru.yandex.market.deliverycalculator.storage.model.metastorage.ShopModifiersGenerationEntity.ShopModifiersGenerationEntityId;
import ru.yandex.market.deliverycalculator.storage.model.modifier.Condition;
import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.deliverycalculator.storage.util.PostgresJsonDataType.OBJECT_MAPPER;

/**
 * Тест для {@link ShopModifiersGenerationRepository}.
 */
class ShopModifiersGenerationRepositoryTest extends FunctionalTest {

    @Autowired
    private ShopModifiersGenerationRepository tested;

    /**
     * Тест для {@link ShopModifiersGenerationRepository#saveAll(Iterable)}
     * Случай: успешное сохранение данных
     *
     * @throws JsonProcessingException когда формирование тестовых модификаторов упало на сериализации
     */
    @Test
    @DbUnitDataSet(before = "database/storeShopModifiersMeta.before.csv",
            after = "database/storeShopModifiersMeta.after.csv")
    void testSave() throws JsonProcessingException {
        // поколение загрузки магазинных модификаторов
        ShopModifiersGenerationEntity gen1 = new ShopModifiersGenerationEntity();
        gen1.setGenerationId(1L);
        gen1.setShopId(1L);
        gen1.setModifiersBucketUrl("http://someurl.com");
        gen1.setDeleted(false);
        gen1.setModifiersMeta(OBJECT_MAPPER.writeValueAsString(createShopSettingsMeta()));
        //поколение удаления магазинных модификаторов
        ShopModifiersGenerationEntity gen2 = new ShopModifiersGenerationEntity();
        gen2.setGenerationId(2L);
        gen2.setShopId(1L);
        gen2.setDeleted(true);

        tested.saveAll(asList(gen1, gen2));

        assertTrue(tested.findById(new ShopModifiersGenerationEntityId(1L, 1L)).isPresent());
        assertTrue(tested.findById(new ShopModifiersGenerationEntityId(1L, 2L)).isPresent());
    }

    /**
     * Тест для {@link ShopModifiersGenerationRepository#findById(Object)}
     * Случай: успешно найдена запись, десериализация вложенного jsonb успешна
     */
    @Test
    @DbUnitDataSet(before = "database/storeShopModifiersMeta.after.csv")
    void testFind() throws IOException {
        Optional<ShopModifiersGenerationEntity> found = tested.findById(new ShopModifiersGenerationEntityId(1L, 1L));

        assertTrue(found.isPresent());
        assertEquals(createShopSettingsMeta(),
                OBJECT_MAPPER.readValue(found.get().getModifiersMeta(), ShopSettingsMeta.class));
    }

    private ShopSettingsMeta createShopSettingsMeta() {
        return new ShopSettingsMeta.Builder()
                .withAutoCalculatedCourierCarrierIds(Sets.newHashSet(51L, 9L))
                .withModifiers(createModifiersMeta())
                .build();
    }

    private DeliveryModifiersMeta createModifiersMeta() {
        return new DeliveryModifiersMeta.Builder()
                .withCarrierAvailabilityModifiers(asList(
                        new DeliveryModifiersMeta.CarrierAvailabilityModifierMeta.Builder()
                                .withId(1L)
                                .withPriority(1)
                                .withIsCarrierAvailable(true)
                                .withCondition(new Condition.Builder()
                                        .withCarrierIds(Sets.newHashSet(51L))
                                        .withDeliveryDestinations(Sets.newHashSet(21))
                                        .build())
                                .build(),
                        new DeliveryModifiersMeta.CarrierAvailabilityModifierMeta.Builder()
                                .withId(2L)
                                .withPriority(1)
                                .withIsCarrierAvailable(true)
                                .withCondition(new Condition.Builder()
                                        .withCarrierIds(Sets.newHashSet(9L))
                                        .withDeliveryDestinations(Sets.newHashSet(21))
                                        .build())
                                .build()
                ))
                .build();
    }
}
