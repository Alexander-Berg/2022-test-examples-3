package ru.yandex.market.mbi.msapi.logbroker.config;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

import com.yandex.ydb.persqueue.PersqueueErrorCodes;
import io.grpc.Status;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.kikimr.persqueue.consumer.ConsumerException;
import ru.yandex.kikimr.persqueue.consumer.stream.retry.RetryConfig;
import ru.yandex.market.mbi.msapi.logbroker.ReceiveConfig;

/**
 * @author kateleb
 *
 * Это кастомный конфигуратор для retryPolicy в StreamConsumer-e,
 * используется в тестовых целях для проверки возможности использования кастомного конфигуратора
 *
 * Основные параметры взяты хэндлера из mbi
 *  ru.yandex.market.core.logbroker.RetryHandlerWithSensors
 * В котором при срабатывании retry отправляются метрики.
 *
 * При использовании библиотеки можно реализовать свой кастомный конфиг или использовать дефолтовый
 */
public class CustomLogbrokerRetryConfiguratorForTest implements Consumer<RetryConfig.Builder> {

    private String moduleName;
    private String consumerName;
    private NoCloseRetryHandlerWithSensors consumerRetryHandler;

    public CustomLogbrokerRetryConfiguratorForTest(ReceiveConfig receiveConfig, String moduleName) {
        this.moduleName = moduleName;
        this.consumerName =
                StringUtils.substringAfterLast(receiveConfig.getClientId(), "/") + "--" + receiveConfig.getLogType();
        this.consumerRetryHandler = new NoCloseRetryHandlerWithSensors(consumerName, moduleName);
    }

    @Override
    public void accept(RetryConfig.Builder builder) {
        consumerRetryHandler.configureRetries(builder);
    }

    /**
     * Копипаста хэндлера из mbi: ru.yandex.market.core.logbroker.RetryHandlerWithSensors для тестовых целей
     */
    private class NoCloseRetryHandlerWithSensors {
        private final Logger log = LoggerFactory.getLogger(NoCloseRetryHandlerWithSensors.class);
        private final String consumer;
        private final String module;

        /**
         * Эмпирически установленный список ошибок, которые возвращает logbroker при неправильной конфигурации.
         */
        private final Set<PersqueueErrorCodes.EErrorCode> codesNoRetry = EnumSet.of(
                // Не аутентифицированный пользователь, нет прав на чтение топика
                PersqueueErrorCodes.EErrorCode.ACCESS_DENIED,
                // Для consumer нет ни read-rule из указанного топика
                PersqueueErrorCodes.EErrorCode.BAD_REQUEST,
                // Не существующий топик
                PersqueueErrorCodes.EErrorCode.UNKNOWN_TOPIC
        );

        NoCloseRetryHandlerWithSensors(String consumer, String module) {
            this.consumer = consumer;
            this.module = module;
        }

        void configureRetries(RetryConfig.Builder retries) {
            retries
                    .enable()
                    .setShouldRestartOnError(this::onError)
                    .setShouldRestartOnClose(this::onClose)
                    .setShouldRetryOnConnect(this::onConnect);
        }

        private PersqueueErrorCodes.EErrorCode getErrorCode(Status status) {
            Throwable throwable = status.getCause();
            if (throwable instanceof ConsumerException) {
                ConsumerException ce = (ConsumerException) throwable;
                return ce.getErrorCode();
            }
            return PersqueueErrorCodes.EErrorCode.UNRECOGNIZED;
        }

        boolean onClose() {
            log.warn("{} {}  Reading closed, will not reconnect", moduleName, consumerName);
            return false;
        }

        boolean onError(Throwable t) {
            Status status = Status.fromThrowable(t);
            PersqueueErrorCodes.EErrorCode errorCode = getErrorCode(status);
            log.warn("{} {} Channel got error message: {}, {}. Reconnect", moduleName, consumerName, errorCode, status);
            return !codesNoRetry.contains(errorCode);
        }

        boolean onConnect(Throwable t) {
            Status status = Status.fromThrowable(t);
            log.warn("{} {} Error while connecting: {}. Reconnect", moduleName, consumerName, status);
            return true;
        }
    }

}
