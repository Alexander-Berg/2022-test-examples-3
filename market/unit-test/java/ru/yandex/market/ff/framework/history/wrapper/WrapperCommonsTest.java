package ru.yandex.market.ff.framework.history.wrapper;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Data;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class WrapperCommonsTest {


    @Test
    public void ofVoidRunsItsGivenCodeBlock() {

        Runnable myTestRunnable = mock(Runnable.class);

        RandomCodeBlock<Void> nonVoidBlock = WrapperCommons.ofVoid(() -> myTestRunnable.run());
        nonVoidBlock.go();

        verify(myTestRunnable, times(1)).run();

    }

    @Test
    public void ofVoidPassesErrorOutsideIfTheExceptionThrownInItsGivenCodeBlock() {

        Runnable myTestRunnable = () -> {
            throw new RuntimeException("myException!");
        };

        Assertions.assertThrows(
                RuntimeException.class,
                () -> WrapperCommons.ofVoid(() -> myTestRunnable.run()).go(),
                "myException!");
    }

    @ParameterizedTest
    @MethodSource("safeWrapParams")
    public void safeWrapIsReallySafeTest(TestParams args) {

        Supplier<Object> preBlock = () -> {
            if (args.isPreBlockThrowsEx()) {
                throw new RuntimeException("myPreBlockEx!!!");
            }

            if (args.isPreBlockRetursNull()) {
                return null;
            }

            return MyAnswer.builder()
                    .myField("myPreBlockAnswer!!!")
                    .build();
        };

        RandomCodeBlock<Object> block = () -> {
            if (args.getBlockException() != null) {
                throw args.getBlockException();
            }

            return args.getBlockAnswer();
        };

        BiConsumer<Object, Object> successHandler = mock(BiConsumer.class);
        if (args.isSuccessHandlerThrowsEx()) {
            Mockito.doThrow(new RuntimeException("mySuccessHandlerEx!!!")).when(successHandler).accept(any(), any());
        }

        BiConsumer<Object, Throwable> failureHandler = mock(BiConsumer.class);

        if (args.isFailureHandlerThrowsEx()) {
            Mockito.doThrow(new RuntimeException("myFailureHandlerEx!!!")).when(failureHandler).accept(any(), any());
        }

        if (args.getWrapException() != null) {
            Exception e = null;
            try {
                WrapperCommons.safeWrap(
                        preBlock, block,
                        successHandler,
                        failureHandler);
            } catch (Exception ex) {
                e = ex;
            }

            MatcherAssert.assertThat("thrown exception matches", args.getWrapException().matches(e));
        }

        if (args.getWrapAnswer() != null) {
            MatcherAssert.assertThat("answer matches",
                    args.getWrapAnswer().matches(WrapperCommons.safeWrap(
                            preBlock, block,
                            successHandler,
                            failureHandler)));
        }

        Mockito.verify(successHandler, Mockito.times(args.isSuccessHandlerIsCalled() ? 1 : 0)).
                accept(any(), any());
        Mockito.verify(failureHandler, Mockito.times(args.isFailureHandlerIsCalled() ? 1 : 0)).
                accept(any(), any());
    }

    public static Stream<TestParams> safeWrapParams() {
        // Arguments list:
        // preBlock throws exception ?
        // preBlock returns null ?
        // block exception
        // block answer
        // successHandler throws exception ?
        // failureHandler throws exception ?
        // successHandler is called ?
        // failureHandler is called ?
        // wrap exception matcher
        // wrap answer matcher


        return Stream.of(
                // WHEN preBlock returns null AND block throws Ex
                // THEN successHandler/failureHandler are not called AND wrap throws Ex
                getArgs(false,
                        true,
                        new RuntimeException("blockEx1"),
                        null,
                        false,
                        false,
                        false,
                        false,
                        (ex) -> ex.getMessage().equals("blockEx1"),
                        null),
                // WHEN preBlock returns null AND block returns X
                // THEN successHandler/failureHandler are not called AND wrap returns X
                getArgs(false,
                        true,
                        null,
                        MyAnswer.builder()
                                .myField("myValue1")
                                .build(),
                        false,
                        false,
                        false,
                        false,
                        null,
                        (ans) -> ((MyAnswer) ans).getMyField().equals("myValue1")),
                // WHEN preBlock throws exception Ex2 AND block throws exception Ex2
                // THEN successHandler/failureHandler are not called AND  wrap throws Ex2
                getArgs(true,
                        false,
                        new RuntimeException("blockEx2"),
                        null,
                        false,
                        false,
                        false,
                        false,
                        (ex) -> ex.getMessage().equals("blockEx2"),
                        null),
                // WHEN preBlock throws exception Ex2 AND block returns
                // THEN successHandler/failureHandler are not called AND wrap returns X
                getArgs(true,
                        false,
                        null,
                        MyAnswer.builder()
                                .myField("myValue2")
                                .build(),
                        false,
                        false,
                        false,
                        false,
                        null,
                        (ans) -> ((MyAnswer) ans).getMyField().equals("myValue2")),
                // WHEN preBlock returns smth AND block returns X AND successHandler throws Ex
                // THEN successHandler is called AND wrap returns X
                getArgs(false,
                        false,
                        null,
                        MyAnswer.builder()
                                .myField("myValue3")
                                .build(),
                        true,
                        true,
                        true,
                        false,
                        null,
                        (ans) -> ((MyAnswer) ans).getMyField().equals("myValue3")),
                // WHEN preBlock returns smth AND block throws Ex1 AND failureHandler throws Ex2
                // THEN successHandler is NOT called AND failureHandler is called AND wrap throws Ex1
                getArgs(false,
                        false,
                        new RuntimeException("blockEx3"),
                        null,
                        true,
                        true,
                        false,
                        true,
                        (ex) -> ex.getMessage().equals("blockEx3"),
                        null)
        );

    }


    @SuppressWarnings("checkstyle:parameterNumber")
    private static TestParams getArgs(boolean preBlockThrowsEx,
                                      boolean preBlockRetursNull,
                                      RuntimeException blockException,
                                      Object blockAnswer,
                                      boolean successHandlerThrowsEx,
                                      boolean failureHandlerThrowsEx,
                                      boolean successHandlerIsCalled,
                                      boolean failureHandlerIsCalled,
                                      ArgumentMatcher<Exception> wrapException,
                                      ArgumentMatcher<Object> wrapAnswer) {
        return TestParams.builder()
                .preBlockThrowsEx(preBlockThrowsEx)
                .preBlockRetursNull(preBlockRetursNull)
                .blockException(blockException)
                .blockAnswer(blockAnswer)
                .successHandlerThrowsEx(successHandlerThrowsEx)
                .failureHandlerThrowsEx(failureHandlerThrowsEx)
                .successHandlerIsCalled(successHandlerIsCalled)
                .failureHandlerIsCalled(failureHandlerIsCalled)
                .wrapException(wrapException)
                .wrapAnswer(wrapAnswer)
                .build();
    }


    @Builder
    @Data
    public static class TestParams {
        private final boolean preBlockThrowsEx;
        private final boolean preBlockRetursNull;
        private final RuntimeException blockException;
        private final Object blockAnswer;
        private final boolean successHandlerThrowsEx;
        private final boolean failureHandlerThrowsEx;
        private final boolean successHandlerIsCalled;
        private final boolean failureHandlerIsCalled;
        private final ArgumentMatcher<Exception> wrapException;
        private final ArgumentMatcher<Object> wrapAnswer;
    }

    @Builder
    @Data
    public static class MyAnswer {
        private final String myField;
    }
}
