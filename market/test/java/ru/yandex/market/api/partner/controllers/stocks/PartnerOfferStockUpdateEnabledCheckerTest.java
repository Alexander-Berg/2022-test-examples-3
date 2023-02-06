package ru.yandex.market.api.partner.controllers.stocks;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.controllers.stocks.checkers.PartnerOfferStockUpdateEnabledChecker;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.core.param.model.ParamType.PARTNER_API_PUSH_STOCKS_ENABLE;

@DbUnitDataSet(before = "PartnerOfferStockUpdateEnabledCheckerTest.before.csv")
class PartnerOfferStockUpdateEnabledCheckerTest extends FunctionalTest {
    @Autowired
    ParamService paramService;

    PartnerOfferStockUpdateEnabledChecker checker;

    @BeforeEach
    void setUp() {
        checker = new PartnerOfferStockUpdateEnabledChecker(paramService);
    }

    @ParameterizedTest(name = "[{index}] value: \"{0}\", partnerId: {1}, expected: {2}")
    @MethodSource("provideArgumentsForIsEnabledForPartner")
    void isEnabledForPartner(List<Long> enabledPartnerIds,
                             List<Long> disabledPartnerIds, long partnerId,
                             boolean expected) {
        setEnabledPartners(enabledPartnerIds, disabledPartnerIds);

        assertThat(checker.isEnabledForPartner(partnerId), equalTo(expected));
    }

    private static Stream<Arguments> provideArgumentsForIsEnabledForPartner() {
        return Stream.of(
                Arguments.of(
                        List.of(190L, 388L, 100L, 238L, 190L, 388L),
                        List.of(),
                        100L,
                        true
                ),
                Arguments.of(
                        List.of(190L, 388L, 100L, 238L, 190L, 388L),
                        List.of(),
                        101L,
                        true),
                Arguments.of(
                        List.of(190L, 388L, 100L, 238L, 190L, 388L),
                        List.of(101L),
                        101L,
                        false
                )
        );
    }

    private void setEnabledPartners(List<Long> enabledPartnerIds, List<Long> disabledPartnerIds) {
        for (var partner : enabledPartnerIds) {
            paramService.setParam(new BooleanParamValue(PARTNER_API_PUSH_STOCKS_ENABLE, partner, true), 1);
        }
        for (var partner : disabledPartnerIds) {
            paramService.setParam(new BooleanParamValue(PARTNER_API_PUSH_STOCKS_ENABLE, partner, false), 1);
        }
    }
}
