package ru.yandex.direct.logshatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.kikimr.persqueue.consumer.StreamConsumer;
import ru.yandex.kikimr.persqueue.consumer.StreamListener;
import ru.yandex.kikimr.persqueue.consumer.stream.StreamConsumerConfig;
import ru.yandex.kikimr.persqueue.consumer.transport.message.CommitMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerInitResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerLockMessage;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReleaseMessage;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;
import ru.yandex.library.ticket_parser2.TvmApiSettings;
import ru.yandex.library.ticket_parser2.TvmClient;

public class LogbrokerReaderTest {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static StreamConsumer streamConsumer;
    private static Throwable ex;

    public static void main(String[] args) throws Throwable {

        String host = "myt.logbroker-prestable.yandex.net";
        int logbrokerClientId = 2001147;
        int logshatterClientId = 2011544;
        String logshatterSecret = "put secret here";

        String clientId = "direct-test/direct-logshatter2";//.replace('@', '/');

        int maxReadBatchSize = 1048576;
        int maxInflightReads = 2;
        int maxUnconsumedReads = 2;

        LogbrokerClientFactory clientFactory = new LogbrokerClientFactory(new ProxyBalancer(host));

        Supplier<Credentials> credentialsProvider = credentialsProvider(
            logbrokerClientId, logshatterClientId, logshatterSecret);

        String topic = "direct-test/direct-trace-log";

        streamConsumer = createStreamConsumer(clientFactory, topic, clientId, maxReadBatchSize,
            maxInflightReads, maxUnconsumedReads, credentialsProvider, executorService);

        streamConsumer.startConsume(new StreamListenerTestImpl());
        boolean good = executorService.awaitTermination(5, TimeUnit.SECONDS);

        if (ex != null)
            throw ex;
        if (!good)
            throw new RuntimeException("consumer not topped in 5 seconds interval");
        System.exit(0);
    }

    public static StreamConsumer createStreamConsumer(
        LogbrokerClientFactory clientFactory, String topic, String clientId,
        int maxReadBatchSize, int maxInflightReads, int maxUnconsumedReads,
        Supplier<Credentials> credentialsProvider, ExecutorService executorService) {
        try {
            return clientFactory
                .streamConsumer(
                    StreamConsumerConfig.builder(Collections.singletonList(topic), clientId)
                        .setExecutor(executorService)
                        .setCredentialsProvider(credentialsProvider)
                        .configureSession(builder -> builder
                            // Когда readOnlyLocal=true, Логброкер будет присылать только данные, которые были записаны
                            // в том ДЦ, из которого читаем. Когда readOnlyLocal=false, Логброкер будет присылать все
                            // данные из всех ДЦ. Мы ходим во все ДЦ Логброкера, поэтому из каждого ДЦ можно читать
                            // только те данные, которые были записаны в этом ДЦ.
                            .setReadOnlyLocal(true)
                            // Когда clientSideLocksAllowed=true, Логброкер будет присылать сообщения Lock для каждой
                            // партиции и дожидаться Locked в ответ прежде чем присылать данные из этой партиции.
                            // Это единственный правильный способ для Логшаттера узнать список партиций. Ещё в таком
                            // режиме Логброкер будет балансировать партиции между Логшаттерами, которые читают из
                            // одного ДЦ. Подробнее здесь: https://nda.ya.ru/3UXih6
                            .setClientSideLocksAllowed(true)
                            // Когда forceBalancePartitions=false, при перебалансировке партиций между Логшаттерами
                            // Логброкер будет дожидаться коммита всех оффсетов прежде чем отдать партицию другому
                            // Логшаттеру. Это гарантирует что ни в какой момент времени два Логшаттера не будут читать
                            // одну партицию.
                            .setForceBalancePartitions(false)
                            // Если ничего не прочитали за 10 минут, то сессия порестартится
                            .setIdleTimeoutSec(10 * 60)
                        )
                        .configureReader(builder -> builder
                            // Максимальное количество сообщений в пачке данных. Нет смысла ограничивать, потому что
                            // лучше ограничить максимальный размер пачки в байтах.
                            .setMaxCount(Integer.MAX_VALUE)
                            // Максимальный размер пачки сообщений от Логброкера в байтах. Маленькие значения создадут
                            // оверхед при запросе/получении пачек данных (много маленьких запросов). Большие значения
                            // приведут к тому, что если парсинг или сохранение в Кликхаус идут медленно, то в памяти
                            // будут лежать maxUnconsumedReads больших пачек с данными.
                            .setMaxSize(maxReadBatchSize)
                            // Количество параллельных запросов к Логброкеру, которые просят новую пачку данных.
                            .setMaxInflightReads(maxInflightReads)
                            // Количество пачек данных, которые могут стоять в очереди на обработку.
                            .setMaxUnconsumedReads(maxUnconsumedReads)
                        )
                        .configureCommiter(builder -> builder
                            .setMaxUncommittedReads(Integer.MAX_VALUE)
                        )
                        .build()
                );
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static Supplier<Credentials> credentialsProvider(
        int logBrokerTvmClientId,
        int logshatterTvmClientId,
        String logshatterTvmSecret
    ) {

        TvmApiSettings settings = TvmApiSettings.create();
        settings.setSelfClientId(logshatterTvmClientId);
        settings.enableServiceTicketsFetchOptions(logshatterTvmSecret, new int[]{logBrokerTvmClientId});
        TvmClient tvmClient = new TvmClient(settings);

        return () -> Credentials.tvm(tvmClient.getServiceTicketFor(logBrokerTvmClientId));
    }

    private static void stop() {
        streamConsumer.stopConsume();
        executorService.shutdown();
    }

    private static class StreamListenerTestImpl implements StreamListener {
        @Override
        public void onInit(ConsumerInitResponse init) {
            System.out.println(init);
            stop();
        }

        @Override
        public void onRead(ConsumerReadResponse read, ReadResponder readResponder) {
            System.out.println(read);
            stop();
        }

        @Override
        public void onCommit(CommitMessage commit) {
            System.out.println(commit);
            stop();
        }

        @Override
        public void onLock(ConsumerLockMessage lock, LockResponder lockResponder) {
            System.out.println(lock);
            StreamListener.super.onLock(lock, lockResponder);
            stop();
        }

        @Override
        public void onRelease(ConsumerReleaseMessage release) {
            System.out.println(release);
            StreamListener.super.onRelease(release);
            stop();
        }

        @Override
        public void onClose() {
            System.out.println("Closed");
            executorService.shutdownNow();
        }

        @Override
        public void onError(Throwable e) {
            String error = ExceptionsUtils.exceptionToString(e);
            System.out.println(error);
            ex = e;
            stop();
        }
    }

    public static class ExceptionsUtils {
        public static String getStackTrace(final Throwable throwable) {
            final StringWriter sw = new StringWriter();
            final PrintWriter pw = new PrintWriter(sw, true);
            throwable.printStackTrace(pw);
            return sw.getBuffer().toString();
        }

        public static String exceptionToString(final Throwable throwable) {
            if (throwable == null)
                return null;
            return String.format("%s%n%s", throwable.getMessage(), getStackTrace(throwable));
        }
    }

}
