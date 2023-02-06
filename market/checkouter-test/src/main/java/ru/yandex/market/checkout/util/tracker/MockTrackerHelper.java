package ru.yandex.market.checkout.util.tracker;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.Stubbing;
import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpHeaders;
import org.springframework.http.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

public final class MockTrackerHelper {

    public static final long TRACKER_ID = 999999L;

    private MockTrackerHelper() {
    }

    public static void mockGetDeliveryServices(long deliveryServiceId, Stubbing deliveryTrackerMock) {
        deliveryTrackerMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/services"))
                .willReturn(ResponseDefinitionBuilder.responseDefinition()
                        .withBody("{\"deliveryServices\":[{\"id\": " + deliveryServiceId + "}]}")
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)));
    }

    public static void mockGetDeliveryServices(Collection<Long> deliveryServiceIds, Stubbing deliveryTrackerMock) {
        deliveryTrackerMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/services"))
                .willReturn(ResponseDefinitionBuilder.okForJson(
                        ImmutableMap.of(
                                "deliveryServices",
                                deliveryServiceIds.stream().map(id -> ImmutableMap.of("id", id))
                                        .collect(Collectors.toList())
                        )
                ))
        );
    }

    public static void mockPutTrack(WireMockServer trackerMock, long trackerId) {
        mockPutTrack(trackerMock, trackerId, null);
    }

    public static void mockPutTrack(WireMockServer trackerMock, long trackerId, @Nullable String trackCode) {
        MappingBuilder builder = put(urlPathEqualTo("/track"));
        if (trackCode != null) {
            builder = builder.withQueryParam("trackCode", equalTo(trackCode));
        }

        trackerMock.stubFor(builder
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{ \"id\": " + trackerId + " }")
                        .withHeader("Content-Type", "application/json"))
        );
    }

    public static void mockDeleteTrack(WireMockServer trackerMock, long trackId) {
        String deleteTrackUrl = "/track/" + trackId + "/delete";

        trackerMock.stubFor(put(urlPathEqualTo(deleteTrackUrl))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{ \"id\": " + trackId + " }")
                        .withHeader("Content-Type", "application/json")
                )
        );
    }
}
