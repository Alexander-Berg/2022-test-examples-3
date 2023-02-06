package ru.yandex.autotests.directapi.test.units;

import java.util.Collection;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;

import static java.util.Collections.emptyList;
import static ru.yandex.autotests.directapi.rules.ApiSteps.USE_OPERATOR_UNITS_AUTO;
import static ru.yandex.autotests.directapi.rules.ApiSteps.USE_OPERATOR_UNITS_FALSE;
import static ru.yandex.autotests.directapi.rules.ApiSteps.USE_OPERATOR_UNITS_TRUE;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.AGENCY;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.AGENCY_REP;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.SUB_1;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.SUB_1_REP;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.SUB_2;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.SUB_2_REP_1;
import static ru.yandex.autotests.directapi.test.units.UnitsLogins.SUB_3;

public class UnitsWithdrawalTestData {

    static final String BRAND_CHIEF_LOGIN = SUB_1;
    static final String BRAND_MEMBER_LOGIN = SUB_2;

    /**
     * Базовый лимит, который будет установлен неглавному клиенту бренда.
     * Лимит главного клиента бренда считается как сумма данного значения и разницы в лимитах,
     * которая задаётся тестовыми данными.
     * <p>
     * Условия:
     * <ul>
     * <li>больше, чем {@value UNITS_LIMIT_MIN_VALUE}
     * <li>не равен {@value DEFAULT_UNITS_LIMIT_REAL_VALUE}
     * </ul>
     * </p>
     */
    static final Long DEFAULT_UNITS_LIMIT = 200_000L;

    /**
     * Неактивный лимит, т.е. если тестирование проходит на ручных лимитах, то это значение
     * будет проставлено клиентам в автолимиты, и наоборот.
     * <p>
     * Условия:
     * <ul>
     * <li>больше, чем {@value UNITS_LIMIT_MIN_VALUE}
     * <li>меньше, чем {@value DEFAULT_UNITS_LIMIT}
     * </ul>
     * </p>
     */
    static final Long INACTIVE_UNITS_LIMIT = 10_000L;

    private static final Long UNITS_LIMIT_MIN_VALUE = 0L;
    private static final Long DEFAULT_UNITS_LIMIT_REAL_VALUE = 160_000L;

    private UnitsWithdrawalTestData() {
    }

    /**
     * @return {@link Stream} тест кейсов для проверки списания баллов.
     */
    static Stream<Object[]> provideData() {
        return Stream.of(

                dataSet(
                        "Главный представитель клиента : он же : false",
                        SUB_1, SUB_1, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(),
                        SUB_1
                ),
                dataSet(
                        "Главный представитель клиента : Неглавный представитель клиента : false",
                        SUB_1, SUB_1_REP, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1, SUB_1_REP),
                        ImmutableSet.of(),
                        SUB_1_REP
                ),
                dataSet(
                        "Главный представитель агентства : Главный представитель клиента : false",
                        AGENCY, SUB_1, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(AGENCY),
                        SUB_1
                ),
                dataSet(
                        "Главный представитель агентства : Главный представитель клиента : true",
                        AGENCY, SUB_1, USE_OPERATOR_UNITS_TRUE,
                        ImmutableSet.of(AGENCY),
                        ImmutableSet.of(SUB_1),
                        AGENCY
                ),
                dataSet(
                        "Главный представитель агентства : Главный представитель клиента : auto",
                        AGENCY, SUB_1, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(AGENCY),
                        SUB_1
                ),
                dataSet(
                        "Главный представитель агентства : Главный представитель клиента : auto : "
                                + "Главному клиенту не хватает баллов",
                        AGENCY, SUB_1, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(AGENCY),
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(SUB_1),
                        AGENCY
                ),
                dataSet(
                        "Главный представитель агентства : Главный представитель клиента : auto : "
                                + "Главному клиенту и агентству не хватает баллов",
                        AGENCY, SUB_1, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(),
                        ImmutableSet.of(SUB_1, AGENCY),
                        ImmutableSet.of(SUB_1, AGENCY),
                        AGENCY
                ),
                dataSet(
                        "Главный представитель агентства : Неглавный представитель клиента : false",
                        AGENCY, SUB_1_REP, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1_REP),
                        ImmutableSet.of(AGENCY),
                        SUB_1_REP
                ),
                dataSet(
                        "Главный представитель агентства : Неглавный представитель клиента : true",
                        AGENCY, SUB_1_REP, USE_OPERATOR_UNITS_TRUE,
                        ImmutableSet.of(AGENCY),
                        ImmutableSet.of(SUB_1_REP),
                        AGENCY
                ),
                dataSet("Главный представитель агентства : Неглавный представитель клиента : auto",
                        AGENCY, SUB_1_REP, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(SUB_1_REP),
                        ImmutableSet.of(AGENCY),
                        SUB_1_REP
                ),
                dataSet("Главный представитель агентства : Неглавный представитель клиента : auto : "
                                + "Главному клиенту не хватает баллов",
                        AGENCY, SUB_1_REP, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(AGENCY),
                        ImmutableSet.of(SUB_1, SUB_1_REP),
                        ImmutableSet.of(SUB_1),
                        AGENCY
                ),
                dataSet("Главный представитель агентства : Неглавный представитель клиента : auto : "
                                + "Главному клиенту и агентству не хватает баллов",
                        AGENCY, SUB_1_REP, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(),
                        ImmutableSet.of(AGENCY, SUB_1, SUB_1_REP),
                        ImmutableSet.of(SUB_1, AGENCY),
                        AGENCY
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель главного клиента бренда : false",
                        AGENCY_REP, SUB_1_REP, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1_REP),
                        ImmutableSet.of(AGENCY_REP),
                        SUB_1_REP
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель главного клиента бренда : true",
                        AGENCY_REP, SUB_1_REP, USE_OPERATOR_UNITS_TRUE,
                        ImmutableSet.of(AGENCY_REP),
                        ImmutableSet.of(SUB_1_REP),
                        AGENCY_REP
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель главного клиента бренда : auto",
                        AGENCY_REP, SUB_1_REP, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(SUB_1_REP),
                        ImmutableSet.of(AGENCY_REP),
                        SUB_1_REP
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель главного клиента бренда : auto :"
                                + "Главному клиенту не хватает баллов",
                        AGENCY_REP, SUB_1_REP, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(AGENCY_REP),
                        ImmutableSet.of(SUB_1, SUB_1_REP),
                        ImmutableSet.of(SUB_1),
                        AGENCY_REP
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель главного клиента бренда : auto :"
                                + "Главному клиенту и агентству не хватает баллов",
                        AGENCY_REP, SUB_1_REP, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(),
                        ImmutableSet.of(AGENCY_REP, SUB_1, SUB_1_REP),
                        ImmutableSet.of(SUB_1, AGENCY),
                        AGENCY_REP
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель неглавного клиента бренда : false",
                        AGENCY_REP, SUB_2_REP_1, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1, SUB_1_REP),
                        ImmutableSet.of(AGENCY_REP, SUB_2_REP_1, SUB_2),
                        SUB_2_REP_1
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель неглавного клиента бренда : true",
                        AGENCY_REP, SUB_2_REP_1, USE_OPERATOR_UNITS_TRUE,
                        ImmutableSet.of(AGENCY_REP),
                        ImmutableSet.of(SUB_2_REP_1),
                        AGENCY_REP
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель неглавного клиента бренда : auto",
                        AGENCY_REP, SUB_2_REP_1, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(SUB_1, SUB_1_REP),
                        ImmutableSet.of(AGENCY_REP, SUB_2_REP_1, SUB_2),
                        SUB_2_REP_1
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель неглавного клиента бренда : auto : "
                                + "Главному клиенту не хватает баллов",
                        AGENCY_REP, SUB_2_REP_1, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(AGENCY_REP),
                        ImmutableSet.of(SUB_2, SUB_2_REP_1),
                        ImmutableSet.of(SUB_1, SUB_2),
                        AGENCY_REP
                ),
                dataSet(
                        "Неглавный представитель агентства : Неглавный представитель неглавного клиента бренда : auto : "
                                + "Главному клиенту и агентству не хватает баллов",
                        AGENCY_REP, SUB_2_REP_1, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(),
                        ImmutableSet.of(AGENCY_REP, SUB_2, SUB_2_REP_1),
                        ImmutableSet.of(SUB_1, AGENCY, SUB_2),
                        AGENCY_REP
                ),
                dataSet(
                        "Главный представитель главного клиента бренда :"
                                + " Неглавный представитель главного клиента бренда : false",
                        SUB_1, SUB_1_REP, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1, SUB_1_REP),
                        ImmutableSet.of(),
                        SUB_1_REP
                ),
                dataSet(
                        "Главный представитель неглавного клиента бренда : он же : false",
                        SUB_2, SUB_2, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(SUB_2),
                        SUB_2
                ),
                dataSet(
                        "Главный представитель неглавного клиента бренда : Client-Login не указан : false",
                        SUB_2, null, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(SUB_2),
                        SUB_2
                ),
                dataSet(
                        "Неглавный представитель неглавного клиента бренда : он же : false",
                        SUB_2_REP_1, SUB_2_REP_1, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(SUB_2, SUB_2_REP_1),
                        SUB_2_REP_1
                ),
                dataSet(
                        "Неглавный представитель агентства : Главный представитель клиента вне бренда : false",
                        AGENCY_REP, SUB_3, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_3),
                        ImmutableSet.of(AGENCY, AGENCY_REP, SUB_1),
                        SUB_3
                ),
                dataSet(
                        "Неглавный представитель агентства : Главный представитель клиента вне бренда : true",
                        AGENCY_REP, SUB_3, USE_OPERATOR_UNITS_TRUE,
                        ImmutableSet.of(AGENCY, AGENCY_REP),
                        ImmutableSet.of(SUB_3, SUB_1),
                        AGENCY_REP
                ),
                dataSet(
                        "Неглавный представитель агентства : Главный представитель клиента вне бренда : auto",
                        AGENCY_REP, SUB_3, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(SUB_3),
                        ImmutableSet.of(AGENCY, AGENCY_REP, SUB_1),
                        SUB_3
                ),
                dataSet(
                        "Неглавный представитель агентства : Главный представитель клиента вне бренда : auto : "
                                + "Клиенту вне бренда не хватает баллов",
                        AGENCY_REP, SUB_3, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(AGENCY, AGENCY_REP),
                        ImmutableSet.of(SUB_3, SUB_1),
                        ImmutableSet.of(SUB_3),
                        AGENCY_REP
                ),
                dataSet(
                        "Неглавный представитель агентства : Главный представитель клиента вне бренда : auto : "
                                + "Клиенту вне бренда и агентству не хватает баллов",
                        AGENCY_REP, SUB_3, USE_OPERATOR_UNITS_AUTO,
                        ImmutableSet.of(),
                        ImmutableSet.of(AGENCY, AGENCY_REP, SUB_3, SUB_1),
                        ImmutableSet.of(SUB_3, AGENCY),
                        AGENCY_REP
                ),
                dataSet(
                        "Главный представитель клиента вне бренда : он же : false",
                        SUB_3, SUB_3, USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(SUB_3),
                        ImmutableSet.of(AGENCY, SUB_1),
                        SUB_3
                ),
                dataSet(
                        "Главный представитель агентства :"
                                + " Главный представитель неглавного клиента бренда : false : diff-",
                        AGENCY, SUB_2, USE_OPERATOR_UNITS_FALSE, -100,
                        ImmutableSet.of(SUB_2),
                        ImmutableSet.of(AGENCY, SUB_1),
                        SUB_2
                ),
                dataSet(
                        "Главный представитель агентства :"
                                + " Главный представитель неглавного клиента бренда : false : diff+",
                        AGENCY, SUB_2, USE_OPERATOR_UNITS_FALSE, 100,
                        ImmutableSet.of(SUB_1),
                        ImmutableSet.of(AGENCY, SUB_2),
                        SUB_2
                ),
                dataSet(
                        "Главный представитель агентства : неправильный клиент",
                        AGENCY, "11xxx22xxx33", USE_OPERATOR_UNITS_FALSE,
                        ImmutableSet.of(AGENCY),
                        ImmutableSet.of(),
                        AGENCY
                )
        );
    }

    /**
     * Сформировать набор входных и выходных тестовых данных для одного кейса.
     *
     * @see UnitsWithdrawalTestData#dataSet(String, String, String, String, int, Collection, Collection, Collection, String)
     */
    private static Object[] dataSet(String description, String operatorLogin, String clientLogin,
            String useOperatorUnits, Collection<String> withdrawLogins, Collection<String> keepLogins,
            String unitsUsedLogin)
    {
        return dataSet(description, operatorLogin, clientLogin, useOperatorUnits, 0,
                withdrawLogins, keepLogins, ImmutableSet.of(), unitsUsedLogin);
    }

    /**
     * Сформировать набор входных и выходных тестовых данных для одного кейса.
     *
     * @see UnitsWithdrawalTestData#dataSet(String, String, String, String, int, Collection, Collection, Collection, String)
     */
    private static Object[] dataSet(String description, String operatorLogin, String clientLogin,
            String useOperatorUnits, Collection<String> withdrawLogins, Collection<String> keepLogins,
            Collection<String> clientsWithoutUnits, String unitsUsedLogin)
    {
        return dataSet(description, operatorLogin, clientLogin, useOperatorUnits, 0,
                withdrawLogins, keepLogins, clientsWithoutUnits, unitsUsedLogin);
    }

    /**
     * Сформировать набор входных и выходных тестовых данных для одного кейса.
     *
     * @see UnitsWithdrawalTestData#dataSet(String, String, String, String, int, Collection, Collection, Collection, String)
     */
    private static Object[] dataSet(String description, String operatorLogin, String clientLogin,
            String useOperatorUnits, int unitsDiff, Collection<String> withdrawLogins, Collection<String> keepLogins,
            String unitsUsedLogin)
    {
        return dataSet(description, operatorLogin, clientLogin, useOperatorUnits, unitsDiff,
                withdrawLogins, keepLogins, emptyList(), unitsUsedLogin);
    }

    /**
     * Сформировать набор входных и выходных тестовых данных для одного кейса.
     *
     * @param description           описание кейса
     * @param operatorLogin         логин юзера, чей токен используется в запросах
     * @param clientLogin           логин, указываемый в заголовке {@code Client-Login}
     * @param useOperatorUnits      значение заголовка {@code Use-Operator-Units}
     * @param unitsDiff             разница между лимитами баллов бренд шефа и клиента бренда, если положительна, то у шефа больше
     * @param withdrawLogins        логины юзеров, с которых должны списаться баллы
     * @param keepLogins            логины юзеров, с которых баллы списаться не должны
     * @param clientWithoutUnits    логины юзеров, у которых нет баллов
     * @param unitsUsedLogin        ожидаемый логин в заголовке UnitsUsedLogin
     */
    private static Object[] dataSet(String description, String operatorLogin, String clientLogin,
            String useOperatorUnits, int unitsDiff, Collection<String> withdrawLogins, Collection<String> keepLogins,
            Collection<String> clientWithoutUnits, String unitsUsedLogin)
    {
        return new Object[]{description, operatorLogin, clientLogin, useOperatorUnits, unitsUsedLogin, clientWithoutUnits,
                unitsDiff, withdrawLogins, keepLogins};
    }

}
