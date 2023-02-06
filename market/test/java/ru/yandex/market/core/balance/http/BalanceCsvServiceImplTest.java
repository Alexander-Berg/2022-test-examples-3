package ru.yandex.market.core.balance.http;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.core.balance.http.BalanceCsvServiceImpl.DataLoader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.core.balance.http.BalanceCsvService.BALANCE_TIMESTAMP_FORMAT;

/**
 * Unit тесты для {@link BalanceCsvServiceImpl}.
 *
 * @author avetokhin 29/06/17.
 */
public class BalanceCsvServiceImplTest {

    private static final long SERVICE_ID = 172;
    private static final String FROM = "20170617021229";
    private static final String TO = "20170618000000";

    private static final String BALANCE_URL = "http://balance-xmlrpc-ipv6.yandex.net:8002/httpapi/";

    @Test
    public void findOrderTransactions() throws IOException {
        final DataLoader loader = Mockito.mock(DataLoader.class);
        final BalanceCsvServiceImpl service = new BalanceCsvServiceImpl(BALANCE_URL, loader);

        service.findOrderTransactions(SERVICE_ID,
                BALANCE_TIMESTAMP_FORMAT.parse(FROM, ZonedDateTime::from).toInstant(),
                BALANCE_TIMESTAMP_FORMAT.parse(TO, ZonedDateTime::from).toInstant());

        final URL url = new URL(BALANCE_URL + BalanceCsvServiceImpl.METHOD_MARKET_PAYMENTS_STAT
                + "?from_trantime=" + FROM + "&service_id=" + SERVICE_ID + "&to_trantime=" + TO);

        verify(loader).load(eq(url), any());
        verifyNoMoreInteractions(loader);
    }

}
