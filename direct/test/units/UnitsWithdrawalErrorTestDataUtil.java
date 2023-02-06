package ru.yandex.autotests.directapi.test.units;

import java.util.function.Function;

import org.junit.Assert;

import ru.yandex.autotests.directapi.apiclient.errors.Api5Error;
import ru.yandex.autotests.directapi.apiclient.errors.Api5JsonError;
import ru.yandex.autotests.directapi.apiclient.errors.Api5XmlError;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class UnitsWithdrawalErrorTestDataUtil {

    /**
     * Вызов сервиса с проверкой типа и кода ошибки: {@link Api5JsonError}
     *
     * @param exType      класс, соответствующий ожидаемому типу ошибки
     * @param errorCode   ожидаемый код ошибки
     * @param serviceCall вызов сервиса
     * @param <Exc>       тип ошибки
     * @return {@link Runnable}, осуществляющий вызов сервиса с проверкой получаемой ошибки.
     * Если ошибка не возвращается или возвращается другой тип/код ошибки, тест проваливается.
     */
    public static <Exc extends Api5JsonError> Runnable jsonErrorExpected(Class<? extends Exc> exType, int errorCode,
            RunnableThrowingCheckedException serviceCall)
    {
        return errorExpected(exType, serviceCall, Api5JsonError::getErrorCode, errorCode);
    }

    /**
     * Вызов сервиса с проверкой типа и кода ошибки: {@link Api5XmlError}
     *
     * @param exType      класс, соответствующий ожидаемому типу ошибки
     * @param errorCode   ожидаемый код ошибки
     * @param serviceCall вызов сервиса
     * @param <Exc>       тип ошибки
     * @return {@link Runnable}, осуществляющий вызов сервиса с проверкой получаемой ошибки.
     * Если ошибка не возвращается или возвращается другой тип/код ошибки, тест проваливается.
     */
    public static <Exc extends Api5XmlError> Runnable xmlErrorExpected(Class<? extends Exc> exType, int errorCode,
            RunnableThrowingCheckedException serviceCall)
    {
        return errorExpected(exType, serviceCall, Api5XmlError::getErrorCode, errorCode);
    }

    /**
     * Вызов сервиса с проверкой типа и кода ошибки: {@link Api5Error}
     *
     * @param exType      класс, соответствующий ожидаемому типу ошибки
     * @param errorCode   ожидаемый код ошибки
     * @param serviceCall вызов сервиса
     * @param <Exc>       тип ошибки
     * @return {@link Runnable}, осуществляющий вызов сервиса с проверкой получаемой ошибки.
     * Если ошибка не возвращается или возвращается другой тип/код ошибки, тест проваливается.
     */
    public static <Exc extends Api5Error> Runnable api5ErrorExpected(Class<? extends Exc> exType, int errorCode,
            RunnableThrowingCheckedException serviceCall)
    {
        return errorExpected(exType, serviceCall, (exc) -> exc.getFaultInfo().getErrorCode(), errorCode);
    }

    /**
     * Вызов сервиса с проверкой типа и кода ошибки.
     * Этот обобщённый метод сделан {@code public}, чтобы проверять многочисленные API-шные исключения,
     * которые наследуются только от {@link Exception}.
     *
     * @param exType            класс, соответствующий ожидаемому типу ошибки
     * @param expectedErrorCode ожидаемый код ошибки
     * @param serviceCall       вызов сервиса
     * @param <Exc>             тип ошибки
     * @return {@link Runnable}, осуществляющий вызов сервиса с проверкой получаемой ошибки.
     * Если ошибка не возвращается или возвращается другой тип/код ошибки, тест проваливается.
     */
    public static <Exc extends Exception> Runnable errorExpected(Class<Exc> exType,
            RunnableThrowingCheckedException serviceCall,
            Function<Exc, Integer> errorCodeProvider,
            int expectedErrorCode)
    {
        return () -> {
            try {
                serviceCall.run();
                Assert.fail("Ожидалась ошибка");
            } catch (Exception ex) {
                assumeThat("Получена ошибка " + exType.getSimpleName(),
                        ex, instanceOf(exType));
                assumeThat("Код ошибки " + expectedErrorCode,
                        errorCodeProvider.apply(exType.cast(ex)), equalTo(expectedErrorCode));
            }
        };
    }

    @FunctionalInterface
    public interface RunnableThrowingCheckedException {
        void run() throws Exception;
    }

}
