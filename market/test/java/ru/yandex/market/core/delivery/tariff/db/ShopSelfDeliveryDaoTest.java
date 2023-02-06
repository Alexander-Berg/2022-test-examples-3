package ru.yandex.market.core.delivery.tariff.db;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.model.ShopSelfDeliveryState;
import ru.yandex.market.core.delivery.repository.ShopSelfDeliveryDao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ShopSelfDeliveryDaoTest extends FunctionalTest {

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private ShopSelfDeliveryDao shopSelfDeliveryDao;

    @PostConstruct
    public void init() {
        shopSelfDeliveryDao = new ShopSelfDeliveryDao(namedParameterJdbcTemplate);
    }

    @Test
    @DbUnitDataSet(
            before = "ShopSelfDeliveryDaoTest.before.csv",
            after = "ShopSelfDeliveryDaoTest.testSaveNewShopDeliverySettings.after.csv"
    )
    public void testSaveNewShopDeliveryState() {
        long shopId = 222L;

        shopSelfDeliveryDao.saveShopSelfDeliveryState(
                ShopSelfDeliveryState.builder()
                        .setShopId(shopId)
                        .setHasCourierDelivery(true)
                        .setHasPickupDelivery(true)
                        .setHasPrepay(true)
                        .setCourierRegions(List.of(1111L, 2222L, 3333L))
                        .setLastEventMillis(1234567)
                        .build()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "ShopSelfDeliveryDaoTest.before.csv",
            after = "ShopSelfDeliveryDaoTest.testUpdateShopSelfDeliverySettings.after.csv"
    )
    public void testUpdateShopSelfDeliveryState() {
        long shopId = 111L;

        shopSelfDeliveryDao.saveShopSelfDeliveryState(
                ShopSelfDeliveryState.builder()
                        .setShopId(shopId)
                        .setHasCourierDelivery(false)
                        .setHasPickupDelivery(false)
                        .setHasPrepay(false)
                        .setCourierRegions(List.of(123L, 456L))
                        .setLastEventMillis(9000000)
                        .build()
        );
    }

    @Test
    @DbUnitDataSet(
            before = "ShopSelfDeliveryDaoTest.before.csv",
            after = "ShopSelfDeliveryDaoTest.testUpdateShopSelfDeliveryBatch.after.csv"
    )
    public void testUpdateShopSelfDeliveryBatch() {
        long firstShopId = 111L;
        long secondShopId = 222L;

        List<ShopSelfDeliveryState> shopDeliveryStates = List.of(
                ShopSelfDeliveryState.builder()
                        .setShopId(firstShopId)
                        .setHasCourierDelivery(true)
                        .setHasPickupDelivery(false)
                        .setHasPrepay(false)
                        .setCourierRegions(List.of(1111L))
                        .setLastEventMillis(500000)
                        .build(),
                ShopSelfDeliveryState.builder()
                        .setShopId(secondShopId)
                        .setHasCourierDelivery(false)
                        .setHasPickupDelivery(false)
                        .setHasPrepay(true)
                        .setCourierRegions(List.of(2222L))
                        .setLastEventMillis(600000)
                        .build()
        );

        shopSelfDeliveryDao.saveShopSelfDeliveryStatesBatch(shopDeliveryStates);
    }

    @Test
    @DbUnitDataSet(
            before = "ShopSelfDeliveryDaoTest.before.csv"
    )
    public void testGetShopSelfDeliveryState() {
        long shopId = 111L;

        Optional<ShopSelfDeliveryState> shopDeliveryState = shopSelfDeliveryDao.getShopDeliveryState(shopId);

        assertTrue(shopDeliveryState.isPresent());

        ShopSelfDeliveryState expectedShopSelfDelivery = ShopSelfDeliveryState.builder()
                .setShopId(shopId)
                .setHasPickupDelivery(true)
                .setHasCourierDelivery(true)
                .setHasPrepay(false)
                .setLastEventMillis(123456)
                .setCourierRegions(List.of(123L, 456L, 789L))
                .build();

        assertEquals(expectedShopSelfDelivery, shopDeliveryState.get());
    }

    @Test
    @DbUnitDataSet(
            before = "ShopSelfDeliveryDaoTest.before.csv"
    )
    public void testRegionsNullValues() {
        long shopId = 222L;

        Optional<ShopSelfDeliveryState> shopDeliveryState = shopSelfDeliveryDao.getShopDeliveryState(shopId);

        assertTrue(shopDeliveryState.isPresent());

        ShopSelfDeliveryState expectedShopSelfDelivery = ShopSelfDeliveryState.builder()
                .setShopId(shopId)
                .setHasPickupDelivery(false)
                .setHasCourierDelivery(true)
                .setHasPrepay(true)
                .setCourierRegions(Collections.emptyList())
                .setLastEventMillis(444444)
                .build();

        assertEquals(expectedShopSelfDelivery, shopDeliveryState.get());
    }

    @Test
    @DbUnitDataSet(
            before = "ShopSelfDeliveryDaoTest.before.csv"
    )
    public void testGetNonExistingShopDeliverySettings() {
        long shopId = 999L;

        Optional<ShopSelfDeliveryState> shopDeliveryState = shopSelfDeliveryDao.getShopDeliveryState(shopId);

        assertFalse(shopDeliveryState.isPresent());
    }
}
