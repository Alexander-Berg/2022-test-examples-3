package ru.yandex.market.wrap.infor.service.common.upsert;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.wrap.core.processing.validation.TokenContextHolder;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wrap.infor.functional.AbstractFunctionalTestWithIrisCommunication;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.ItemMeta;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.UpsertableItem;
import ru.yandex.market.wrap.infor.service.inbound.converter.meta.UpsertableItemsWithDbInfo;

class UpsertSkusFilterServiceTest extends AbstractFunctionalTestWithIrisCommunication {

    @Autowired
    private UpsertSkuFilterService filterService;

    @Autowired
    private TokenContextHolder tokenContextHolder;

    @BeforeEach
    public void setUp() {
        tokenContextHolder.setToken("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
    }

    /**
     * Состояние в БД WMS:
     * TST0000000000000000001 - отсутствует полностью (товара нет в таблице SKU-> потребуется запрос в IRIS)
     * TST0000000000000000002 - присутствует в таблице SKU, но имеет pack = STD (потребуется запрос в IRIS)
     * TST0000000000000000003 - присутствует в таблице SKU и имеет уже заполненный пак (не потребуется запрос в IRIS)
     * TST0000000000000000004 - аналогично п.1, однако данных по нему не будет в IRIS.
     * <p>
     * Состояние в IRIS:
     * Есть полные данные по товару TST0000000000000000001;
     * Есть данные только по dimensions для TST0000000000000000002;
     * Данных нет совсем для TST0000000000000000004;
     * <p>
     * Ожидаемое поведение:
     * * Ожидаем запрос в IRIS с товарами TST0000000000000000001,TST0000000000000000002,TST0000000000000000004;
     * * На выходе ожидаем получить все 4 товара, однако PackMeta будет только у
     * 1) TST0000000000000000001 (со всеми полями из IRIS)
     * 2) TST0000000000000000002 (только с частью полей из IRIS)
     */
    @Test
    @DatabaseSetup(
        connection = "wmsConnection",
        value = "classpath:fixtures/integration/upsert_items/sku.xml"
    )
    void correctUpsertableItemsAcquired() {
        mockIrisCommunication(
            "fixtures/integration/upsert_items/iris_request.json",
            "fixtures/integration/upsert_items/iris_response.json"
        );

        UpsertableItemsWithDbInfo result = filterService.selectItemsToUpsert(Arrays.asList(
            new ItemMeta(new Item.ItemBuilder(null, null, null).build(),
                new UnitId("sku1", 1L, "sku1"), "TST0000000000000000001"),
            new ItemMeta(new Item.ItemBuilder(null, null, null).build(),
                new UnitId("sku2", 2L, "sku2"), "TST0000000000000000002"),
            new ItemMeta(new Item.ItemBuilder(null, null, null).build(),
                new UnitId("sku3", 3L, "sku3"), "TST0000000000000000003"),
            new ItemMeta(new Item.ItemBuilder(null, null, null).build(),
                new UnitId("sku4", 4L, "sku4"), "TST0000000000000000004")
        ), true);

        Collection<UpsertableItem> upsertableItems = result.getUpsertableItems();
        softly.assertThat(upsertableItems).hasSize(4);

        Map<String, UpsertableItem> mappedValues = upsertableItems.stream().collect(Collectors.toMap(
            (UpsertableItem item) -> item.getItemMeta().getWarehouseSku(),
            Function.identity()
        ));

        //checking first item
        UpsertableItem firstItem = mappedValues.get("TST0000000000000000001");

        softly.assertThat(firstItem.getPackMeta().getWidth()).isEqualByComparingTo(new BigDecimal("10"));
        softly.assertThat(firstItem.getPackMeta().getHeight()).isEqualByComparingTo(new BigDecimal("11"));
        softly.assertThat(firstItem.getPackMeta().getLength()).isEqualByComparingTo(new BigDecimal("12"));
        softly.assertThat(firstItem.getPackMeta().getWeightGross()).isEqualByComparingTo(new BigDecimal("1"));

        //checking second item
        UpsertableItem secondItem = mappedValues.get("TST0000000000000000002");
        softly.assertThat(secondItem.getPackMeta().getWidth()).isEqualByComparingTo(new BigDecimal("20"));
        softly.assertThat(secondItem.getPackMeta().getHeight()).isEqualByComparingTo(new BigDecimal("21"));
        softly.assertThat(secondItem.getPackMeta().getLength()).isEqualByComparingTo(new BigDecimal("22"));
        softly.assertThat(secondItem.getPackMeta().getWeightGross()).isNull();

        //checking third item
        UpsertableItem thirdItem = mappedValues.get("TST0000000000000000003");
        softly.assertThat(thirdItem.getPackMeta()).isNull();

        //checking fourth item
        UpsertableItem fourthItem = mappedValues.get("TST0000000000000000004");
        softly.assertThat(fourthItem.getPackMeta()).isNull();
    }
}
