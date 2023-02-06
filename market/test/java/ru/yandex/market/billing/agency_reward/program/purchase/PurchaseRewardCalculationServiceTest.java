package ru.yandex.market.billing.agency_reward.program.purchase;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.agency_reward.program.ProgramRewardAmount;
import ru.yandex.market.billing.agency_reward.program.SubclientProgramRewardAmount;
import ru.yandex.market.billing.agency_reward.program.matchers.ProgramRewardAmountMatchers;
import ru.yandex.market.core.agency.program.ProgramRewardType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

/**
 * Тесты для {@link PurchaseRewardCalculationService}.
 */
public class PurchaseRewardCalculationServiceTest extends FunctionalTest {
    @Autowired
    private PurchaseRewardCalculationService purchaseProgramRewardCalculationService;

    @Test
    void test_calcRewardsByAgency() {
        List<ProgramRewardAmount> res = purchaseProgramRewardCalculationService.calcRewardsByAgency(Map.of(
                ProgramRewardType.ON_BOARDING, List.of(
                        SubclientProgramRewardAmount.newBuilder()
                                .setAgencyId(1)
                                .setSubclientId(11)
                                .setAmountKop(1)
                                .setProgramType(ProgramRewardType.ON_BOARDING)
                                .setCalcInfo("someCalcInfo")
                                .build(),
                        SubclientProgramRewardAmount.newBuilder()
                                .setAgencyId(2)
                                .setSubclientId(23)
                                .setAmountKop(1)
                                .setProgramType(ProgramRewardType.ON_BOARDING)
                                .setCalcInfo("someCalcInfo")
                                .build()),
                ProgramRewardType.GMV, List.of(
                        SubclientProgramRewardAmount.newBuilder()
                                .setAgencyId(1)
                                .setSubclientId(12)
                                .setAmountKop(1)
                                .setProgramType(ProgramRewardType.GMV)
                                .setCalcInfo("someCalcInfo")
                                .build(),
                        SubclientProgramRewardAmount.newBuilder()
                                .setAgencyId(1)
                                .setSubclientId(13)
                                .setAmountKop(1)
                                .setProgramType(ProgramRewardType.GMV)
                                .setCalcInfo("someCalcInfo")
                                .build(),
                        SubclientProgramRewardAmount.newBuilder()
                                .setAgencyId(1)
                                .setSubclientId(13)
                                .setAmountKop(1)
                                .setProgramType(ProgramRewardType.GMV)
                                .setCalcInfo("someCalcInfo")
                                .build(),
                        SubclientProgramRewardAmount.newBuilder()
                                .setAgencyId(2)
                                .setSubclientId(21)
                                .setAmountKop(1)
                                .setProgramType(ProgramRewardType.GMV)
                                .setCalcInfo("someCalcInfo")
                                .build())
        ));

        assertThat(res, hasSize(4));
        assertThat(res, containsInAnyOrder(
                allOf(
                        ProgramRewardAmountMatchers.hasAgencyId(1L),
                        ProgramRewardAmountMatchers.hasAmount(1L),
                        ProgramRewardAmountMatchers.hasType(ProgramRewardType.ON_BOARDING)
                ),
                allOf(
                        ProgramRewardAmountMatchers.hasAgencyId(1L),
                        ProgramRewardAmountMatchers.hasAmount(3L),
                        ProgramRewardAmountMatchers.hasType(ProgramRewardType.GMV)
                ),
                allOf(
                        ProgramRewardAmountMatchers.hasAgencyId(2L),
                        ProgramRewardAmountMatchers.hasAmount(1L),
                        ProgramRewardAmountMatchers.hasType(ProgramRewardType.ON_BOARDING)
                ),
                allOf(
                        ProgramRewardAmountMatchers.hasAgencyId(2L),
                        ProgramRewardAmountMatchers.hasAmount(1L),
                        ProgramRewardAmountMatchers.hasType(ProgramRewardType.GMV)
                )
        ));
    }
}
