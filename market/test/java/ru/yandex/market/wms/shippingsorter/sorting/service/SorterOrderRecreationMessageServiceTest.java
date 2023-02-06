package ru.yandex.market.wms.shippingsorter.sorting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import ru.yandex.market.wms.shippingsorter.core.sorting.entity.BoxInfo;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.DimensionsDTO;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.response.ArrivalToNokReasonCode;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.response.RecommendedActionCode;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.response.RecommendedActionReasonCode;
import ru.yandex.market.wms.shippingsorter.core.sorting.model.response.SorterOrderRecreationMessage;
import ru.yandex.market.wms.shippingsorter.sorting.IntegrationTest;
import ru.yandex.market.wms.shippingsorter.sorting.entity.RecommendedActionEntity;
import ru.yandex.market.wms.shippingsorter.sorting.entity.SorterExitEntity;
import ru.yandex.market.wms.shippingsorter.sorting.entity.id.SorterExitId;
import ru.yandex.market.wms.shippingsorter.sorting.model.ArrivalToNokReason;
import ru.yandex.market.wms.shippingsorter.sorting.model.RecommendedActionReason;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static java.math.BigDecimal.TEN;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.CALCULATED_HEIGHT;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.CALCULATED_LENGTH;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.CALCULATED_WEIGHT;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.CALCULATED_WIDTH;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.DELIVERY_SERVICE;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.HEIGHT;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.LENGTH;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.MAX_HEIGHT;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.MAX_LENGTH;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.MAX_WEIGHT;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.MAX_WIDTH;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.MIN_WEIGHT;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.NOK_STATION;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.TARGET_EXIT;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.WEIGHT;
import static ru.yandex.market.wms.shippingsorter.sorting.constants.SorterOrderRecreationMessageParameter.WIDTH;
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

@DatabaseSetup("/sorting/service/recreation-message/immutable.xml")
@ExpectedDatabase(value = "/sorting/service/recreation-message/immutable.xml", assertionMode = NON_STRICT)
@Import(ShippingSorterSecurityTestConfiguration.class)
public class SorterOrderRecreationMessageServiceTest extends IntegrationTest {

    @Autowired
    @MockBean
    protected CoreClient coreClient;

    @Autowired
    private SorterOrderRecreationMessageService sorterOrderRecreationMessageService;

    @MockBean
    @Autowired
    @Qualifier("configPropertyPostgreSqlDao")
    private GlobalConfigurationDao configPropertyPostgreSqlDao;

    @BeforeEach
    protected void reset() {
        Mockito.reset(coreClient);
    }

    @Test
    public void composeMessageWhenActionContactSupervisorToSetupDeliveryService() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(
                CONTACT_SUPERVISOR_TO_SETUP_DELIVERY_SERVICE,
                SORTER_ORDER_IS_CREATED_TO_NOK
        );
        var arrivalReason = ArrivalToNokReason.NO_READ_OR_NO_SCAN;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.CONTACT_SUPERVISOR_TO_SETUP_DELIVERY_SERVICE)
                .recommendedActionReasonCode(RecommendedActionReasonCode.SORTER_ORDER_IS_CREATED_TO_NOK)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.NO_READ_OR_NO_SCAN)
                .dimensions(null)
                .params(Map.of(DELIVERY_SERVICE, "DPD"))
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    public void composeMessageWhenActionContactSupervisorForHelp() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(CONTACT_SUPERVISOR_FOR_HELP, SORTER_ORDER_RECREATION_ERROR);
        var arrivalReason = ArrivalToNokReason.NO_READ_OR_NO_SCAN;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.CONTACT_SUPERVISOR_FOR_HELP)
                .recommendedActionReasonCode(RecommendedActionReasonCode.SORTER_ORDER_RECREATION_ERROR)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.NO_READ_OR_NO_SCAN)
                .dimensions(null)
                .params(Collections.emptyMap())
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    public void composeMessageWhenActionContactSupport() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(CONTACT_SUPPORT, SORTER_ORDER_RECREATION_ERROR);
        var arrivalReason = ArrivalToNokReason.NO_READ_OR_NO_SCAN;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.CONTACT_SUPPORT)
                .recommendedActionReasonCode(RecommendedActionReasonCode.SORTER_ORDER_RECREATION_ERROR)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.NO_READ_OR_NO_SCAN)
                .dimensions(null)
                .params(Collections.emptyMap())
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    public void composeMessageWhenActionReprintAndPutOnConveyor() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(REPRINT_AND_PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var arrivalReason = ArrivalToNokReason.NO_READ_OR_NO_SCAN;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.REPRINT_AND_PUT_ON_CONVEYOR)
                .recommendedActionReasonCode(RecommendedActionReasonCode.LAST_ARRIVAL_TO_NOK_REASON)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.NO_READ_OR_NO_SCAN)
                .dimensions(null)
                .params(Collections.emptyMap())
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    public void composeMessageWhenActionPutOnConveyorFromAnotherZone() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(PUT_ON_CONVEYOR_FROM_ANOTHER_ZONE, LAST_ARRIVAL_TO_NOK_REASON);
        var arrivalReason = ArrivalToNokReason.ROUTE_NOT_FOUND;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.PUT_ON_CONVEYOR_FROM_ANOTHER_ZONE)
                .recommendedActionReasonCode(RecommendedActionReasonCode.LAST_ARRIVAL_TO_NOK_REASON)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.ROUTE_NOT_FOUND)
                .dimensions(null)
                .params(Map.of(NOK_STATION, "SR2_NOK-01", TARGET_EXIT, "SR2_CH-05"))
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit("SR2_CH-05", "SSORT_ZN_2"));
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    public void composeMessageWhenActionManualSort() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(MANUAL_SORT, LAST_ARRIVAL_TO_NOK_REASON);
        var arrivalReason = ArrivalToNokReason.STUCK_WMS_ORDER_FAILED;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.MANUAL_SORT)
                .recommendedActionReasonCode(RecommendedActionReasonCode.LAST_ARRIVAL_TO_NOK_REASON)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.STUCK_WMS_ORDER_FAILED)
                .dimensions(null)
                .params(Collections.emptyMap())
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OVERFLOW", "BYPASS_OVERFLOW", "NO_WMS_ORDER", "NO_VENDOR_ORDER",
            "ERRONEOUS_FINISH_TO_EXIT", "TARGET_EXIT_IS_NOK", "CANCELED", "FAILED_TO_MOVE_TO_TARGET_EXIT", "UNDEFINED"})
    public void composeMessageWhenActionPutOnConveyor(String arrivalToNokReasonString) {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var arrivalReason = ArrivalToNokReason.valueOf(arrivalToNokReasonString);
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.PUT_ON_CONVEYOR)
                .recommendedActionReasonCode(RecommendedActionReasonCode.LAST_ARRIVAL_TO_NOK_REASON)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.valueOf(arrivalToNokReasonString))
                .dimensions(null)
                .params(Collections.emptyMap())
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    public void composeMessageWhenActionPutOnConveyorAndErroneousWeightWasCalculated() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var arrivalReason = ArrivalToNokReason.ERRONEOUS_WEIGHT_CALCULATED;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.PUT_ON_CONVEYOR)
                .recommendedActionReasonCode(RecommendedActionReasonCode.LAST_ARRIVAL_TO_NOK_REASON)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.ERRONEOUS_WEIGHT_CALCULATED)
                .dimensions(null)
                .params(Map.of(CALCULATED_WEIGHT, BigDecimal.valueOf(25000)))
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    public void composeMessageWhenActionPutOnConveyorAndErroneousDimensionsWereCalculated() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(PUT_ON_CONVEYOR, LAST_ARRIVAL_TO_NOK_REASON);
        var arrivalReason = ArrivalToNokReason.ERRONEOUS_DIMENSIONS_CALCULATED;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.PUT_ON_CONVEYOR)
                .recommendedActionReasonCode(RecommendedActionReasonCode.LAST_ARRIVAL_TO_NOK_REASON)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.ERRONEOUS_DIMENSIONS_CALCULATED)
                .dimensions(null)
                .params(Map.of(
                        CALCULATED_WIDTH, BigDecimal.valueOf(15),
                        CALCULATED_HEIGHT, BigDecimal.valueOf(25),
                        CALCULATED_LENGTH, BigDecimal.valueOf(35)))
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    public void composeMessageWhenActionManualSortAndWeightNotSuitable() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(MANUAL_SORT, WEIGHT_NOT_SUITABLE_FOR_CONVEYOR);
        var arrivalReason = ArrivalToNokReason.NO_READ_OR_NO_SCAN;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.MANUAL_SORT)
                .recommendedActionReasonCode(RecommendedActionReasonCode.WEIGHT_NOT_SUITABLE_FOR_CONVEYOR)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.NO_READ_OR_NO_SCAN)
                .dimensions(DimensionsDTO.builder()
                        .weight(BigDecimal.valueOf(30100))
                        .width(TEN)
                        .height(TEN)
                        .length(TEN)
                        .build())
                .params(Map.of(
                        MIN_WEIGHT, BigDecimal.valueOf(50.0),
                        MAX_WEIGHT, BigDecimal.valueOf(30000.0),
                        WEIGHT, BigDecimal.valueOf(30100)
                ))
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        mockConfig();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(BigDecimal.valueOf(30100), TEN, TEN, TEN, Instant.MIN)
        )));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @Test
    public void composeMessageWhenActionManualSortAndDimensionsNotSuitable() {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(MANUAL_SORT, DIMENSIONS_NOT_SUITABLE_FOR_CONVEYOR);
        var arrivalReason = ArrivalToNokReason.NO_READ_OR_NO_SCAN;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.MANUAL_SORT)
                .recommendedActionReasonCode(RecommendedActionReasonCode.DIMENSIONS_NOT_SUITABLE_FOR_CONVEYOR)
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.NO_READ_OR_NO_SCAN)
                .dimensions(DimensionsDTO.builder()
                        .weight(TEN)
                        .width(BigDecimal.valueOf(30.1))
                        .height(TEN)
                        .length(TEN)
                        .build())
                .params(Map.of(
                        MAX_WIDTH, BigDecimal.valueOf(30.0),
                        MAX_HEIGHT, BigDecimal.valueOf(31.0),
                        MAX_LENGTH, BigDecimal.valueOf(32.0),
                        WIDTH, BigDecimal.valueOf(30.1),
                        HEIGHT, BigDecimal.valueOf(10),
                        LENGTH, BigDecimal.valueOf(10)))
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        mockConfig();
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of(
                new DimensionDto(TEN, BigDecimal.valueOf(30.1), TEN, TEN, Instant.MIN)
        )));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMMON_THRESHOLD_EXCEEDED", "OVERFLOW_THRESHOLD_EXCEEDED",
            "NO_READ_OR_NO_SCAN_THRESHOLD_EXCEEDED", "ERRONEOUS_FINISH_TO_EXIT_THRESHOLD_EXCEEDED"})
    public void composeMessageWhenActionManualSortAndThresholdExceeded(String recommendedActionString) {
        var boxId = BoxId.of("P123456780");
        var action = RecommendedActionEntity.of(MANUAL_SORT, RecommendedActionReason.valueOf(recommendedActionString));
        var arrivalReason = ArrivalToNokReason.NO_READ_OR_NO_SCAN;
        var expectedMessage = SorterOrderRecreationMessage.builder()
                .recommendedActionCode(RecommendedActionCode.MANUAL_SORT)
                .recommendedActionReasonCode(RecommendedActionReasonCode.valueOf(recommendedActionString))
                .arrivalToNokReasonCode(ArrivalToNokReasonCode.NO_READ_OR_NO_SCAN)
                .dimensions(null)
                .params(Collections.emptyMap())
                .build();
        var boxInfo = Optional.of(makeBoxInfo());
        var previousBoxInfo = Optional.of(makePreviousBoxInfo());
        var targetExit = Optional.of(makeTargetExit());
        when(coreClient.getBoxDimensions(boxId.getId())).thenReturn(new BoxDimensionsResponse(List.of()));

        var message = sorterOrderRecreationMessageService
                .composeMessage(boxId, boxInfo, previousBoxInfo, targetExit, action, arrivalReason);

        Assertions.assertEquals(expectedMessage, message);
    }

    private SorterExitEntity makeTargetExit() {
        return makeTargetExit("SR1_CH-05", "SSORT_ZONE");
    }

    private SorterExitEntity makeTargetExit(String location, String zone) {
        return SorterExitEntity.builder()
                .sorterExitId(SorterExitId.of(location))
                .conveyorLoc(location)
                .zone(zone)
                .build();
    }

    private BoxInfo makeBoxInfo() {
        return BoxInfo.builder()
                .boxWeight(15000)
                .boxWidth(BigDecimal.valueOf(10))
                .boxHeight(BigDecimal.valueOf(20))
                .boxLength(BigDecimal.valueOf(30))
                .carrierCode("171")
                .carrierName("DPD")
                .build();
    }

    private BoxInfo makePreviousBoxInfo() {
        return BoxInfo.builder()
                .boxWeight(25000)
                .boxWidth(BigDecimal.valueOf(15))
                .boxHeight(BigDecimal.valueOf(25))
                .boxLength(BigDecimal.valueOf(35))
                .carrierCode("171")
                .carrierName("DPD")
                .build();
    }

    private void mockConfig() {
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_WEIGHT_GRAMS"))
                .thenReturn("30000.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MIN_WEIGHT_GRAMS")).thenReturn("50.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_WIDTH")).thenReturn("30.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_HEIGHT")).thenReturn("31.0");
        when(configPropertyPostgreSqlDao.getStringConfigValue("PACKING_MAX_LENGTH")).thenReturn("32.0");
    }
}
