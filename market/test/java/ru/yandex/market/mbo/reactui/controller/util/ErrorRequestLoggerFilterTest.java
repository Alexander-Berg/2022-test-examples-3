package ru.yandex.market.mbo.reactui.controller.util;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import ru.yandex.market.mbo.reactui.controller.util.ErrorRequestLoggerFilter.LimitedContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ErrorRequestLoggerFilterTest {
    LimitedContentCachingResponseWrapper response;

    @Before
    public void setup() {
        response = new LimitedContentCachingResponseWrapper(new MockHttpServletResponse(), 100);
    }

    @Test
    public void testCachingOutputStream() throws IOException {
        response.getOutputStream().print("Hey there!!");

        Assertions.assertThat(response.getContentAsUtf8String()).isEqualTo("Hey there!!");
    }

    @Test
    public void testCachingOutputStreamLimit() throws IOException {
        String longString = IntStream.range(0, 100).mapToObj(i -> "Hey there!!").collect(Collectors.joining());
        response.getOutputStream().print(longString);

        Assertions.assertThat(response.getContentAsUtf8String()).isEqualTo(longString.substring(0, 100));
    }

    @Test
    public void testLimitIsBytes() throws IOException {
        String longString = IntStream.range(0, 100).mapToObj(i -> "Приветы!!!").collect(Collectors.joining());
        response.setCharacterEncoding("utf-8");
        response.getWriter().print(longString);

        Assertions.assertThat(response.getContentAsByteArray())
            .isEqualTo(Arrays.copyOf(longString.getBytes(StandardCharsets.UTF_8), 100));
    }
}
