package ru.yandex.market.vendors.analytics.core.service.dashboard.color;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.jpa.entity.color.DashboardEntityColor;
import ru.yandex.market.vendors.analytics.core.model.dashboard.DashboardEntityType;

/**
 * Functional tests for {@link DashboardColorService}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "DashboardColorServiceTest.csv")
class DashboardColorServiceTest extends FunctionalTest {

    private static final long DASHBOARD_ID = 10L;

    @Autowired
    private DashboardColorService dashboardColorService;

    @Test
    @DbUnitDataSet(before = "DashboardColorServiceTest.nonRequired.csv")
    void nonRequired() {
        List<DashboardEntityColor> colors = dashboardColorService.paintDashboard(
                DASHBOARD_ID,
                DashboardEntityType.CATEGORY,
                Set.of(10L, 15L, 20L)
        );
        Assertions.assertEquals(
                List.of(
                        buildCategoryEntity(1L, 3L, 10L),
                        buildCategoryEntity(2L, 5L, 15L),
                        buildCategoryEntity(3L, 7L, 20L)
                ),
                colors
        );
    }

    @Test
    @DbUnitDataSet(after = "DashboardColorServiceTest.allRequired.csv")
    void allRequired() {
        List<DashboardEntityColor> colors = dashboardColorService.paintDashboard(
                DASHBOARD_ID,
                DashboardEntityType.BRAND,
                Set.of(1L, 2L, 3L)
        );
        Assertions.assertEquals(
                List.of(
                        buildBrandEntity(1L, 1L, 1L),
                        buildBrandEntity(2L, 2L, 2L),
                        buildBrandEntity(3L, 3L, 3L)
                ),
                colors
        );
    }

    @Test
    @DbUnitDataSet(
            before = "DashboardColorServiceTest.regionsDefaultPainting.before.csv",
            after = "DashboardColorServiceTest.regionsDefaultPainting.after.csv"
    )
    void regionsDefaultPainting() {
        List<DashboardEntityColor> colors = dashboardColorService.paintDashboard(
                DASHBOARD_ID,
                DashboardEntityType.REGION,
                Set.of(0L, 100L, 10174L)
        );
        Assertions.assertEquals(
                List.of(
                        buildRegionEntity(1L, 1L, 10174L),
                        buildRegionEntity(3L, 0L, 0L),
                        buildRegionEntity(4L, 3L, 100L)
                ),
                colors
        );
    }

    @Test
    @DbUnitDataSet(
            before = "DashboardColorServiceTest.withLoop.before.csv",
            after = "DashboardColorServiceTest.withLoop.after.csv"
    )
    void withLoop() {
        var ids = LongStream.range(10, 30)
                .boxed()
                .collect(Collectors.toSet());

        List<DashboardEntityColor> colors = dashboardColorService.paintDashboard(
                DASHBOARD_ID,
                DashboardEntityType.BRAND,
                ids
        );
    }

    private static DashboardEntityColor buildBrandEntity(long id, long colorId, long entityId) {
        return buildEntity(DashboardEntityType.BRAND, id, colorId, entityId);
    }

    private static DashboardEntityColor buildCategoryEntity(long id, long colorId, long entityId) {
        return buildEntity(DashboardEntityType.CATEGORY, id, colorId, entityId);
    }

    private static DashboardEntityColor buildRegionEntity(long id, long colorId, long entityId) {
        return buildEntity(DashboardEntityType.REGION, id, colorId, entityId);
    }

    private static DashboardEntityColor buildEntity(DashboardEntityType entityType, long id, long colorId, long entityId) {
        var entityColor = DashboardEntityColor.builder()
                .dashboardId(DASHBOARD_ID)
                .entityId(entityId)
                .entityType(entityType)
                .colorId(colorId)
                .build();
        entityColor.setId(id);
        return entityColor;
    }
}
