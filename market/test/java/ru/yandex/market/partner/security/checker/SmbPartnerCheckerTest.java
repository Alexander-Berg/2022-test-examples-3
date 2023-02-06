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
 * Тесты для {@link SmbPartnerChecker}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = "SmbPartnerCheckerTest.before.csv")
class SmbPartnerCheckerTest extends FunctionalTest {

    private static final int USER_ID = 123;
    private static final String CHECKER_NAME = "SMB";

    @Autowired
    private SmbPartnerChecker smbPartnerChecker;

    @ParameterizedTest
    @MethodSource("testCheckerData")
    void testChecker(final String name, final PartnerId partnerId, final String params, final boolean expected) {
        final MockPartnerRequest data = new MockPartnerRequest(USER_ID, USER_ID, partnerId);
        final Authority authority = new Authority(CHECKER_NAME, params);
        final boolean actual = smbPartnerChecker.checkTyped(data, authority);
        Assertions.assertEquals(expected, actual);
    }

    private static Stream<Arguments> testCheckerData() {
        return Stream.of(
                Arguments.of(
                        "smb + SMB(true) = true",
                        PartnerId.datasourceId(774),
                        "true",
                        true
                ),
                Arguments.of(
                        "smb + SMB(false) = false",
                        PartnerId.datasourceId(774),
                        "false ",
                        false
                ),
                Arguments.of(
                        "smb + SMB(bad_status) = false",
                        PartnerId.datasourceId(774),
                        "bad_status",
                        false
                ),
                Arguments.of(
                        "not smb + SMB(true) = false",
                        PartnerId.datasourceId(775),
                        "true",
                        false
                ),
                Arguments.of(
                        "not smb + SMB(false) = true",
                        PartnerId.datasourceId(775),
                        "false",
                        true
                ),
                Arguments.of(
                        "supplier + SMB(true) = false",
                        PartnerId.supplierId(776),
                        "true",
                        false
                ),
                Arguments.of(
                        "supplier + SMB(false) = true",
                        PartnerId.supplierId(776),
                        "false",
                        true
                )
        );
    }
}
