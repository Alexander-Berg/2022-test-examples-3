package ru.yandex.market.mock;

import org.apache.http.StatusLine;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author korolyov
 * 21.06.16
 */
public class StatusLineMockFactory {
    private StatusLineMockFactory() {

    }

    public static StatusLine mockStatusLineWithCode(int code) {
        StatusLine result = mock(StatusLine.class);
        when(result.getStatusCode()).thenReturn(code);
        return result;
    }

}
