package ru.yandex.direct.ugcdb.client;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;

public class UgcDbClientDeleteFeedbackTest extends UgcDbClientTestBase {
    private Long freelancerId = 5L;
    private String feedbackId = "asdf";

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("DELETE");
                softAssertions.assertThat(request.getPath()).isEqualTo(
                        "/" + Api.VERSION + "/" + Api.MAIN_TABLE + "/" + freelancerId + "/" + Api.REVIEWS_TABLE
                                + "/" + feedbackId);
                return new MockResponse();
            }
        };
    }

    @Test
    public void getReviews() {
        Void success = ugcDbClient.api.deleteFeedback(freelancerId, feedbackId).execute().getSuccess();
        softAssertions.assertThat(success).isNull();
    }
}
