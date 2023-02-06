package ru.yandex.autotests.directapi.test.units;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface UnitsWithdrawalTestDataProvider {

    /**
     * Сформировать наборы тестовых данных для всех вызовов сервисов, переданных в аргументе.
     *
     * @param serviceCalls вызовы серсисов, на которых нужно тестировать списание баллов
     * @return Полный набор тестовых данных, содержащий для каждого переданного на вход {@link Runnable}
     * данные всех тест кейсов из метода {@link UnitsWithdrawalTestData#provideData()}.
     * Размер возвращаемого набора равен произведению числа переданных runnable-ов на число тест кейсов
     * из {@link UnitsWithdrawalTestData#provideData()}.
     * @see UnitsWithdrawalTestData#provideData()
     */
    Iterable<Object[]> buildDataSets(Runnable... serviceCalls);

    /**
     * Сформировать наборы тестовых данных для всех вызовов сервисов, переданных в аргументе.
     *
     * @param serviceCalls вызовы серсисов, на которых нужно тестировать списание баллов
     * @return Полный набор тестовых данных, содержащий для каждого переданного на вход {@link Supplier}
     * данные всех тест кейсов из метода {@link UnitsWithdrawalTestData#provideData()}.
     * Размер возвращаемого набора равен произведению числа переданных supplier-ов на число тест кейсов
     * из {@link UnitsWithdrawalTestData#provideData()}.
     * @see UnitsWithdrawalTestData#provideData()
     */
    Iterable<Object[]> buildDataSets(Supplier<?>... serviceCalls);

    /**
     * Сформировать наборы тестовых данных для всех вызовов сервисов, переданных в аргументе.
     *
     * @param serviceCalls вызовы серсисов, на которых нужно тестировать списание баллов.
     * @return Полный набор тестовых данных, содержащий для каждого переданного на вход {@link Function}
     * данные всех тест кейсов из метода {@link UnitsWithdrawalTestData#provideData()}.
     * Размер возвращаемого набора равен произведению числа переданных функций на число тест кейсов
     * из {@link UnitsWithdrawalTestData#provideData()}.
     * @see UnitsWithdrawalTestData#provideData()
     */
    Iterable<Object[]> buildDataSets(Function<?, ?>... serviceCalls);

    /**
     * Сформировать наборы тестовых данных для всех вызовов сервисов, переданных в аргументе
     * для кастомных данных, переданных в виде стрима кейсов.
     *
     * @param data         тестовые данные
     * @param serviceCalls вызовы серсисов, на которых нужно тестировать списание баллов.
     * @return Полный набор тестовых данных, содержащий для каждого переданного на вход {@link Function}
     * данные всех тест кейсов {@link data}.
     * Размер возвращаемого набора равен произведению числа переданных функций на число тест кейсов
     * из стрима {@link data}.
     */
    Iterable<Object[]> buildDataSets(Stream<Object[]> data, Function<?, ?>... serviceCalls);

}
