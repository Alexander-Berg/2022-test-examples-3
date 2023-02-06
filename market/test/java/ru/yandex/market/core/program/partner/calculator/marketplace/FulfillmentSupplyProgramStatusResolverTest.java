package ru.yandex.market.core.program.partner.calculator.marketplace;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.program.partner.model.ProgramArgs;
import ru.yandex.market.core.program.partner.model.ProgramStatus;
import ru.yandex.market.core.program.partner.model.Status;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class FulfillmentSupplyProgramStatusResolverTest extends FunctionalTest {

    @Autowired
    private FulfillmentSupplyProgramStatusResolver tested;

    @Test
    @DbUnitDataSet(before = "FulfillmentSupplyProgramStatusResolverTest.success.before.csv")
    void testFFSupplyExists() {
        Optional<ProgramStatus.Builder> result = tested.resolve(1L, ProgramArgs.builder().build());

        assertTrue(result.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentSupplyProgramStatusResolverTest.fail.before.csv")
    void testFFSupplyNotExists() {
        Optional<ProgramStatus.Builder> result = tested.resolve(1L, ProgramArgs.builder().build());

        assertFalse(result.isEmpty());
        assertFalse(result.get().getEnabled());
        assertEquals(Status.ENABLING, result.get().getStatus());
    }

    @Test
    @DbUnitDataSet(before = "FulfillmentSupplyProgramStatusResolverTest.success.notnewbie.before.csv")
    void testFFSupplyNotExistsNotNewbie() {
        Optional<ProgramStatus.Builder> result = tested.resolve(1L, ProgramArgs.builder().build());

        assertTrue(result.isEmpty());
    }
}
