package ru.yandex.market.wms.common.spring.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.pojo.ItrnCreateParams;
import ru.yandex.market.wms.common.spring.pojo.MovingItem;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SerialInventoryServiceTest extends IntegrationTest {
    public static final String USER_ID = "test";
    public static final String NOT_HOLD = "NOT_HOLD";
    public static final String HOLD = "HOLD";
    public static final String TO = "TO";
    public static final String SOURCE_TYPE = "sourceType";
    public static final String SOURCE_KEY = "sourceKey";

    @Autowired
    SerialInventoryService service;

    private static Stream<Arguments> generateMovingItemsWithNoChangesInQtyOnHold() {
        return Stream.of(
                Arguments.of(
                        movingItems_FromHoldLotAndHoldLoc(),
                        NOT_HOLD,
                        "From hold LOT and hold LOC to not hold LOC"
                ), Arguments.of(
                        movingItems_FromHoldLotAndHoldLoc(),
                        HOLD,
                        "From hold LOT and hold LOC to hold LOC"
                ), Arguments.of(
                        movingItems_FromHoldLotAndNotHoldLoc("123"),
                        NOT_HOLD,
                        "From hold LOT and not hold LOC to not hold LOC"
                ), Arguments.of(
                        movingItems_FromHoldLotAndNotHoldLoc("123"),
                        HOLD,
                        "From hold LOT and not hold LOC to not hold LOC"
                ), Arguments.of(
                        movingItems_FromNotHoldLotAndHoldLoc("234"),
                        HOLD,
                        "From not hold LOT and hold LOC to hold LOC"
                ), Arguments.of(
                        movingItems_FromNotHoldLotAndNotHoldLoc(),
                        NOT_HOLD,
                        "From not hold LOT and not hold LOC to not hold LOC"
                )
        );
    }

    private static List<MovingItem> movingItems_FromHoldLotAndHoldLoc() {
        return Collections.singletonList(
                MovingItem.builder()
                        .lot("123")
                        .sku("777")
                        .storerKey("111")
                        .fromLoc("LOST")
                        .fromId("FROM")
                        .quantity(BigDecimal.valueOf(2))
                        .quantityPicked(BigDecimal.valueOf(2))
                        .build()
        );
    }

    private static List<MovingItem> movingItems_FromHoldLotAndNotHoldLoc(String lot) {
        return Collections.singletonList(
                MovingItem.builder()
                        .lot(lot)
                        .sku("777")
                        .storerKey("111")
                        .fromLoc("1-01")
                        .fromId("FROM")
                        .quantity(BigDecimal.valueOf(2))
                        .quantityPicked(BigDecimal.valueOf(2))
                        .build()
        );
    }

    private static List<MovingItem> movingItems_FromNotHoldLotAndHoldLoc(String lot) {
        return List.of(
                MovingItem.builder()
                        .lot(lot)
                        .sku("777")
                        .storerKey("111")
                        .fromLoc("LOST")
                        .fromId("FROM")
                        .quantity(BigDecimal.valueOf(2))
                        .quantityPicked(BigDecimal.valueOf(2))
                        .build()
        );
    }

    private static List<MovingItem> movingItems_FromNotHoldLotAndNotHoldLoc() {
        return List.of(
                MovingItem.builder()
                        .lot("234")
                        .sku("777")
                        .storerKey("111")
                        .fromLoc("1-01")
                        .fromId("FROM")
                        .quantity(BigDecimal.valueOf(2))
                        .quantityPicked(BigDecimal.valueOf(2))
                        .build()
        );
    }

    @Test
    void moveFromBomSkuToMasterSkuNotFoundTest() {
        Throwable e = assertThrows(IllegalArgumentException.class,
                () -> service.moveFromBomSkuToMasterSku("123", "123", USER_ID));
        assertEquals("SKU = '123' with storerKey = 123 not found", e.getMessage());
    }

    @Test
    @DatabaseSetup("/db/service/serialInventory/move-from-bom-sku-to-master-sku/before.xml")
    @ExpectedDatabase(value = "/db/service/serialInventory/move-from-bom-sku-to-master-sku/fixed.xml", assertionMode
            = NON_STRICT_UNORDERED)
    void moveFromBomSkuToMasterSkuTest() {
        service.moveFromBomSkuToMasterSku("111", "777BOM1", USER_ID);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("generateMovingItemsWithNoChangesInQtyOnHold")
    @DatabaseSetup("/db/service/serialInventory/before.xml")
    @ExpectedDatabase(value = "/db/service/serialInventory/with-no-changes.xml", assertionMode = NON_STRICT_UNORDERED)
    void moveToLocAndIdWithUpdateQtyOnHoldFromHoldLot(List<MovingItem> movingItems, String loc) {
        ItrnCreateParams itrnCreateParams = ItrnCreateParams.mv(SOURCE_TYPE, SOURCE_KEY);

        service.moveToLocAndIdWithUpdateQtyOnHold(movingItems, loc, TO, itrnCreateParams, USER_ID);
    }

    @Test
    @DatabaseSetup("/db/service/serialInventory/before.xml")
    @ExpectedDatabase(
            value = "/db/service/serialInventory/move-from-not-hold-lot/from-hold-loc-to-not-hold-loc.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void moveToLocAndIdWithUpdateQtyOnHold_FromNotHoldLotAndHoldLoc_ToNotHoldLoc() {
        List<MovingItem> movingItems = movingItems_FromNotHoldLotAndHoldLoc("234");
        ItrnCreateParams itrnCreateParams = ItrnCreateParams.mv(SOURCE_TYPE, SOURCE_KEY);

        service.moveToLocAndIdWithUpdateQtyOnHold(movingItems, NOT_HOLD, TO, itrnCreateParams, USER_ID);
    }

    @Test
    @DatabaseSetup("/db/service/serialInventory/before.xml")
    @ExpectedDatabase(
            value = "/db/service/serialInventory/move-from-not-hold-lot/from-not-hold-loc-to-hold-loc.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void moveToLocAndIdWithUpdateQtyOnHold_FromNotHoldLotAndNotHoldLoc_ToHoldLoc() {
        List<MovingItem> movingItems = movingItems_FromNotHoldLotAndNotHoldLoc();
        ItrnCreateParams itrnCreateParams = ItrnCreateParams.mv(SOURCE_TYPE, SOURCE_KEY);

        service.moveToLocAndIdWithUpdateQtyOnHold(movingItems, HOLD, TO, itrnCreateParams, USER_ID);
    }

    @Test
    @DatabaseSetup("/db/service/serialInventory/before.xml")
    @ExpectedDatabase(
            value = "/db/service/serialInventory/from-mixed-holds-to-hold-loc.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void moveToLocAndIdWithUpdateQtyOnHold_MixedHolds_ToHoldLoc() {
        List<MovingItem> movingItems = new ArrayList<>();
        movingItems.addAll(movingItems_FromNotHoldLotAndNotHoldLoc());
        movingItems.addAll(movingItems_FromNotHoldLotAndHoldLoc("345"));
        movingItems.addAll(movingItems_FromHoldLotAndHoldLoc());
        movingItems.addAll(movingItems_FromHoldLotAndNotHoldLoc("456"));

        ItrnCreateParams itrnCreateParams = ItrnCreateParams.mv(SOURCE_TYPE, SOURCE_KEY);

        service.moveToLocAndIdWithUpdateQtyOnHold(movingItems, HOLD, TO, itrnCreateParams, USER_ID);
    }

    @Test
    @DatabaseSetup("/db/service/serialInventory/before.xml")
    @ExpectedDatabase(
            value = "/db/service/serialInventory/from-mixed-holds-to-not-hold-loc.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void moveToLocAndIdWithUpdateQtyOnHold_MixedHolds_ToNotHoldLoc() {
        List<MovingItem> movingItems = new ArrayList<>();
        movingItems.addAll(movingItems_FromNotHoldLotAndNotHoldLoc());
        movingItems.addAll(movingItems_FromNotHoldLotAndHoldLoc("345"));
        movingItems.addAll(movingItems_FromHoldLotAndHoldLoc());
        movingItems.addAll(movingItems_FromHoldLotAndNotHoldLoc("456"));

        ItrnCreateParams itrnCreateParams = ItrnCreateParams.mv(SOURCE_TYPE, SOURCE_KEY);

        service.moveToLocAndIdWithUpdateQtyOnHold(movingItems, NOT_HOLD, TO, itrnCreateParams, USER_ID);
    }
}
