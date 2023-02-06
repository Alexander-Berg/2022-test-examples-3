package ru.yandex.direct.core.testing.stub;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import one.util.streamex.StreamEx;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.turboapps.client.TurboAppsClient;
import ru.yandex.direct.turboapps.client.model.TurboAppInfoRequest;
import ru.yandex.direct.turboapps.client.model.TurboAppInfoResponse;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static org.mockito.Mockito.mock;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;

@ParametersAreNonnullByDefault
public class TurboAppsClientStub extends TurboAppsClient {

    private final Map<String, TurboAppInfoResponse> stubResponses;

    public TurboAppsClientStub() {
        super("http://superapp-http-direct.advmachine.yandex.net",
                10,
                mock(TvmIntegration.class),
                TvmService.DIRECT_SCRIPTS_TEST,
                mock(ParallelFetcherFactory.class));

        this.stubResponses = new HashMap<>();
    }

    public void addTurboAppInfo(String href, TurboAppInfoResponse turboAppInfoResponse) {
        stubResponses.put(href, turboAppInfoResponse);
    }

    @Override
    public Map<Long, TurboAppInfoResponse> getTurboApps(List<TurboAppInfoRequest> requests) {
        return StreamEx.of(requests)
                .mapToEntry(TurboAppInfoRequest::getBannerId, TurboAppInfoRequest::getBannerUrl)
                .mapToValue((bid, href) -> ifNotNull(stubResponses.get(href), response -> new TurboAppInfoResponse()
                        .withBannerId(bid)
                        .withBannerUrl(href)
                        .withAppId(response.getAppId())
                        .withContent(response.getContent())
                        .withMetaContent(response.getMetaContent())))
                .filterValues(Objects::nonNull)
                .toMap();
    }
}
