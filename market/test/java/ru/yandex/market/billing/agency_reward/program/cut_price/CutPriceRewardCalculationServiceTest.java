package ru.yandex.market.billing.agency_reward.program.cut_price;

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
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Тесты для {@link CutPriceRewardCalculationService}.
 *
 * @author vbudnev
 */
class CutPriceRewardCalculationServiceTest extends FunctionalTest {
    @Autowired
    private CutPriceRewardCalculationService calculationService;

    @Test
    void test_calcRewardsByAgency() {
        List<ProgramRewardAmount> res = calculationService.calcRewardsByAgency(Map.of(
                ProgramRewardType.CUT_PRICE, List.of(
                    SubclientProgramRewardAmount.newBuilder()
                            .setAgencyId(1)
                            .setSubclientId(11)
                            .setContractId(111L)
                            .setContractEid("111/111")
                            .setAmountKop(1)
                            .setProgramType(ProgramRewardType.CUT_PRICE)
                            .setCalcInfo("someCalcInfo")
                            .build(),
                    SubclientProgramRewardAmount.newBuilder()
                            .setAgencyId(1)
                            .setSubclientId(12)
                            .setContractId(111L)
                            .setContractEid("111/111")
                            .setAmountKop(1)
                            .setProgramType(ProgramRewardType.CUT_PRICE)
                            .setCalcInfo("someCalcInfo")
                            .build(),
                    SubclientProgramRewardAmount.newBuilder()
                            .setAgencyId(1)
                            .setSubclientId(13)
                            .setContractId(111L)
                            .setContractEid("111/111")
                            .setAmountKop(1)
                            .setProgramType(ProgramRewardType.CUT_PRICE)
                            .setCalcInfo("someCalcInfo")
                            .build(),
                    SubclientProgramRewardAmount.newBuilder()
                            .setAgencyId(2)
                            .setSubclientId(21)
                            .setContractId(222L)
                            .setContractEid("222/222")
                            .setAmountKop(1)
                            .setProgramType(ProgramRewardType.CUT_PRICE)
                            .setCalcInfo("someCalcInfo")
                            .build(),
                    SubclientProgramRewardAmount.newBuilder()
                            .setAgencyId(2)
                            .setSubclientId(23)
                            .setContractId(222L)
                            .setContractEid("222/222")
                            .setAmountKop(1)
                            .setProgramType(ProgramRewardType.CUT_PRICE)
                            .setCalcInfo("someCalcInfo")
                            .build())
        ));

        assertThat(res, containsInAnyOrder(
                allOf(
                        ProgramRewardAmountMatchers.hasAgencyId(1L),
                        ProgramRewardAmountMatchers.hasAmount(3L),
                        ProgramRewardAmountMatchers.hasContractId(111L),
                        ProgramRewardAmountMatchers.hasContractEid("111/111"),
                        ProgramRewardAmountMatchers.hasType(ProgramRewardType.CUT_PRICE)
                ),
                allOf(
                        ProgramRewardAmountMatchers.hasAgencyId(2L),
                        ProgramRewardAmountMatchers.hasAmount(2L),
                        ProgramRewardAmountMatchers.hasContractId(222L),
                        ProgramRewardAmountMatchers.hasContractEid("222/222"),
                        ProgramRewardAmountMatchers.hasType(ProgramRewardType.CUT_PRICE)
                )
        ));
    }
}
