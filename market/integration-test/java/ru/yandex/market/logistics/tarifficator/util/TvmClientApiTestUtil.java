package ru.yandex.market.logistics.tarifficator.util;

import java.util.List;
import java.util.Map;

import lombok.experimental.UtilityClass;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;

import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.util.client.tvm.client.TvmTicketStatus;
import ru.yandex.market.logistics.util.client.tvm.client.TvmUserTicket;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.util.client.HttpTemplate.USER_TICKET_HEADER;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class TvmClientApiTestUtil {
    private static final String USER_TICKET_HEADER_VALUE = "test-user-ticket";
    public static final HttpHeaders USER_HEADERS = new HttpHeaders(new LinkedMultiValueMap<>(Map.of(
        USER_TICKET_HEADER,
        List.of(USER_TICKET_HEADER_VALUE)
    )));

    public static void mockTvmClientApiUserTicket(TvmClientApi tvmClientApi, Long value) {
        when(tvmClientApi.checkUserTicket(eq(USER_TICKET_HEADER_VALUE))).thenReturn(new TvmUserTicket(
            value,
            TvmTicketStatus.OK
        ));
    }
}
