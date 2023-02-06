package ru.yandex.market.partner.security.checker;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.partner.mvc.MockPartnerRequest;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.security.model.Authority;

import static org.junit.Assert.assertEquals;


@DbUnitDataSet(before = "InMigrationCheckerTest.before.csv")
public class InMigrationCheckerTest extends FunctionalTest {
    @Autowired
    private InMigrationChecker inMigrationChecker;

    private static final int USER_ID = 123;
    private static final String CHECKER_NAME = "IN_MIGRATION";
    @ParameterizedTest
    @MethodSource("testCheckerData")
    public void checkTypedCatalogEditChecker(final PartnerId partnerId, final String params, final boolean expected) {
        final MockPartnerRequest data = new MockPartnerRequest(USER_ID, USER_ID, partnerId);
        final Authority authority = new Authority(CHECKER_NAME, params);
        final boolean actual = inMigrationChecker.checkTyped(data, authority);
        assertEquals(expected, actual);
    }


    /**
     * 774 - id партнера бизнес которго находится в миграции
     * 776 - id партнера бизнес которго не нахдоится в миграции, но у него есть params других типов
     * 777 - id партнера бизнес которго не нахдоится в миграции, у него нет никаких параметров в params
     */
    private static Stream<Arguments> testCheckerData() {
        return Stream.of(
                //IS_IN_MIGRATION(true)
                //Находится ли бизнес в миграции
                Arguments.of(
                        PartnerId.datasourceId(774),
                        "true",
                        true
                ),
                Arguments.of(
                        PartnerId.datasourceId(776),
                        "true",
                        false
                ),
                Arguments.of(
                        PartnerId.datasourceId(777),
                        "true",
                        false
                ),
                //IS_IN_MIGRATION(false)
                //Не находится ли бизнес в миграции
                Arguments.of(
                        PartnerId.datasourceId(774),
                        "false",
                        false
                ),
                Arguments.of(
                        PartnerId.datasourceId(776),
                        "false",
                        true
                ),
                Arguments.of(
                        PartnerId.datasourceId(777),
                        "false",
                        true
                )
        );
    }
}
