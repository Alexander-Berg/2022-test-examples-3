package ru.yandex.market.abo.clch.checker;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.abo.clch.ClchTest;
import ru.yandex.market.abo.clch.model.DeliveryRegion;
import ru.yandex.market.abo.clch.model.SimpleDeliveryRegion;
import ru.yandex.market.abo.clch.provider.DataProvider;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
@Transactional("pgTransactionManager")
public class DeliveryRegionCheckerTest extends ClchTest {

    private static final int SHOP_FIRST = 134191;
    private static final int SHOP_SECOND = 138936;

    private static final int REGION_FIRST = 10951;
    private static final int REGION_SECOND = 38;

    @Autowired
    private DeliveryRegionChecker deliveryRegionChecker;
    private DeliveryRegionChecker spyChecker;

    @Autowired
    private JdbcTemplate pgJdbcTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        spyChecker = spy(deliveryRegionChecker);
        doNothing().when(spyChecker).doConfigure();
        spyChecker.configure(new CheckerDescriptor(0, "testChecker"));

        insertRegions(REGION_FIRST);
        insertRegions(REGION_SECOND);
        spyChecker.regionTree = new HashMap<>();
        spyChecker.regionTree.put(REGION_FIRST, new DeliveryRegionChecker.LiteRegionItem(REGION_FIRST));
        spyChecker.regionTree.put(REGION_SECOND, new DeliveryRegionChecker.LiteRegionItem(REGION_SECOND));
        // mock provider
        DataProvider<List<SimpleDeliveryRegion>> provider = mock(DataProvider.class);
        when(provider.getValue(SHOP_FIRST)).thenReturn(Collections.singletonList(new DeliveryRegion(REGION_FIRST, "", true, true)));
        when(provider.getValue(SHOP_SECOND)).thenReturn(Collections.singletonList(new DeliveryRegion(REGION_SECOND, "", true, true)));
        spyChecker.setDataProvider(provider);
    }

    private void insertRegions(int id) {
        pgJdbcTemplate.update("INSERT INTO region (id, parent_id, type, tz_offset, is_million_city, chief_id) " +
                "VALUES (?, 0, 0, 0, FALSE, 0)", id);
    }


    @Test
    public void testCompare() {
        CheckerResult checkerResult = spyChecker.checkShops(SHOP_FIRST, SHOP_SECOND);
        assertTrue(checkerResult.getResult() == 0);
    }
}
