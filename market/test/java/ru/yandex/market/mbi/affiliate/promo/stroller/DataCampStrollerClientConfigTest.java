package ru.yandex.market.mbi.affiliate.promo.stroller;

import java.io.StringReader;
import java.util.List;

import Market.DataCamp.DataCampPromo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.Timestamp;
import okhttp3.ResponseBody;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.GET;

import ru.yandex.market.mbi.affiliate.promo.common.RetrofitHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataCampStrollerClientConfigTest {
    @Test
    public void testCustomDeserializer() {
        String body = "{\n" +
                "    \"constraints\": {\n" +
                "        \"enabled\": false,\n" +
                "        \"meta\": {\n" +
                "            \"source\": \"MARKET_MBI\",\n" +
                "            \"timestamp\": {\n" +
                "                \"seconds\": 1628750602\n" +
                "            }\n" +
                "        }\n" +
                "    },\n" +
                "    \"primary_key\": {\n" +
                "        \"business_id\": 111181111,\n" +
                "        \"promo_id\": \"aff_parent_1000\",\n" +
                "        \"source\": \"AFFILIATE\"\n" +
                "    } "+
                "}";

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(
                        DataCampPromo.PromoDescription.class,
                        DataCampStrollerClientConfig.getCustomDeserializer())
                .create();
        var result = gson.fromJson(new StringReader(body), DataCampPromo.PromoDescription.class);
        Timestamp timestamp = result.getConstraints().getMeta().getTimestamp();
        assertThat(timestamp.getSeconds(), is(1628750602L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCollectAsyncResult() {
        Api api = mock(Api.class);
        Call<String> callA = mock(Call.class, RETURNS_DEEP_STUBS);
        Call<String> callB = mock(Call.class, RETURNS_DEEP_STUBS);
        when(api.call("a")).thenReturn(callA);
        when(api.call("b")).thenReturn(callB);
        doAnswer(invocation -> {
            var callback = (Callback<String>) invocation.getArgument(0);
            callback.onResponse(callA, Response.success("a!"));
            return null;
        }).when(callA).enqueue(any());
        doAnswer(invocation -> {
            var callback = (Callback<String>) invocation.getArgument(0);
            callback.onResponse(callB, Response.success("b!"));
            return null;
        }).when(callB).enqueue(any());

        var result = RetrofitHelper.collectAsyncResult(
                List.of("a", "b"), api::call, Response::body, 1000);
        assertThat(result, containsInAnyOrder("a!", "b!"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCollectAsyncResultError() {
        Api api = mock(Api.class);
        Call<String> callA = mock(Call.class, RETURNS_DEEP_STUBS);
        Call<String> callB = mock(Call.class, RETURNS_DEEP_STUBS);
        when(api.call("a")).thenReturn(callA);
        when(api.call("b")).thenReturn(callB);
        doAnswer(invocation -> {
            var callback = (Callback<String>) invocation.getArgument(0);
            callback.onResponse(callA, Response.error(500,
                    ResponseBody.create(null, "internal-error")));
            return null;
        }).when(callA).enqueue(any());
        doAnswer(invocation -> {
            var callback = (Callback<String>) invocation.getArgument(0);
            callback.onResponse(callB, Response.success("b!"));
            return null;
        }).when(callB).enqueue(any());

        var result = RetrofitHelper.collectAsyncResult(
                List.of("a", "b"), api::call, Response::body, 1000);
        assertThat(result, containsInAnyOrder("b!"));
    }

    private interface Api {
        @GET
        Call<String> call(String arg);
    }

}
