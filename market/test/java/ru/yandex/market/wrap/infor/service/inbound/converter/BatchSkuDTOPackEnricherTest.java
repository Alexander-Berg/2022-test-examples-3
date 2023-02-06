package ru.yandex.market.wrap.infor.service.inbound.converter;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.wrap.infor.client.model.SkuAndPackDTO;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.ItemMeta;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.PackMeta;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.UpsertableItem;
import ru.yandex.market.wrap.infor.service.pack.PackKeyUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.wrap.infor.service.inbound.converter.BatchSkuDTOPackEnricher.BOX_COUNT_FOR_SINGLE_PACKAGED_ITEM;
import static ru.yandex.market.wrap.infor.service.inbound.converter.BatchSkuDTOPackEnricher.SUSR_1_FOR_SINGLE_PACKAGED_ITEM;
import static ru.yandex.market.wrap.infor.service.inbound.converter.BatchSkuDTOPackEnricher.isMultiPackaged;

class BatchSkuDTOPackEnricherTest {
    private static BatchSkuDTOPackEnricher enricher;

    @BeforeAll
    static void initClass() {
        PackKeyUtil packKeyUtil = mock(PackKeyUtil.class);
        doReturn("")
            .when(packKeyUtil)
            .createPackKey(any());
        enricher = new BatchSkuDTOPackEnricher(packKeyUtil);
    }

    /**
     * Если boxCount == null, то товар считается одноместным
     */
    @Test
    void shouldRecognizeItemAsSinglePackagedIfBoxCountIsNull() {
        Item item = new Item.ItemBuilder(null, null, null).build();

        assertFalse(isMultiPackaged(item));
    }

    /**
     * Если boxCount == BOX_COUNT_FOR_SINGLE_PACKAGED_ITEM, то товар считается одноместным
     */
    @Test
    void shouldRecognizeItemAsSinglePackagedIfBoxCountIsEqualToSpecialValue() {
        Item item = new Item.ItemBuilder(null, null, null)
            .setBoxCount(BOX_COUNT_FOR_SINGLE_PACKAGED_ITEM)
            .build();

        assertFalse(isMultiPackaged(item));
    }

    /**
     * Если boxCount != null && boxCount != BOX_COUNT_FOR_SINGLE_PACKAGED_ITEM, то товар считается многоместным
     */
    @Test
    void shouldRecognizeItemAsMultiPackagedIfBoxCountIsNotNullAndIsNotEqualToSpecialValue() {
        Item item = new Item.ItemBuilder(null, null, null)
            .setBoxCount(BOX_COUNT_FOR_SINGLE_PACKAGED_ITEM + 1)
            .build();

        assertTrue(isMultiPackaged(item));
    }

    /**
     * Если товар многоместный, то поле susr1 заполняется значением boxCount
     */
    @Test
    void shouldSetBoxCountInSusr1IfItemIsMultiPackaged() {
        int boxCount = 10;
        Item item = new Item.ItemBuilder(null, null, null).setBoxCount(boxCount).build();
        ItemMeta itemMeta = new ItemMeta(item, null, null);
        UpsertableItem upsertableItem = new UpsertableItem(itemMeta, null);
        SkuAndPackDTO result = new SkuAndPackDTO();

        enricher.enrichWithPackingInformation(result, upsertableItem);

        String expectedSusr1 = String.valueOf(boxCount);
        assertEquals(expectedSusr1, result.getSusr1());
    }

    /**
     * Если товар однооместный, то поле susr1 заполняется значением по-умолчанию
     */
    @Test
    void shouldSetDefaultValueInSusr1IfItemIsSinglePackaged() {
        int boxCount = 1;
        Item item = new Item.ItemBuilder(null, null, null).setBoxCount(boxCount).build();
        ItemMeta itemMeta = new ItemMeta(item, null, null);
        UpsertableItem upsertableItem = new UpsertableItem(itemMeta, null);
        SkuAndPackDTO result = new SkuAndPackDTO();

        enricher.enrichWithPackingInformation(result, upsertableItem);

        assertEquals(SUSR_1_FOR_SINGLE_PACKAGED_ITEM, result.getSusr1());
    }

    /**
     * Если товар многоместный и не найдены ВГХ или найденные ВГХ невалидны, то должна передаваться пустая упаковка
     */
    @Test
    void shouldSetEmptyPackIfItemIsMultiPackaged() {
        int boxCount = 2;
        Item item = new Item.ItemBuilder(null, null, null).setBoxCount(boxCount).build();
        ItemMeta itemMeta = new ItemMeta(item, null, null);
        UpsertableItem upsertableItem = new UpsertableItem(itemMeta, null);
        SkuAndPackDTO result = new SkuAndPackDTO();

        enricher.enrichWithPackingInformation(result, upsertableItem);

        assertNotNull(result.getPack());
        assertNotNull(result.getPackkey());
    }

    @Test
    void shouldSetCubeIfPackMetaIsNotNull() {
        int boxCount = 2;
        final Double height = 16.0;
        final Double width = 8.0;
        final Double widthGross = 8.0;
        final Double length = 12.0;
        final Double cube = 1536.0;
        Item item = new Item.ItemBuilder(null, null, null).setBoxCount(boxCount).build();
        ItemMeta itemMeta = new ItemMeta(item, null, null);
        PackMeta packMeta = new PackMeta(
            PackMeta.PackMetaBuilder
                .builder()
                .withHeight(BigDecimal.valueOf(height))
                .withWidth(BigDecimal.valueOf(width))
                .withWeightGross(BigDecimal.valueOf(widthGross))
                .withLength(BigDecimal.valueOf(length))
        );

        UpsertableItem upsertableItem = new UpsertableItem(itemMeta, packMeta);
        SkuAndPackDTO result = new SkuAndPackDTO();

        enricher.enrichWithPackingInformation(result, upsertableItem);

        assertNotNull(result.getPack());
        assertNotNull(result.getPackkey());
        assertEquals(cube, Double.valueOf(result.getStdcube().doubleValue()));
        assertNotNull(result.getPack());
        assertEquals(height, result.getPack().getHeightuom3());
        assertEquals(width, result.getPack().getWidthuom3());
        assertEquals(length, result.getPack().getLengthuom3());
        assertEquals(cube, result.getPack().getCubeuom3());
    }

    @Test
    void shouldCubeBeNullIfPackMetaContainsInvalidHeight() {
        int boxCount = 2;
        final Double height = 0.0;
        final Double width = 8.0;
        final Double widthGross = 8.0;
        final Double length = 12.0;
        Item item = new Item.ItemBuilder(null, null, null).setBoxCount(boxCount).build();
        ItemMeta itemMeta = new ItemMeta(item, null, null);
        PackMeta packMeta = new PackMeta(
            PackMeta.PackMetaBuilder
                .builder()
                .withHeight(BigDecimal.valueOf(height))
                .withWidth(BigDecimal.valueOf(width))
                .withWeightGross(BigDecimal.valueOf(widthGross))
                .withLength(BigDecimal.valueOf(length))
        );

        UpsertableItem upsertableItem = new UpsertableItem(itemMeta, packMeta);
        SkuAndPackDTO result = new SkuAndPackDTO();

        enricher.enrichWithPackingInformation(result, upsertableItem);

        assertNotNull(result.getPack());
        assertNotNull(result.getPackkey());
        assertNull(result.getStdcube());
        assertNull(result.getPack().getCubeuom3());
    }

}
