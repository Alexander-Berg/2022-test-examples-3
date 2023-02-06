package ru.yandex.market.logistics.lom.controller.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.util.client.tvm.client.TvmServiceTicket;
import ru.yandex.market.logistics.util.client.tvm.client.TvmTicketStatus;
import ru.yandex.market.logistics.util.client.tvm.client.TvmUserTicket;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.util.client.HttpTemplate.SERVICE_TICKET_HEADER;
import static ru.yandex.market.logistics.util.client.HttpTemplate.USER_TICKET_HEADER;

public class TvmClientApiTestUtil {

    public static final BigDecimal USER_UID = new BigDecimal(111);
    public static final Long SERVICE_ID = 222L;
    private static final long USER_UID_LONG_VALUE = USER_UID.longValue();
    private static final int SERVICE_ID_INT_VALUE = SERVICE_ID.intValue();
    private static final String USER_TICKET_HEADER_VALUE = "test-user-ticket";
    private static final String SERVICE_TICKET_HEADER_VALUE = "test-service-ticket";
    public static final Map<String, List<String>> USER_HEADERS = Map.of(
        USER_TICKET_HEADER,
        List.of(USER_TICKET_HEADER_VALUE)
    );
    public static final Map<String, List<String>> SERVICE_HEADERS = Map.of(
        SERVICE_TICKET_HEADER,
        List.of(SERVICE_TICKET_HEADER_VALUE)
    );
    public static final Map<String, List<String>> USER_AND_SERVICE_HEADERS = Map.of(
        USER_TICKET_HEADER,
        List.of(USER_TICKET_HEADER_VALUE),
        SERVICE_TICKET_HEADER,
        List.of(SERVICE_TICKET_HEADER_VALUE)
    );

    private TvmClientApiTestUtil() {
        throw new UnsupportedOperationException();
    }

    public static void mockTvmClientApi(TvmClientApi tvmClientApi) {
        when(tvmClientApi.checkUserTicket(eq(USER_TICKET_HEADER_VALUE))).thenReturn(new TvmUserTicket(
            USER_UID_LONG_VALUE,
            TvmTicketStatus.OK
        ));
        when(tvmClientApi.checkServiceTicket(SERVICE_TICKET_HEADER_VALUE)).thenReturn(new TvmServiceTicket(
            SERVICE_ID_INT_VALUE,
            TvmTicketStatus.OK,
            ""
        ));
    }
}
