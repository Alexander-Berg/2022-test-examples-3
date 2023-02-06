package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.security.DefaultBusinessUidable;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

/**
 * Проверяем {@link BusinessRoleChecker}.
 */
@DbUnitDataSet(before = "businessOwnerRoleCheckerTest.before.csv")
class BusinessRoleCheckerTest extends FunctionalTest {
    static final Authority AUTHORITY_OWNER = new Authority("name", "BUSINESS_OWNER");
    static final Authority AUTHORITY_ADMIN = new Authority("name", "BUSINESS_ADMIN");
    static final Authority AUTHORITY_OWNER_ADMIN = new Authority("name", "BUSINESS_OWNER,BUSINESS_ADMIN");

    @Autowired
    private BusinessRoleChecker businessRoleChecker;

    @ParameterizedTest
    @MethodSource("testCheckerData")
    void testCheckTyped(final String name, final Long businessId,
                        final int userId,
                        final boolean expected,
                        final Authority authority) {
        final DefaultBusinessUidable data = new DefaultBusinessUidable(businessId, userId, userId);
        final boolean actual = businessRoleChecker.checkTyped(data, authority);
        Assertions.assertEquals(expected, actual);
    }


    private static Stream<Arguments> testCheckerData() {
        return Stream.of(
                Arguments.of("BUSINESS_OWNER к своему бизнесу", 31L, 10, true, AUTHORITY_OWNER),
                Arguments.of("BUSINESS_OWNER не к своему бизнесу", 32L, 10, false, AUTHORITY_OWNER),
                Arguments.of("BUSINESS_OWNER не к своему бизнесу", 32L, 20, false, AUTHORITY_OWNER),
                Arguments.of("BUSINESS_OWNER не к своему бизнесу", 32L, 30, true, AUTHORITY_OWNER),
                Arguments.of("пользователь BUSINESS_ADMIN ", 31L, 20, true, AUTHORITY_ADMIN),
                Arguments.of("пользователь BUSINESS_ADMIN ", 32L, 20, false, AUTHORITY_ADMIN),
                Arguments.of("пользователь BUSINESS_ADMIN или  BUSINESS_OWNER", 31L, 10, true, AUTHORITY_OWNER_ADMIN),
                Arguments.of("пользователь BUSINESS_ADMIN или BUSINESS_OWNER", 31L, 20, true, AUTHORITY_OWNER_ADMIN),
                Arguments.of("пользователь BUSINESS_ADMIN", null, 10, true, AUTHORITY_OWNER_ADMIN),
                Arguments.of("пользователь не BUSINESS_ADMIN", null, 60, false, AUTHORITY_ADMIN)
        );
    }
}
