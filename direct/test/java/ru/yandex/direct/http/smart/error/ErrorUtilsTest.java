package ru.yandex.direct.http.smart.error;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.asynchttp.Result;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.utils.JsonUtils.toJson;

public class ErrorUtilsTest {

    @Test
    public void checkResultForErrors() {
        String detailMessage = "test detail message";
        Object argument = singleton(Integer.MAX_VALUE);

        Result<Object> result = new Result<>(0);
        result.addError(new IllegalArgumentException(detailMessage, new NullPointerException()));
        ThrowableAssert.ThrowingCallable throwingCallable =
                () -> ErrorUtils.checkResultForErrors(result, RuntimeException::new, argument);
        Throwable throwable = catchThrowable(throwingCallable);

        assertSoftly(soft -> {
            soft.assertThat(throwable.getSuppressed()[0])
                    .as("Suppressed").isInstanceOf(IllegalArgumentException.class);
            soft.assertThat(throwable.getMessage())
                    .as("Detail message").contains(detailMessage);
            soft.assertThat(throwable.getMessage())
                    .as("Arguments").contains(toJson(argument));
        });
    }

}
