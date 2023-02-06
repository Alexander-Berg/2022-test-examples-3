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

class ReplicationPartnerCheckerTest extends FunctionalTest {

    @Autowired
    private ReplicationPartnerChecker replicationPartnerChecker;

    public static Stream<Arguments> data() {
        return Stream.of(
                data(10, "Настоящий партнер", false),
                data(20, "Реплецированный партнер", true)
        );
    }

    private static Arguments data(long campaignId, String authorityValue, boolean expected) {
        return Arguments.of(campaignId, authorityValue, expected);
    }

    @ParameterizedTest
    @MethodSource("data")
    @DbUnitDataSet(before = "ReplicationPartnerCheckerTest.before.csv")
    void test(long campaignId, String authorityValue, boolean expected) {
        Authority auth = new Authority("test", authorityValue);
        MockPartnerRequest mockPartnerRequest = new MockPartnerRequest(-1, -1L, campaignId, campaignId);
        Assertions.assertEquals(expected, replicationPartnerChecker.checkTyped(mockPartnerRequest, auth));
    }
}
