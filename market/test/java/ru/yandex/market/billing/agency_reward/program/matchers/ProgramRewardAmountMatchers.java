package ru.yandex.market.billing.agency_reward.program.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.billing.agency_reward.program.ProgramRewardAmount;
import ru.yandex.market.core.agency.program.ProgramRewardType;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчеры для {@link ProgramRewardAmount}.
 *
 * @author vbudnev
 */
public class ProgramRewardAmountMatchers {

    public static Matcher<ProgramRewardAmount> hasAgencyId(Long expectedValue) {
        return MbiMatchers.<ProgramRewardAmount>newAllOfBuilder()
                .add(ProgramRewardAmount::getAgencyId, expectedValue, "agencyId")
                .build();
    }

    public static Matcher<ProgramRewardAmount> hasContractId(Long expectedValue) {
        return MbiMatchers.<ProgramRewardAmount>newAllOfBuilder()
                .add(ProgramRewardAmount::getContractId, expectedValue, "contractId")
                .build();
    }

    public static Matcher<ProgramRewardAmount> hasContractEid(String expectedValue) {
        return MbiMatchers.<ProgramRewardAmount>newAllOfBuilder()
                .add(ProgramRewardAmount::getContractEid, expectedValue, "contractEid")
                .build();
    }

    public static Matcher<ProgramRewardAmount> hasAmount(Long expectedValue) {
        return MbiMatchers.<ProgramRewardAmount>newAllOfBuilder()
                .add(ProgramRewardAmount::getAmount, expectedValue, "amount")
                .build();
    }

    public static Matcher<ProgramRewardAmount> hasType(ProgramRewardType expectedValue) {
        return MbiMatchers.<ProgramRewardAmount>newAllOfBuilder()
                .add(ProgramRewardAmount::getType, expectedValue, "type")
                .build();
    }

}
