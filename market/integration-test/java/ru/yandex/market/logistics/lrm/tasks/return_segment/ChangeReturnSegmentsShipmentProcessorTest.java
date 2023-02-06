package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.base.EventPayload;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.dto.CargoUnitDto;
import ru.yandex.market.logistics.les.dto.CargoUnitGroupDto;
import ru.yandex.market.logistics.les.dto.CargoUnitGroupType;
import ru.yandex.market.logistics.les.dto.CarrierDto;
import ru.yandex.market.logistics.les.dto.CarrierType;
import ru.yandex.market.logistics.les.dto.CodeDto;
import ru.yandex.market.logistics.les.dto.CodeType;
import ru.yandex.market.logistics.les.dto.OutboundShipmentDto;
import ru.yandex.market.logistics.les.dto.PartnerDto;
import ru.yandex.market.logistics.les.dto.PointDto;
import ru.yandex.market.logistics.les.tpl.StorageUnitUpdateSegmentRequestEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.BusinessProcessType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ChangeReturnSegmentsShipmentReason;
import ru.yandex.market.logistics.lrm.model.entity.enums.LogisticPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentRecipientType;
import ru.yandex.market.logistics.lrm.queue.payload.ChangeReturnSegmentsShipmentPayload;
import ru.yandex.market.logistics.lrm.queue.payload.LogisticPointDto;
import ru.yandex.market.logistics.lrm.queue.payload.RecipientDto;
import ru.yandex.market.logistics.lrm.queue.processor.ChangeReturnSegmentsShipmentProcessor;
import ru.yandex.market.logistics.lrm.repository.ReturnRepository;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsApiFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@DisplayName("Изменение отгрузки возвратных сегментов")
@DatabaseSetup("/database/tasks/return-segment/change-return-segments-shipment/before/prepare.xml")
class ChangeReturnSegmentsShipmentProcessorTest extends AbstractIntegrationTest {

    private static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");
    private static final Long RETURN_ID = 1L;
    private static final Long DELIVERY_PARTNER_ID = 111L;
    private static final Long CURRENT_PARTNER_ID = 222L;
    private static final Long CURRENT_LOGISTIC_POINT_ID = 20L;
    private static final Long OUTBOUND_PARTNER_ID = 333L;
    private static final Long OUTBOUND_LOGISTIC_POINT_ID = 30L;

    @Autowired
    private ChangeReturnSegmentsShipmentProcessor processor;
    @Autowired
    private ReturnRepository returnRepository;
    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private LesProducer lesProducer;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, lesProducer);
    }

    @Test
    @DisplayName("Пустой список сегментов")
    void emptySegmentsList() {
        execute(List.of());

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=WARN\t\
                    format=plain\t\
                    payload=Segments list is empty\t\
                    request_id=test-request-id\
                    """
            );
    }

    @Test
    @DisplayName("Сегменты принадлежат разным возвратам")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/change-return-segments-shipment/before/another_return.xml",
        type = DatabaseOperation.REFRESH
    )
    void differentReturns() {
        softly.assertThatThrownBy(() -> execute(List.of(11L, 21L)))
            .hasMessage("Expected all segments to have same returnEntity");
    }

    @Test
    @DisplayName("У сегментов различные логистические точки")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/change-return-segments-shipment/before/different_points.xml",
        type = DatabaseOperation.UPDATE
    )
    void differentLogisticPoints() {
        softly.assertThatThrownBy(() -> execute(List.of(11L, 21L)))
            .hasMessage("Expected all segments to have same logisticPoint");
    }

    @Test
    @DisplayName("Сегмента не существует")
    void segmentNotFound() {
        softly.assertThatThrownBy(() -> execute(List.of(11L, 404L)))
            .hasMessage("Failed to find segments from payload");
    }

    @EnumSource(
        value = ReturnStatus.class,
        names = {"DELIVERED", "FULFILMENT_RECEIVED", "CANCELLED", "EXPIRED"},
        mode = EnumSource.Mode.INCLUDE
    )
    @ParameterizedTest
    @DisplayName("Неверный статус возврата, возврат в терминальном статусе")
    void returnInFinalStatus(ReturnStatus returnStatus) {
        setReturnStatus(returnStatus);

        execute(List.of(11L, 21L));

        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                """
                    level=INFO\t\
                    format=plain\t\
                    payload=Expected return to be in status READY_FOR_IM but was %s\t\
                    request_id=test-request-id\
                    """.formatted(returnStatus)
            );
    }

    @EnumSource(
        value = ReturnStatus.class,
        names = {"READY_FOR_IM", "DELIVERED", "FULFILMENT_RECEIVED", "CANCELLED", "EXPIRED"},
        mode = EnumSource.Mode.EXCLUDE
    )
    @ParameterizedTest
    @DisplayName("Неверный статус возврата, но возможно статус изменится на READY_FOR_IM")
    void wrongReturnStatus(ReturnStatus returnStatus) {
        setReturnStatus(returnStatus);

        softly.assertThatThrownBy(() -> execute(List.of(11L, 21L)))
            .hasMessage("Expected return to be in status READY_FOR_IM but was %s".formatted(returnStatus));
    }

    @Test
    @DisplayName("Текущая логистическая точка не найдена в LMS")
    void noCurrentLogisticPoint() throws Exception {
        try (var ignored1 = mockLogisticsPoint(null)) {
            softly.assertThatThrownBy(() -> execute(List.of(11L, 21L)))
                .hasMessage("Failed to find LOGISTICS_POINT with id [20]");
        }
    }

    @Test
    @DisplayName("Партнер текущей логистической точки не найден в LMS")
    void noCurrentPartner() throws Exception {
        try (
            var ignored1 = mockLogisticsPoint();
            var ignored2 = mockPartner(null)
        ) {
            softly.assertThatThrownBy(() -> execute(List.of(11L, 21L)))
                .hasMessage("Failed to find PARTNER with id [222]");
        }
    }

    @Test
    @DisplayName("Настройки партнёра не найдены в LMS")
    void noApiSettings() throws Exception {
        try (
            var ignored1 = mockLogisticsPoint();
            var ignored2 = mockPartner();
            var ignored3 = mockApiSettings(List.of())
        ) {
            softly.assertThatCode(() -> execute(List.of(11L, 21L)))
                .hasMessage("No FF API token for partner 222")
                .isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @DisplayName("Успех")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-segments-shipment/after/bp_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void success() throws Exception {
        try (
            var ignored1 = mockLogisticsPoint();
            var ignored2 = mockPartner();
            var ignored3 = mockApiSettings()
        ) {
            execute(List.of(11L, 21L));
        }

        verify(lesProducer).send(
            getExpectedEvent(
                List.of(
                    getCargoUnit(1L, "box-external-id-1", "segment-11"),
                    getCargoUnit(2L, "box-external-id-2", "segment-21")
                )
            ),
            OUT_LES_QUEUE
        );
    }

    @Nonnull
    private Event getExpectedEvent(List<CargoUnitDto> cargoUnits) {
        return new Event(
            SOURCE_FOR_LES,
            "e11c5e64-3694-40c9-b9b4-126efedaa098",
            FIXED_TIME.toEpochMilli(),
            BusinessProcessType.CHANGE_RETURN_SEGMENTS_SHIPMENT.name(),
            getExpectedEventPayload(cargoUnits),
            ""
        );
    }

    @Nonnull
    private EventPayload getExpectedEventPayload(List<CargoUnitDto> cargoUnits) {
        return new StorageUnitUpdateSegmentRequestEvent(
            "test-request-id/1",
            new PartnerDto(
                CURRENT_PARTNER_ID,
                "partner-token",
                CURRENT_LOGISTIC_POINT_ID.toString()
            ),
            cargoUnits,
            new CargoUnitGroupDto(
                "RETURN_CLIENT_" + RETURN_ID,
                "return-external-id",
                new CodeDto("order-external-id", CodeType.ORDER_BARCODE),
                BigDecimal.valueOf(10000L),
                cargoUnits.size(),
                true,
                CargoUnitGroupType.RETURN_CLIENT
            )
        );
    }

    @Nonnull
    private CargoUnitDto getCargoUnit(Long boxId, String boxExternalId, String segmentUUID) {
        return new CargoUnitDto(
            boxId.toString(),
            FIXED_TIME.toEpochMilli(),
            segmentUUID,
            null,
            "RETURN_CLIENT_1",
            List.of(new CodeDto(boxExternalId, CodeType.CARGO_BARCODE)),
            null,
            null,
            null,
            null,
            new OutboundShipmentDto(
                FIXED_TIME,
                new CarrierDto(
                    CarrierType.DELIVERY_SERVICE,
                    DELIVERY_PARTNER_ID,
                    "delivery-service",
                    null
                ),
                new PointDto(
                    ru.yandex.market.logistics.les.dto.PointType.SORTING_CENTER,
                    OUTBOUND_PARTNER_ID,
                    null,
                    "sc-30"
                )
            )
        );
    }

    private void setReturnStatus(ReturnStatus returnStatus) {
        transactionTemplate.execute(transactionStatus -> {
            returnRepository.findById(RETURN_ID).orElseThrow().setStatus(returnStatus, clock);
            return null;
        });
    }

    private void execute(List<Long> segmentIds) {
        processor.execute(ChangeReturnSegmentsShipmentPayload.builder()
            .returnId(RETURN_ID)
            .returnSegmentIds(segmentIds)
            .nextLogisticPoint(
                LogisticPointDto.builder()
                    .id(OUTBOUND_LOGISTIC_POINT_ID)
                    .partnerId(OUTBOUND_PARTNER_ID)
                    .externalId("logistic-point-external-id-30")
                    .name("sc-30")
                    .type(LogisticPointType.SORTING_CENTER)
                    .build()
            )
            .recipient(
                RecipientDto.builder()
                    .type(ShipmentRecipientType.DELIVERY_SERVICE)
                    .partnerId(DELIVERY_PARTNER_ID)
                    .name("delivery-service")
                    .courier(null)
                    .build()
            )
            .reason(ChangeReturnSegmentsShipmentReason.WAITING_FOR_SHOP_EXPIRED)
            .build()
        );
    }

    @Nonnull
    private AutoCloseable mockLogisticsPoint() {
        return mockLogisticsPoint(
            LogisticsPointResponse.newBuilder()
                .id(CURRENT_LOGISTIC_POINT_ID)
                .partnerId(CURRENT_PARTNER_ID)
                .externalId("logistics-point-external-id-20")
                .name("sc-20")
                .type(PointType.WAREHOUSE)
                .build()
        );
    }

    @Nonnull
    private AutoCloseable mockLogisticsPoint(@Nullable LogisticsPointResponse logisticsPoint) {
        when(lmsClient.getLogisticsPoint(CURRENT_LOGISTIC_POINT_ID))
            .thenReturn(Optional.ofNullable(logisticsPoint));

        return () -> verify(lmsClient).getLogisticsPoint(CURRENT_LOGISTIC_POINT_ID);
    }

    @Nonnull
    private AutoCloseable mockPartner() {
        return mockPartner(
            PartnerResponse.newBuilder()
                .id(CURRENT_PARTNER_ID)
                .name("current-partner")
                .partnerType(PartnerType.SORTING_CENTER)
                .build()
        );
    }

    @Nonnull
    private AutoCloseable mockPartner(@Nullable PartnerResponse partner) {
        when(lmsClient.getPartner(CURRENT_PARTNER_ID))
            .thenReturn(Optional.ofNullable(partner));

        return () -> verify(lmsClient).getPartner(CURRENT_PARTNER_ID);
    }

    @Nonnull
    private AutoCloseable mockApiSettings() {
        return mockApiSettings(List.of(
            SettingsApiDto.newBuilder()
                .partnerId(CURRENT_PARTNER_ID)
                .token("partner-token")
                .build()
        ));
    }

    @Nonnull
    private AutoCloseable mockApiSettings(List<SettingsApiDto> result) {
        SettingsApiFilter filter = SettingsApiFilter.newBuilder()
            .partnerIds(Set.of(CURRENT_PARTNER_ID))
            .apiType(ApiType.FULFILLMENT)
            .build();

        when(lmsClient.searchPartnerApiSettings(filter))
            .thenReturn(result);

        return () -> verify(lmsClient).searchPartnerApiSettings(filter);
    }

}
