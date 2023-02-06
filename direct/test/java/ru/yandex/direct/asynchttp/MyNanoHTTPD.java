package ru.yandex.direct.asynchttp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Реализация NanoHTTPD для тестов AsyncHttpLibrary.
 * <p>
 * Принимаемые параметры:
 * <ul>
 * <li>{@value PARAM_RESPONSE_TIME} - время ответа в мс
 * (default: {@link MyNanoHTTPD#DEFAULT_RESPONSE_TIME} = {@value DEFAULT_RESPONSE_TIME})</li>
 * <li>{@value PARAM_RESPONSE_TEXT} - текст ответа (default: {@link MyNanoHTTPD#DEFAULT_RESPONSE_TEXT})</li>
 * <li>{@value PARAM_REQUEST_ID} - идентификатор запроса (обязательный параметр)</li>
 * <li>{@value PARAM_FAST_RESPONSE_ORDER_NUMBER} - порядковый номер ответа, для которого ответ будет дан без задержки
 * ({@value PARAM_RESPONSE_TIME}); для тестирования таймаутов</li>
 * </ul>
 */
class MyNanoHTTPD extends NanoHTTPD {
    private static final Logger logger = LoggerFactory.getLogger(MyNanoHTTPD.class);

    static final String PARAM_RESPONSE_TIME = "responseTime";
    static final String PARAM_RESPONSE_TEXT = "responseText";
    static final String PARAM_RESPONSE_CODE = "responseCode";
    static final String PARAM_REQUEST_ID = "requestId";
    static final String PARAM_FAST_RESPONSE_ORDER_NUMBER = "fastResponseOrderNumber";

    private static final int DEFAULT_RESPONSE_TIME = 200;
    private static final String DEFAULT_RESPONSE_TEXT = "Hello";
    private static final int ZERO_RESPONSE_TIME = 0;

    private final AtomicInteger counter = new AtomicInteger();
    private final Map<String, Integer> uniqueRequestCounter = new ConcurrentHashMap<>();

    MyNanoHTTPD(int threadsCount) {
        super(0);
        setAsyncRunner(new FixedPoolAsyncRunner(threadsCount));
    }

    /**
     * Один экземпляр MyNano может использоваться в нескольких тестах,
     * в таком случае после каждого теста нужно очистить мапу с уникальными id-запросов,
     * чтобы переиспользовать одни и те же id тестах
     */
    void resetUniqueRequestCounter() {
        uniqueRequestCounter.clear();
    }

    @Override
    public Response serve(IHTTPSession session) {
        Map<String, String> params = session.getParms();
        int requestNumber = counter.incrementAndGet();
        logger.info("Request #{}. Params: {}", requestNumber, params);
        if (!params.containsKey(PARAM_REQUEST_ID)) {
            logger.error("No requestId specified for request #{}", requestNumber);
            throw new RuntimeException("No requestId specified");
        }

        String requestId = params.get(PARAM_REQUEST_ID);

        uniqueRequestCounter.putIfAbsent(requestId, 0);
        uniqueRequestCounter.computeIfPresent(requestId, (key, oldValue) -> oldValue + 1);
        logger.info("(requestId '{}') attempt #{}", requestId, uniqueRequestCounter.get(requestId));

        int responseTimeDelay;
        if (params.containsKey(PARAM_RESPONSE_TIME)) {
            responseTimeDelay = Integer.parseInt(params.get(PARAM_RESPONSE_TIME));
        } else {
            responseTimeDelay = DEFAULT_RESPONSE_TIME;
        }

        if (params.containsKey(PARAM_FAST_RESPONSE_ORDER_NUMBER)) {
            int fastResponseOrderNumber = Integer.parseInt(params.get(PARAM_FAST_RESPONSE_ORDER_NUMBER));
            int attemptNumber = uniqueRequestCounter.get(requestId);
            if (attemptNumber >= fastResponseOrderNumber) {
                logger.info(
                        "(request '{}') attempt #{} is greater or equal fastResponseOrderNumber parameter. Use zero response delay",
                        requestId, attemptNumber);
                responseTimeDelay = ZERO_RESPONSE_TIME;
            }
        }

        if (responseTimeDelay > ZERO_RESPONSE_TIME) {
            logger.info("Sleep {}ms before returning response", responseTimeDelay);
            try {
                Thread.sleep(responseTimeDelay);
            } catch (InterruptedException e) {
                logger.error("Interrupt during response delay sleep", e);
            }
        }

        String responseText = params.getOrDefault(PARAM_RESPONSE_TEXT, DEFAULT_RESPONSE_TEXT);
        logger.info("(request '{}') return '{}'", requestId, responseText);
        Response.IStatus responseCode = Response.Status.OK;
        if (params.containsKey(PARAM_RESPONSE_CODE)) {
            responseCode = Response.Status.valueOf(params.get(PARAM_RESPONSE_CODE));
        }
        return newFixedLengthResponse(responseCode, NanoHTTPD.MIME_HTML, responseText);
    }

    /**
     * По факту &ndash; дубликат {@link NanoHTTPD.DefaultAsyncRunner},
     * использующий {@link Executors#newFixedThreadPool(int)}
     */
    private static class FixedPoolAsyncRunner implements AsyncRunner {

        private final ExecutorService executorService;
        private final List<ClientHandler> running = Collections.synchronizedList(new ArrayList<ClientHandler>());

        FixedPoolAsyncRunner(int threadsCount) {
            executorService = Executors.newFixedThreadPool(threadsCount);
        }

        @Override
        public void closeAll() {
            // copy of the list for concurrency
            for (ClientHandler clientHandler : new ArrayList<>(this.running)) {
                clientHandler.close();
            }
        }

        @Override
        public void closed(ClientHandler clientHandler) {
            this.running.remove(clientHandler);
        }

        @Override
        public void exec(ClientHandler code) {
            executorService.execute(code);
            this.running.add(code);
        }
    }
}
