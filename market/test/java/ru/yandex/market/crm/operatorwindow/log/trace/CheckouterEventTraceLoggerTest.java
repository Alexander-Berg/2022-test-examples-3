package ru.yandex.market.crm.operatorwindow.log.trace;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.request.trace.Module;
import ru.yandex.market.request.trace.RequestContextHolder;


public class CheckouterEventTraceLoggerTest {

    private static final String TEST_REQ_ID = "TestReqId";
    private CheckouterEventTraceLogger traceLogger;

    @BeforeEach
    public void init() {
        Module module = Module.MARKET_OPERATOR_WINDOW;
        this.traceLogger = new CheckouterEventTraceLogger(module);
    }

    @AfterEach
    public void tearDown() {
        RequestContextHolder.clearContext();
    }

    // Тяжело нормально проверить метод т.к. внутри используются статические методы, для которых нельзя сдеалать мок
    // А единственный параметр - enum, который тоже не мокнуть. Поэтому лишь минимальный смоук-тест.
    @Test
    public void testToWithLogSmoke() {
        RequestContextHolder.createContext(Optional.of(TEST_REQ_ID));

        Boolean result = traceLogger.doWithLog(() -> new SimpleResultWithTraceDataBuilder<Boolean>()
                .setResult(true)
                .setDescription("Test description")
                .addTraceParameter("traceParameterOne", "traceValue")
                .build());

        Assertions.assertTrue(result, "Метод возвратил неправильный ответ");
    }

}
