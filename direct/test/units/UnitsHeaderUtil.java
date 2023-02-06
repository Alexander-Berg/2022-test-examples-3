package ru.yandex.autotests.directapi.test.units;

import java.util.concurrent.Callable;
import java.util.function.Function;

import javax.xml.ws.BindingProvider;

import com.sun.xml.ws.transport.Headers;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.xml.ws.handler.MessageContext.HTTP_RESPONSE_HEADERS;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTester.UNITS_HEADER;
import static ru.yandex.autotests.directapi.test.units.UnitsWithdrawalTester.UNITS_USED_LOGIN_HEADER;
import static ru.yandex.autotests.irt.testutils.allure.TestSteps.assumeThat;

public class UnitsHeaderUtil {

    private static Logger logger = LoggerFactory.getLogger(UnitsHeaderUtil.class);

    /**
     * Получить функцию <br>
     * {@code (логин клиента) -> (значение заголовка ответа Units-Used-Login)} <br>
     * на основе функции <br>
     * {@code (логин клиента) -> {вызов сервиса} -> (Jax-WS port)}
     *
     * @param serviceCallReturningWebServicePort Функция, реализующая вызов сервиса по SOAP,
     *                                           и возвращающая WS Port.
     *                                           Функция может кидать проверяемые исключения.
     *                                           В случае выброса любого исключения тест будет провален.
     * @return Функция, обращающаяся к WS-сервису с заданным логином клиента, и возвращающая
     * значение заголовка ответа {@code Units-Used-Login}.
     */
    public static Function<String, String> getUnitsUsedLoginHeader(
            FunctionThrowingCheckedException<String, Object> serviceCallReturningWebServicePort)
    {
        return getHeaderAndFailOnAnyException(UNITS_USED_LOGIN_HEADER, serviceCallReturningWebServicePort);
    }

    /**
     * Получить функцию <br>
     * {@code (логин клиента) -> (значение заголовка ответа Units)} <br>
     * на основе функции <br>
     * {@code (логин клиента) -> {вызов сервиса} -> (Jax-WS port)}
     *
     * @param serviceCallReturningWebServicePort Функция, реализующая вызов сервиса по SOAP,
     *                                           и возвращающая WS Port.
     *                                           Функция может кидать проверяемые исключения.
     *                                           В случае выброса любого исключения тест будет провален.
     * @return Функция, обращающаяся к WS-сервису с заданным логином клиента, и возвращающая
     * значение заголовка ответа {@code Units}.
     */
    public static Function<String, String> getUnitsHeader(
            FunctionThrowingCheckedException<String, Object> serviceCallReturningWebServicePort)
    {
        return getHeaderAndFailOnAnyException(UNITS_HEADER, serviceCallReturningWebServicePort);
    }

    private static Function<String, String> getHeaderAndFailOnAnyException(String headerName,
            FunctionThrowingCheckedException<String, Object> serviceCallReturningWebServicePort)
    {
        return (String clientLogin) ->
                failOnException(() -> {
                            Object wsPort = serviceCallReturningWebServicePort.apply(clientLogin);
                            return getHeader(wsPort, headerName);
                        }
                );
    }

    /**
     * Извлечь значение заголовка {@code headerName} из ответа, полученного после вызова WS-сервиса.
     *
     * @param port       WS Port, по которому был предварительно вызван сервис.
     * @param headerName значение заголовка, значение которого нужно получить.
     * @return Значение заголовка {@code Units-Used-Login}, {@link String}.
     * @throws IllegalArgumentException если {@code (port instanceof} {@link BindingProvider} {@code == false)}.
     */
    public static String getHeader(Object port, String headerName) {
        if (!BindingProvider.class.isInstance(port)) {
            throw new IllegalArgumentException("Port should be cast to BindingProvider type");
        }
        BindingProvider bp = (BindingProvider) port;
        Headers responseHeaders = (Headers) bp.getResponseContext().get(HTTP_RESPONSE_HEADERS);

        assumeThat("В ответе содержится заголовок " + headerName,
                responseHeaders.containsKey(headerName), is(true));

        String headerValue = responseHeaders.get(headerName).get(0);
        logger.info("{} header: {}", headerName, headerValue);

        return headerValue;
    }

    private static <T> T failOnException(Callable<T> callable) {
        T result = null;
        try {
            result = callable.call();
        } catch (Exception ex) {
            logger.error("Exception caught: {}", ex);
            Assert.fail();
        }
        return result;
    }

    @FunctionalInterface
    public interface FunctionThrowingCheckedException<T, R> {
        R apply(T arg) throws Exception;
    }

}
