package ru.yandex.market.fmcg.bff.test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import lombok.SneakyThrows;
import org.springframework.util.StreamUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * TODO
 *
 * @author semin-serg
 */
public class TestUtil {
    @SneakyThrows
    public static String loadResourceAsString(String resourceName) {
        return StreamUtils.copyToString(TestUtil.class.getClassLoader().getResourceAsStream(resourceName),
            StandardCharsets.UTF_8);
    }

    @SneakyThrows
    public static byte[] loadResourceAsBytes(String resourceName) {
        return StreamUtils.copyToByteArray(TestUtil.class.getClassLoader().getResourceAsStream(resourceName));
    }

    public static HttpServletRequest requestWithHeadersMock(Map<String, String> headers) {
        HttpServletRequest mock = mock(HttpServletRequest.class);
        for (String header : headers.keySet()) {
            doReturn(headers.get(header)).when(mock).getHeader(eq(header));
        }
        return mock;
    }
}
