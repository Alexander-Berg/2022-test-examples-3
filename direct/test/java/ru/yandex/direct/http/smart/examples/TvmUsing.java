package ru.yandex.direct.http.smart.examples;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.inside.passport.tvm2.TvmHeaders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TvmUsing extends MockServerBase {
    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse().setBody(request.getHeader(TvmHeaders.SERVICE_TICKET));
            }
        };
    }

    interface Api {
        @GET("/")
        Call<String> get();
    }

    @Test
    public void tvmTicket() {
        String ticketBody = "ticketBody";
        TvmIntegration tvmIntegration = mock(TvmIntegration.class);
        when(tvmIntegration.isEnabled()).thenReturn(true);
        when(tvmIntegration.getTicket(any())).thenReturn(ticketBody);

        Smart smart = builder().useTvm(tvmIntegration, TvmService.UNKNOWN).build();
        Api api = smart.create(Api.class);
        String resp = api.get().execute().getSuccess();
        softAssertions.assertThat(resp).isEqualTo(ticketBody);
    }
}
