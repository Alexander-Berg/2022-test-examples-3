package ru.yandex.market.delivery.deliveryintegrationtests.wms.extensions;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Для корректной работы с параллельным запуском необходимо дополнительно
 * повесить аннотацию @ResourceLock со значением равным @DisplayName
 *
 * Это подскажет движку junit что не стоит запускать 2 инстанса одного теста одновременно
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(RetryableTestExtension.class)
public @interface RetryableTest {

    /**
     * Ожидаемое время выполнения теста в минутах
     * для особо длинных тестов стоит прописать кастомное,
     * чтобы они не выпадали за лимит выполнения
     */
    int duration() default 15;

    /**
     * Время, в которое должен уложиться весь тест-ран
     * по дефолту таймаут выполнения large тестов - время на подготовку сендбокс-задачи
     * 60 - 15 = 45
     */
    int maxRunTime() default 45;

    /**
     * Исключения, при которых тест ретраится
     */
    Class<? extends Throwable>[] retryOnExceptions() default Throwable.class;

    /**
     * Исключения, при которых тест фейлится сразу
     */
    Class<? extends Throwable>[] failOnExceptions() default {};

    /**
     * Количество выполнений теста
     * В случае одного успеха или получения ошибки не из списка ожидаемых
     * Повторения останавливаются
     */
    int repeats() default 3;
}
