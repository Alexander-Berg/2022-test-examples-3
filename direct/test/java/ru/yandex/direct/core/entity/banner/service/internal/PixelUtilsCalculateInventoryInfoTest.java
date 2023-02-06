package ru.yandex.direct.core.entity.banner.service.internal;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.type.pixels.InventoryType;
import ru.yandex.direct.core.entity.banner.type.pixels.PixelUtils;
import ru.yandex.direct.core.entity.placements.model.Placement;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class PixelUtilsCalculateInventoryInfoTest {
    @Parameterized.Parameter()
    public List<Placement> bannerPlacements;

    @Parameterized.Parameter(1)
    public boolean hasPrivateSegments;

    @Parameterized.Parameter(2)
    public boolean isCpmDeal;

    @Parameterized.Parameter(3)
    public InventoryType expected;

    @Parameterized.Parameters()
    public static Object[][] params() {
        return new Object[][]{
                //not cpm deal campaign
                {emptyList(), true, false, InventoryType.NOT_DEAL},
                //calculated by placements
                {emptyList(), true, true, InventoryType.UNKNOWN_INVENTORY},
                {asList(new Placement().withIsYandexPage(1L), new Placement().withIsYandexPage(1L)), true, true,
                        InventoryType.YANDEX_INVENTORY},
                {asList(new Placement().withIsYandexPage(1L), new Placement().withIsYandexPage(0L)), true, true,
                        InventoryType.UNKNOWN_INVENTORY},
                //calculated NOT by placements
                {asList(new Placement().withIsYandexPage(0L), new Placement().withIsYandexPage(0L)), true, true,
                        InventoryType.PRIVATE_CONDITIONS_FOREIGN_INVENTORY},
                {asList(new Placement().withIsYandexPage(0L), new Placement().withIsYandexPage(0L)), false, true,
                        InventoryType.PUBLIC_CONDITIONS_FOREIGN_INVENTORY},
        };
    }

    @Test
    public void test() {
        InventoryType inventoryType =
                PixelUtils.calculateInventoryInfo(bannerPlacements, hasPrivateSegments, isCpmDeal);
        assertThat(inventoryType, is(expected));
    }
}
