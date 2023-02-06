package ru.yandex.market.billing.agency_reward.program.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.billing.agency_reward.program.SubclientProgramRewardAmount;
import ru.yandex.market.core.agency.program.ProgramRewardType;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчеры для {@link SubclientProgramRewardAmount}.
 *
 * @author vbudnev
 */
public class SubclientProgramRewardAmountMatchers {

    public static Matcher<SubclientProgramRewardAmount> hasAgencyId(Long expectedValue) {
        return MbiMatchers.<SubclientProgramRewardAmount>newAllOfBuilder()
                .add(SubclientProgramRewardAmount::getAgencyId, expectedValue, "agencyId")
                .build();
    }

    public static Matcher<SubclientProgramRewardAmount> hasSubclientId(Long expectedValue) {
        return MbiMatchers.<SubclientProgramRewardAmount>newAllOfBuilder()
                .add(SubclientProgramRewardAmount::getSubclientId, expectedValue, "subclientId")
                .build();
    }

    public static Matcher<SubclientProgramRewardAmount> hasContractId(Long expectedValue) {
        return MbiMatchers.<SubclientProgramRewardAmount>newAllOfBuilder()
                .add(SubclientProgramRewardAmount::getContractId, expectedValue, "contractId")
                .build();
    }

    public static Matcher<SubclientProgramRewardAmount> hasContractEid(String expectedValue) {
        return MbiMatchers.<SubclientProgramRewardAmount>newAllOfBuilder()
                .add(SubclientProgramRewardAmount::getContractEid, expectedValue, "contractEid")
                .build();
    }

    public static Matcher<SubclientProgramRewardAmount> hasAmountKop(Long expectedValue) {
        return MbiMatchers.<SubclientProgramRewardAmount>newAllOfBuilder()
                .add(SubclientProgramRewardAmount::getAmountKop, expectedValue, "amountKop")
                .build();
    }

    public static Matcher<SubclientProgramRewardAmount> hasProgramType(ProgramRewardType expectedValue) {
        return MbiMatchers.<SubclientProgramRewardAmount>newAllOfBuilder()
                .add(SubclientProgramRewardAmount::getProgramType, expectedValue, "programType")
                .build();
    }


    public static Matcher<SubclientProgramRewardAmount> hasCalcInfo(String expectedValue) {
        return MbiMatchers.<SubclientProgramRewardAmount>newAllOfBuilder()
                .add(SubclientProgramRewardAmount::getCalcInfo, expectedValue, "calcInfo")
                .build();
    }
}
