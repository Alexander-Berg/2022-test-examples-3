package ru.yandex.direct.libs.laas;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.asynchttp.JsonParsableRequest;
import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.http.smart.core.Call;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LaasClientTest {
    private LaasApi laasApi;
    private LaasClient laasClient;

    @Before
    public void before() {
        laasApi = mock(LaasApi.class);
        laasClient = new LaasClient(laasApi);
    }

    @Test
    public void getRegionId_Success() {
        var regionResponse = new RegionResponse();
        regionResponse.setRegionId(123L);
        Call<RegionResponse> call = getCall(regionResponse);
        when(laasApi.getRegion(anyString(), anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(call);
        Optional<Long> regionId = laasClient.getRegionId("ip", "321123", 333L, "yp");
        assertThat(regionId.isPresent()).isTrue();
        assertThat(regionId.get()).isEqualTo(regionResponse.getRegionId());
    }

    @Test
    public void getRegionId_Error() {
        Call<RegionResponse> errorCall = getErrorCall();
        when(laasApi.getRegion(anyString(), anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(errorCall);
        Optional<Long> regionId = laasClient.getRegionId("ip", "321123", 333L, "yp");
        assertThat(regionId.isEmpty()).isTrue();
    }

    private <R> Call<R> getCall(R result) {
        Result<R> response = mock(Result.class);
        when(response.getSuccess()).thenReturn(result);
        return getCall(response);
    }

    private <R> Call<R> getErrorCall() {
        Result<R> response = mock(Result.class);
        when(response.getErrors()).thenReturn(List.of(new RuntimeException("some error")));
        return getCall(response);
    }

    private <R> Call<R> getCall(Result<R> response) {
        Call<R> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        when(call.getRequest()).thenReturn(new JsonParsableRequest(0L, null, null));
        return call;
    }
}
