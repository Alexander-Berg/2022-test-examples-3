package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

public class TaxSystemCheckerTest extends FunctionalTest {

    @Autowired
    private TaxSystemChecker taxSystemChecker;

    public static Stream<Arguments> data() {
        return Stream.of(
                data(10, "OSN", true),
                data(20, "USN", false)
        );
    }

    private static Arguments data(long partnerId, String authorityValue, boolean expected) {
        return Arguments.of(partnerId, authorityValue, expected);
    }

    @ParameterizedTest
    @MethodSource("data")
    @DbUnitDataSet(before = "TaxSystemCheckerTest.before.csv")
    void test(long partnerId, String authorityParam, boolean expected) {
        Authority auth = new Authority("TAX_SYSTEM", authorityParam);
        MockPartnerRequest mockPartnerRequest = new MockPartnerRequest(
                -1,
                partnerId,
                PartnerId.partnerId(partnerId, CampaignType.SUPPLIER)
        );
        Assertions.assertEquals(expected, taxSystemChecker.checkTyped(mockPartnerRequest, auth));
    }
}
