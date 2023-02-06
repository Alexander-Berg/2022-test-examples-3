package ru.yandex.autotests.directapi.test.units;

import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import static ru.yandex.autotests.directapi.rules.ApiSteps.USE_OPERATOR_UNITS_FALSE;
import static ru.yandex.autotests.directapi.rules.ApiSteps.USE_OPERATOR_UNITS_TRUE;

public class UnitsWithdrawalPageErrorTestData {

    public static final int PAGE_LIMIT_EXCEEDED_ERROR_CODE = 4002;

    private UnitsWithdrawalPageErrorTestData() {/* preventing instantiation */}

    /**
     * Получить набор тестовых данных для проверки ошибки в случае превышения максимального значения
     * аргумента запроса {@code PageLimit}.
     *
     * @return {@link Stream} параметров для параметризованного теста списания баллов
     * в случае превышения {@code PageLimit}.
     */
    public static Stream<Object[]> pageLimitExceeded() {
        return Stream.of(
                new Object[]{
                        "Превышен максимальный PageLimit : Agency : Subclient : 0",
                        UnitsLogins.AGENCY, UnitsLogins.SUB_1, USE_OPERATOR_UNITS_FALSE, 0,
                        ImmutableSet.of(UnitsLogins.SUB_1),
                        ImmutableSet.of(UnitsLogins.AGENCY)
                },
                new Object[]{
                        "Превышен максимальный PageLimit : Agency : Subclient : 1",
                        UnitsLogins.AGENCY, UnitsLogins.SUB_1, USE_OPERATOR_UNITS_TRUE, 0,
                        ImmutableSet.of(UnitsLogins.AGENCY),
                        ImmutableSet.of(UnitsLogins.SUB_1)
                },
                new Object[]{
                        "Превышен максимальный PageLimit : Subclient : Subclient : 0",
                        UnitsLogins.SUB_1, UnitsLogins.SUB_1, USE_OPERATOR_UNITS_FALSE, 0,
                        ImmutableSet.of(UnitsLogins.SUB_1),
                        ImmutableSet.of(UnitsLogins.AGENCY)
                },
                new Object[]{
                        "Превышен максимальный PageLimit : BrandMember : BrandMember : 0",
                        UnitsLogins.SUB_2, UnitsLogins.SUB_2, USE_OPERATOR_UNITS_FALSE, 0,
                        ImmutableSet.of(UnitsLogins.SUB_1),
                        ImmutableSet.of(UnitsLogins.SUB_2, UnitsLogins.AGENCY)
                }
        );
    }

}
