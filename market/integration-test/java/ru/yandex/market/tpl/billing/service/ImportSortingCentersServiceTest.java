package ru.yandex.market.tpl.billing.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.client.billing.BillingClient;
import ru.yandex.market.tpl.client.billing.dto.BillingSortingCenterContainerDto;
import ru.yandex.market.tpl.client.billing.dto.BillingSortingCenterDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ImportSortingCentersServiceTest extends AbstractFunctionalTest {
    private static final double MOSCOW_LAT = 55.753215;
    private static final double MOSCOW_LON = 37.622504;
    private static final long MOSCOW_REG_ID = 213;
    private static final long MOSCOW_OBL_REG_ID = 1;
    private static final double LEN_OBL_VIL_LAT = 55.352235;
    private static final double LEN_OBL_VIL_LON = 27.869190;
    private static final long LEN_OBL_VIL_REG_ID = 205959;
    private static final long LEN_OBL_REG_ID = 10174;
    private static final double OTHER_LAT = 58.522810;
    private static final double OTHER_LON = 31.269915;
    private static final long OTHER_REG_ID = 24;
    private static final List<Long> SUBJECT_FEDERATION_REGION_IDS =
            List.of(MOSCOW_OBL_REG_ID, LEN_OBL_REG_ID, OTHER_REG_ID);

    @Autowired
    ImportSortingCentersService importSortingCentersService;

    @Autowired
    BillingClient billingClient;

    @Autowired
    HttpGeobase httpGeobase;

    @Test
    @DbUnitDataSet(
            before = "/database/service/importsortingcentersservice/before/sorting_centers.csv",
            after = "/database/service/importsortingcentersservice/after/sorting_centers.csv"
    )
    void importSortingCenters() {
        when(httpGeobase.getRegionId(MOSCOW_LAT, MOSCOW_LON)).thenReturn((int) MOSCOW_REG_ID);
        when(httpGeobase.getRegionId(LEN_OBL_VIL_LAT, LEN_OBL_VIL_LON)).thenReturn((int) LEN_OBL_VIL_REG_ID);
        when(httpGeobase.getRegionId(OTHER_LAT, OTHER_LON)).thenReturn((int) OTHER_REG_ID);
        when(billingClient.getRegionIdsOfSubjectFederationType(any()))
                .thenReturn(SUBJECT_FEDERATION_REGION_IDS);
        when(billingClient.findSortingCenters()).thenReturn(getSortingCenters());
        importSortingCentersService.importSortingCenters();
    }

    private BillingSortingCenterContainerDto getSortingCenters() {
        return BillingSortingCenterContainerDto
                .builder()
                .sortingCenters(
                        List.of(
                                BillingSortingCenterDto
                                        .builder()
                                        .id(1L)
                                        .name("Сортировочный центр России будущего")
                                        .latitude(MOSCOW_LAT)
                                        .longitude(MOSCOW_LON)
                                        .regionId(MOSCOW_REG_ID)
                                        .build(),
                                BillingSortingCenterDto
                                        .builder()
                                        .id(2L)
                                        .name("Сортировочный центр России будущего 2")
                                        .latitude(LEN_OBL_VIL_LAT)
                                        .longitude(LEN_OBL_VIL_LON)
                                        .regionId(LEN_OBL_VIL_REG_ID)
                                        .build(),
                                BillingSortingCenterDto
                                        .builder()
                                        .id(3L)
                                        .name("Сортировочный центр России будущего 3")
                                        .latitude(OTHER_LAT)
                                        .longitude(OTHER_LON)
                                        .regionId(OTHER_REG_ID)
                                        .build()
                        )
                )
                .build();
    }


}
