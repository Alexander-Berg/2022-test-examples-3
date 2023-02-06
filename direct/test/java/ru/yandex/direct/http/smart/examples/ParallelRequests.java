package ru.yandex.direct.http.smart.examples;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.asynchttp.ParallelFetcher;
import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.asynchttp.ParsableRequest;
import ru.yandex.direct.asynchttp.Result;
import ru.yandex.direct.http.smart.annotations.Id;
import ru.yandex.direct.http.smart.core.Call;
import ru.yandex.direct.http.smart.core.Smart;
import ru.yandex.direct.http.smart.http.GET;
import ru.yandex.direct.http.smart.http.Path;

public class ParallelRequests extends MockServerBase {
    @Autowired
    public ParallelFetcherFactory parallelFetcherFactory;

    @Override
    public Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return new MockResponse().setBody(request.getPath());
            }
        };
    }

    interface Api {
        @GET("/{tmpl}")
        Call<String> get(@Path("tmpl") String tmpl, @Id Long id);
    }

    @Test
    public void getRequest() throws InterruptedException {
        Smart smart = builder().build();
        Api api = smart.create(Api.class);

        List<ParsableRequest<String>> requests = StreamEx.of("1", "2", "3")
                .map(t -> api.get(t, Long.valueOf(t)).getRequest())
                .toList();

        try (ParallelFetcher<String> parallelFetcher = parallelFetcherFactory.getParallelFetcher()) {
            Map<Long, Result<String>> resultMap = parallelFetcher.execute(requests);
            List<String> result = resultMap.values().stream().map(Result::getSuccess).collect(Collectors.toList());
            softAssertions.assertThat(resultMap).containsKeys(1L, 2L, 3L);
            softAssertions.assertThat(result).containsExactly("/1", "/2", "/3");
        }
    }
}
