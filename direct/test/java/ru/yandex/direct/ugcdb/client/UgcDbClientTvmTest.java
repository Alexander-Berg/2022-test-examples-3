package ru.yandex.direct.ugcdb.client;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;

import ru.yandex.inside.passport.tvm2.TvmHeaders;

public class UgcDbClientTvmTest extends UgcDbClientTestBase {
    private Long freelancerId = 5L;
    private String feedbackId = "asdf";

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String ticketHeader = request.getHeader(TvmHeaders.SERVICE_TICKET);
                softAssertions.assertThat(ticketHeader).isEqualTo(TICKET_BODY);
                return new MockResponse();
            }
        };
    }

    @Test
    public void getReviews() {
        ugcDbClient.api.getFeedback(freelancerId, feedbackId).execute();
    }
}
