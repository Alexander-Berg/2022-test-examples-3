package ru.yandex.direct.audience.client;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.asynchttp.ErrorResponseWrapperException;
import ru.yandex.direct.audience.client.exception.SegmentNotModifiedException;
import ru.yandex.direct.audience.client.exception.YaAudienceClientException;
import ru.yandex.direct.audience.client.exception.YaAudienceClientTypedException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.audience.client.YaAudienceClientUtils.errorIsSegmentNotChanged;
import static ru.yandex.direct.audience.client.YaAudienceClientUtils.getException;

class YaAudienceClientUtilsTest {
    @Test
    void errorIsSegmentNotChanged_NullResponse_False() {
        boolean result = errorIsSegmentNotChanged(new ErrorResponseWrapperException("some", null, new Throwable()));
        assertThat(result, is(false));
    }

    @Test
    void errorIsSegmentNotChanged_NullResponseBody_False() {
        Response response = mock(Response.class);
        when(response.getResponseBody()).thenReturn(null);
        boolean result = errorIsSegmentNotChanged(new ErrorResponseWrapperException("some", response, new Throwable()));
        assertThat(result, is(false));
    }

    @Test
    void getException_YaAudienceClientException() {
        Response response = mock(Response.class);
        when(response.getResponseBody()).thenReturn(null);
        YaAudienceClientException result = getException(List.of(new ErrorResponseWrapperException("some", response,
                new Throwable())), "Message");

        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result instanceof YaAudienceClientTypedException)
                .as("Is not YaAudienceClientTypedException")
                .isEqualTo(false);
        softAssertions.assertThat(result instanceof SegmentNotModifiedException)
                .as("Is not SegmentNotModifiedException")
                .isEqualTo(false);
        softAssertions.assertThat(result.getMessage())
                .as("Has Message")
                .isEqualTo("Message");
        softAssertions.assertAll();
    }

    @Test
    void getException_YaAudienceClientTypedException() {
        Response response = mock(Response.class);
        when(response.getResponseBody()).thenReturn(null);
        String causeMessage = "{\"message\":\"Example error\", \"error_type\":\"invalid_param\"}";
        YaAudienceClientException result = getException(List.of(new ErrorResponseWrapperException("some", response,
                new Throwable(causeMessage))), "Message");
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result instanceof SegmentNotModifiedException)
                .as("Is not SegmentNotModifiedException")
                .isEqualTo(false);
        softAssertions.assertThat(result instanceof YaAudienceClientTypedException)
                .as("Is not YaAudienceClientTypedException")
                .isEqualTo(true);
        softAssertions.assertThat(result.getMessage())
                .as("Has Message")
                .isEqualTo("Example error");
        softAssertions.assertAll();
    }
}
