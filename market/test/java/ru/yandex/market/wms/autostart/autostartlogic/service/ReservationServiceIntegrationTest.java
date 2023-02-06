package ru.yandex.market.wms.autostart.autostartlogic.service;

import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.exception.IncorrectPickDetailStatusException;
import ru.yandex.market.wms.autostart.exception.PickDetailIsNotAllocatedException;
import ru.yandex.market.wms.common.model.enums.PickDetailStatus;
import ru.yandex.market.wms.common.spring.dao.entity.PickDetail;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class ReservationServiceIntegrationTest extends AutostartIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Test
    @DatabaseSetup("/empty-db.xml")
    @ExpectedDatabase(value = "/empty-db.xml", assertionMode = NON_STRICT_UNORDERED)
    void cancelAllocatedEmptyPickDetailsList() {
        reservationService.cancelAllocatedReservation(Collections.emptyList(), null);
    }

    @Test
    @DatabaseSetup("/empty-db.xml")
    @ExpectedDatabase(value = "/empty-db.xml", assertionMode = NON_STRICT_UNORDERED)
    void cancelPickedEmptyPickDetailsList() {
        reservationService.cancelPickedReservation(Collections.emptyList(), null);
    }

    @ParameterizedTest
    @EnumSource(value = PickDetailStatus.class,
            names = {"CLOSED", "PACKED", "SORTED_BY_DELIVERY_SERVICE", "LOADED", "SHIPPED"})
    @DatabaseSetup("/empty-db.xml")
    @ExpectedDatabase(value = "/empty-db.xml", assertionMode = NON_STRICT_UNORDERED)
    void cancelAllocatedPickDetailsInImproperStatus(PickDetailStatus status) {
        var pickDetails = Collections.singletonList(createPickDetail(status));
        assertions.assertThatThrownBy(() -> reservationService.cancelAllocatedReservation(pickDetails, null))
                .isExactlyInstanceOf(PickDetailIsNotAllocatedException.class)
                .hasMessage("400 BAD_REQUEST \"PickDetail PD-1 имеет статус не NORMAL, RELEASED или IN_PROCESS\"");
    }

    @ParameterizedTest
    @EnumSource(value = PickDetailStatus.class,
            names = {"CLOSED", "LOADED", "SHIPPED"})
    @DatabaseSetup("/empty-db.xml")
    @ExpectedDatabase(value = "/empty-db.xml", assertionMode = NON_STRICT_UNORDERED)
    void cancelPickedPickDetailsInImproperStatus(PickDetailStatus status) {
        var pickDetails = Collections.singletonList(createPickDetail(status));
        assertions.assertThatThrownBy(() -> reservationService.cancelPickedReservation(pickDetails, null))
                .isExactlyInstanceOf(IncorrectPickDetailStatusException.class)
                .hasMessage("Детали отбора заказов имеют неподходящие статусы: {ORDER-1=[%s]}", status);
    }

    @ParameterizedTest
    @EnumSource(value = PickDetailStatus.class,
            names = {"NORMAL", "RELEASED", "IN_PROCESS"})
    @DatabaseSetup("/autostartlogic/service/reservationservice/db/before-cancel-allocated-reservation.xml")
    @ExpectedDatabase(value = "/autostartlogic/service/reservationservice/db/after-cancel-allocated-reservation.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelAllocatedReservation(PickDetailStatus status) {
        var pd = createPickDetail(
                status
        );
        var pickDetails = Collections.singletonList(pd);
        reservationService.cancelAllocatedReservation(pickDetails, "USER_AFTER");
    }

    @Test
    @DatabaseSetup("/autostartlogic/service/reservationservice/db/before-cancel-picked-reservation.xml")
    @ExpectedDatabase(value = "/autostartlogic/service/reservationservice/db/after-cancel-picked-reservation.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelPickedReservation() {
        var pd = createPickDetail(
                PickDetailStatus.PICKED
        );
        var pickDetails = Collections.singletonList(pd);
        reservationService.cancelPickedReservation(pickDetails, "USER_AFTER");
    }

    private static PickDetail createPickDetail(
            PickDetailStatus status) {
        return PickDetail.builder()
                .pickDetailKey("PD-1")
                .status(status)
                .orderKey("ORDER-1")
                .sku("SKU-1")
                .storerKey("STORER-1")
                .loc("LOC-1")
                .lot("LOT-1")
                .id("CART1234")
                .build();
    }
}
