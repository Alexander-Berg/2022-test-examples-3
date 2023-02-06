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

@DbUnitDataSet(before = "ModifyRegionGroupCheckerTest.before.csv")
public class ModifyRegionGroupCheckerTest extends FunctionalTest {
    @Autowired
    private ModifyRegionGroupChecker modifyRegionGroupChecker;

    private static final long SHOP_ID = 774L;

    private static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(Boolean.TRUE, SHOP_ID, 38022L),
                Arguments.of(Boolean.TRUE, SHOP_ID, 1L)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    void test(boolean expected, Long shopId, Long regionGroupId) {
        MockPartnerRequest mockPartnerRequest = new MockPartnerRequest(0, 0, shopId, shopId);
        mockPartnerRequest.setParam("regionGroupId", String.valueOf(regionGroupId));
        Assertions.assertEquals(modifyRegionGroupChecker.checkTyped(mockPartnerRequest, new Authority()), expected);
    }
}
