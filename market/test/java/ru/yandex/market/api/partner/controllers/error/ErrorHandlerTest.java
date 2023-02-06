package ru.yandex.market.api.partner.controllers.error;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ErrorHandlerTest {
    private ExceptionHandler defaultExceptionHandler = new SimpleExceptionHandler<Exception>();
    private ExceptionHandler exceptionHandler1 = new CheckouterExceptionHandler();
    private ExceptionHandler exceptionHandler2 = new CheckouterExceptionHandler();

    @Test
    public void getExceptionHandlerTest() {
        ErrorHandler handler = new ErrorHandler();
        handler.setDefaultExceptionHandler(defaultExceptionHandler);
        Map<Class, ExceptionHandler> map = new HashMap<Class, ExceptionHandler>();
        map.put(IOException.class, exceptionHandler2);
        map.put(SocketTimeoutException.class, exceptionHandler1);
        handler.setExceptionHandlersMap(map);
        assertTrue(defaultExceptionHandler == handler.getExceptionHandler(RuntimeException.class));
        assertTrue(exceptionHandler1 == handler.getExceptionHandler(SocketTimeoutException.class));
        assertTrue(exceptionHandler2 == handler.getExceptionHandler(IOException.class));
        assertTrue(exceptionHandler2 == handler.getExceptionHandler(InterruptedIOException.class));
    }

}
