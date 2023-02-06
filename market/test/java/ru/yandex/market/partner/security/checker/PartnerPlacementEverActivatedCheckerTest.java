package ru.yandex.market.partner.security.checker;


import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

@DbUnitDataSet(before = "PartnerPlacementEverActivatedCheckerTest.csv")
public class PartnerPlacementEverActivatedCheckerTest extends FunctionalTest {

    @Autowired
    PartnerPlacementEverActivatedChecker partnerPlacementEverActivatedChecker;

    @ParameterizedTest
    @MethodSource("data")
    public void test(long campaignId, String authorityValue, boolean expected) {
        MockPartnerRequest mockPartnerRequest = new MockPartnerRequest(-1, -1L, -1L, campaignId);
        Authority authority = new Authority("test", authorityValue);
        Assertions.assertEquals(expected,
                partnerPlacementEverActivatedChecker.checkTyped(mockPartnerRequest, authority));
    }

    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(11001L, "DROPSHIP", true),
                Arguments.of(11001L, "DROPSHIP,TURBO_PLUS", true),
                Arguments.of(11001L, "FULFILLMENT,DROPSHIP", true),
                Arguments.of(11002L, "FULFILLMENT,DROPSHIP", true),
                Arguments.of(11002L, "FULFILLMENT,DROPSHIP,CLICK_AND_COLLECT", true),
                Arguments.of(11002L, "CROSSDOCK", false),
                Arguments.of(11002L, "CLICK_AND_COLLECT", false),
                Arguments.of(11004L, "TURBO_PLUS", true),
                Arguments.of(11004L, "TURBO_PLUS,DROPSHIP_BY_SELLER", true),
                Arguments.of(11005L, "DROPSHIP_BY_SELLER", false),
                Arguments.of(11005L, "CPC", false),
                Arguments.of(11005L, "DROPSHIP_BY_SELLER,CPC", false),
                // Одиночные запреты
                Arguments.of(11001L, "-DROPSHIP", false),
                Arguments.of(11004L, "-DROPSHIP", true),
                Arguments.of(11005L, "-DROPSHIP", true),
                Arguments.of(11001L, "-FULFILLMENT", false),
                Arguments.of(11004L, "-FULFILLMENT", true),
                // Множественные запреты
                Arguments.of(11001L, "-FULFILLMENT,TURBO_PLUS", false),
                Arguments.of(11002L, "-FULFILLMENT,TURBO_PLUS", false),
                Arguments.of(11004L, "-FULFILLMENT,TURBO_PLUS", false),
                Arguments.of(11005L, "-FULFILLMENT,CROSSDOCK,DROPSHIP,CLICK_AND_COLLECT,TURBO_PLUS", true),
                // Комбинации разрешений и запретов
                Arguments.of(11006L, "DROPSHIP_BY_SELLER,-TURBO_PLUS", false),
                Arguments.of(11007L, "DROPSHIP_BY_SELLER,-TURBO_PLUS", true),
                Arguments.of(11008L, "DROPSHIP_BY_SELLER,-TURBO_PLUS", false),
                Arguments.of(11006L, "DROPSHIP_BY_SELLER,-CPC", false),
                Arguments.of(11008L, "DROPSHIP_BY_SELLER,-CPC", true),
                Arguments.of(11009L, "DROPSHIP_BY_SELLER,-CPC,TURBO_PLUS", true),
                Arguments.of(11008L, "DROPSHIP_BY_SELLER,-CPC,TURBO_PLUS", false),
                Arguments.of(11010L, "DROPSHIP_BY_SELLER", true),
                Arguments.of(11011L, "FULFILLMENT", true),
                Arguments.of(11012L, "FULFILLMENT", false),
                Arguments.of(11013L, "FULFILLMENT", false)
        );
    }

}
