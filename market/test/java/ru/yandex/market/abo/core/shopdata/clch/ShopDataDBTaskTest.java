package ru.yandex.market.abo.core.shopdata.clch;

import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.shopdata.clch.model.ShopValue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 08.11.17.
 */
class ShopDataDBTaskTest {
    private static final long SHOP_ID = 0;
    private static final int DATA_TYPE = 1;
    private static final String SHOP_VALUE = "42";

    @Mock
    ShopDataTypeDbLoader dbLoader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(dbLoader.getShopValues(SHOP_ID)).thenReturn(Collections.singleton(SHOP_VALUE));
    }

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @CsvSource({SHOP_VALUE + ", true", SHOP_VALUE + "foo, false"})
    void shopHasDataIntersections(String shopValue, boolean hasIntersections) {
        long otherShopId = SHOP_ID + 1;
        doAnswer(inv -> {
            Consumer<ShopValue> shopValueConsumer = (Consumer<ShopValue>) inv.getArguments()[0];
            shopValueConsumer.accept(new ShopValue(SHOP_ID, SHOP_VALUE));
            shopValueConsumer.accept(new ShopValue(otherShopId, shopValue));
            return null;
        }).when(dbLoader).iterateOverAllShops(any());
        ShopDataDBTask task = new ShopDataDBTask(DATA_TYPE, "descr", true, dbLoader);
        ShopDataResult alikeShops = task.findAlikeShops(SHOP_ID);

        if (hasIntersections) {
            assertNotNull(alikeShops);
            Set<Long> alikeShopIds = alikeShops.getShops();
            assertEquals(1, alikeShopIds.size());
            assertTrue(alikeShopIds.contains(otherShopId));
        } else {
            assertNull(alikeShops);
        }
    }

    @Test
    void noShopValues() {
        when(dbLoader.getShopValues(SHOP_ID)).thenReturn(Collections.emptySet());
        ShopDataDBTask task = new ShopDataDBTask(DATA_TYPE, "descr", true, dbLoader);
        assertNull(task.findAlikeShops(SHOP_ID));
    }
}
