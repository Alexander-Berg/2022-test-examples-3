package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.model.SupplierType;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;


class PartnerPlacementTypeCheckerTest extends FunctionalTest {

    static private final Pair<String, Long> FULFILLMENT = new Pair<>("FULFILLMENT", 1L);
    static private final Pair<String, Long> DROPSHIP_NOT_CC = new Pair<>("DROPSHIP_NOT_CC", 2L);
    static private final Pair<String, Long> CLICK_COLLECT = new Pair<>("CLICK_COLLECT", 3L);
    static private final Pair<String, Long> CROSSDOCK = new Pair<>("CROSSDOC", 4L);
    static private final Pair<String, Long> DROPSHIP_BY_SELLER_NOT_CPC = new Pair<>("DROPSHIP_BY_SELLER_NOT_CPC", 6L);
    static private final Pair<String, Long> TURBO_DROPSHIP_BY_SELLER_CPC = new Pair<>("DROPSHIP_BY_SELLER_CPC", 7L);
    static private final Pair<String, Long> CPC_NOT_DSBS = new Pair<>("CPC_NOT_DSBS", 9L);
    static private final Pair<String, Long> NOT_CPC_NOT_DSBS = new Pair<>("NOT_CPC_NOT_DSBS", 8L);

    static private Pair<String, Long> ONE_PI = new Pair<>("FIRST_PARTY", 71L);


    @Autowired
    private PartnerPlacementTypeChecker partnerPlacementTypeChecker;

    static private Arguments getArguments(String authorityValue, Pair<String, Long> supplier, Boolean expected) {
        String testDescription = String.format(
                "Supplier %s should be %s by query '%s'",
                supplier.getFirst(),
                expected ? "allowed" : "forbidden",
                authorityValue
        );
        return Arguments.of(testDescription, supplier.getSecond(), authorityValue, expected);
    }

    public static Stream<Arguments> args() {
        return Stream.of(
                // Одиночные разрешения
                getArguments("FULFILLMENT", FULFILLMENT, Boolean.TRUE),
                getArguments("FULFILLMENT", DROPSHIP_NOT_CC, Boolean.FALSE),
                getArguments("FULFILLMENT", CLICK_COLLECT, Boolean.FALSE),
                getArguments("FULFILLMENT", CROSSDOCK, Boolean.FALSE),
                getArguments("DROPSHIP", DROPSHIP_NOT_CC, Boolean.TRUE),
                getArguments("DROPSHIP", CLICK_COLLECT, Boolean.TRUE),
                getArguments("CLICK_AND_COLLECT", CLICK_COLLECT, Boolean.TRUE),
                getArguments("CLICK_AND_COLLECT", DROPSHIP_NOT_CC, Boolean.FALSE),
                getArguments("CROSSDOCK", CROSSDOCK, Boolean.TRUE),
                getArguments("DROPSHIP_BY_SELLER", DROPSHIP_BY_SELLER_NOT_CPC, Boolean.TRUE),
                getArguments("DROPSHIP_BY_SELLER", DROPSHIP_BY_SELLER_NOT_CPC, Boolean.TRUE),
                // Множественные разрешения
                getArguments("FULFILLMENT,DROPSHIP", FULFILLMENT, Boolean.TRUE),
                getArguments("FULFILLMENT,DROPSHIP", DROPSHIP_NOT_CC, Boolean.TRUE),
                getArguments("FULFILLMENT,DROPSHIP", CLICK_COLLECT, Boolean.TRUE),
                getArguments("FULFILLMENT,DROPSHIP", CROSSDOCK, Boolean.FALSE),
                getArguments("CPC,DROPSHIP_BY_SELLER", NOT_CPC_NOT_DSBS, Boolean.FALSE),
                // Одиночные запреты
                getArguments("-DROPSHIP", FULFILLMENT, Boolean.TRUE),
                getArguments("-DROPSHIP", DROPSHIP_NOT_CC, Boolean.FALSE),
                getArguments("-DROPSHIP", CLICK_COLLECT, Boolean.FALSE),
                getArguments("-DROPSHIP", CROSSDOCK, Boolean.TRUE),
                getArguments("-TURBO_PLUS", TURBO_DROPSHIP_BY_SELLER_CPC, Boolean.FALSE),
                getArguments("-CPC", TURBO_DROPSHIP_BY_SELLER_CPC, Boolean.FALSE),
                getArguments("-CPC", DROPSHIP_BY_SELLER_NOT_CPC, Boolean.TRUE),
                // Множественные запреты
                getArguments("-FULFILLMENT,CROSSDOCK", FULFILLMENT, Boolean.FALSE),
                getArguments("-FULFILLMENT,CROSSDOCK", DROPSHIP_NOT_CC, Boolean.TRUE),
                getArguments("-FULFILLMENT,CROSSDOCK", CLICK_COLLECT, Boolean.TRUE),
                getArguments("-FULFILLMENT,CROSSDOCK", CROSSDOCK, Boolean.FALSE),
                // Комбинации разрешений и запретов
                getArguments("DROPSHIP,-CLICK_AND_COLLECT", FULFILLMENT, Boolean.FALSE),
                getArguments("DROPSHIP,-CLICK_AND_COLLECT", DROPSHIP_NOT_CC, Boolean.TRUE),
                getArguments("DROPSHIP,-CLICK_AND_COLLECT", CLICK_COLLECT, Boolean.FALSE),
                getArguments("DROPSHIP,-CLICK_AND_COLLECT", CROSSDOCK, Boolean.FALSE),
                getArguments("DROPSHIP_BY_SELLER,-TURBO_PLUS", TURBO_DROPSHIP_BY_SELLER_CPC, Boolean.FALSE),
                getArguments("DROPSHIP_BY_SELLER,-TURBO_PLUS", DROPSHIP_BY_SELLER_NOT_CPC, Boolean.TRUE),
                getArguments("DROPSHIP_BY_SELLER,-CPC", DROPSHIP_BY_SELLER_NOT_CPC, Boolean.TRUE),
                getArguments("DROPSHIP_BY_SELLER,-CPC", TURBO_DROPSHIP_BY_SELLER_CPC, Boolean.FALSE),
                getArguments("CPC,-DROPSHIP_BY_SELLER", CPC_NOT_DSBS, Boolean.TRUE),
                getArguments("CPC,-DROPSHIP_BY_SELLER", TURBO_DROPSHIP_BY_SELLER_CPC, Boolean.FALSE),
                // Опечатки и неизвестные элементы должны игнорироваться
                getArguments("FFFFULFILLMENT", FULFILLMENT, Boolean.FALSE),
                // Элементы условия должны тримиться
                getArguments("  DROPSHIP  ,  -CLICK_AND_COLLECT  ", DROPSHIP_NOT_CC, Boolean.TRUE),
                // _PARTY
                getArguments("FIRST_PARTY", ONE_PI, Boolean.TRUE),
                getArguments("THIRD_PARTY", ONE_PI, Boolean.FALSE),
                getArguments("-FIRST_PARTY", ONE_PI, Boolean.FALSE),
                // extra spaces is OK
                getArguments("   FIRST_PARTY,           -THIRD_PARTY   ", ONE_PI, Boolean.TRUE),
                // undefined behavior
                getArguments("FIRST_PARTY,-THIRD_PARTY", ONE_PI, Boolean.TRUE)
        );
    }

    @ParameterizedTest
    @MethodSource("args")
    @DbUnitDataSet(before = "partnerPlacementTypeTableCheckerTest.csv")
    void testWithTable(String testDescription, long campaignId, String authorityValue, boolean expected) {
        MockPartnerRequest mockPartnerRequest = new MockPartnerRequest(-1, -1L, -1L, campaignId);
        Authority authority = new Authority("test", authorityValue);
        Assertions.assertEquals(
                expected,
                partnerPlacementTypeChecker.checkTyped(mockPartnerRequest, authority),
                testDescription
        );
    }
}
