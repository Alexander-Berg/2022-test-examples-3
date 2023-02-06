package ru.yandex.market.logistics.logging.backlog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

import ru.yandex.market.logistics.logging.AbstractTest;
import ru.yandex.market.request.trace.RequestContextHolder;

/**
 * Специальный базовый класс для тестирования асинхронных реализаций логирующих фреймворков.
 * Реализации находятся каждая в своём отдельном пакете для чистоты classpath от других имплементаций логирования.
 */
@ParametersAreNonnullByDefault
public abstract class BackLogAsyncTestBase extends AbstractTest {
    private static final String TEST_REQUEST_ID = "testRequestId";

    private Logger logger;
    private final List<String> capturedMessages = new ArrayList<>();

    /**
     * Подготавливает асинхронное логирование. Метод обязан:
     * <ul>
     *     <li>Создать {@link Logger} с максимальным уровнем логирования,
     *     вызов любого логирующего метода которого проходит асинхронным образом;</li>
     *     <li>Применить {@code tskv} форматирование, классы для которого лежат
     *     в {@link ru.yandex.market.logistics.logging.backlog.layout};</li>
     *     <li>Настроить моки указанных форматеров: передать {@code messageCapturingAnswer} на вызов сериализации;</li>
     *     <li>Вернуть указанный в первом пункте {@link Logger}.</li>
     * </ul>
     *
     * @param messageCapturingAnswer {@link Answer}, добавляющий результат вызова в список на проверку
     * @return асинхронный логгер с настроенными моками.
     */
    @Nonnull
    protected abstract Logger prepareAsyncLogging(Answer<String> messageCapturingAnswer);

    /**
     * Завершает асинхронное логирование, дожидаясь обработки всех отправленных во время теста сообщений.
     * После выхода из этого метода ожидается, что все сообщения, отправленные в лог, прошли через все аппендеры.
     *
     * @param logger асинхронный логгер, который был возвращен в {@link #prepareAsyncLogging}
     */
    protected abstract void joinAsyncLogging(Logger logger);

    @BeforeEach
    void setupLogging() {
        RequestContextHolder.createContext(TEST_REQUEST_ID);
        capturedMessages.clear();
        logger = prepareAsyncLogging(
            invocation -> {
                String message = (String) invocation.callRealMethod();
                capturedMessages.add(message);
                return message;
            }
        );
    }

    @AfterEach
    void cleanup() {
        RequestContextHolder.clearContext();
    }

    @Test
    @DisplayName("Без оборачивания BackLogWrapper")
    void withoutWrapper() {
        logger.info("Info message");

        joinAsync();

        softly.assertThat(capturedMessages)
            .hasSize(1);

        softly.assertThat(capturedMessages.get(0))
            .startsWith("tskv")
            .contains(
                "level=INFO\t"
                    + "format=plain\t"
                    + "payload=Info message\t"
                    + "request_id=testRequestId\n"
            );
    }

    @Test
    @DisplayName("С оборачиванием BackLogWrapper")
    void withWrapper() {
        BackLogWrapper.of(logger)
            .withEntities("entity", ImmutableList.of("1", "2"))
            .withEntity("another", 3)
            .withVcsId("r9999999")
            .withTags(ImmutableSet.of("TAG1", "TAG2"))
            .withCode("EVENT_CODE")
            .withExtra("extra_k", "extra_v")
            .withExtra("ex_k", "ex_v")
            .info("Wrapped info message");

        joinAsync();

        softly.assertThat(capturedMessages)
            .hasSize(1);

        softly.assertThat(capturedMessages.get(0))
            .startsWith("tskv")
            .contains(
                "level=INFO\t"
                    + "format=plain\t"
                    + "code=EVENT_CODE\t"
                    + "payload=Wrapped info message\t"
                    + "request_id=testRequestId\t"
                    + "tags=TAG1,TAG2\t"
                    + "entity_types=entity,another\t"
                    + "entity_values=entity:1,entity:2,another:3\t"
                    + "extra_keys=ex_k,extra_k\t"
                    + "extra_values=ex_v,extra_v\t"
                    + "vcs_id=r9999999\n"
            );
    }

    @Test
    @DisplayName("Все уровни логирования корректно отображаются")
    void allLevelsSupported() {
        logger.trace("Trace message");
        logger.debug("Debug message");
        logger.info("Info message");
        logger.warn("Warn message");
        logger.error("Error message");

        joinAsync();

        softly.assertThat(capturedMessages)
            .extracting(this::trimToLevel)
            .containsExactly(
                "level=TRACE\tformat=plain\tpayload=Trace message\trequest_id=testRequestId\n",
                "level=DEBUG\tformat=plain\tpayload=Debug message\trequest_id=testRequestId\n",
                "level=INFO\tformat=plain\tpayload=Info message\trequest_id=testRequestId\n",
                "level=WARN\tformat=plain\tpayload=Warn message\trequest_id=testRequestId\n",
                "level=ERROR\tformat=plain\tpayload=Error message\trequest_id=testRequestId\n"
            );
    }

    @Test
    @DisplayName("Форматирование строки и печать исключений")
    void formattingAndExceptions() {
        BackLogWrapper.of(logger)
            .withTags("ERROR_TAG")
            .error(
                "Error message with param 1: {}, param 2: {}, and exception",
                1,
                2,
                new RuntimeException("Exception message!")
            );

        joinAsync();

        softly.assertThat(capturedMessages)
            .hasSize(1)
            .extracting(this::trimToLevel)
            .allMatch(str -> Pattern.matches(
                "^level=ERROR\\t"
                    + "format=json-exception\\t"
                    + "code=java.lang.RuntimeException\\t"
                    + "payload=\\{"
                    + "\\\\\"eventMessage\\\\\":\\\\\"Error message with param 1: 1, param 2: 2, and exception\\\\\","
                    + "\\\\\"exceptionMessage\\\\\":\\\\\"RuntimeException: Exception message!\\\\\","
                    + "\\\\\"stackTrace\\\\\":\\\\\""
                    + "java.lang.RuntimeException: Exception message!"
                    + "(\\\\\\\\n\\\\\\\\tat .+)+\\\\\\\\n"
                    + "\\\\\""
                    + "}\\t"
                    + "request_id=testRequestId\\t"
                    + "tags=ERROR_TAG\\n$",
                str
            ));
    }

    @Nonnull
    private String trimToLevel(String tskvRecord) {
        return tskvRecord.substring(tskvRecord.indexOf("level"));
    }

    private void joinAsync() {
        joinAsyncLogging(logger);
    }
}
