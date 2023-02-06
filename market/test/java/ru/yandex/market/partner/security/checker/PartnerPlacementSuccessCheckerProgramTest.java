package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

@DbUnitDataSet(before = "partnerPlacementSuccessCheckerTestProgram.before.csv")
class PartnerPlacementSuccessCheckerProgramTest extends FunctionalTest {

    @Autowired
    private PartnerPlacementSuccessChecker partnerPlacementSuccessChecker;

    public static Stream<Arguments> args() {
        return Stream.of(
                //dropship feature success/ have cutoff
                Arguments.of(24L, "DROPSHIP", false),
                //dropship feature fail/ have cutoff
                Arguments.of(25L, "DROPSHIP", false),
                //crossdock feature success/ have cutoff
                Arguments.of(26L, "CROSSDOCK", false),
                //crossdock feature fail/ have cutoff
                Arguments.of(27L, "CROSSDOCK", false),
                //dbs feature success/ have cutoff
                Arguments.of(28L, "DROPSHIP_BY_SELLER", false),
                //dbs feature fail/ have cutoff
                Arguments.of(29L, "DROPSHIP_BY_SELLER", false),
                // success crossdock
                Arguments.of(1L, "CROSSDOCK", true),
                // crossdock restricted
                Arguments.of(1L, "-CROSSDOCK", false),
                // success dropship
                Arguments.of(2L, "DROPSHIP", true),
                // dropship restricted
                Arguments.of(2L, "-DROPSHIP", false),
                // success click and collect
                Arguments.of(3L, "CLICK_AND_COLLECT", true),
                // click and collect restricted
                Arguments.of(3L, "-CLICK_AND_COLLECT", false),
                // success fulfillment
                Arguments.of(4L, "FULFILLMENT", true),
                // fulfillment restricted
                Arguments.of(4L, "-FULFILLMENT", false),
                // crossdock is not dropship
                Arguments.of(1L, "DROPSHIP", false),
                // crossdock is not restricted
                Arguments.of(1L, "-DROPSHIP", true),
                // crossdock is not click and collect
                Arguments.of(1L, "CLICK_AND_COLLECT", false),
                // crossdock is not fullfilment
                Arguments.of(1L, "FULFILLMENT", false),
                // dropship is not crossdock
                Arguments.of(2L, "CROSSDOCK", false),
                // dropship is not click and collect
                Arguments.of(2L, "CLICK_AND_COLLECT", false),
                // dropship is not fulfillment
                Arguments.of(2L, "FULFILLMENT", false),
                // click and collect is not crossdock
                Arguments.of(3L, "CROSSDOCK", false),
                // click and collect is not dropship
                Arguments.of(3L, "DROPSHIP", false),
                // click and collect is not fulfillment
                Arguments.of(3L, "FULFILLMENT", false),
                // fulfillment is not crossdock
                Arguments.of(4L, "CROSSDOCK", false),
                // fulfillment is not dropship
                Arguments.of(4L, "DROPSHIP", false),
                // fulfillment is not click and collect
                Arguments.of(4L, "CLICK_AND_COLLECT", false),
                // crossdock marketplace feature == failed
                Arguments.of(6L, "CROSSDOCK", false),
                // crossdock crossdock feature == failed
                Arguments.of(7L, "CROSSDOCK", false),
                // dropship marketplace feature == failed
                Arguments.of(8L, "DROPSHIP", false),
                // dropship dropship feature == failed
                Arguments.of(9L, "DROPSHIP", false),
                // dropship dropship available == false
                Arguments.of(10L, "DROPSHIP", false),
                // click and collect marketplace feature == fail
                Arguments.of(11L, "CLICK_AND_COLLECT", false),
                // click and collect dropship feature == fail
                Arguments.of(12L, "CLICK_AND_COLLECT", false),
                // click and collect dropship available == false
                Arguments.of(13L, "CLICK_AND_COLLECT", false),
                // click and collect click and collect param == false
                Arguments.of(14L, "CLICK_AND_COLLECT", false),
                // fulfillment marketplace feature == fail
                Arguments.of(15L, "FULFILLMENT", false),
                // multiple params
                Arguments.of(1L, "DROPSHIP,CLICK_AND_COLLECT,CROSSDOCK", true),
                // wrong param
                Arguments.of(1L, "CROSSDOC", false),
                // wrong restricted param
                Arguments.of(1L, "-CROSSDOC", false),
                // empty param
                Arguments.of(1L, "", false),
                // partially wrong params
                Arguments.of(1L, "DROPSHIB,CLICK_AND_COLLECT,CROSSDOCK", true),
                // partially wrong restricted params
                Arguments.of(1L, "-DROPSHIP,CLICK_AND_COLLECT,CROSSDOC", true),
                // success dropship for shop
                Arguments.of(20L, "DROPSHIP_BY_SELLER", true),
                // success turbo_plus for shop
                Arguments.of(21L, "TURBO_PLUS", true),
                // fail turbo_plus as self-delivery is failed
                Arguments.of(22L, "TURBO_PLUS", false),
                // fail turbo_plus as marketplace is failed
                Arguments.of(23L, "TURBO_PLUS", false),
                // fail cpc for shop
                Arguments.of(21L, "CPC", false),
                // success cpc for shop
                Arguments.of(20L, "CPC", true),
                // fail cpc for supplier
                Arguments.of(3L, "CPC", false),
                //unprocessing by checker type
                Arguments.of(3L, "FIRST_PARTY", false),
                // crossdock allowed and restricted
                Arguments.of(1L, "CROSSDOCK,-CROSSDOCK", false),
                // crossdock is allowed and other restricted
                Arguments.of(1L, "CROSSDOCK,-CPC", true),
                // crossdock is restricted and other allowed
                Arguments.of(1L, "CPC,-CROSSDOCK", false)
        );
    }

    @DisplayName("Тест расчета чекера на программах")
    @ParameterizedTest
    @MethodSource("args")
    public void testProgramChecker(long campaignId, String authorityValue, boolean expected) {
        MockPartnerRequest mockPartnerRequest = new MockPartnerRequest(-1, -1L, -1L, campaignId);
        Authority authority = new Authority("test", authorityValue);
        Assertions.assertEquals(expected, partnerPlacementSuccessChecker.checkTyped(mockPartnerRequest, authority));
    }
}
