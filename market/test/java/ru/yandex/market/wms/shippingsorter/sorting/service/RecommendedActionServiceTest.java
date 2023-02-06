package ru.yandex.market.wms.shippingsorter.sorting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import ru.yandex.market.wms.core.base.dto.DimensionDto;
import ru.yandex.market.wms.core.base.response.BoxDimensionsResponse;
import ru.yandex.market.wms.core.client.CoreClient;
import ru.yandex.market.wms.shared.libs.configproperties.dao.GlobalConfigurationDao;
import ru.yandex.market.wms.shippingsorter.configuration.ShippingSorterSecurityTestConfiguration;
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxId;
import ru.yandex.market.wms.shippingsorter.core.sorting.exception.WmsErrorCode;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.entity.RecommendedActionEntity;
import ru.yandex.market.wms.shippingsorter.sorting.exception.WmsError;
import ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedActionReason;
import ru.yandex.market.wms.shippingsorter.sorting.repository.ArrivalToNokRepository;
import ru.yandex.market.wms.shippingsorter.sorting.repository.SorterOrderRepository;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedAction.CONTACT_SUPERVISOR_FOR_HELP;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedAction.CONTACT_SUPERVISOR_TO_SETUP_DELIVERY_SERVICE;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedAction.CONTACT_SUPPORT;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedAction.MANUAL_SORT;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedAction.PUT_ON_CONVEYOR;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedAction.PUT_ON_CONVEYOR_FROM_ANOTHER_ZONE;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedAction.REPRINT_AND_PUT_ON_CONVEYOR;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedActionReason.DIMENSIONS_NOT_SUITABLE_FOR_CONVEYOR;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedActionReason.LAST_ARRIVAL_TO_NOK_REASON;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedActionReason.SORTER_ORDER_IS_CREATED_TO_NOK;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedActionReason.SORTER_ORDER_RECREATION_ERROR;
import static ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedActionReason.WEIGHT_NOT_SUITABLE_FOR_CONVEYOR;

@DatabaseSetup("/sorting/service/recommended-action/immutable.xml")
@ExpectedDatabase(value = "/sorting/service/recommended-action/immutable.xml", assertionMode = NON_STRICT)
@Import(ShippingSorterSecurityTestConfiguration.class)
public class RecommendedActionServiceTest extends IntegrationTest {

    private static final BigDecimal TEN_THOUSANDS = BigDecimal.valueOf(10000);

    @Autowired
    @MockBean
    protected CoreClient coreClient;

    @Autowired
    private RecommendedActionService recommendedActionService;

    @Autowired
    private SorterOrderRepository sorterOrderRepository;

    @Autowired
    private ArrivalToNokRepository arrivalToNokRepository;

    @MockBean
    @Autowired
    @Qualifier("configPropertyPostgreSqlDao")
    private GlobalConfigurationDao configPropertyPostgreSqlDao;

    @BeforeEach
    protected void reset() {
        Mockito.reset(coreClient);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-created-to-exit.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-created-to-exit.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenNoDimensionsAreMeasured() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-created-to-exit.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-created-to-exit.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenAllMeasuredDimensionsAreIncorrect() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(ZERO, TEN, TEN, TEN, Instant.parse("2020-04-01T12:31:56.789Z")),
                new DimensionDto(TEN_THOUSANDS, ZERO, ZERO, ZERO, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-created-to-exit.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-created-to-exit.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenLastMeasuredDimensionsAreValid() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(BigDecimal.valueOf(31000), TEN, TEN, TEN, Instant.parse("2020-04-01T12:31:56.789Z")),
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-created-to-exit.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-created-to-exit.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenLastMeasuredDimensionsAreWithInvalidWeight() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(MANUAL_SORT, WEIGHT_NOT_SUITABLE_FOR_CONVEYOR);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:31:56.789Z")),
                new DimensionDto(BigDecimal.valueOf(31000), TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @ParameterizedTest
    @CsvSource({"100.1,10,10", "10,80.1,10", "10,10,90.1"})
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-created-to-exit.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-created-to-exit.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenLastMeasuredDimensionsAreWithInvalidDimension(
            BigDecimal width,
            BigDecimal height,
            BigDecimal length
    ) {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(MANUAL_SORT, DIMENSIONS_NOT_SUITABLE_FOR_CONVEYOR);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:31:56.789Z")),
                new DimensionDto(TEN_THOUSANDS, width, height, length, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-created-to-exit.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-created-to-exit.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenLastMeasuredDimensionsAreInvalid() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(MANUAL_SORT, WEIGHT_NOT_SUITABLE_FOR_CONVEYOR);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:31:56.789Z")),
                new DimensionDto(
                        BigDecimal.valueOf(31000),
                        BigDecimal.valueOf(100.1),
                        BigDecimal.valueOf(80.1),
                        BigDecimal.valueOf(90.1),
                        Instant.parse("2020-04-01T12:33:56.789Z")
                )
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @ParameterizedTest
    @CsvSource({"2,2,2,3,OVERFLOW_THRESHOLD_EXCEEDED", "3,2,2,3,NO_READ_OR_NO_SCAN_THRESHOLD_EXCEEDED",
            "3,3,2,3,ERRONEOUS_FINISH_TO_EXIT_THRESHOLD_EXCEEDED", "3,3,3,6,COMMON_THRESHOLD_EXCEEDED"})
    @DatabaseSetup("/sorting/service/recommended-action/arrivals-count-thresholds-exceeded.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/arrivals-count-thresholds-exceeded.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenArrivalCountWithOverflowThresholdExceeded(
            int overflowThreshold,
            int noReadOrNoScanThreshold,
            int erroneousFinishToExitThreshold,
            int commonThreshold,
            RecommendedActionReason expectedActionReason
    ) {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(MANUAL_SORT, expectedActionReason);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig(overflowThreshold, noReadOrNoScanThreshold, erroneousFinishToExitThreshold, commonThreshold);

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-created-to-nok.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-created-to-nok.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenSorterOrderIsCreatedToNok() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(
                CONTACT_SUPERVISOR_TO_SETUP_DELIVERY_SERVICE,
                SORTER_ORDER_IS_CREATED_TO_NOK
        );
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @ParameterizedTest
    @CsvSource({"ALTERNATE_SORTER_EXIT_NOT_FOUND", "BOX_WEIGHT_EXCEEDED"})
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-creation-failed.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-creation-failed.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenSorterOrderCreationFailedAndSupportCanHelpWithError(WmsErrorCode errorCode) {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(CONTACT_SUPPORT, SORTER_ORDER_RECREATION_ERROR);
        var creationError = Optional.of(new WmsError("error", errorCode, Collections.emptyMap()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, Optional.empty(), creationError, arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-creation-failed.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-creation-failed.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenSorterOrderCreationFailedAndSupervisorCanHelpWithError() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(CONTACT_SUPERVISOR_FOR_HELP, SORTER_ORDER_RECREATION_ERROR);
        var creationError = Optional.of(new WmsError("error", WmsErrorCode.UNDEFINED, Collections.emptyMap()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, Optional.empty(), creationError, arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @ParameterizedTest
    @CsvSource({"WEIGHT_GREATER_THAN_POSSIBLE", "WEIGHT_LESS_THAN_POSSIBLE", "WIDTH_GREATER_THAN_POSSIBLE",
            "LENGTH_GREATER_THAN_POSSIBLE", "HEIGHT_GREATER_THAN_POSSIBLE"})
    @DatabaseSetup("/sorting/service/recommended-action/sorter-order-creation-failed.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/sorter-order-creation-failed.xml",
            assertionMode = NON_STRICT)
    public void determineActionWhenSorterOrderCreationFailedAndCalculatedDimensionsNotValid(WmsErrorCode errorCode) {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(MANUAL_SORT, SORTER_ORDER_RECREATION_ERROR);
        var creationError = Optional.of(new WmsError("validation failed", errorCode, Collections.emptyMap()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, Optional.empty(), creationError, arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-no-read-or-no-scan-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-no-read-or-no-scan-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByNoReadOrNoScanReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(REPRINT_AND_PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-route-not-found-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-route-not-found-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByRouteNotFoundReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR_FROM_ANOTHER_ZONE, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-stuck-wms-order-failed-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-stuck-wms-order-failed-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByStuckWmsOrderFailedReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(MANUAL_SORT, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-failed-to-move-to-target-exit-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-failed-to-move-to-target-exit-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByFailedToMoveToTargetExitReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-overflow-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-overflow-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByOverflowReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-bypass-overflow-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-bypass-overflow-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByBypassOverflowReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-no-wms-order-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-no-wms-order-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByNoWmsOrderReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-no-vendor-order-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-no-vendor-order-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByNoVendorOrderReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-erroneous-dimensions-calculated-arrival-reason.xml")
    @ExpectedDatabase(
            value = "/sorting/service/recommended-action/by-erroneous-dimensions-calculated-arrival-reason.xml",
            assertionMode = NON_STRICT
    )
    public void determineActionByErroneousDimensionsCalculatedReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-erroneous-weight-calculated-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-erroneous-weight-calculated-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByErroneousWeightCalculatedReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-erroneous-finish-to-exit-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-erroneous-finish-to-exit-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByErroneousFinishToExitReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-canceled-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-canceled-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByCanceledReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-undefined-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-undefined-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByUndefinedReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    @Test
    @DatabaseSetup("/sorting/service/recommended-action/by-target-exit-is-nok-arrival-reason.xml")
    @ExpectedDatabase(value = "/sorting/service/recommended-action/by-target-exit-is-nok-arrival-reason.xml",
            assertionMode = NON_STRICT)
    public void determineActionByTargetExitIsNokReasonWhenSorterOrderIsCreated() {
        var boxId = BoxId.of("P123456780");
        var expectedAction = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var sorterOrder = Optional.of(sorterOrderRepository.tryGetLastOrderByBoxId(boxId.getId()));
        var arrival = arrivalToNokRepository.findLastByBoxId(boxId.getId()).orElseThrow();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN_THOUSANDS, TEN, TEN, TEN, Instant.parse("2020-04-01T12:33:56.789Z"))
        )));
        mockConfig();

        var action = recommendedActionService.determineAction(boxId, sorterOrder, Optional.empty(), arrival);

        Assertions.assertEquals(expectedAction, action);
    }

    private void mockConfig() {
         mockConfig(2, 2, 2, 3);
    }

    private void mockConfig(
            int overflowThreshold,
            int noReadOrNoScanThreshold,
            int erroneousFinishToExitThreshold,
            int commonThreshold
    ) {
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_WEIGHT_GRAMS"))
                .thenReturn("30000.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MIN_WEIGHT_GRAMS")).thenReturn("50");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_WIDTH")).thenReturn("100.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_LENGTH")).thenReturn("90.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_HEIGHT")).thenReturn("80.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("OVERFLOW_THRESHOLD"))
                .thenReturn(Integer.toString(overflowThreshold));
        when(configPropertyPostgreSqlDao.getStringConfigValue("NO_READ_OR_NO_SCAN_THRESHOLD"))
                .thenReturn(Integer.toString(noReadOrNoScanThreshold));
        when(configPropertyPostgreSqlDao.getStringConfigValue("ERRONEOUS_FINISH_THRESHOLD"))
                .thenReturn(Integer.toString(erroneousFinishToExitThreshold));
        when(configPropertyPostgreSqlDao.getStringConfigValue("COMMON_THRESHOLD"))
                .thenReturn(Integer.toString(commonThreshold));
    }
}
