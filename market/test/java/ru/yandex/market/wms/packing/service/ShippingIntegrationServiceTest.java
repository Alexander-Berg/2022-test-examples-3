package ru.yandex.market.wms.packing.service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.packing.dao.LocSorterDao;
import ru.yandex.market.wms.packing.pojo.LocSorter;
import ru.yandex.market.wms.shippingsorter.client.ShippingsorterClient;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxId;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxStatus;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.PackStationId;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.request.SorterOrderRequest;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.response.SorterOrderCreationResponse;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.response.SorterOrderCreationStatus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShippingIntegrationServiceTest extends IntegrationTest {

    @Autowired
    private ShippingIntegrationService shippingIntegrationService;

    @MockBean
    @Autowired
    protected ShippingsorterClient shippingsorterClient;

    @MockBean
    @Autowired
    protected LocSorterDao locSorterDao;

    @BeforeEach
    public void reset() {
        Mockito.reset(shippingsorterClient);
        Mockito.reset(locSorterDao);
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/dao/drop-id/db_setup.xml")
    public void shouldSuccessCreate() {
        when(locSorterDao.getAdjacentSorterLoc("STAGE01")).thenReturn(Optional.of(LocSorter.builder().build()));
        when(shippingsorterClient.createSorterOrder(any()))
                .thenReturn(SorterOrderCreationResponse.ofOk("", null, null));

        SorterOrderCreationResponse response = shippingIntegrationService
                .createSorterOrder("P000000501", "STAGE01", Collections.emptyList());

        SorterOrderRequest expectedRequest = SorterOrderRequest.builder()
                .boxId(BoxId.of("P000000501"))
                .boxInfo(BoxInfo.builder()
                        .boxWeight(30200)
                        .boxWidth(new BigDecimal("60.00000"))
                        .boxHeight(new BigDecimal("40.00000"))
                        .boxLength(new BigDecimal("100.00000"))
                        .carrierCode("CARRIER-MP1")
                        .carrierName("DPD")
                        .operationDayId(18854L)
                        .boxStatus(BoxStatus.builder()
                                .isBoxDropped(false)
                                .isBoxLoaded(false)
                                .isBoxShipped(false)
                                .build())
                        .build())
                .packStationId(PackStationId.of("STAGE01"))
                .build();

        verify(shippingsorterClient, times(1)).createSorterOrder(expectedRequest);
        assertions.assertThat(response.getStatus()).isEqualTo(SorterOrderCreationStatus.OK);
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/service/shipping-integration/before-already-shipped.xml")
    public void shouldSuccessCreate_alreadyShipped() {
        when(locSorterDao.getAdjacentSorterLoc("STAGE01")).thenReturn(Optional.of(LocSorter.builder().build()));
        when(shippingsorterClient.createSorterOrder(any()))
                .thenReturn(SorterOrderCreationResponse.ofFailed("Box P000000501 already shipped"));

        SorterOrderCreationResponse response = shippingIntegrationService
                .createSorterOrder("P000000501", "STAGE01", Collections.emptyList());

        SorterOrderRequest expectedRequest = SorterOrderRequest.builder()
                .boxId(BoxId.of("P000000501"))
                .boxInfo(BoxInfo.builder()
                        .boxWeight(30200)
                        .boxWidth(new BigDecimal("60.00000"))
                        .boxHeight(new BigDecimal("40.00000"))
                        .boxLength(new BigDecimal("100.00000"))
                        .carrierCode("CARRIER-MP1")
                        .carrierName("DPD")
                        .operationDayId(18854L)
                        .boxStatus(BoxStatus.builder()
                                .isBoxDropped(false)
                                .isBoxLoaded(false)
                                .isBoxShipped(true)
                                .build())
                        .build())
                .packStationId(PackStationId.of("STAGE01"))
                .build();

        verify(shippingsorterClient, times(1)).createSorterOrder(expectedRequest);
        assertions.assertThat(response.getStatus()).isEqualTo(SorterOrderCreationStatus.FAILED);
    }


    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/service/shipping-integration/before-already-loaded.xml")
    public void shouldSuccessCreate_alreadyLoaded() {
        when(locSorterDao.getAdjacentSorterLoc("STAGE01")).thenReturn(Optional.of(LocSorter.builder().build()));
        when(shippingsorterClient.createSorterOrder(any()))
                .thenReturn(SorterOrderCreationResponse.ofOk("", null, null));

        SorterOrderCreationResponse response = shippingIntegrationService
                .createSorterOrder("P000000501", "STAGE01", Collections.emptyList());

        SorterOrderRequest expectedRequest = SorterOrderRequest.builder()
                .boxId(BoxId.of("P000000501"))
                .boxInfo(BoxInfo.builder()
                        .boxWeight(30200)
                        .boxWidth(new BigDecimal("60.00000"))
                        .boxHeight(new BigDecimal("40.00000"))
                        .boxLength(new BigDecimal("100.00000"))
                        .carrierCode("CARRIER-MP1")
                        .carrierName("DPD")
                        .operationDayId(18854L)
                        .boxStatus(BoxStatus.builder()
                                .isBoxDropped(false)
                                .isBoxLoaded(true)
                                .isBoxShipped(false)
                                .build())
                        .build())
                .packStationId(PackStationId.of("STAGE01"))
                .build();

        verify(shippingsorterClient, times(1)).createSorterOrder(expectedRequest);
        assertions.assertThat(response.getStatus()).isEqualTo(SorterOrderCreationStatus.OK);
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/service/shipping-integration/before-already-dropped.xml")
    public void shouldSuccessCreate_alreadyDropped() {
        when(locSorterDao.getAdjacentSorterLoc("STAGE01")).thenReturn(Optional.of(LocSorter.builder().build()));
        when(shippingsorterClient.createSorterOrder(any()))
                .thenReturn(SorterOrderCreationResponse.ofOk("", null, null));

        SorterOrderCreationResponse response = shippingIntegrationService
                .createSorterOrder("P000000501", "STAGE01", Collections.emptyList());

        SorterOrderRequest expectedRequest = SorterOrderRequest.builder()
                .boxId(BoxId.of("P000000501"))
                .boxInfo(BoxInfo.builder()
                        .boxWeight(30200)
                        .boxWidth(new BigDecimal("60.00000"))
                        .boxHeight(new BigDecimal("40.00000"))
                        .boxLength(new BigDecimal("100.00000"))
                        .carrierCode("CARRIER-MP1")
                        .carrierName("DPD")
                        .operationDayId(18854L)
                        .boxStatus(BoxStatus.builder()
                                .isBoxDropped(true)
                                .isBoxLoaded(false)
                                .isBoxShipped(false)
                                .build())
                        .build())
                .packStationId(PackStationId.of("STAGE01"))
                .build();

        verify(shippingsorterClient, times(1)).createSorterOrder(expectedRequest);
        assertions.assertThat(response.getStatus()).isEqualTo(SorterOrderCreationStatus.OK);
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/service/shipping-integration/before-box-status-all-false.xml")
    public void shouldNotSuccessCreateIfShippingReturnError() {
        when(locSorterDao.getAdjacentSorterLoc("STAGE01")).thenReturn(Optional.of(LocSorter.builder().build()));
        when(shippingsorterClient.createSorterOrder(any()))
                .thenThrow(
                        new WebClientResponseException(500, "Internal error occurred.", HttpHeaders.EMPTY, null, null)
                );

        SorterOrderCreationResponse response = shippingIntegrationService
                .createSorterOrder("P000000501", "STAGE01", Collections.emptyList());

        SorterOrderRequest expectedRequest = SorterOrderRequest.builder()
                .boxId(BoxId.of("P000000501"))
                .boxInfo(BoxInfo.builder()
                        .boxWeight(30200)
                        .boxWidth(new BigDecimal("60.00000"))
                        .boxHeight(new BigDecimal("40.00000"))
                        .boxLength(new BigDecimal("100.00000"))
                        .carrierCode("CARRIER-MP1")
                        .carrierName("DPD")
                        .operationDayId(18854L)
                        .boxStatus(BoxStatus.builder()
                                .isBoxDropped(false)
                                .isBoxLoaded(false)
                                .isBoxShipped(false)
                                .build())
                        .build())
                .packStationId(PackStationId.of("STAGE01"))
                .build();

        verify(shippingsorterClient, times(1)).createSorterOrder(expectedRequest);
        assertions.assertThat(response.getStatus()).isEqualTo(SorterOrderCreationStatus.FAILED);
    }

    @Test
    public void shouldNotSuccessCreateWithNotAdjacentTableLoc() {
        SorterOrderCreationResponse response = shippingIntegrationService
                .createSorterOrder("P00012448", "STAGE01", Collections.emptyList());

        assertions.assertThat(response.getStatus()).isEqualTo(SorterOrderCreationStatus.FAILED);
        assertions.assertThat(response.getMessage()).isEqualTo("Table loc STAGE01 is not adjacent for sorter");
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/service/shipping-integration/before-box-status-all-false.xml")
    public void getBoxStatus_notShipped() {
        BoxStatus expectedBoxStatus = BoxStatus.builder()
                .isBoxDropped(false)
                .isBoxLoaded(false)
                .isBoxShipped(false)
                .build();

        BoxStatus boxStatus = shippingIntegrationService.getBoxStatus("P000000501");

        Assertions.assertEquals(expectedBoxStatus, boxStatus);
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/service/shipping-integration/before-already-shipped.xml")
    public void isBoxShipped_shipped() {
        BoxStatus expectedBoxStatus = BoxStatus.builder()
                .isBoxDropped(false)
                .isBoxLoaded(false)
                .isBoxShipped(true)
                .build();

        BoxStatus boxStatus = shippingIntegrationService.getBoxStatus("P000000501");

        Assertions.assertEquals(expectedBoxStatus, boxStatus);
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/service/shipping-integration/before-already-dropped.xml")
    public void isBoxShipped_dropped() {
        BoxStatus expectedBoxStatus = BoxStatus.builder()
                .isBoxDropped(true)
                .isBoxLoaded(false)
                .isBoxShipped(false)
                .build();

        BoxStatus boxStatus = shippingIntegrationService.getBoxStatus("P000000501");

        Assertions.assertEquals(expectedBoxStatus, boxStatus);
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/service/shipping-integration/before-already-loaded.xml")
    public void isBoxShipped_loaded() {
        BoxStatus expectedBoxStatus = BoxStatus.builder()
                .isBoxDropped(false)
                .isBoxLoaded(true)
                .isBoxShipped(false)
                .build();

        BoxStatus boxStatus = shippingIntegrationService.getBoxStatus("P000000501");

        Assertions.assertEquals(expectedBoxStatus, boxStatus);
    }

    @Test
    @DatabaseSetup("/db/dao/loc-sorter/db_setup.xml")
    @DatabaseSetup("/db/service/shipping-integration/before-already-shipped.xml")
    public void isBoxShipped_emptyPickDetail() {
        BoxStatus expectedBoxStatus = BoxStatus.builder()
                .isBoxDropped(false)
                .isBoxLoaded(false)
                .isBoxShipped(true)
                .build();

        BoxStatus boxStatus = shippingIntegrationService.getBoxStatus("P000000502");

        Assertions.assertEquals(expectedBoxStatus, boxStatus);
    }
}
