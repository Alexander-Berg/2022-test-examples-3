package ru.yandex.travel.orders.services.avia;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.travel.commons.logging.AsyncHttpClientWrapper;
import ru.yandex.travel.workflow.exceptions.RetryableException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class AviaApiProxyTest {
    @Test
    public void refreshVariant() {
        AsyncHttpClientWrapper ahcWrapper = Mockito.mock(AsyncHttpClientWrapper.class);
        AviaApiProperties config = new AviaApiProperties();
        config.setBaseUrl("http://localhost/");
        config.setHttpReadTimeout(Duration.ofSeconds(1));
        config.setHttpRequestTimeout(Duration.ofSeconds(1));
        AviaApiProxy proxy = new AviaApiProxy(ahcWrapper, config, null);

        when(ahcWrapper.executeRequest(any(), any())).thenReturn(
                CompletableFuture.failedFuture(new ConnectException("Network is unreachable: ...")));
        assertThatThrownBy(() -> proxy.refreshVariant("variantId"))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("Network is unreachable");
    }
}
