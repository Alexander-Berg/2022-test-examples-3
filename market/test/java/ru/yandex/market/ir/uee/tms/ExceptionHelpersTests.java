package ru.yandex.market.ir.uee.tms;

import java.net.http.HttpTimeoutException;

import org.apache.http.HttpException;
import org.junit.Test;

import ru.yandex.market.ir.uee.tms.utils.ExceptionHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExceptionHelpersTests {
    @Test
    public void exception_withoutMessage_withoutCause_shouldYieldSingleMessage_withExceptionType() {
        // arrange
        var ex = new IllegalStateException();

        // act
        var messages = ExceptionHelpers.getExceptionMessageChain(ex, true);

        // assert
        assertEquals(1, messages.size());
        assertEquals(ex.getClass().getSimpleName(), messages.get(0));
    }

    @Test
    public void exception_withMessage_withoutCause_shouldYieldSingleMessage_withExceptionTypeAndMessage() {
        // arrange
        var ex = new IllegalStateException("Shit happened");

        // act
        var messages = ExceptionHelpers.getExceptionMessageChain(ex, true);

        // assert
        assertEquals(1, messages.size());
        assertTrue(messages.get(0).startsWith(ex.getClass().getSimpleName()));
        assertTrue(messages.get(0).contains(ex.getMessage()));    }

    @Test
    public void exception_withMessage_withCause_havingNoMessage_shouldYieldTwoMessages_inExpectedOrder() {
        // arrange
        var ex = new IllegalStateException("Shit happened", new ArithmeticException());

        // act
        var messages = ExceptionHelpers.getExceptionMessageChain(ex, true);

        // assert
        assertEquals(2, messages.size());
        assertEquals(ex.getCause().getClass().getSimpleName(), messages.get(0));
        assertTrue(messages.get(1).startsWith(ex.getClass().getSimpleName()));
        assertTrue(messages.get(1).contains(ex.getMessage()));
    }

    @Test
    public void exception_withMessage_withCause_havingMessage_shouldYieldBothMessages_inExpectedOrder() {
        // arrange
        var ex = new IllegalStateException("Shit happened", new ArithmeticException("Division by 0"));

        // act
        var messagesFromInnermost = ExceptionHelpers.getExceptionMessageChain(ex, true);
        var messagesFromOutermost = ExceptionHelpers.getExceptionMessageChain(ex, false);

        // assert
        assertEquals(2, messagesFromInnermost.size());
        assertTrue(messagesFromInnermost.get(0).contains(ex.getCause().getMessage()));
        assertTrue(messagesFromInnermost.get(1).contains(ex.getMessage()));

        assertEquals(2, messagesFromOutermost.size());
        assertTrue(messagesFromOutermost.get(0).contains(ex.getMessage()));
        assertTrue(messagesFromOutermost.get(1).contains(ex.getCause().getMessage()));
    }

    @Test
    public void equalAdjacentTypesAndMessages_shouldBeOmitted() {
        // arrange
        var ex = new IllegalStateException(
                "Shit happened",
                new HttpException("Network timeout", new HttpException("Network timeout")));

        // act
        var messages = ExceptionHelpers.getExceptionMessageChain(ex, true);

        // assert
        assertEquals(2, messages.size());
        assertTrue(messages.get(0).contains(ex.getCause().getMessage()));
        assertTrue(messages.get(1).contains(ex.getMessage()));
    }

    @Test
    public void recursiveCauses_shouldBeBroken() {
        // arrange
        var outer = new IllegalStateException("Shit happened");
        var inner = new IllegalStateException("Something went wrong", outer);
        outer.initCause(inner);

        // act
        var messages = ExceptionHelpers.getExceptionMessageChain(outer, true);

        // assert
        assertEquals(10, messages.size());
    }
}
