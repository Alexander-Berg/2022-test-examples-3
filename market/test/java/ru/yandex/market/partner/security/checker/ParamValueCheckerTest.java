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

import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * @author stani on 16.07.18.
 */
@DbUnitDataSet(before = "ParamValueCheckerTest.before.csv")
class ParamValueCheckerTest extends FunctionalTest {

    private final static Long DROPSHIP_SUPPLIER_ID = 101L;
    private final static Long NOT_DROPSHIP_SUPPLIER_ID = 102L;
    private final static Long WITHOUT_DROPSHIP_PARAM_SUPPLIER_ID = 103L;
    private final static Long FF_STATUS_PARAM_SHOP_ID = 104L;
    private final static Long NOT_EXIST_PARTNER_ID = 404L;

    @Autowired
    ParamValueChecker paramValueChecker;

    private static Stream<Arguments> data() {
        return Stream.of(
                data(Boolean.TRUE, DROPSHIP_SUPPLIER_ID, 0, new Authority("test", "DROPSHIP_AVAILABLE:true")),
                data(Boolean.FALSE, DROPSHIP_SUPPLIER_ID, 0, new Authority("test", "DROPSHIP_AVAILABLE:false")),
                data(Boolean.TRUE, NOT_DROPSHIP_SUPPLIER_ID, 0, new Authority("test", "DROPSHIP_AVAILABLE:false")),
                data(Boolean.FALSE, NOT_DROPSHIP_SUPPLIER_ID, 0, new Authority("test", "DROPSHIP_AVAILABLE:true")),
                data(Boolean.FALSE, NOT_EXIST_PARTNER_ID, 0, new Authority("test", "DROPSHIP_AVAILABLE:true")),
                data(Boolean.FALSE, NOT_DROPSHIP_SUPPLIER_ID, 0, new Authority("test", "")),
                data(Boolean.TRUE, NOT_DROPSHIP_SUPPLIER_ID, 0, new Authority("test", "DROPSHIP_AVAILABLE:false,MARKET_PILOT_PROGRAMS_MEMBER:true")),
                data(Boolean.FALSE, NOT_DROPSHIP_SUPPLIER_ID, 0, new Authority("test", "DROPSHIP_AVAILABLE:true,MARKET_PILOT_PROGRAMS_MEMBER:true")),
                data(Boolean.TRUE, WITHOUT_DROPSHIP_PARAM_SUPPLIER_ID, 0, new Authority("test", "DROPSHIP_AVAILABLE:false")),
                data(Boolean.TRUE, WITHOUT_DROPSHIP_PARAM_SUPPLIER_ID, 0, new Authority("test", "DROPSHIP_AVAILABLE:false,MARKET_PILOT_PROGRAMS_MEMBER:true")),
                data(Boolean.TRUE, DROPSHIP_SUPPLIER_ID, 0, new Authority("test", "FULFILLMENT_STATUS:DONT_WANT")),
                data(Boolean.TRUE, FF_STATUS_PARAM_SHOP_ID, 0, new Authority("test", "FULFILLMENT_STATUS:NEW")),
                data(Boolean.TRUE, DROPSHIP_SUPPLIER_ID, 0, new Authority("test", "IGNORE_STOCKS:false")),
                data(Boolean.FALSE, FF_STATUS_PARAM_SHOP_ID, 1002, new Authority("test", "UNKNOWN_CHECK:true")),
                data(Boolean.TRUE, DROPSHIP_SUPPLIER_ID, 1002, new Authority("test", "CPA_IS_PARTNER_INTERFACE:false"))
        );
    }

    private static Arguments data(Boolean expected, long shopId, long uid, Authority auth) {
        return of(expected, shopId, uid, auth);
    }

    @ParameterizedTest
    @MethodSource("data")
    void testParamCheck(Boolean expected, Long shopId, Long uid, Authority auth) {
        Assertions.assertEquals(
                expected,
                paramValueChecker.checkTyped(new MockPartnerRequest(uid, 0, shopId, shopId), auth)
        );
    }
}
