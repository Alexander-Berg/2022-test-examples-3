package ru.yandex.market.billing.agency_reward.program.cut_price;

import java.time.LocalDate;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.agency_reward.AgencyScale;
import ru.yandex.market.billing.agency_reward.program.ProgramAgencyInfo;
import ru.yandex.market.billing.agency_reward.program.SubclientProgramRewardAmount;
import ru.yandex.market.core.agency.program.cutprice.model.ARPCutpriceClientInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static ru.yandex.market.billing.agency_reward.program.matchers.SubclientProgramRewardAmountMatchers.hasAmountKop;
import static ru.yandex.market.billing.agency_reward.program.matchers.SubclientProgramRewardAmountMatchers.hasCalcInfo;

/**
 * Тесты для {@link CutPriceTariffService}.
 *
 * @author vbudnev
 */
class CutPriceTariffServiceTest extends FunctionalTest {
    private static final long AGENCY_CLID = 1000L;
    private static final long AGENCY_SUBCLID = 11111;
    private static final long CONTRACT_ID = 20L;
    private static final String CONTRACT_EID = "20/80";
    private static final LocalDate LD_2019_1_1 = LocalDate.of(2019, 1, 1);
    private static final LocalDate LD_2019_10_1 = LocalDate.of(2019, 10, 1);
    private static final boolean MANUALLY_DISABLED_REWARD = true;
    private static final boolean PROGRAM_SIGNED = true;

    @Autowired
    private CutPriceTariffService cutPriceTariffService;

    static Stream<Arguments> calcArgs() {
        return Stream.of(
                Arguments.of(
                        "Базовая сумма за подключение для msc_spb",
                        cutpriceData(2, 10),
                        new ProgramAgencyInfo(AGENCY_CLID, CONTRACT_ID, CONTRACT_EID, PROGRAM_SIGNED,
                                AgencyScale.MSC_SPB, LD_2019_1_1, LD_2019_10_1
                        ),
                        allOf(hasAmountKop(10_200_00L))
                ),
                Arguments.of(
                        "Базовая сумма за подключение для regions",
                        cutpriceData(2, 10),
                        new ProgramAgencyInfo(AGENCY_CLID, CONTRACT_ID, CONTRACT_EID, PROGRAM_SIGNED,
                                AgencyScale.REGIONS, LD_2019_1_1, LD_2019_10_1
                        ),
                        allOf(hasAmountKop(5_200_00L))
                ),
                Arguments.of(
                        "Ограничение сверху для одного субклиента для msc_spb",
                        cutpriceData(1000, 10),
                        new ProgramAgencyInfo(AGENCY_CLID, CONTRACT_ID, CONTRACT_EID, PROGRAM_SIGNED,
                                AgencyScale.MSC_SPB, LD_2019_1_1, LD_2019_10_1
                        ),
                        allOf(hasAmountKop(20_000_00L))
                ),
                Arguments.of(
                        "Ограничение сверху для одного субклиента для regions",
                        cutpriceData(1000, 10),
                        new ProgramAgencyInfo(AGENCY_CLID, CONTRACT_ID, CONTRACT_EID, PROGRAM_SIGNED,
                                AgencyScale.REGIONS, LD_2019_1_1, LD_2019_10_1
                        ),
                        allOf(hasAmountKop(10_000_00L))
                ),
                Arguments.of(
                        "Выплата 0, если не подписано ДС",
                        cutpriceData(2, 10),
                        new ProgramAgencyInfo(AGENCY_CLID, CONTRACT_ID, CONTRACT_EID, !PROGRAM_SIGNED,
                                AgencyScale.MSC_SPB, LD_2019_1_1, LD_2019_10_1
                        ),
                        hasAmountKop(0L)
                ),
                Arguments.of(
                        "Выплата 0, если кол-во дней не переходит порог",
                        cutpriceData(2, 9),
                        new ProgramAgencyInfo(AGENCY_CLID, CONTRACT_ID, CONTRACT_EID, !PROGRAM_SIGNED,
                                AgencyScale.MSC_SPB, LD_2019_1_1, LD_2019_10_1
                        ),
                        hasAmountKop(0L)
                )
        );
    }

    private static ARPCutpriceClientInfo cutpriceData(final int offersCount, final int offersDays) {
        return ARPCutpriceClientInfo.builder()
                .agencyId(AGENCY_CLID)
                .clientId(AGENCY_SUBCLID)
                .offersCount(offersCount)
                .offersDays(offersDays)
                .commitDate(LocalDate.now())
                .build();
    }

    @MethodSource("calcArgs")
    @ParameterizedTest(name = "{0}")
    void test_calculateReward(
            String description,
            ARPCutpriceClientInfo data,
            ProgramAgencyInfo info,
            Matcher<SubclientProgramRewardAmount> matcher
    ) {
        SubclientProgramRewardAmount actual
                = cutPriceTariffService.calculateReward(data, info, !MANUALLY_DISABLED_REWARD);
        assertThat(actual, matcher);
    }

    @DisplayName("Выплата 0, если вручную отключили начисления")
    @Test
    void test_calculateReward_0rewardForManuallyDisabled() {
        SubclientProgramRewardAmount actual = cutPriceTariffService.calculateReward(
                cutpriceData(1000, 10),
                new ProgramAgencyInfo(AGENCY_CLID, CONTRACT_ID, CONTRACT_EID, PROGRAM_SIGNED, AgencyScale.MSC_SPB,
                        LD_2019_1_1, LD_2019_10_1
                ),
                MANUALLY_DISABLED_REWARD
        );
        assertThat(actual, hasAmountKop(0L));
    }

    @DisplayName("Check meta")
    @Test
    void test_calculateReward_checkMeta() {
        SubclientProgramRewardAmount actual = cutPriceTariffService.calculateReward(
                cutpriceData(1000, 10),
                new ProgramAgencyInfo(AGENCY_CLID, CONTRACT_ID, CONTRACT_EID, PROGRAM_SIGNED, AgencyScale.MSC_SPB,
                        LD_2019_1_1, LD_2019_10_1
                ),
                MANUALLY_DISABLED_REWARD
        );
        assertThat(actual, hasCalcInfo(
                //language=json
                "{" +
                        "\"curPriceProgramSignedStatus\":true," +
                        "\"offersDays\":10," +
                        "\"tresholdOffersDays\":10," +
                        "\"offersCount\":1000," +
                        "\"agencyScale\":\"MSC_SPB\"," +
                        "\"tariffPerOfferKop\":10000," +
                        "\"tariffActivationKop\":1000000," +
                        "\"tariffMaxSubclientRewardKop\":2000000," +
                        "\"originalAmountKop\":0," +
                        "\"manuallyDisabledReward\":true" +
                        "}"
        ));
    }

}