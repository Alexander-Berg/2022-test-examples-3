package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.les.base.Event;
import ru.yandex.market.logistics.les.client.producer.LesProducer;
import ru.yandex.market.logistics.les.dto.CarDto;
import ru.yandex.market.logistics.les.dto.CargoUnitDto;
import ru.yandex.market.logistics.les.dto.CargoUnitGroupDto;
import ru.yandex.market.logistics.les.dto.CargoUnitGroupType;
import ru.yandex.market.logistics.les.dto.CarrierDto;
import ru.yandex.market.logistics.les.dto.CarrierType;
import ru.yandex.market.logistics.les.dto.CodeDto;
import ru.yandex.market.logistics.les.dto.CodeType;
import ru.yandex.market.logistics.les.dto.CourierDto;
import ru.yandex.market.logistics.les.dto.InboundShipmentDto;
import ru.yandex.market.logistics.les.dto.KorobyteDto;
import ru.yandex.market.logistics.les.dto.OutboundShipmentDto;
import ru.yandex.market.logistics.les.dto.PartnerDto;
import ru.yandex.market.logistics.les.dto.PersonDto;
import ru.yandex.market.logistics.les.dto.PhoneDto;
import ru.yandex.market.logistics.les.dto.PointDto;
import ru.yandex.market.logistics.les.dto.PointType;
import ru.yandex.market.logistics.les.tpl.StorageUnitCreateRequestEvent;
import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.BusinessProcessType;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentIdPayload;
import ru.yandex.market.logistics.lrm.queue.processor.CreateStorageUnitsInScProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsApiFilter;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.settings.SettingsApiDto;
import ru.yandex.market.logistics.management.entity.type.ApiType;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lrm.config.LocalsConfiguration.TEST_UUID;
import static ru.yandex.market.logistics.lrm.config.LrmTestConfiguration.TEST_REQUEST_ID;

@DisplayName("Создание грузомест в СЦ")
@DatabaseSetup("/database/tasks/return-segment/create-in-sc/request/before/prepare.xml")
class CreateStorageUnitsInScProcessorTest extends AbstractIntegrationTest {
    private static final Instant FIXED_TIME = Instant.parse("2019-06-12T00:00:00Z");
    private static final BigDecimal ASSESSED_COST = BigDecimal.valueOf(700);
    private static final long COURIER_ID = 123;
    private static final long COURIER_UID = 234;
    private static final long COURIER_PARTNER_ID = 300;
    private static final String COURIER_NAME = "courier";
    private static final String COURIER_CAR_NUMBER = "car";
    private static final String COURIER_PHONE_NUMBER = "+7-000-000-00-00";

    @Autowired
    private CreateStorageUnitsInScProcessor processor;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private LesProducer lesProducer;

    @BeforeEach
    void setup() {
        when(lmsClient.getLogisticsPoint(1235L))
            .thenReturn(Optional.of(LogisticsPointResponse.newBuilder().externalId("lp1235").partnerId(100L).build()));

        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, lesProducer);
    }

    @Test
    @DisplayName("У сегмента с такой же логточкой status = null")
    @DatabaseSetup(
        type = DatabaseOperation.REFRESH,
        value = "/database/tasks/return-segment/create-in-sc/request/before/segment_on_same_point_status_null.xml"
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-in-sc/request/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentOnSamePointStatusIsNull() throws Exception {
        try (var ignored = mockPartnerToken()) {
            processor.execute(payload(2));
        }

        verify(lesProducer).send(
            lesEvent(requestEvent("RETURN_PARTIAL_1", CargoUnitGroupType.RETURN_PARTIAL)),
            OUT_LES_QUEUE
        );
    }

    @Test
    @DisplayName("Сегмент присутствует в пути от сегмента с такой же логточкой и активным статусом")
    @DatabaseSetup(
        type = DatabaseOperation.REFRESH,
        value = "/database/tasks/return-segment/create-in-sc/request/before/segment_on_same_point.xml"
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-in-sc/request/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentIsOnActivePath() throws Exception {
        try (var ignored = mockPartnerToken()) {
            processor.execute(payload(2));
        }

        verify(lesProducer).send(
            lesEvent(requestEvent("RETURN_PARTIAL_1", CargoUnitGroupType.RETURN_PARTIAL)),
            OUT_LES_QUEUE
        );
    }

    @Test
    @DisplayName("Сегмент не присутствует в пути от сегмента с такой же логточкой и активным статусом")
    @DatabaseSetup(
        type = DatabaseOperation.REFRESH,
        value = "/database/tasks/return-segment/create-in-sc/request/before/segment_on_same_point_not_connected.xml"
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void segmentIsOnInactivePath() {
        processor.execute(payload(2));
    }

    @Test
    @DisplayName("Успешная отправка события в LES, отгрузка курьером, ПВЗ возврат")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-in-sc/request/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successPvz() throws Exception {
        try (var ignored = mockPartnerToken()) {
            processor.execute(payload(2));
        }

        verify(lesProducer).send(
            lesEvent(requestEvent("RETURN_PARTIAL_1", CargoUnitGroupType.RETURN_PARTIAL)),
            OUT_LES_QUEUE
        );
    }

    @Test
    @DisplayName("Успешная отправка события в LES, отгрузка курьером, клиентский возврат")
    @DatabaseSetup("/database/tasks/return-segment/create-in-sc/request/before/prepare_client.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-in-sc/request/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successClient() throws Exception {
        try (var ignored = mockPartnerToken()) {
            processor.execute(payload(2));
        }
        verify(lesProducer).send(
            lesEvent(requestEvent("RETURN_CLIENT_1", CargoUnitGroupType.RETURN_CLIENT)),
            OUT_LES_QUEUE
        );
    }

    @Test
    @DisplayName("Успешная отправка события в LES, отгрузка без курьера")
    @DatabaseSetup("/database/tasks/return-segment/create-in-sc/request/before/prepare_without_courier.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-in-sc/request/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successWithoutCourier() throws Exception {
        try (var ignored = mockPartnerToken()) {
            processor.execute(payload(2));
        }
        verify(lesProducer).send(
            lesEvent(requestEventWithoutCourier()),
            OUT_LES_QUEUE
        );
    }

    @Test
    @DisplayName("Успешная отправка события в LES, невыкуп")
    @DatabaseSetup("/database/tasks/return-segment/create-in-sc/request/before/prepare_cancellation.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-in-sc/request/after/task_created_cancellation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successCancellationReturn() throws Exception {
        try (var ignored = mockPartnerToken()) {
            processor.execute(payload(1));
        }
        verify(lesProducer).send(
            lesEvent(
                requestEvent(
                    null,
                    List.of(
                        new CodeDto("box-external-id", CodeType.CARGO_BARCODE),
                        new CodeDto("order-external-id", CodeType.ORDER_BARCODE)
                    ),
                    new KorobyteDto(
                        1,
                        2,
                        3,
                        4,
                        null,
                        null
                    ),
                    "CANCELLATION_RETURN_1",
                    "order-external-id",
                    CargoUnitGroupType.CANCELLATION_RETURN,
                    BigDecimal.valueOf(10000)
                )
            ),
            OUT_LES_QUEUE
        );
    }

    @Test
    @DisplayName("Успешная отправка события в LES, неопознанная коробка")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/create-in-sc/request/before/prepare_unknown_box.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-in-sc/request/after/task_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void successUnknownBox() throws Exception {
        try (var ignored = mockPartnerToken()) {
            processor.execute(payload(2));
        }

        verify(lesProducer).send(
            lesEvent(
                requestEvent(
                    null,
                    List.of(new CodeDto("box-external-id", CodeType.CARGO_BARCODE)),
                    null,
                    "RETURN_PARTIAL_1",
                    "return_ext_id",
                    CargoUnitGroupType.RETURN_PARTIAL,
                    BigDecimal.valueOf(700)
                )
            ),
            OUT_LES_QUEUE
        );
    }

    @Test
    @DisplayName("Неуспешная отправка события в LES")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void lesFail() throws Exception {
        Event lesEvent = lesEvent(requestEvent("RETURN_PARTIAL_1", CargoUnitGroupType.RETURN_PARTIAL));

        doThrow(new RuntimeException("error")).when(lesProducer).send(lesEvent, OUT_LES_QUEUE);

        try (var ignored = mockPartnerToken()) {
            softly.assertThatCode(() -> processor.execute(payload(2)))
                .hasMessage("error")
                .isInstanceOf(RuntimeException.class);
        }

        verify(lesProducer).send(lesEvent, OUT_LES_QUEUE);
    }

    @Test
    @DisplayName("Настройки партнёра не найдены в LMS")
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noApiSettings() throws Exception {
        try (var ignored = mockPartnerToken(List.of())) {
            softly.assertThatCode(() -> processor.execute(payload(2)))
                .hasMessage("No FF API token for partner 100")
                .isInstanceOf(RuntimeException.class);
        }
    }

    @Test
    @DisplayName("Не создавать грузоместа на сегменте Утилизатора")
    @DatabaseSetup(
        type = DatabaseOperation.UPDATE,
        value = "/database/tasks/return-segment/create-in-sc/request/before/segment_utilizer.xml"
    )
    @ExpectedDatabase(
        value = "/database/tasks/no_tasks.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noCallForUtilizer() throws Exception {
        processor.execute(payload(2));
    }

    @Nonnull
    private Event lesEvent(StorageUnitCreateRequestEvent storageUnitCreateRequestEvent) {
        return new Event(
            SOURCE_FOR_LES,
            TEST_UUID,
            FIXED_TIME.toEpochMilli(),
            BusinessProcessType.CREATE_STORAGE_UNITS_IN_SC.name(),
            storageUnitCreateRequestEvent,
            ""
        );
    }

    @Nonnull
    private AutoCloseable mockPartnerToken() {
        return mockPartnerToken(List.of(SettingsApiDto.newBuilder().token("token100").build()));
    }

    @Nonnull
    private AutoCloseable mockPartnerToken(List<SettingsApiDto> result) {
        SettingsApiFilter filter = SettingsApiFilter.newBuilder()
            .partnerIds(Set.of(100L))
            .apiType(ApiType.FULFILLMENT)
            .build();

        when(lmsClient.searchPartnerApiSettings(filter))
            .thenReturn(result);

        return () -> verify(lmsClient).searchPartnerApiSettings(filter);
    }

    @Nonnull
    private ReturnSegmentIdPayload payload(long returnSegmentId) {
        return ReturnSegmentIdPayload.builder()
            .returnSegmentId(returnSegmentId)
            .requestId(TEST_REQUEST_ID)
            .build();
    }

    @Nonnull
    private StorageUnitCreateRequestEvent requestEvent(String groupId, CargoUnitGroupType groupType) {
        return requestEvent(courier(), groupId, groupType, ASSESSED_COST);
    }

    @Nonnull
    private StorageUnitCreateRequestEvent requestEventWithoutCourier() {
        return requestEvent(null, "RETURN_PARTIAL_1", CargoUnitGroupType.RETURN_PARTIAL, ASSESSED_COST);
    }

    @Nonnull
    private StorageUnitCreateRequestEvent requestEvent(
        CourierDto courier,
        String groupId,
        CargoUnitGroupType groupType,
        BigDecimal assessedCost
    ) {
        return requestEvent(
            courier,
            List.of(new CodeDto("box-external-id", CodeType.CARGO_BARCODE)),
            groupId,
            groupType,
            assessedCost
        );
    }

    @Nonnull
    private StorageUnitCreateRequestEvent requestEvent(
        CourierDto courierDto,
        List<CodeDto> codes,
        String groupId,
        CargoUnitGroupType groupType,
        BigDecimal assessedCost
    ) {
        return requestEvent(
            new InboundShipmentDto(
                Instant.parse("2022-01-01T11:00:00Z"),
                new CarrierDto(
                    CarrierType.DELIVERY_SERVICE_WITH_COURIER,
                    300,
                    "previous point",
                    courierDto
                ),
                new PointDto(PointType.SORTING_CENTER, 3000, null, "from point")
            ),
            codes,
            null,
            groupId,
            "return_ext_id",
            groupType,
            assessedCost
        );
    }

    @Nonnull
    private StorageUnitCreateRequestEvent requestEvent(
        InboundShipmentDto inboundShipmentDto,
        List<CodeDto> codes,
        KorobyteDto dimensions,
        String groupId,
        String checkoutId,
        CargoUnitGroupType groupType,
        BigDecimal assessedCost
    ) {
        return new StorageUnitCreateRequestEvent(
            TEST_REQUEST_ID,
            new PartnerDto(100L, "token100", "1235"),
            List.of(new CargoUnitDto(
                "1",
                FIXED_TIME.toEpochMilli(),
                "4c853d61-7a5f-4383-af32-cc56935f787d",
                null,
                groupId,
                codes,
                dimensions,
                null,
                null,
                inboundShipmentDto,
                new OutboundShipmentDto(
                    Instant.parse("2022-01-01T12:00:00Z"),
                    new CarrierDto(CarrierType.DELIVERY_SERVICE, 200, "next point", null),
                    new PointDto(PointType.SHOP, 2000, 900L, "dest point")
                )
            )),
            new CargoUnitGroupDto(
                groupId,
                checkoutId,
                new CodeDto("order-external-id", CodeType.ORDER_BARCODE),
                assessedCost,
                1,
                true,
                groupType
            )
        );
    }

    @Nonnull
    private CourierDto courier() {
        return new CourierDto(
            COURIER_ID,
            COURIER_UID,
            COURIER_PARTNER_ID,
            new PersonDto(COURIER_NAME, null, null),
            new PhoneDto(COURIER_PHONE_NUMBER, null),
            new CarDto(COURIER_CAR_NUMBER, null),
            null
        );
    }
}
