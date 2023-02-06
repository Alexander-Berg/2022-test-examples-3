package ru.yandex.market.core.outlet.db;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.delivery.PointType;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.OutletSource;
import ru.yandex.market.core.outlet.OutletType;

import static ru.yandex.market.core.outlet.OutletInfo.NOT_DEFINED_ID;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.DELIVERY_SERVICE_ID;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.DELIVERY_SERVICE_OUTLET_CODE1;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.DELIVERY_SERVICE_OUTLET_CODE2;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.POINT_TYPE;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.getOutletWithChildData;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.getOutletWithChildDataAndNulls;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.getOutletWithNoChildData;
import static ru.yandex.market.core.outlet.db.DbMarketDeliveryOutletInfoProvider.getOutletWithNullChildData;

/**
 * Тест для {@link DbMarketDeliveryOutletInfoService#refreshMarketDeliveryOutletInfo}.
 */
@DbUnitDataSet(before = "DbMarketDeliveryOutletInfoServiceTest.before.csv")
class DbMarketDeliveryOutletInfoServiceTest extends FunctionalTest {

    @Autowired
    private DbMarketDeliveryOutletInfoService dbMarketDeliveryOutletInfoService;

    @Test
    @DbUnitDataSet(
            before = "DbMarketDeliveryOutletInfoServiceTest.before.point1.csv",
            after = "DbMarketDeliveryOutletInfoServiceTest.before.point1.csv"
    )
    void refreshNoPoints() {
        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                POINT_TYPE,
                List.of(),
                OutletSource.MARKET_DELIVERY
        );
    }

    @Test
    @DbUnitDataSet(
            before = "DbMarketDeliveryOutletInfoServiceTest.before.point1.csv",
            after = "DbMarketDeliveryOutletInfoServiceTest.after.delete.csv"
    )
    void refreshWithDeleteChildTablesFromEmpty() {
        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                POINT_TYPE,
                List.of(getOutletWithNoChildData(DELIVERY_SERVICE_OUTLET_CODE1)),
                OutletSource.MARKET_DELIVERY
        );
    }

    @Test
    @DbUnitDataSet(
            before = "DbMarketDeliveryOutletInfoServiceTest.before.point1.csv",
            after = "DbMarketDeliveryOutletInfoServiceTest.after.delete.csv"
    )
    void refreshWithDeleteChildTablesFromNull() {
        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                POINT_TYPE,
                List.of(getOutletWithNullChildData(DELIVERY_SERVICE_OUTLET_CODE1)),
                OutletSource.MARKET_DELIVERY
        );
    }

    @Test
    @DbUnitDataSet(
            before = "DbMarketDeliveryOutletInfoServiceTest.before.keep.csv",
            after = "DbMarketDeliveryOutletInfoServiceTest.after.keep.csv"
    )
    void refreshWithKeepChildTables() {
        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                POINT_TYPE,
                List.of(getOutletWithNoChildData(DELIVERY_SERVICE_OUTLET_CODE1)),
                OutletSource.MARKET_DELIVERY
        );
    }

    @Test
    @DbUnitDataSet(
            before = "DbMarketDeliveryOutletInfoServiceTest.before.point1.csv",
            after = "DbMarketDeliveryOutletInfoServiceTest.after.refresh.point1.csv"
    )
    void refreshSinglePoint() {
        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                POINT_TYPE,
                List.of(getOutletWithChildData(DELIVERY_SERVICE_OUTLET_CODE1)),
                OutletSource.MARKET_DELIVERY
        );
    }

    @Test
    @DbUnitDataSet(
            before = "DbMarketDeliveryOutletInfoServiceTest.before.point1.csv",
            after = "DbMarketDeliveryOutletInfoServiceTest.after.refresh.point1.csv"
    )
    void refreshSinglePointWithNull() {
        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                POINT_TYPE,
                List.of(getOutletWithChildDataAndNulls(DELIVERY_SERVICE_OUTLET_CODE1)),
                OutletSource.MARKET_DELIVERY
        );
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "DbMarketDeliveryOutletInfoServiceTest.before.point1.csv",
                    "DbMarketDeliveryOutletInfoServiceTest.before.point2.csv",
            },
            after = "DbMarketDeliveryOutletInfoServiceTest.after.refresh.multi.csv"
    )
    void refreshMultiPoints() {
        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                POINT_TYPE,
                List.of(
                        getOutletWithChildData(DELIVERY_SERVICE_OUTLET_CODE1),
                        getOutletWithChildData(DELIVERY_SERVICE_OUTLET_CODE2)
                ),
                OutletSource.MARKET_DELIVERY
        );
    }

    @Test
    @DbUnitDataSet(
            before = "DbMarketDeliveryOutletInfoServiceTest.before.point1.csv",
            after = "DbMarketDeliveryOutletInfoServiceTest.after.refresh.point1.csv"
    )
    void refreshRepeatPoints() {
        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                POINT_TYPE,
                List.of(
                        getOutletWithChildData(DELIVERY_SERVICE_OUTLET_CODE1),
                        getOutletWithChildData(DELIVERY_SERVICE_OUTLET_CODE1)
                ),
                OutletSource.MARKET_DELIVERY
        );
    }

    @Test
    @DbUnitDataSet(
            before = {
                    "DbMarketDeliveryOutletInfoServiceTest.before.point1.csv",
                    "DbMarketDeliveryOutletInfoServiceTest.before.point2.csv",
                    "DbMarketDeliveryOutletInfoServiceTest.before.keep.csv",
            },
            after = "DbMarketDeliveryOutletInfoServiceTest.after.refresh.mix.csv"
    )
    void refreshWithDeleteAndKeep() {
        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                POINT_TYPE,
                List.of(
                        getOutletWithNoChildData(DELIVERY_SERVICE_OUTLET_CODE1),
                        getOutletWithChildDataAndNulls(DELIVERY_SERVICE_OUTLET_CODE2)
                ),
                OutletSource.MARKET_DELIVERY
        );
    }

    @Test
    @DisplayName("Проверка установки флага DELETED = 1, если инлет был в базе, но больше не импортится из LMS")
    @DbUnitDataSet(
            before = "DbMarketDeliveryOutletInfoServiceTest.inlets.MarkAsDeleted.before.csv",
            after = "DbMarketDeliveryOutletInfoServiceTest.inlets.MarkAsDeleted.after.csv"
    )
    void inletsMarkAsDeleted() {
        String dsOutletId = "123";
        OutletInfo inlet = new OutletInfo(NOT_DEFINED_ID, NOT_DEFINED_ID, OutletType.DEPOT, "new inlet", false,
                dsOutletId);
        inlet.setInlet(true);
        inlet.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        inlet.setDeliveryServiceOutletId(dsOutletId);
        inlet.setDeliveryServiceOutletCode("some code");

        dbMarketDeliveryOutletInfoService.refreshMarketDeliveryOutletInfo(
                DELIVERY_SERVICE_ID,
                PointType.INLET,
                List.of(inlet),
                OutletSource.LMS
        );
    }
}
