package ru.yandex.market.tpl.core.service.equeue;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.service.equeue.model.EqueuePropertiesStateDto;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties.ELECTRONIC_QUEUE_ENABLED;

@RequiredArgsConstructor
class EqueuePropertiesServiceTest extends TplAbstractTest {

    private final TestUserHelper testUserHelper;
    private final SortingCenterPropertyService scPropertyService;
    private final EqueuePropertiesService equeuePropertiesService;

    public static final Long EXPECTED_PARKING_CAPACITY = 1L;
    public static final Long EXPECTED_MINUTES_TO_LOADING = 20L;
    public static final Long EXPECTED_QTY_TO_NEXT_SLOT = 300L;
    public static final Long EXPECTED_DURATION_IN_MINUTES = 4000L;
    public static final Long EXPECTED_RATE_PASS_LATECOMERS = 5L;

    @Test
    void isEqueueEnabled_enabled() {
        //given
        long scId = 777L;
        SortingCenter sc = testUserHelper.sortingCenter(scId);
        scPropertyService.upsertPropertyToSortingCenter(sc, ELECTRONIC_QUEUE_ENABLED, false);

        //when
        boolean equeueEnabled = equeuePropertiesService.isEqueueEnabled(scId);

        //then
        assertFalse(equeueEnabled);
    }

    @Test
    void isEqueueEnabled_disabled() {
        //given
        long scId = 777L;
        SortingCenter sc = testUserHelper.sortingCenter(scId);
        scPropertyService.upsertPropertyToSortingCenter(sc, ELECTRONIC_QUEUE_ENABLED, true);

        //when
        boolean equeueEnabled = equeuePropertiesService.isEqueueEnabled(scId);

        //then
        assertTrue(equeueEnabled);
    }

    @Test
    void updateEnabledState() {
         //given
        long scId = 777L;
        testUserHelper.sortingCenter(scId);

         //when
        equeuePropertiesService.updateEnabledState(scId, true);

         //then
        assertTrue(equeuePropertiesService.isEqueueEnabled(scId));
    }

    @Test
    void updateState() {
        //given
        long scId = 777L;
        testUserHelper.sortingCenter(scId);

        //when
        equeuePropertiesService.updateState(EqueuePropertiesStateDto
                .builder()
                .scId(777L)
                .ratePassLatecomers(EXPECTED_RATE_PASS_LATECOMERS)
                .slotDurationInMinutes(EXPECTED_DURATION_IN_MINUTES)
                .qtyFreePlacesToNextSlot(EXPECTED_QTY_TO_NEXT_SLOT)
                .minutesToArriveToLoading(EXPECTED_MINUTES_TO_LOADING)
                .parkingCapacity(EXPECTED_PARKING_CAPACITY)
                .build());

        //then
        EqueuePropertiesStateDto updatedStateDto = equeuePropertiesService.getState(scId);
        assertEquals(EXPECTED_RATE_PASS_LATECOMERS, updatedStateDto.getRatePassLatecomers());
        assertEquals(EXPECTED_DURATION_IN_MINUTES, updatedStateDto.getSlotDurationInMinutes());
        assertEquals(EXPECTED_QTY_TO_NEXT_SLOT, updatedStateDto.getQtyFreePlacesToNextSlot());
        assertEquals(EXPECTED_PARKING_CAPACITY, updatedStateDto.getParkingCapacity());
        assertEquals(EXPECTED_MINUTES_TO_LOADING, updatedStateDto.getMinutesToArriveToLoading());
    }
}
