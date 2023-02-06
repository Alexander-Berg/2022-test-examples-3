package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;
import ru.yandex.market.logistics.lrm.model.entity.enums.LogisticPointType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ShipmentRecipientType;
import ru.yandex.market.logistics.lrm.queue.payload.ChangeReturnSegmentsShipmentPayload;
import ru.yandex.market.logistics.lrm.queue.payload.LogisticPointDto;
import ru.yandex.market.logistics.lrm.queue.payload.RecipientDto;
import ru.yandex.market.logistics.lrm.queue.processor.CreateSegmentsOnShipmentChangedProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Создание сегментов при успешном изменении отгрузки на СЦ")
@DatabaseSetup("/database/tasks/return-segment/change-return-segments-shipment/before/prepare.xml")
class CreateSegmentsOnShipmentChangedProcessorTest extends AbstractIntegrationTest {

    private static final Long RETURN_ID = 1L;
    private static final Long DELIVERY_PARTNER_ID = 111L;
    private static final Long OUTBOUND_PARTNER_ID = 333L;
    private static final Long OUTBOUND_LOGISTIC_POINT_ID = 30L;

    @Autowired
    private CreateSegmentsOnShipmentChangedProcessor processor;

    @Autowired
    private UuidGenerator uuidGenerator;

    @Autowired
    private LMSClient lmsClient;

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-01-01T12:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
        when(uuidGenerator.get()).thenReturn(
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa091"),
            UUID.fromString("e11c5e64-3694-40c9-b9b4-126efedaa092")
        );
    }

    @Test
    @DisplayName("Ошибка: не найдена контрольная точка")
    void errorControlPointNotFound() {
        softly.assertThatCode(() -> execute(List.of(11L, 21L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Control point with CREATED status is not found for return=1");
    }

    @Test
    @DisplayName("Успех: контрольная точка на следующем сегменте, не на Утилизаторе")
    @DatabaseSetup("/database/tasks/return-segment/change-return-segments-shipment/before/control_point.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-segments-shipment/after/segments_with_shipment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successNextSegmentOnControlPoint_NotUtilizer() throws Exception {
        try (
            var ignored1 = mockGetShopPartner(75735L);
            var ignored2 = mockGetShopLogisticPoint(75735L, 13L)
        ) {
            execute(List.of(11L, 21L));
        }
    }

    @Test
    @DisplayName("Успех: контрольная точка на Утилизаторе, есть промежуточный СЦ")
    @DatabaseSetup("/database/tasks/return-segment/change-return-segments-shipment/before/control_point_utilizer.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/change-return-segments-shipment/after/segments_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successEmptyShipmentFields() {
        execute(List.of(11L, 21L));
    }

    @Nonnull
    private AutoCloseable mockGetShopPartner(long partnerId) {
        when(lmsClient.getPartner(partnerId)).thenReturn(
            Optional.of(
                PartnerResponse.newBuilder()
                    .id(partnerId)
                    .name("partner-" + partnerId)
                    .build()
            )
        );
        return () -> verify(lmsClient).getPartner(partnerId);
    }

    @Nonnull
    private AutoCloseable mockGetShopLogisticPoint(long partnerId, long logisticPointId) {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(partnerId))
            .partnerTypes(Set.of(PartnerType.DROPSHIP))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
        when(lmsClient.getLogisticsPoints(filter)).thenReturn(
            List.of(
                LogisticsPointResponse.newBuilder()
                    .id(logisticPointId)
                    .partnerId(partnerId)
                    .name("logisticPoint-" + logisticPointId)
                    .externalId("logisticPointExternal-" + logisticPointId)
                    .build()
            )
        );

        return () -> verify(lmsClient).getLogisticsPoints(filter);
    }

    private void execute(List<Long> segmentIds) {
        execute(segmentIds, OUTBOUND_LOGISTIC_POINT_ID, OUTBOUND_PARTNER_ID);
    }

    private void execute(List<Long> segmentIds, long logisticPointId, long partnerId) {
        processor.execute(ChangeReturnSegmentsShipmentPayload.builder()
            .returnId(RETURN_ID)
            .returnSegmentIds(segmentIds)
            .nextLogisticPoint(
                LogisticPointDto.builder()
                    .id(logisticPointId)
                    .partnerId(partnerId)
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
            .build()
        );
    }
}
