package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

/**
 * Тесты для {@link PushSchemeChecker}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = "PushSchemeCheckerTest.before.csv")
class PushSchemeCheckerTest extends FunctionalTest {

    private static final int USER_ID = 123;
    private static final String CHECKER_NAME = "PUSH_SCHEME";

    @Autowired
    private PushSchemeChecker pushSchemeChecker;

    @ParameterizedTest
    @MethodSource("testCheckerData")
    void testChecker(final String name, final PartnerId partnerId, final String params, final boolean expected) {
        final MockPartnerRequest data = new MockPartnerRequest(USER_ID, USER_ID, partnerId);
        final Authority authority = new Authority(CHECKER_NAME, params);
        final boolean actual = pushSchemeChecker.checkTyped(data, authority);
        Assertions.assertEquals(expected, actual);
    }

    private static Stream<Arguments> testCheckerData() {
        return Stream.of(
                Arguments.of(
                        "PushPartnerStatus#NO + PUSH_PARTNER(true) = false",
                        PartnerId.datasourceId(774),
                        "true",
                        false
                ),
                Arguments.of(
                        "PushPartnerStatus#NO + PUSH_PARTNER(false) = true",
                        PartnerId.datasourceId(774),
                        "false",
                        true
                ),
                Arguments.of(
                        "PushPartnerStatus#PUSH_TO_PULL + PUSH_PARTNER(false) = true",
                        PartnerId.datasourceId(664),
                        "false",
                        true
                ),
                Arguments.of(
                        "PushPartnerStatus#PULL_TO_PUSH + PUSH_PARTNER(true) = true",
                        PartnerId.datasourceId(775),
                        "true",
                        true
                ),
                Arguments.of(
                        "PushPartnerStatus#REAL + PUSH_PARTNER(true) = true",
                        PartnerId.datasourceId(665),
                        "true",
                        true
                ),
                Arguments.of(
                        "PushPartnerStatus#REAL + PUSH_PARTNER(false) = false",
                        PartnerId.datasourceId(665),
                        "false",
                        false
                ),
                Arguments.of(
                        "no status + PUSH_PARTNER(false) = true",
                        PartnerId.datasourceId(776),
                        "false",
                        true
                ),
                Arguments.of(
                        "no status + PUSH_PARTNER(true) = false",
                        PartnerId.datasourceId(776),
                        "true",
                        false
                ),
                Arguments.of(
                        "PushPartnerStatus#REAL + PUSH_PARTNER(bad_status) = false",
                        PartnerId.datasourceId(665),
                        "bad_status",
                        false
                )
        );
    }
}
