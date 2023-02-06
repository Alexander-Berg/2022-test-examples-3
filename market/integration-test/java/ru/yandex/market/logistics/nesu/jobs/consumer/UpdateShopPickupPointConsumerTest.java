package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointUpdateRequest;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.core.Phone;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse;
import ru.yandex.market.logistics.management.entity.type.PhoneType;
import ru.yandex.market.logistics.management.entity.type.PickupPointType;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.model.ShopPickupPointMetaIdPayload;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@DisplayName("Обновление точки в LMS")
class UpdateShopPickupPointConsumerTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private UpdateShopPickupPointConsumer updateShopPickupPointConsumer;

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успешное обновление логистической точки в LMS")
    @DatabaseSetup("/jobs/consumer/create_shop_pickup_point/before/shop_pickup_point_with_lms_id.xml")
    @ExpectedDatabase(
        value = "/jobs/consumer/create_shop_pickup_point/before/shop_pickup_point_with_lms_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    // Запись в маппинге не появляется, т.к. OLD.lms_id совпадает с NEW.lms_id
    @ExpectedDatabase(
        value = "/controller/shop-pickup-points/after/shop_pickup_point_id_mapping_empty.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void successUpdate() throws Exception {
        try (var ignored = mockLmsUpdateLogisticsPoint()) {
            updateShopPickupPointConsumer.execute(createTask());
        }
    }

    @Nonnull
    private Task<ShopPickupPointMetaIdPayload> createTask() {
        return new Task<>(
            new QueueShardId("1"),
            new ShopPickupPointMetaIdPayload(REQUEST_ID, 800L),
            1,
            clock.instant().atZone(DateTimeUtils.MOSCOW_ZONE),
            null,
            null
        );
    }

    @Nonnull
    private AutoCloseable mockLmsUpdateLogisticsPoint() {
        LogisticsPointUpdateRequest request = LogisticsPointUpdateRequest.newBuilder()
            .pickupPointType(PickupPointType.PICKUP_POINT)
            .name("ПВЗ Яндекс маркет")
            .externalId("shop-pickup-point-external-id-400")
            .ownerPartnerId(123L)
            .address(
                Address.newBuilder()
                    .locationId(213)
                    .settlement("Москва")
                    .postCode("115522")
                    .latitude(new BigDecimal("55.653415"))
                    .longitude(new BigDecimal("37.646280"))
                    .street("Каширское шоссе")
                    .house("26")
                    .estate("newEstate")
                    .km(1)
                    .region("Москва")
                    .build()
            )
            .phones(Set.of(new Phone(
                "+7 (925) 335-00-81",
                null,
                null,
                PhoneType.PRIMARY
            )))
            .active(true)
            .schedule(Set.of(
                new ScheduleDayResponse(null, 1, LocalTime.of(10, 0), LocalTime.of(22, 0)),
                new ScheduleDayResponse(null, 2, LocalTime.of(10, 0), LocalTime.of(22, 0)),
                new ScheduleDayResponse(null, 3, LocalTime.of(10, 0), LocalTime.of(22, 0)),
                new ScheduleDayResponse(null, 4, LocalTime.of(10, 0), LocalTime.of(22, 0)),
                new ScheduleDayResponse(null, 5, LocalTime.of(10, 0), LocalTime.of(22, 0)),
                new ScheduleDayResponse(null, 6, LocalTime.of(10, 0), LocalTime.of(22, 0)),
                new ScheduleDayResponse(null, 7, LocalTime.of(10, 0), LocalTime.of(22, 0))
            ))
            .closedOnRegionHolidays(false)
            .storagePeriod(10)
            .build();
        when(lmsClient.updateLogisticsPoint(eq(2000L), refEq(request)))
            .thenReturn(LogisticsPointResponse.newBuilder().id(2000L).build());
        return () -> verify(lmsClient).updateLogisticsPoint(eq(2000L), refEq(request));
    }
}
