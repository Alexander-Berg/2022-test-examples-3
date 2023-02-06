package ru.yandex.market.wms.shippingsorter.sorting.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.common.model.dto.TransportUnitId;
import ru.yandex.market.wms.common.model.dto.TransportUnitLocation;
import ru.yandex.market.wms.common.model.dto.TransportUnitTrackingDTO;
import ru.yandex.market.wms.common.model.enums.TransportUnitStatus;
import ru.yandex.market.wms.core.base.dto.DimensionDto;
import ru.yandex.market.wms.core.base.request.TransportUnitTrackingRequest;
import ru.yandex.market.wms.core.base.response.BoxDimensionsResponse;
import ru.yandex.market.wms.core.base.response.TransportUnitTrackingResponse;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxId;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.entity.ArrivalToNokEntity;
import ru.yandex.market.wms.shippingsorter.sorting.model.ArrivalToNokReason;
import ru.yandex.market.wms.shippingsorter.sorting.repository.SorterOrderRepository;
import ru.yandex.market.wms.shippingsorter.sorting.utils.DateTimeUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.math.BigDecimal.TEN;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.model.enums.TransportUnitStatus.ERROR_NOORDER;
import static ru.yandex.market.wms.common.model.enums.TransportUnitStatus.NOTIFICATION;
import static ru.yandex.market.wms.common.model.enums.TransportUnitStatus.OVERFLOW;
import static ru.yandex.market.wms.common.model.enums.TransportUnitStatus.ROUTE_NOT_FOUND;

@DatabaseSetup("/sorting/service/arrival-to-nok/immutable.xml")
@ExpectedDatabase(value = "/sorting/service/arrival-to-nok/immutable.xml", assertionMode = NON_STRICT)
@Import(ShippingSorterSecurityTestConfiguration.class)
public class ArrivalToNokServiceTest extends IntegrationTest {

    @Autowired
    @MockBean
    protected CoreClient coreClient;

    @Autowired
    private ArrivalToNokService arrivalToNokService;

    @Autowired
    private SorterOrderRepository sorterOrderRepository;

    @MockBean
    @Autowired
    @Qualifier("configPropertyPostgreSqlDao")
    private GlobalConfigurationDao configPropertyPostgreSqlDao;

    @BeforeEach
    protected void reset() {
        Mockito.reset(coreClient);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/not-completed-order/nok-to-nok-assigned-reason-exists.xml")
    @ExpectedDatabase(
            value = "/sorting/service/arrival-to-nok/not-completed-order/nok-to-nok-assigned-reason-exists.xml",
            assertionMode = NON_STRICT
    )
    public void determineArrivalReasonWhenSorterOrderFromNokToNokWithAssignedStatusAndLastReasonExists() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.OVERFLOW, 6693493L);
        expectedArrival.setId(1L);
        expectedArrival.setDeterminationTime(LocalDateTime.parse("2020-04-01T12:34:56.789"));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/not-completed-order/nok-to-nok-assigned-reason-not-exist.xml")
    @ExpectedDatabase(
            value = "/sorting/service/arrival-to-nok/not-completed-order/nok-to-nok-assigned-reason-not-exist.xml",
            assertionMode = NON_STRICT
    )
    public void determineArrivalReasonWhenSorterOrderFromNokToNokWithAssignedStatusAndLastReasonDoesNotExist() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.UNDEFINED, null);

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/not-completed-order/nok-to-nok-in-progress.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/not-completed-order/nok-to-nok-in-progress.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderFromNokToNokWithInProgressStatus() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_READ_OR_NO_SCAN, 6693494L);

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/not-completed-order/nok-to-not-nok-assigned.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/not-completed-order/nok-to-not-nok-assigned.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderFromNokToNotNokWithAssignedStatus() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_READ_OR_NO_SCAN, 6693494L);

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/not-completed-order/not-nok-to-nok-assigned.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/not-completed-order/not-nok-to-nok-assigned.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderFromNotNokToNokWithAssignedStatus() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_READ_OR_NO_SCAN, 6693494L);

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/not-completed-order/not-nok-to-not-nok-assigned.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/not-completed-order/not-nok-to-not-nok-assigned.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderFromNotNokToNotNokWithAssignedStatus() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_READ_OR_NO_SCAN, 6693494L);

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/canceled/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/canceled/immutable.xml", assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderWithCanceledStatus() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.CANCELED, 6693493L);

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/finished-to-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/finished-to-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNokWithFinishedStatus() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.TARGET_EXIT_IS_NOK, 6693493L);

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/finished-to-not-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/finished-to-not-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNotNokWithFinishedStatus() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.ERRONEOUS_FINISH_TO_EXIT, 6693493L);

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/failed-to-not-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/failed-to-not-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNotNokWithFailedStatus() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.STUCK_WMS_ORDER_FAILED, 6693493L);

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNokWithFailedStatusAndRouteNotFoundMessagesExist() {
        var boxId = BoxId.of("P123456780");
        var sorterOrder = sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId());
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.ROUTE_NOT_FOUND, 6693493L);
        var sorterOrderCreationTime = DateTimeUtils.fromDateTime(sorterOrder.getAddDate());
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(ROUTE_NOT_FOUND, OVERFLOW, NOTIFICATION, ERROR_NOORDER),
                DateTimeUtils.fromDateTime(sorterOrder.getAddDate())
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                        .externalZoneName("SHIPPING1")
                        .status(ROUTE_NOT_FOUND)
                        .build()
        )));
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN, TEN, TEN, TEN, sorterOrderCreationTime.plusSeconds(10))
        )));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNokWithFailedStatusAndNotificationsOnlyFromAnotherZonesExist() {
        var boxId = BoxId.of("P123456780");
        var sorterOrder = sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId());
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.BYPASS_OVERFLOW, 6693493L);
        var sorterOrderCreationTime = DateTimeUtils.fromDateTime(sorterOrder.getAddDate());
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(ROUTE_NOT_FOUND, OVERFLOW, NOTIFICATION, ERROR_NOORDER),
                DateTimeUtils.fromDateTime(sorterOrder.getAddDate())
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR2_NOK-01"))
                        .externalZoneName("SHIPPING2")
                        .status(NOTIFICATION)
                        .build()
        )));
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN, TEN, TEN, TEN, sorterOrderCreationTime.plusSeconds(10))
        )));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNokWithFailedStatusAndOverflowMessagesExist() {
        var boxId = BoxId.of("P123456780");
        var sorterOrder = sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId());
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.OVERFLOW, 6693493L);
        var sorterOrderCreationTime = DateTimeUtils.fromDateTime(sorterOrder.getAddDate());
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(ROUTE_NOT_FOUND, OVERFLOW, NOTIFICATION, ERROR_NOORDER),
                DateTimeUtils.fromDateTime(sorterOrder.getAddDate())
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_CH-05"))
                        .externalZoneName("SHIPPING1")
                        .status(OVERFLOW)
                        .build(),
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_CH-05"))
                        .externalZoneName("SHIPPING1")
                        .status(OVERFLOW)
                        .build()
        )));
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN, TEN, TEN, TEN, sorterOrderCreationTime.plusSeconds(10)),
                new DimensionDto(TEN, TEN, TEN, TEN, sorterOrderCreationTime.plusSeconds(70))
        )));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNokWithFailedStatusAndOverflowMessagesOnlyFromNokExist() {
        var boxId = BoxId.of("P123456780");
        var sorterOrder = sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId());
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.FAILED_TO_MOVE_TO_TARGET_EXIT, 6693493L);
        var sorterOrderCreationTime = DateTimeUtils.fromDateTime(sorterOrder.getAddDate());
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(ROUTE_NOT_FOUND, OVERFLOW, NOTIFICATION, ERROR_NOORDER),
                DateTimeUtils.fromDateTime(sorterOrder.getAddDate())
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_NOK-01"))
                        .externalZoneName("SHIPPING1")
                        .status(OVERFLOW)
                        .build(),
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_NOK-01"))
                        .externalZoneName("SHIPPING1")
                        .status(OVERFLOW)
                        .build()
        )));
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN, TEN, TEN, TEN, sorterOrderCreationTime.plusSeconds(10)),
                new DimensionDto(TEN, TEN, TEN, TEN, sorterOrderCreationTime.plusSeconds(70))
        )));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNokWithFailedStatusAndNoOrderTrackingExistAtCheckpoint() {
        var boxId = BoxId.of("P123456780");
        var sorterOrder = sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId());
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_VENDOR_ORDER, 6693493L);
        var sorterOrderCreationTime = DateTimeUtils.fromDateTime(sorterOrder.getAddDate());
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(ROUTE_NOT_FOUND, OVERFLOW, NOTIFICATION, ERROR_NOORDER),
                sorterOrderCreationTime
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.ERROR_NOORDER)
                        .build()
        )));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNokWithFailedStatusAndNoOrderTrackingNotExistAtCheckpoint() {
        var boxId = BoxId.of("P123456780");
        var sorterOrder = sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId());
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_READ_OR_NO_SCAN, 6693493L);
        var sorterOrderCreationTime = DateTimeUtils.fromDateTime(sorterOrder.getAddDate());
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(ROUTE_NOT_FOUND, OVERFLOW, NOTIFICATION, ERROR_NOORDER),
                sorterOrderCreationTime
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-02"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.ERROR_NOORDER)
                        .build(),
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.NOTIFICATION)
                        .build()
        )));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/failed-to-nok/immutable.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenSorterOrderToNokWithFailedStatusAndTrackingNotExistAtCheckpoint() {
        var boxId = BoxId.of("P123456780");
        var sorterOrder = sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId());
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_READ_OR_NO_SCAN, 6693493L);
        var sorterOrderCreationTime = DateTimeUtils.fromDateTime(sorterOrder.getAddDate());
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(ROUTE_NOT_FOUND, OVERFLOW, NOTIFICATION, ERROR_NOORDER),
                sorterOrderCreationTime
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-02"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.ERROR_NOORDER)
                        .build()
        )));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/no-order/noorder-not-exist-arrival-reason-exist.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/no-order/noorder-not-exist-arrival-reason-exist.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenNoOrderMessagesNotExistAndLastArrivalDetermined() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.OVERFLOW, 6693493L);
        expectedArrival.setId(1L);
        expectedArrival.setDeterminationTime(LocalDateTime.parse("2020-04-01T12:34:56.789"));
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(TransportUnitStatus.ERROR_NOORDER),
                Instant.parse("2020-04-01T12:34:56.789Z")
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of()));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/no-order/noorder-not-exist-arrival-reason-without-order-exist.xml")
    @ExpectedDatabase(
            value = "/sorting/service/arrival-to-nok/no-order/noorder-not-exist-arrival-reason-without-order-exist.xml",
            assertionMode = NON_STRICT
    )
    public void determineArrivalReasonWhenNoOrderMessagesNotExistAndLastArrivalWithoutOrderDetermined() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.OVERFLOW);
        expectedArrival.setId(1L);
        expectedArrival.setDeterminationTime(LocalDateTime.parse("2020-04-01T12:34:56.789"));
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(TransportUnitStatus.ERROR_NOORDER),
                Instant.parse("2020-04-01T12:34:56.789Z")
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of()));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/no-order/noorder-not-exist-arrival-reason-not-exist.xml")
    @ExpectedDatabase(value = "/sorting/service/arrival-to-nok/no-order/noorder-not-exist-arrival-reason-not-exist.xml",
            assertionMode = NON_STRICT)
    public void determineArrivalReasonWhenNoOrderMessagesNotExistAndLastArrivalNotDetermined() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_READ_OR_NO_SCAN);
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of()));

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/no-order/noorder-exist-weight-greater-than-upper-bound.xml")
    @ExpectedDatabase(
            value = "/sorting/service/arrival-to-nok/no-order/noorder-exist-weight-greater-than-upper-bound.xml",
            assertionMode = NON_STRICT
    )
    public void determineArrivalReasonWhenNoOrderMessagesExistAndWeightGreaterThanUpperBound() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.ERRONEOUS_WEIGHT_CALCULATED);
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.ERROR_NOORDER)
                        .build()
        )));
        mockConfig();

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/no-order/noorder-exist-weight-less-than-lower-bound.xml")
    @ExpectedDatabase(
            value = "/sorting/service/arrival-to-nok/no-order/noorder-exist-weight-less-than-lower-bound.xml",
            assertionMode = NON_STRICT
    )
    public void determineArrivalReasonWhenNoOrderMessagesExistAndWeightLessThanLowerBound() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.ERRONEOUS_WEIGHT_CALCULATED);
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.ERROR_NOORDER)
                        .build()
        )));
        mockConfig();

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/no-order/noorder-exist-dimensions-out-of-bounds.xml")
    @ExpectedDatabase(
            value = "/sorting/service/arrival-to-nok/no-order/noorder-exist-dimensions-out-of-bounds.xml",
            assertionMode = NON_STRICT
    )
    public void determineArrivalReasonWhenNoOrderMessagesExistAndDimensionsOutOfBounds() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.ERRONEOUS_DIMENSIONS_CALCULATED);
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.ERROR_NOORDER)
                        .build()
        )));
        mockConfig();

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/no-order/noorder-exist-arrival-reason-exist.xml")
    @ExpectedDatabase(
            value = "/sorting/service/arrival-to-nok/no-order/noorder-exist-arrival-reason-exist.xml",
            assertionMode = NON_STRICT
    )
    public void determineArrivalReasonWhenNoOrderMessagesExistAndAllDimensionsInBoundsAndLastArrivalDetermined() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_WMS_ORDER);
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(TransportUnitStatus.ERROR_NOORDER),
                Instant.parse("2020-04-01T12:34:56.789Z")
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.ERROR_NOORDER)
                        .build()
        )));
        mockConfig();

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/no-order/noorder-exist-arrival-reason-without-order-exist.xml")
    @ExpectedDatabase(
            value = "/sorting/service/arrival-to-nok/no-order/noorder-exist-arrival-reason-without-order-exist.xml",
            assertionMode = NON_STRICT
    )
    public void determineArrivalReasonWhenNoOrderMessagesExistAndLastArrivalWithoutOrderDetermined() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_WMS_ORDER);
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(TransportUnitStatus.ERROR_NOORDER),
                Instant.parse("2020-04-01T12:34:56.789Z")
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.ERROR_NOORDER)
                        .build()
        )));
        mockConfig();

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    @Test
    @DatabaseSetup("/sorting/service/arrival-to-nok/no-order/noorder-exist-arrival-reason-not-exist.xml")
    @ExpectedDatabase(
            value = "/sorting/service/arrival-to-nok/no-order/noorder-exist-arrival-reason-not-exist.xml",
            assertionMode = NON_STRICT
    )
    public void determineArrivalReasonWhenNoOrderMessagesExistAndAllDimensionsInBoundsAndLastArrivalNotDetermined() {
        var boxId = BoxId.of("P123456780");
        var expectedArrival = ArrivalToNokEntity.of(boxId, ArrivalToNokReason.NO_WMS_ORDER);
        var request = new TransportUnitTrackingRequest(
                boxId.getId(),
                List.of(TransportUnitStatus.ERROR_NOORDER),
                null
        );
        when(coreClient.getTracking(request)).thenReturn(new TransportUnitTrackingResponse(List.of(
                TransportUnitTrackingDTO.builder()
                        .transportUnitId(TransportUnitId.of(boxId.getId()))
                        .currentLocation(TransportUnitLocation.of("SR1_TP-01"))
                        .externalZoneName("SHIPPING1")
                        .status(TransportUnitStatus.ERROR_NOORDER)
                        .build()
        )));
        mockConfig();

        var arrival = arrivalToNokService.determineArrivalReason(boxId);

        Assertions.assertEquals(expectedArrival, arrival);
    }

    private void mockConfig() {
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_WEIGHT_GRAMS"))
                .thenReturn("30000.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MIN_WEIGHT_GRAMS")).thenReturn("50.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_WIDTH")).thenReturn("100.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_LENGTH")).thenReturn("90.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_HEIGHT")).thenReturn("80.0");
    }
}
