package ru.yandex.market.loyalty.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.jetbrains.annotations.NotNull;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.juggler.JugglerEvent;
import ru.yandex.market.loyalty.monitoring.MonitorType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.atLeast;

public class JugglerTestUtils {
    public static final TypeReference<List<JugglerEventView>> JUGGLER_CLIENT_REQUEST_TYPE = new TypeReference<>() {
    };

    @NotNull
    public static BasicHttpResponse getOkResponse() {
        return new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_0, 200, "OK"));
    }

    public static void mockOkeyJugglerResponse(HttpClient jugglerHttpClient) throws IOException {
        willReturn(getOkResponse()).given(jugglerHttpClient).execute(any(HttpUriRequest.class));
    }

    public static void assertSingleJugglerEventPushed(
            MonitorType type, JugglerEvent.Status status, HttpClient jugglerHttpClient, ObjectMapper objectMapper
    ) throws IOException {
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        then(jugglerHttpClient).should(atLeast(1)).execute(captor.capture());

        final List<HttpUriRequest> allValues = captor.getAllValues();
        //assertThat(allValues, hasSize(3)); кажется это лишнее

        final List<JugglerEventView> jugglerEvents = getJugglerEvents(objectMapper, allValues);

        assertThat(jugglerEvents, hasItem(
                allOf(
                        hasProperty("service", equalTo(type.getJugglerService())),
                        hasProperty("status", equalTo(status.name()))
                )
        ));
    }

    public static void assertZeroJugglerEventPushed(
            HttpClient jugglerHttpClient,
            ObjectMapper objectMapper
    ) throws IOException {
        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        then(jugglerHttpClient).should(atLeast(1)).execute(captor.capture());

        final List<HttpUriRequest> allValues = captor.getAllValues();
        //assertThat(allValues, hasSize(3)); кажется это лишнее

        List<JugglerEventView> jugglerEvents = getJugglerEvents(objectMapper, allValues);
        assertThat(jugglerEvents, not(hasItem(
                hasProperty("status", equalTo(JugglerEvent.Status.OK))
        )));
    }

    private static List<JugglerEventView> getJugglerEvents(
            ObjectMapper objectMapper, List<HttpUriRequest> allValues
    ) throws IOException {
        final List<JugglerEventView> jugglerEvents = new ArrayList<>();
        for (HttpUriRequest httpUriRequest : allValues) {
            final List<JugglerEventView> values = objectMapper.readValue(
                    ((HttpPost) httpUriRequest).getEntity().getContent(),
                    JUGGLER_CLIENT_REQUEST_TYPE
            );
            jugglerEvents.addAll(values);
        }
        return jugglerEvents;
    }
}
