package ru.yandex.market.billing.agency_reward.program.purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.agency_reward.program.SubclientProgramRewardAmount;
import ru.yandex.market.billing.agency_reward.program.purchase.model.CategoryGmv;
import ru.yandex.market.billing.agency_reward.program.purchase.model.RewardTariff;
import ru.yandex.market.billing.agency_reward.program.purchase.tariff.RewardTariffFinder;
import ru.yandex.market.core.agency.program.purchase.model.ArpAgencyPartnerLink;
import ru.yandex.market.core.agency.program.purchase.model.OnBoardingRewardType;
import ru.yandex.market.core.fulfillment.model.ValueType;
import ru.yandex.market.core.supplier.model.PartnerExternalContract;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.billing.agency_reward.program.matchers.SubclientProgramRewardAmountMatchers.hasAmountKop;
import static ru.yandex.market.billing.agency_reward.program.matchers.SubclientProgramRewardAmountMatchers.hasCalcInfo;

/**
 * Тесты для {@link PurchaseRewardCalcRule}.
 */
public class PurchaseRewardCalcRuleTest extends FunctionalTest {
    private static final long AGENCY_CLID = 1000L;
    private static final long AGENCY_SUBCLID = 11111;
    private static final LocalDate LD_2021_1_1 = LocalDate.of(2021, 1, 1);
    private static final LocalDate LD_2021_2_1 = LocalDate.of(2021, 2, 1);

    @Autowired
    private PurchaseRewardCalcRule calcRule;

    static Stream<Arguments> calcArgsOnBoardingReward() {
        return Stream.of(
                Arguments.of(
                        "Полная премия",
                        new ArpAgencyPartnerLink(AGENCY_CLID, AGENCY_SUBCLID, LD_2021_1_1,
                                OnBoardingRewardType.FULL, false),
                        10L,
                        allOf(hasAmountKop(50_000_00L))),
                Arguments.of(
                        "Частичная премия",
                        new ArpAgencyPartnerLink(AGENCY_CLID, AGENCY_SUBCLID, LD_2021_1_1,
                                OnBoardingRewardType.PARTIAL, false),
                        10L,
                        allOf(hasAmountKop(30_000_00L))),
                Arguments.of(
                        "Нулевая премя",
                        new ArpAgencyPartnerLink(AGENCY_CLID, AGENCY_SUBCLID, LD_2021_1_1,
                                OnBoardingRewardType.PARTIAL, false),
                        8L,
                        allOf(hasAmountKop(0L)))
        );
    }

    @MethodSource("calcArgsOnBoardingReward")
    @ParameterizedTest(name = "{0}")
    void test_calculateOnBoardingReward(
            String description,
            ArpAgencyPartnerLink link,
            Long deliveredOrdersCount,
            Matcher<SubclientProgramRewardAmount> matcher
    ) {
        SubclientProgramRewardAmount actual
                = calcRule.calculateOnBoardingReward(link, deliveredOrdersCount, false,
                PartnerExternalContract.NULL);
        assertThat(actual, matcher);
    }

    @DisplayName("Check meta - OnBoardingReward")
    @Test
    void test_calculateOnBoardingReward_checkMeta() {
        SubclientProgramRewardAmount actual
                = calcRule.calculateOnBoardingReward(
                new ArpAgencyPartnerLink(AGENCY_CLID, AGENCY_SUBCLID, LD_2021_1_1,
                        OnBoardingRewardType.FULL, false),
                15L,
                true,
                PartnerExternalContract.NULL);
        assertThat(actual, hasCalcInfo(
                //language=json
                "{" +
                        "\"deliveredOrdersCount\":15," +
                        "\"deliveredOrdersCountThreshold\":10," +
                        "\"originalAmountKop\":5000000," +
                        "\"manuallyDisabledReward\":true" +
                        "}"
        ));
    }

    static Stream<Arguments> calcArgsGmvReward() {
        return Stream.of(
                Arguments.of(
                        "Расчет премии",
                        new ArpAgencyPartnerLink(AGENCY_CLID, AGENCY_SUBCLID, LD_2021_1_1,
                                OnBoardingRewardType.FULL, false),
                        List.of(
                                new CategoryGmv(AGENCY_SUBCLID, AGENCY_CLID, 90563L, BigDecimal.valueOf(100)),
                                new CategoryGmv(AGENCY_SUBCLID, AGENCY_CLID, 90607L, BigDecimal.valueOf(100)),
                                new CategoryGmv(AGENCY_SUBCLID, AGENCY_CLID, 90401L, BigDecimal.valueOf(100))
                        ),
                        Matchers.containsInAnyOrder(
                                allOf(hasAmountKop(2L)),
                                allOf(hasAmountKop(2L)),
                                allOf(hasAmountKop(4L)))),
                Arguments.of(
                        "Расчет премии при превышении лимита по обороту",
                        new ArpAgencyPartnerLink(AGENCY_CLID, AGENCY_SUBCLID, LD_2021_1_1,
                                OnBoardingRewardType.FULL, false),
                        List.of(
                                new CategoryGmv(AGENCY_SUBCLID, AGENCY_CLID, 90563L,
                                        BigDecimal.valueOf(6_000_000_00L)),
                                new CategoryGmv(AGENCY_SUBCLID, AGENCY_CLID, 90607L,
                                        BigDecimal.valueOf(4_000_000_00L)),
                                new CategoryGmv(AGENCY_SUBCLID, AGENCY_CLID, 90401L,
                                        BigDecimal.valueOf(10_000_000_00L))
                        ),
                        Matchers.containsInAnyOrder(
                                allOf(hasAmountKop(90000_00L)),
                                allOf(hasAmountKop(60000_00L)),
                                allOf(hasAmountKop(300000_00L))))
        );
    }

    @MethodSource("calcArgsGmvReward")
    @ParameterizedTest(name = "{0}")
    void test_calculateGmvReward(
            String description,
            ArpAgencyPartnerLink link,
            List<CategoryGmv> categoryGmv,
            Matcher<List<? extends SubclientProgramRewardAmount>> matcher
    ) {
        RewardTariffFinder tariffFinder = mock(RewardTariffFinder.class);

        doReturn(new RewardTariff(198118, null, 200, ValueType.RELATIVE))
                .when(tariffFinder).find(eq(90563L), anyLong());
        doReturn(new RewardTariff(198119, null, 200, ValueType.RELATIVE))
                .when(tariffFinder).find(eq(90607L), anyLong());
        doReturn(new RewardTariff(90401, null, 400, ValueType.RELATIVE))
                .when(tariffFinder).find(eq(90401L), anyLong());

        List<SubclientProgramRewardAmount> actual = calcRule.calculateGmvReward(
                        link.getPartnerAgencyPair(),
                        categoryGmv,
                        tariffFinder,
                        false,
                        PartnerExternalContract.NULL)
                .collect(Collectors.toList());

        assertThat(actual, matcher);
    }

}
