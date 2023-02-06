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
 * Тесты для {@link AssortmentModificationChecker}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = "AssortmentModificationCheckerTest.before.csv")
class AssortmentModificationCheckerTest extends FunctionalTest {

    private static final int USER_ID = 123;
    private static final String CHECKER_NAME = "ASSORTMENT_MODIFICATION_TYPE";

    @Autowired
    private AssortmentModificationChecker assortmentModificationChecker;

    @SuppressWarnings("unused")
    @ParameterizedTest
    @MethodSource("testCheckerData")
    void testChecker(String name, PartnerId partnerId, String params, boolean expected) {
        MockPartnerRequest data = new MockPartnerRequest(USER_ID, USER_ID, partnerId);
        Authority authority = new Authority(CHECKER_NAME, params);
        boolean actual = assortmentModificationChecker.checkTyped(data, authority);
        Assertions.assertEquals(expected, actual);
    }

    private static Stream<Arguments> testCheckerData() {
        return Stream.of(
                Arguments.of(
                        "Белый апплоадный фид",
                        PartnerId.datasourceId(774),
                        "MANUAL",
                        true
                ),
                Arguments.of(
                        "Белый апплоадный фид",
                        PartnerId.datasourceId(774),
                        "AUTO",
                        false
                ),
                Arguments.of(
                        "Белый дефолтный фид",
                        PartnerId.datasourceId(775),
                        "manual",
                        true
                ),
                Arguments.of(
                        "Белый ссылочный фид",
                        PartnerId.datasourceId(776),
                        "manual",
                        false
                ),
                Arguments.of(
                        "Белый ссылочный фид",
                        PartnerId.datasourceId(776),
                        "assortment:auto",
                        true
                ),
                Arguments.of(
                        "Белый ценовой ссылочный фид",
                        PartnerId.datasourceId(777),
                        "prices:auto",
                        true
                ),
                Arguments.of(
                        "Белый стоковый ссылочный фид",
                        PartnerId.datasourceId(778),
                        "stocks:auto",
                        true
                ),
                Arguments.of(
                        "Синий апплоадный фид",
                        PartnerId.supplierId(874),
                        "manual",
                        true
                ),
                Arguments.of(
                        "Синий ссылочный фид",
                        PartnerId.supplierId(875),
                        "auto",
                        true
                ),
                Arguments.of(
                        "Синий дефолтный фид",
                        PartnerId.supplierId(876),
                        "manual",
                        true
                ),
                Arguments.of(
                        "Синий ценовой ссылочный фид",
                        PartnerId.supplierId(877),
                        "prices:auto",
                        true
                ),
                Arguments.of(
                        "Синий стоковый и ценовой ссылочный фид",
                        PartnerId.supplierId(878),
                        "stocks:auto",
                        true
                ),
                Arguments.of(
                        "Синий стоковый и ценовой ссылочный фид, но запрос по ассортиментному фиду",
                        PartnerId.datasourceId(878),
                        " ASSOrtmENT :ManUAL ",
                        true
                )
        );
    }
}
