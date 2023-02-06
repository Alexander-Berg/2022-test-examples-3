package ru.yandex.direct.ugcdb.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;

import ru.yandex.kernel.ugc.protos.direct.TDirectReview;

public class UgcDbClientGetFeedbackTest extends UgcDbClientTestBase {
    private Long freelancerId = 5L;
    private String feedbackId = "asdf";
    private TDirectReview tDirectReview =
            TDirectReview.newBuilder()
                    .setReviewId(feedbackId)
                    .setContractorId(freelancerId.toString()).build();

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("GET");
                softAssertions.assertThat(request.getPath()).isEqualTo(
                        "/" + Api.VERSION + "/" + Api.MAIN_TABLE + "/" + freelancerId + "/" + Api.REVIEWS_TABLE
                                + "/" + feedbackId);
                try {
                    return new MockResponse().setBody(JsonFormat.printer().print(tDirectReview));
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    public void getReviews() {
        TDirectReview actual = ugcDbClient.api.getFeedback(freelancerId, feedbackId).execute().getSuccess();
        softAssertions.assertThat(actual.getReviewId()).isEqualTo(tDirectReview.getReviewId());
        softAssertions.assertThat(actual.getContractorId()).isEqualTo(tDirectReview.getContractorId());
    }
}
