package ru.yandex.market.checkout.pushapi.service.shop;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerInitResponse;
import ru.yandex.kikimr.persqueue.producer.transport.message.inbound.ProducerWriteResponse;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaDataGetterService;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.service.EnvironmentService;
import ru.yandex.market.checkout.pushapi.service.LogBrokerService;
import ru.yandex.market.checkout.pushapi.shop.HttpBodies;
import ru.yandex.market.checkout.pushapi.shop.ShopApiResponse;
import ru.yandex.market.personal.data.cleaner.PersonalDataCleanerService;

import static org.mockito.ArgumentMatchers.anyLong;

public abstract class AbstractLogTestBase extends AbstractWebTestBase {

    protected LogbrokerRequestPublishService requestPublishService;

    protected long shopId = 1L;
    protected boolean sandbox = false;

    @Autowired
    protected LogbrokerClientFactory logbrokerClientFactory;
    @Mock
    protected AsyncProducer logbrokerAsyncProducer;
    @Autowired
    protected LogBrokerService logBrokerService;
    @Captor
    protected ArgumentCaptor<byte[]> messageCaptor;
    @Autowired
    @Qualifier("shopService")
    private ShopMetaDataGetterService shopMetaDataGetterService;
    @Autowired
    protected PersonalDataCleanerService personalDataCleanerService;
    @Autowired
    protected EnvironmentService environmentService;

    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1) {
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(
                Runnable command, long initialDelay, long delay, TimeUnit unit
        ) {
            command.run();
            return null;
        }
    };

    @Override
    @BeforeEach
    public void setUp() throws InterruptedException {
        MockitoAnnotations.initMocks(this);

        Mockito.when(logbrokerClientFactory.asyncProducer(Mockito.any(AsyncProducerConfig.class)))
                .thenReturn(logbrokerAsyncProducer);

        CompletableFuture future = CompletableFuture.completedFuture(new ProducerWriteResponse(1, 1, false));
        Mockito.doReturn(future)
                .when(logbrokerAsyncProducer).write(messageCaptor.capture(), anyLong());
        CompletableFuture<ProducerInitResponse> initFuture
                = CompletableFuture.completedFuture(
                new ProducerInitResponse(Long.MAX_VALUE, "1", 1, "1")
        );
        Mockito.doReturn(initFuture)
                .when(logbrokerAsyncProducer).init();

        requestPublishService = new LogbrokerRequestPublishService(
                executorService,
                logBrokerService,
                shopMetaDataGetterService,
                personalDataCleanerService,
                environmentService
        );
    }

    protected class MockHttpBodies extends HttpBodies {

        protected String requestHeaders;
        protected String requestBody;
        protected String responseHeaders;
        protected String responseBody;

        protected MockHttpBodies(String requestHeaders, String requestBody, String responseHeaders,
                                 String responseBody) {
            this.requestHeaders = requestHeaders;
            this.requestBody = requestBody;
            this.responseHeaders = responseHeaders;
            this.responseBody = responseBody;
        }

        private ByteArrayOutputStream baos(String str) {
            try {
                final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byteArrayOutputStream.write(str.getBytes());
                return byteArrayOutputStream;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public ByteArrayOutputStream getRequestHeaders() {
            return baos(requestHeaders);
        }

        @Override
        public ByteArrayOutputStream getRequestBody() {
            return baos(requestBody);
        }

        @Override
        public ByteArrayOutputStream getResponseHeaders() {
            return baos(responseHeaders);
        }

        @Override
        public ByteArrayOutputStream getResponseBody() {
            return baos(responseBody);
        }
    }

    protected ShopApiResponse getResponse() {
        String responseBody = String.join(", ", Collections.nCopies(5000, "a"));
        ShopApiResponse response = ShopApiResponse.fromException(null)
                .setUid(123456L)
                .populateBodies(new MockHttpBodies("qwerty", "asdfgh", "zxcvbn", responseBody))
                .setHost("host1")
                .setResponseTime(1111L)
                .setUrl("https://qwerty")
                .setArgs("args")
                .setHttpMethod("GET");
        return response;
    }
}
