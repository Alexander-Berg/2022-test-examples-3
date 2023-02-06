package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.dockerjava.api.exception.BadRequestException;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.payload.CourierDto;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentFromPvzToScPayload;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentFromPvzToScPayload.ReturnSegmentFromPvzToScPayloadBuilder;
import ru.yandex.market.logistics.lrm.queue.processor.CreateReturnSegmentFromPvzToScProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.PointType;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Обработка получения события: Курьер забрал возвратный заказ из ПВЗ")
@DatabaseSetup("/database/tasks/return-segment/create-pvz-to-sc/before/common.xml")
class CreateReturnSegmentFromPvzToScProcessorTest extends AbstractIntegrationTest {

    private static final long PARTNER_ID = 100;
    private static final long WAREHOUSE_ID = 200;
    private static final long COURIER_ID = 123;
    private static final long COURIER_UID = 234;
    private static final String WAREHOUSE_EXTERNAL_ID = "300";
    private static final String BOX_EXTERNAL_ID = "box-external-id";

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private CreateReturnSegmentFromPvzToScProcessor processor;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-09-06T11:12:13.00Z"), ZoneId.systemDefault());
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успешное создание следующего сегмента для СЦ")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-pvz-to-sc/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        mockLms();
        softly.assertThatCode(() -> processor.execute(payload(BOX_EXTERNAL_ID)))
            .doesNotThrowAnyException();
        verifyLms();
    }

    @Test
    @DisplayName("Успешное создание следующего сегмента для СЦ, курьер не передан")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-pvz-to-sc/after/without_courier.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successWithoutCourier() {
        mockLms();
        softly.assertThatCode(() -> processor.execute(
                payloadBase(BOX_EXTERNAL_ID)
                    .courierDto(
                        CourierDto.builder()
                            .deliveryServiceId(WAREHOUSE_ID)
                            .build()
                    )
                    .build())
            )
            .doesNotThrowAnyException();
        verifyLms();
    }

    @Test
    @DisplayName("LMS вернул ошибку")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-pvz-to-sc/before/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failLms() {
        when(lmsClient.getLogisticsPoints(logisticsPointFilter())).thenThrow(new BadRequestException("Error"));

        softly.assertThatThrownBy(() -> processor.execute(payload(BOX_EXTERNAL_ID)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Status 400: Error");

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter());
    }

    @Test
    @DisplayName("У партнера больше одного склада")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-pvz-to-sc/before/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void moreThanOneWarehouse() {
        doReturn(List.of(LogisticsPointResponse.newBuilder().build(), LogisticsPointResponse.newBuilder().build()))
            .when(lmsClient).getLogisticsPoints(logisticsPointFilter());

        softly.assertThatThrownBy(() -> processor.execute(payload(BOX_EXTERNAL_ID)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Partner 100 must have exactly one warehouse");

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter());
    }

    @Test
    @DisplayName("У партнера нет складов")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-pvz-to-sc/before/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noWarehouses() {
        when(lmsClient.getLogisticsPoints(logisticsPointFilter())).thenReturn(List.of());

        softly.assertThatThrownBy(() -> processor.execute(payload(BOX_EXTERNAL_ID)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Partner 100 must have exactly one warehouse");

        verify(lmsClient).getLogisticsPoints(logisticsPointFilter());
    }

    @Test
    @DisplayName("В LMS нет партнера")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-pvz-to-sc/before/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void noPartner() {
        mockLms();
        when(lmsClient.getPartner(PARTNER_ID)).thenReturn(Optional.empty());

        softly.assertThatThrownBy(() -> processor.execute(payload(BOX_EXTERNAL_ID)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Failed to find PARTNER with id [100]");

        verifyLms();
    }

    @Test
    @DisplayName("У коробки нет сегментов")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-pvz-to-sc/before/common.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void boxWithoutSegment() {
        mockLms();

        softly.assertThatThrownBy(() -> processor.execute(payload("invalid")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Cannot find return PICKUP segment for box with externalId invalid");

        verifyLms();
    }

    @Test
    @DisplayName("У коробки нет пикап-сегмента")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/create-pvz-to-sc/before/box_without_pickup_segment.xml",
        type = DatabaseOperation.INSERT
    )
    void boxWithoutPickupSegment() {
        mockLms();

        softly.assertThatThrownBy(() -> processor.execute(payload("box-external-id2")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Cannot find return PICKUP segment for box with externalId box-external-id2");

        verifyLms();
    }

    @Test
    @DisplayName("Несколько идентификаторов партнёров СЦ")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-pvz-to-sc/after/multiple_sorting_center_ids.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sortingCenterPartnerIdsOverride() {
        mockLms();

        processor.execute(
            payloadBase(BOX_EXTERNAL_ID)
                .sortingCenterPartnerIds(List.of(110L, 120L))
                .courierDto(defaultCourier())
                .build()
        );

        verifyLms();
    }

    private void mockLms() {
        when(lmsClient.getLogisticsPoints(logisticsPointFilter()))
            .thenReturn(List.of(
                LogisticsPointResponse.newBuilder()
                    .id(WAREHOUSE_ID)
                    .partnerId(PARTNER_ID)
                    .externalId(WAREHOUSE_EXTERNAL_ID)
                    .name("склад сц")
                    .build()
            ));
        doReturn(Optional.of(
            PartnerResponse.newBuilder()
                .id(PARTNER_ID)
                .name("partner name")
                .build()
        ))
            .when(lmsClient)
            .getPartner(PARTNER_ID);
    }

    private void verifyLms() {
        verify(lmsClient).getLogisticsPoints(logisticsPointFilter());
        verify(lmsClient).getPartner(PARTNER_ID);
    }

    @Nonnull
    private LogisticsPointFilter logisticsPointFilter() {
        return LogisticsPointFilter.newBuilder()
            .partnerIds(Set.of(PARTNER_ID))
            .partnerTypes(Set.of(PartnerType.SORTING_CENTER))
            .type(PointType.WAREHOUSE)
            .active(true)
            .build();
    }

    @Nonnull
    private static ReturnSegmentFromPvzToScPayload payload(String boxExternalId) {
        return payloadBase(boxExternalId)
            .courierDto(defaultCourier())
            .build();
    }

    @Nonnull
    private static ReturnSegmentFromPvzToScPayloadBuilder<?, ?> payloadBase(String boxExternalId) {
        return ReturnSegmentFromPvzToScPayload.builder()
            .sortingCenterId(PARTNER_ID)
            .boxExternalId(boxExternalId);
    }

    @Nonnull
    private static CourierDto defaultCourier() {
        return CourierDto.builder()
            .id(COURIER_ID)
            .uid(COURIER_UID)
            .deliveryServiceId(WAREHOUSE_ID)
            .courierName("courier")
            .phoneNumber("+7-000-000-00-00")
            .carNumber("car")
            .build();
    }

}
