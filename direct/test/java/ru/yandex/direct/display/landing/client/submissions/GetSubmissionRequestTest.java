package ru.yandex.direct.display.landing.client.submissions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.asynchttpclient.RequestBuilder;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class GetSubmissionRequestTest {
    @Test
    public void prepareRequest_turboPageIdsAreSet() {
        GetSubmissionsRequest request = new GetSubmissionsRequest()
                .withTurboPageIds(asList(1L, 2L, 3L));

        RequestBuilder builder = new RequestBuilder();
        request.prepare(builder);
        assertThat(builder.build().getStringData()).contains("TurboPageIds");
    }

    @Test
    public void prepareRequest_clientIdAreSet() {
        GetSubmissionsRequest request = new GetSubmissionsRequest()
                .withClientId(5L);

        RequestBuilder builder = new RequestBuilder();
        request.prepare(builder);
        assertThat(builder.build().getStringData()).contains("ClientId");
    }

    @Test
    public void prepareRequest_dateTimeFromIsSet() {
        GetSubmissionsRequest request = new GetSubmissionsRequest()
                .withDateTimeFrom(OffsetDateTime.of(
                        2017, 11, 3, 16, 12, 45, 0,
                        ZoneOffset.ofHours(0)));

        RequestBuilder builder = new RequestBuilder();
        request.prepare(builder);
        assertThat(builder.build().getStringData()).contains("DateTimeFrom");
    }

    @Test
    public void prepareRequest_dateTimeFromIsInCorrectFormat() {
        GetSubmissionsRequest request = new GetSubmissionsRequest()
                .withDateTimeFrom(OffsetDateTime.of(
                        2017, 11, 3, 16, 12, 45, 0,
                        ZoneOffset.ofHours(0)));

        RequestBuilder builder = new RequestBuilder();
        request.prepare(builder);
        assertThat(builder.build().getStringData()).contains("2017-11-03T16:12:45");
    }

    @Test
    public void prepareRequest_dateTimeToIsSet() {
        GetSubmissionsRequest request = new GetSubmissionsRequest()
                .withDateTimeTo(OffsetDateTime.of(
                        2017, 11, 3, 16, 12, 45, 0,
                        ZoneOffset.ofHours(0)));

        RequestBuilder builder = new RequestBuilder();
        request.prepare(builder);
        assertThat(builder.build().getStringData()).contains("DateTimeTo");
    }

    @Test
    public void prepareRequest_dateTimeToIsInCorrectFormat() {
        GetSubmissionsRequest request = new GetSubmissionsRequest()
                .withDateTimeTo(OffsetDateTime.of(
                        2017, 11, 3, 16, 12, 45, 0,
                        ZoneOffset.ofHours(0)));

        RequestBuilder builder = new RequestBuilder();
        request.prepare(builder);
        assertThat(builder.build().getStringData()).contains("2017-11-03T16:12:45");
    }


    @Test
    public void prepareRequest_limitIsSet() {
        GetSubmissionsRequest request = new GetSubmissionsRequest()
                .withLimit(100);

        RequestBuilder builder = new RequestBuilder();
        request.prepare(builder);
        assertThat(builder.build().getStringData()).contains("Limit");
    }

    @Test
    public void prepareRequest_offsetIsSet() {
        GetSubmissionsRequest request = new GetSubmissionsRequest()
                .withOffset(5);

        RequestBuilder builder = new RequestBuilder();
        request.prepare(builder);
        assertThat(builder.build().getStringData()).contains("Offset");
    }

    @Test
    public void prepareRequest_realLifeExample() {
        GetSubmissionsRequest request = new GetSubmissionsRequest()
                .withTurboPageIds(singletonList(1000005L))
                .withClientId(16948833L);

        RequestBuilder builder = new RequestBuilder();
        request.prepare(builder);
        assertThatCode(builder::build).doesNotThrowAnyException();
    }
}
