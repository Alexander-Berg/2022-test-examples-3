package ru.yandex.market.mbi.api.controller.param;

import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.param.model.ParamCheckStatus;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.mbi.api.config.FunctionalTest;

/**
 * Функциональные тесты для ручки /push-param-check.
 *
 * @author Vadim Lyalin
 */
@ExtendWith(MockitoExtension.class)
class ParamFunctionalTest extends FunctionalTest {
    private static final long SHOP_ID = 1;
    private static final int FAKE_PARAM_TYPE_ID = -123;

    /**
     * Параметры для тестов успешной установки параметра.
     */
    private static Stream<Arguments> successfulPushCheckParamParameters() {
        return Stream.of(
                /* Ставим статус FAIL для параметра CPA_REGION_CHECK_STATUS. Текущее значение параметра не задано
                 * (берется по умолчанию)
                 */
                Arguments.of(
                        ParamType.CPA_REGION_CHECK_STATUS.getId(),
                        ParamCheckStatus.FAIL,
                        null,
                        "pushParamCheck.region.fail.after.csv"
                ),
                /* Ставим статус FAIL_MANUAL для параметра CPA_REGION_CHECK_STATUS. Текущее значение параметра не задано
                 * (берется по умолчанию)
                 */
                Arguments.of(
                        ParamType.CPA_REGION_CHECK_STATUS.getId(),
                        ParamCheckStatus.FAIL_MANUAL,
                        null,
                        "pushParamCheck.region.fail_manual.after.csv"
                ),
                /* Ставим статус SUCCESS для параметра PAYMENT_CHECK_STATUS. Текущее значение параметра задано и
                 * соответствует корректному переходу
                 */
                Arguments.of(
                        ParamType.PAYMENT_CHECK_STATUS.getId(),
                        ParamCheckStatus.SUCCESS,
                        "pushParamCheck.payment.success.before.csv",
                        "pushParamCheck.payment.success.after.csv"
                ),
                /* Ставим статус FAIL_MANUAL для параметра PAYMENT_CHECK_STATUS. Текущее значение параметра задано и
                 * соответствует корректному переходу
                 */
                Arguments.of(
                        ParamType.PAYMENT_CHECK_STATUS.getId(),
                        ParamCheckStatus.FAIL_MANUAL,
                        "pushParamCheck.payment.success.before.csv",
                        "pushParamCheck.payment.fail_manual.after.csv"
                ),
                /* Ставим статус REVOKE для параметра PAYMENT_CHECK_STATUS. Текущее значение параметра задано и
                 * соответствует корректному переходу
                 */
                Arguments.of(
                        ParamType.PAYMENT_CHECK_STATUS.getId(),
                        ParamCheckStatus.REVOKE,
                        "pushParamCheck.payment.success.before.csv",
                        "pushParamCheck.payment.revoke.after.csv"
                ),
                /* Ставим статус SUCCESS для параметра DISCOUNT_STATUS. Текущее значение параметра задано и
                 * соответствует корректному переходу
                 */
                Arguments.of(
                        ParamType.DISCOUNTS_STATUS.getId(),
                        ParamCheckStatus.SUCCESS,
                        "pushParamCheck.discount.revoke.before.csv",
                        "pushParamCheck.discount.success.after.csv"
                )
        );
    }

    /**
     * Параметры для тестов неуспешной установки параметра.
     */
    private static Stream<Arguments> unsuccessfulPushCheckParamParameters() {
        return Stream.of(
                // Выполняем неразрешенный переход параметра PAYMENT_CHECK_STATUS NEW->DONT_WANT
                Arguments.of(
                        ParamType.PAYMENT_CHECK_STATUS.getId(),
                        ParamCheckStatus.DONT_WANT,
                        "pushParamCheck.payment.success.before.csv"
                ),
                // Вызов ручки с несуществующим типом параметра
                Arguments.of(
                        FAKE_PARAM_TYPE_ID,
                        ParamCheckStatus.FAIL_MANUAL,
                        null
                ),
                // Вызов ручки с недопустимым типом параметра
                Arguments.of(
                        ParamType.PROMO_CPA_STATUS.getId(),
                        ParamCheckStatus.FAIL_MANUAL,
                        null
                )
        );
    }

    /**
     * Тесты на корректное изменение параметров.
     */
    @ParameterizedTest
    @DbUnitDataSet
    @MethodSource("successfulPushCheckParamParameters")
    void successfulPushParamCheck(int paramTypeId, ParamCheckStatus status, String initialDbState,
                                  String expectedDbState) {
        dbUnitTester.insertDataSet(initialDbState);
        mbiApiClient.pushParamCheck(SHOP_ID, paramTypeId, status);
        dbUnitTester.assertDataSet(expectedDbState);
    }

    /**
     * Тесты на попытку выполнить недопустимое изменение параметра. Ожидаем проброс {@link HttpClientErrorException}
     */
    @ParameterizedTest
    @DbUnitDataSet
    @MethodSource("unsuccessfulPushCheckParamParameters")
    void unsuccessfulPushParamCheck(int paramTypeId, ParamCheckStatus status, String initialDbState) {
        dbUnitTester.insertDataSet(initialDbState);
        Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> mbiApiClient.pushParamCheck(SHOP_ID, paramTypeId, status)
        );

    }
}
