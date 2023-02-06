package ru.yandex.market.ff.service.implementation.timeslot;


import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import ru.yandex.market.ff.client.enums.SlotPriorityType;
import ru.yandex.market.ff.enums.CalendaringType;
import ru.yandex.market.ff.model.enums.DatePeriodsNeighborshipType;
import ru.yandex.market.ff.service.timeslot.SlotPriorityTypeServiceImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class SlotPriorityTypeServiceUnitTest {

    @InjectMocks
    private SlotPriorityTypeServiceImpl service;


    @Test
    void getSlotPriorityTypeSupplyAndSupplyTest() {
        assertEquals(SlotPriorityType.UNDEFINED,
                service.getSlotPriorityType(CalendaringType.INBOUND, DatePeriodsNeighborshipType.SAME_TIME,
                        CalendaringType.INBOUND
                ));

        assertEquals(SlotPriorityType.SUPPLY_AFTER_SUPPLY,
                service.getSlotPriorityType(CalendaringType.INBOUND, DatePeriodsNeighborshipType.AFTER,
                        CalendaringType.INBOUND
                ));

        assertEquals(SlotPriorityType.SUPPLY_AFTER_SUPPLY,
                service.getSlotPriorityType(CalendaringType.INBOUND, DatePeriodsNeighborshipType.BEFORE,
                        CalendaringType.INBOUND
                ));
    }

    @Test
    void getSlotPriorityTypeWithdrawAndWithdrawTest() {
        assertEquals(SlotPriorityType.WITHDRAW_SAME_TIME_WITHDRAW,
                service.getSlotPriorityType(CalendaringType.OUTBOUND, DatePeriodsNeighborshipType.SAME_TIME,
                        CalendaringType.OUTBOUND
                ));

        assertEquals(SlotPriorityType.UNDEFINED,
                service.getSlotPriorityType(CalendaringType.OUTBOUND, DatePeriodsNeighborshipType.AFTER,
                        CalendaringType.OUTBOUND
                ));

        assertEquals(SlotPriorityType.UNDEFINED,
                service.getSlotPriorityType(CalendaringType.OUTBOUND, DatePeriodsNeighborshipType.BEFORE,
                        CalendaringType.OUTBOUND
                ));
    }


    @Test
    void getSlotPriorityTypeSupplyAfterWithdrawTest() {

        assertEquals(SlotPriorityType.SUPPLY_AFTER_WITHDRAW,
                service.getSlotPriorityType(CalendaringType.INBOUND, DatePeriodsNeighborshipType.AFTER,
                        CalendaringType.OUTBOUND
                ));

        assertEquals(SlotPriorityType.SUPPLY_AFTER_WITHDRAW,
                service.getSlotPriorityType(CalendaringType.OUTBOUND, DatePeriodsNeighborshipType.BEFORE,
                        CalendaringType.INBOUND
                ));
    }

    @Test
    void getSlotPriorityTypeWithdrawAfterSupplyTest() {
        assertEquals(SlotPriorityType.WITHDRAW_AFTER_SUPPLY,
                service.getSlotPriorityType(CalendaringType.OUTBOUND, DatePeriodsNeighborshipType.AFTER,
                        CalendaringType.INBOUND
                ));

        assertEquals(SlotPriorityType.WITHDRAW_AFTER_SUPPLY,
                service.getSlotPriorityType(CalendaringType.INBOUND, DatePeriodsNeighborshipType.BEFORE,
                        CalendaringType.OUTBOUND
                ));
    }

}
