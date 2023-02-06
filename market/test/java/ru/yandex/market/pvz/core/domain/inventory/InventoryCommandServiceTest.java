package ru.yandex.market.pvz.core.domain.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.core.domain.inventory.model.InventoryStatus;
import ru.yandex.market.pvz.core.domain.inventory.params.InventoryParams;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointCommandService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.common.util.logging.Tracer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class InventoryCommandServiceTest {

    private static final Long UID = 1L;
    private static final String LOGIN = "login";

    private final PickupPointCommandService pickupPointCommandService;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestableClock clock;
    private final InventoryCommandService inventoryCommandService;
    private final InventoryQueryService inventoryQueryService;

    @BeforeEach
    void setup() {
        Tracer.putUidToStatic(UID);
        Tracer.putLoginToStatic(LOGIN);
    }

    @Test
    void whenCreateInventoryThenSuccess() {
        clock.setFixed(Instant.parse("2022-01-17T02:29:00Z"), ZoneOffset.ofHours(0));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );

        InventoryParams output = inventoryCommandService.create(pickupPoint.getId());
        InventoryParams expected = InventoryParams.builder()
                .status(InventoryStatus.IN_PROGRESS)
                .date(LocalDate.now(clock))
                .uid(UID)
                .login(LOGIN)
                .durationInSeconds(inventoryQueryService.getInventoryDurationInSeconds())
                .timeOffset(pickupPoint.getTimeOffset())
                .items(List.of())
                .build();;

        assertThat(output).isEqualToIgnoringGivenFields(expected, "id", "createdAt");
    }

    @Test
    void whenCreateInventoryTwiceThenSuccess() {
        Instant now = Instant.parse("2022-01-17T15:00:00Z");
        clock.setFixed(now, ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        long lmsId = 123L;
        pickupPointCommandService.updateLmsId(pickupPoint.getId(), lmsId);

        InventoryParams output = inventoryCommandService.create(pickupPoint.getId());
        InventoryParams expected = InventoryParams.builder()
                .status(InventoryStatus.IN_PROGRESS)
                .uid(UID)
                .login(LOGIN)
                .durationInSeconds(inventoryQueryService.getInventoryDurationInSeconds())
                .timeOffset(pickupPoint.getTimeOffset())
                .items(List.of())
                .build();

        assertThat(output).isEqualToIgnoringGivenFields(expected, "id", "date", "createdAt");

        expected.setId(output.getId());
        expected.setDate(output.getDate());
        expected.setCreatedAt(output.getCreatedAt());

        clock.setFixed(
                now.plusSeconds(inventoryQueryService.getInventoryDurationInSeconds() - 10),
                ZoneOffset.ofHours(3)
        );

        output = inventoryCommandService.create(pickupPoint.getId());
        assertThat(output).isEqualTo(expected);
    }

    @Test
    void whenFinishInventoryThenSuccess() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());

        InventoryParams output = inventoryCommandService.finish(
                pickupPoint.getId(), pickupPoint.getTimeOffset(), inventory.getId()
        );
        InventoryParams expected = InventoryParams.builder()
                .id(inventory.getId())
                .status(InventoryStatus.FINISHED)
                .uid(UID)
                .login(LOGIN)
                .durationInSeconds(inventoryQueryService.getInventoryDurationInSeconds())
                .timeOffset(pickupPoint.getTimeOffset())
                .finishedAt(clock.instant())
                .date(inventory.getDate())
                .createdAt(inventory.getCreatedAt())
                .items(List.of())
                .build();

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void whenFinishInventoryNotBelongsPickupPointThenError() {
        clock.setFixed(Instant.parse("2021-12-05T22:00:00Z"), ZoneOffset.ofHours(3));
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointByBrandingTypeAndDropOffAndPartnerWithOffset(
                PickupPointBrandingType.FULL, true, null, 6
        );
        InventoryParams inventory = inventoryCommandService.create(pickupPoint.getId());

        assertThatThrownBy(() -> inventoryCommandService.finish(
                (pickupPoint.getId() + 1), pickupPoint.getTimeOffset(), inventory.getId())
        )
                .isExactlyInstanceOf(TplInvalidParameterException.class);
    }

}
