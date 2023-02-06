package ru.yandex.market.billing.agency_reward.program.cut_price;

import java.time.LocalDate;
import java.util.Optional;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.agency.program.ProgramRewardType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link CutPriceRewardControlService}.
 *
 * @author vbudnev
 */
@DbUnitDataSet(before = "CutPriceRewardControlServiceTest.before.csv")
class CutPriceRewardControlServiceTest extends FunctionalTest {
    @Autowired
    private CutPriceRewardControlService cutPriceRewardControlService;

    @Test
    void test_getIgnoredAgencyContractsOnImport() {
        assertEquals(
                ImmutableSet.of(222L, 111L),
                cutPriceRewardControlService.getIgnoredAgencyContractsOnImport()
        );
    }

    @Test
    void test_isPublicationForBalanceEnabled() {
        assertTrue(cutPriceRewardControlService.isPublicationForBalanceEnabled());
    }

    @Test
    void test_zeroRewardProgramAgencyClients() {
        assertEquals(
                ImmutableSet.of(666L, 555L),
                cutPriceRewardControlService.zeroRewardProgramAgencyClients(ProgramRewardType.CUT_PRICE)
        );
    }

    @Test
    void test_getLoadAgencyInfoDate() {
        assertEquals(
                Optional.of(LocalDate.of(2019, 1, 10)),
                cutPriceRewardControlService.getLoadAgencyInfoDate()
        );
    }
}
