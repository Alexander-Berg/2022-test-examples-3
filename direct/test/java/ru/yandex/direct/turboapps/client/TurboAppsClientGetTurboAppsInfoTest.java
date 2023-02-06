package ru.yandex.direct.turboapps.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import NSuperApp.Turbo;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.turboapps.client.model.TurboAppInfoRequest;
import ru.yandex.direct.turboapps.client.model.TurboAppInfoResponse;
import ru.yandex.direct.turboapps.client.model.TurboAppMetaContentResponse;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.inside.passport.tvm2.TvmHeaders;

import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class TurboAppsClientGetTurboAppsInfoTest extends TurboAppsClientTestBase {

    private static final String URL_WITH_TURBO_APP = "url_with_turbo_app";

    private static final Long BANNER_ID = 1L;
    private static final Long APP_ID = 1337L;

    private final Turbo.TDirectURLsResponse mockAppResponse = Turbo.TDirectURLsResponse.newBuilder()
            .addData(Turbo.TDirectURLsResponse.TBanner.newBuilder()
                    .setBannerID(BANNER_ID)
                    .setBannerURL(URL_WITH_TURBO_APP)
                    .setAppId(APP_ID)
                    .setContent(Turbo.TTurboAppContent.newBuilder()
                            .setTurboAppUrlType(Turbo.ETurboAppUrlType.AsIs)
                            .build())
                    .setMetaContent(Turbo.TTurboAppMetaContent.newBuilder()
                            .setName("Application Title")
                            .setDescription("Application Description")
                            .setIconURL("https://icon")
                            .build())
                    .build())
            .build();

    private MockResponse response;

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();
        response = new MockResponse().setBody(responseToBuffer(mockAppResponse));
    }

    private static Buffer responseToBuffer(Turbo.TDirectURLsResponse response) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        response.writeDelimitedTo(stream);
        return new Buffer().write(stream.toByteArray());
    }

    @Test
    public void getTurboAppsInfo() throws TurboAppsClientException {
        Map<Long, TurboAppInfoResponse> turboApps = turboappsClient.getTurboApps(List.of(new TurboAppInfoRequest()
                .withBannerId(BANNER_ID)
                .withBannerUrl(URL_WITH_TURBO_APP)));

        TurboAppInfoResponse expected = new TurboAppInfoResponse()
                .withBannerId(BANNER_ID)
                .withBannerUrl(URL_WITH_TURBO_APP)
                .withAppId(APP_ID)
                .withContent("{\"TurboAppUrlType\":\"AsIs\"}")
                .withMetaContent(JsonUtils.toJson(new TurboAppMetaContentResponse()
                        .withName("Application Title")
                        .withDescription("Application Description")
                        .withIconUrl("https://icon")));

        softAssertions.assertThat(turboApps.keySet()).containsExactly(BANNER_ID);
        softAssertions.assertThat(turboApps.get(BANNER_ID)).is(matchedBy(beanDiffer(expected)));
    }

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getHeader(TvmHeaders.SERVICE_TICKET)).isEqualTo(TICKET_BODY);
                softAssertions.assertThat(request.getHeader("Content-type")).isEqualTo("application/protobuf");
                softAssertions.assertThat(request.getPath()).isEqualTo("/url2turboappid");
                return response;
            }
        };
    }
}
