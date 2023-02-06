package ru.yandex.market.mbi.util.url_capacity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UrlCapacityLimitingTest {
    protected UrlCapacityLimiter urlCapacityLimiter = mock(UrlCapacityLimiter.class);
    protected UrlCapacityLimitFlags urlCapacityLimitFlags = mock(UrlCapacityLimitFlags.class);

    public void setFlagEnabled(boolean value) {
        when(urlCapacityLimitFlags.isEnabled()).thenReturn(value);
    }

    public void setLogsOnly(boolean value) {
        when(urlCapacityLimitFlags.isLogsOnly()).thenReturn(value);
    }

    public void setCanProcessOneMoreUrl(boolean value) {
        when(urlCapacityLimiter.tryProcessOneMoreRequest(any())).thenReturn(value);
    }
}
