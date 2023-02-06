package ru.yandex.market.core.metamarket;

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;

import ru.yandex.market.core.offer.OfferId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@SuppressWarnings("unchecked")
class MatchedOffersProviderServiceRetryTest {

    @Test
    void testRetry() {
        NamedParameterJdbcTemplate yqlMock = mock(NamedParameterJdbcTemplate.class);
        RetryListener listener = mock(RetryListener.class);
        when(listener.open(any(), any())).thenReturn(true);
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.registerListener(listener);
        MatchedOffersProviderService service = new MatchedOffersProviderService(yqlMock, "", "", retryTemplate);

        when(yqlMock.query(anyString(), any(ResultSetExtractor.class)))
                .thenThrow(RuntimeException.class)
                .thenThrow(RuntimeException.class)
                .thenReturn(true);

        Consumer<Stream<BlueWhitePair<OfferId>>> consumer = mock(Consumer.class);
        service.streamMatchedOfferPairsFromYt(consumer);

        verify(listener, times(2)).onError(any(), any(), any());
        verify(yqlMock, times(3)).query(anyString(), any(ResultSetExtractor.class));
    }

}
