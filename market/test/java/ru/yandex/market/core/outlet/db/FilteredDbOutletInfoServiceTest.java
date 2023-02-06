package ru.yandex.market.core.outlet.db;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.common.framework.pager.Pager;
import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.backa.persist.addr.Coordinates;
import ru.yandex.market.core.delivery.DeliveryInfoService;
import ru.yandex.market.core.outlet.GeoInfo;
import ru.yandex.market.core.outlet.OutletInfo;
import ru.yandex.market.core.outlet.db.column.ColumnsOutletInfo;
import ru.yandex.market.core.outlet.legal.OutletLegalInfoService;
import ru.yandex.market.core.outlet.license.OutletLicenseService;
import ru.yandex.market.core.outlet.moderation.OutletInfoShort;
import ru.yandex.market.core.outlet.moderation.OutletStatus;
import ru.yandex.market.core.schedule.ScheduleDao;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Функциональный тест для {@link FilteredDbOutletInfoService}.
 *
 * @author avetokhin 19/12/17.
 */
class FilteredDbOutletInfoServiceTest extends FunctionalTest {

    public static final long DELIVERY_SERVICE_ID = 100L;
    private static final long DATASOURCE_ID = 15;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OutletLegalInfoService outletLegalInfoService;

    @Autowired
    private OutletLicenseService outletLicenseService;

    private FilteredDbOutletInfoService outletInfoService;

    private static HashMap<Collection<ColumnsOutletInfo>, List<Object>> conditions() {
        final HashMap<Collection<ColumnsOutletInfo>, List<Object>> condition = new HashMap<>();
        condition.put(
                Collections.singletonList(ColumnsOutletInfo.DATASOURCE_ID),
                Arrays.asList(new Object[]{DATASOURCE_ID})
        );
        return condition;
    }

    @BeforeEach
    void setUp() {
        final FilteredDbOutletInfoService manageOutletInfoService = new FilteredDbOutletInfoService();
        manageOutletInfoService.setJdbcTemplate(jdbcTemplate);
        manageOutletInfoService.setTransactionTemplate(transactionTemplate);
        manageOutletInfoService.setDeliveryInfoService(Mockito.mock(DeliveryInfoService.class));
        manageOutletInfoService.setOutletScheduleDao(Mockito.mock(ScheduleDao.class));
        manageOutletInfoService.setOutletLegalInfoService(outletLegalInfoService);
        manageOutletInfoService.setOutletLicenseService(outletLicenseService);
        outletInfoService = manageOutletInfoService;
    }

    /**
     * Проверяет валидность фильтрации по GPS координатам и успешное извлечение данных из БД.
     */
    @Test
    @DbUnitDataSet(before = "outlets.csv")
    void testFindByCoordinates() {
        final HashMap<Collection<ColumnsOutletInfo>, List<Object>> condition = conditions();
        condition.put(
                Collections.singletonList(ColumnsOutletInfo.LONGITUDE),
                Arrays.asList(new Object[]{new Pair<>(10.5, 20.5)})
        );
        condition.put(
                Collections.singletonList(ColumnsOutletInfo.LATITUDE),
                Arrays.asList(new Object[]{new Pair<>(20.5, 30.5)})
        );

        final List<OutletInfo> outletInfos = outletInfoService.listOutletInfos(condition, new Pager(0, 10));
        assertThat(outletInfos, notNullValue());
        assertThat(outletInfos, hasSize(1));

        final OutletInfo outletInfo = outletInfos.get(0);
        assertThat(outletInfo.getId(), equalTo(1L));

        final GeoInfo geoInfo = outletInfo.getGeoInfo();
        assertThat(geoInfo, notNullValue());
        assertThat(geoInfo.getRegionId(), equalTo(200L));

        final Coordinates gpsCoordinates = geoInfo.getGpsCoordinates();
        assertThat(gpsCoordinates.getLon(), equalTo(15.3));
        assertThat(gpsCoordinates.getLat(), equalTo(25.7));
    }

    @Test
    @DbUnitDataSet(before = "outlets.csv")
    void listOutletShort() {
        List<OutletInfoShort> outlets = outletInfoService.listOutletInfosShort(conditions());

        assertThat(outlets, notNullValue());
        assertThat(outlets, containsInAnyOrder(
                new OutletInfoShort(1L, DATASOURCE_ID, DELIVERY_SERVICE_ID, OutletStatus.UNKNOWN),
                new OutletInfoShort(3L, DATASOURCE_ID, DELIVERY_SERVICE_ID, OutletStatus.UNKNOWN),
                new OutletInfoShort(4L, DATASOURCE_ID, DELIVERY_SERVICE_ID, OutletStatus.UNKNOWN)
        ));
    }

    @Test
    @DbUnitDataSet(before = "outlets.csv")
    void listOutlet() {
        OutletInfo outlets = outletInfoService.getOutletInfo(1);
        assertThat(outlets, notNullValue());
    }

    @Test
    @DbUnitDataSet(before = "outlets.csv")
    void smokeGetOutletInfosCount() {
        Map<Collection<ColumnsOutletInfo>, List<Object>> conditions = new HashMap<>();
        conditions.put(
                singletonList(ColumnsOutletInfo.DATASOURCE_ID),
                singletonList(1)
        );
        conditions.put(
                singletonList(ColumnsOutletInfo.IS_INLET),
                singletonList(true)
        );
        conditions.put(
                singletonList(ColumnsOutletInfo.HAS_ALCOHOL),
                singletonList(true)
        );
        outletInfoService.getOutletInfosCount(conditions);
    }
}
