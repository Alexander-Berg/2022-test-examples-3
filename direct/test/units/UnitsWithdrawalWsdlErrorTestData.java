package ru.yandex.autotests.directapi.test.units;

import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import static ru.yandex.autotests.directapi.rules.ApiSteps.USE_OPERATOR_UNITS_FALSE;
import static ru.yandex.autotests.directapi.rules.ApiSteps.USE_OPERATOR_UNITS_TRUE;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.AGENCY;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.AGENCY_REP;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.SUB_1;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.SUB_2;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.SUB_2_REP_1;

public class UnitsWithdrawalWsdlErrorTestData {

    public static final int WSDL_ERROR_CODE = 8000;

    private UnitsWithdrawalWsdlErrorTestData() {/* preventing instantiation */}

    /**
     * Получить набор тестовых данных для проверки ошибки в случае невалидной структуры запроса.
     *
     * @return {@link Stream} параметров для параметризованного теста списания баллов
     * в случае нарушения WSDL схемы.
     */
    public static Stream<Object[]> wsdlErrorExpected() {

        return Stream.of(
                new Object[]{
                        "Ошибка валидации WSDL : Agency : Subclient : 0",
                        AGENCY, SUB_1, USE_OPERATOR_UNITS_FALSE, 0,
                        ImmutableSet.of(AGENCY),
                        ImmutableSet.of(SUB_1)
                },
                new Object[]{
                        "Ошибка валидации WSDL : Agency : Subclient : 1",
                        AGENCY, SUB_1, USE_OPERATOR_UNITS_TRUE, 0,
                        ImmutableSet.of(AGENCY),
                        ImmutableSet.of(SUB_1)
                },
                new Object[]{
                        "Ошибка валидации WSDL : Subclient : Subclient : 0",
                        SUB_1, SUB_1, USE_OPERATOR_UNITS_FALSE, 0,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(AGENCY)
                },
                new Object[]{
                        "Ошибка валидации WSDL : BrandMember : BrandMember : 0",
                        SUB_2, SUB_2, USE_OPERATOR_UNITS_FALSE, 0,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(SUB_2)
                },
                new Object[]{
                        "Ошибка валидации WSDL : BrandMemberRep : BrandMemberRep : 0",
                        SUB_2_REP_1, SUB_2_REP_1, USE_OPERATOR_UNITS_FALSE, 0,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(SUB_2)
                },
                new Object[]{
                        "Ошибка валидации WSDL : AgRep : BrandMemberRep : 0",
                        AGENCY_REP, SUB_2_REP_1, USE_OPERATOR_UNITS_FALSE, 0,
                        ImmutableSet.of(AGENCY),
                        ImmutableSet.of(SUB_1, SUB_2, SUB_2_REP_1)
                },
                new Object[]{
                        "Ошибка валидации WSDL : AgRep : BrandMemberRep : 1",
                        AGENCY_REP, SUB_2, USE_OPERATOR_UNITS_TRUE, 0,
                        ImmutableSet.of(AGENCY),
                        ImmutableSet.of(SUB_1, SUB_2, SUB_2_REP_1)
                }
        );
    }

}
