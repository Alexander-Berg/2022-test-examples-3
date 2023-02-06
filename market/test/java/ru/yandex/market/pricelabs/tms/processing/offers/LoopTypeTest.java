package ru.yandex.market.pricelabs.tms.processing.offers;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.services.database.model.JobType;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoopTypeTest {

    @Test
    void testTypes() {
        assertEquals(JobType.SHOP_LOOP_FULL, LoopType.FULL.getNormalJobType());
        assertEquals(JobType.SHOP_LOOP_FULL_PRIORITY, LoopType.FULL.getPriorityJobType());
    }

}
