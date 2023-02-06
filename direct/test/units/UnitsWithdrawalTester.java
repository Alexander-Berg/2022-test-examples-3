package ru.yandex.autotests.directapi.test.units;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Класс, инкапсулирующий тестовую логику для проверок определения пользователя,
 * с которого списываются баллы за вызов того или иного сервиса.
 * <p>
 * Применение: предполагается, что тестовые классы делегируют методы init, reset и test... данному,
 * передавая все необходимые параметры, а сами являются лишь runner-ами.
 */
public interface UnitsWithdrawalTester {

    /**
     * Название заголовка, содержащего логин клиента, с которого списаны баллы.
     */
    String UNITS_USED_LOGIN_HEADER = "Units-Used-Login";

    /**
     * Название заголовка, содержащего информацию о количестве списанных баллов и балансе.
     */
    String UNITS_HEADER = "Units";

    /**
     * Инициализировать предусловие теста: клиенты {@value UnitsWithdrawalTestData#BRAND_CHIEF_LOGIN}
     * и {@value UnitsWithdrawalTestData#BRAND_MEMBER_LOGIN} должны быть объединены в бренд.
     * <p>
     * <p>Использовать под аннотацией @{@link BeforeClass}.</p>
     */
    void init();

    /**
     * Восстановить состояние для новых проверок.
     * <p>
     * <p>Использовать под аннотацией @{@link Before}.</p>
     *
     * @param operatorLogin    логин, от чьего имени делается запрос
     * @param clientLogin      значение заголовка {@code Client-Login}
     * @param useOperatorUnits значение заголовка {@code Use-Operator-Units}
     * @param unitsDelta       разность лимитов клиента и бренд шефа
     */
    void reset(String operatorLogin, String clientLogin, String useOperatorUnits, int unitsDelta,
            Collection<String> loginsWithoutUnits, Collection<String> loginsToClearUnits);

    /**
     * Гибкая версия метода {@link #reset} с fluent-интерфейсом.
     * Ленивая к тому же, т.е. вызов этого метода
     * без дальнейшего {@link ResetBuilder#execute} ни к чему не приведёт.
     *
     * @param operatorLogin логин, от чьего имени делается запрос
     * @param clientLogin   значение заголовка {@code Client-Login}
     * @return Билдер {@link ResetBuilder} для дальнейшей установки параметров или запуска.
     */
    ResetBuilder resetBuilder(String operatorLogin, String clientLogin);

    interface ResetBuilder {
        /** Установить значение заголовка {@code Use-Operator-Units}. */
        ResetBuilder useOperatorUnits(String use);
        /** Переопределить дефолтное значение лимита баллов для субклиентов. */
        ResetBuilder baseLimit(long limit);
        /** Установить разность лимитов баллов между клиентом под брендом и бренд-шефом в пользу последнего. */
        ResetBuilder unitsDiff(long diff);
        /** Установить клиентов, у которых не должно хватать баллов на операцию. */
        ResetBuilder loginsWithoutUnits(Collection<String> loginsWithoutUnits);
        /** Установить клиентов, у которых должно хватать баллов на операцию. */
        ResetBuilder loginsToClearUnits(Collection<String> loginsToClearUnits);
        /** Вызвать {@code reset()} – установить сконфигурированные предусловия теста. */
        void execute();
    }

    /**
     * Вызвать сервис, осуществить проверки списания баллов, а также дополнительные проверки
     * ответа вызванного сервиса, если необходимо.
     *
     * @param serviceCall                 {@link Supplier} для вызова сервиса
     * @param resultChecker               {@link Consumer} для проверки результата
     * @param expectedUnitsWithdrawLogins набор логинов клиентов, с которых баллы должны быть списаны
     * @param expectedUnitsKeepLogins     набор логинов клиентов, баллы с которых не должны быть списаны
     * @see UnitsWithdrawalTesterImpl#testUnitsWithdrawal(Runnable, Collection, Collection)
     */
    <R> void testUnitsWithdrawal(Supplier<R> serviceCall, Consumer<? super R> resultChecker,
            Collection<String> expectedUnitsWithdrawLogins,
            Collection<String> expectedUnitsKeepLogins);

    /**
     * Вызвать сервис, осуществить проверки списания баллов, а также проверить,
     * что в заголовке ответа Units-Used-Login указан правильный логин клиента, с которого списались баллы.
     *
     * @param serviceCall                 вызов сервиса, возвращающий значение заголовка Units-Used-Login
     * @param expectedUnitsWithdrawLogins набор логинов клиентов, с которых баллы должны быть списаны
     * @param expectedUnitsKeepLogins     набор логинов клиентов, баллы с которых не должны быть списаны
     */
    void testUnitsWithdrawal(Supplier<String> serviceCall,
            Collection<String> expectedUnitsWithdrawLogins,
            Collection<String> expectedUnitsKeepLogins);

    /**
     * Вызвать сервис, осуществить проверки списания баллов, а также проверить,
     * что в заголовке ответа Units-Used-Login указан правильный логин клиента, с которого списались баллы.
     *
     * @param serviceCallReturningUsedUnitsLoginHeader вызов сервиса, возвращающий значение заголовка Units-Used-Login
     * @param expectedUnitsWithdrawLogins              набор логинов клиентов, с которых баллы должны быть списаны
     * @param expectedUnitsKeepLogins                  набор логинов клиентов, баллы с которых не должны быть списаны
     */
    void testUnitsWithdrawal(Function<String, String> serviceCallReturningUsedUnitsLoginHeader,
            Collection<String> expectedUnitsWithdrawLogins,
            Collection<String> expectedUnitsKeepLogins);

    /**
     * Вызвать сервис, осуществить проверки списания баллов.
     *
     * @param serviceCall                 {@link Runnable} для вызова сервиса
     * @param expectedUnitsWithdrawLogins набор логинов клиентов, с которых баллы должны быть списаны
     * @param expectedUnitsKeepLogins     набор логинов клиентов, баллы с которых не должны быть списаны
     */
    void testUnitsWithdrawal(Runnable serviceCall,
            Collection<String> expectedUnitsWithdrawLogins,
            Collection<String> expectedUnitsKeepLogins);

    /**
     * Отпустить лок.
     * <p>
     * <p>Использовать под аннотацией @{@link AfterClass}.</p>
     */
    void shutdown();

}
