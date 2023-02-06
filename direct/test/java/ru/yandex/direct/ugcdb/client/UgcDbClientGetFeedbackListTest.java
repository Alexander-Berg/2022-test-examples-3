package ru.yandex.direct.ugcdb.client;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;

import ru.yandex.kernel.ugc.protos.direct.TDirectReview;
import ru.yandex.kernel.ugc.protos.direct.TDirectService;

public class UgcDbClientGetFeedbackListTest extends UgcDbClientTestBase {
    private Long freelancerId = 5L;
    private String feedbackId = "asdf";
    private TDirectReview tDirectReview =
            TDirectReview.newBuilder()
                    .setReviewId(feedbackId)
                    .setContractorId(freelancerId.toString()).build();

    private TDirectService tDirectService = TDirectService.newBuilder().addReviews(tDirectReview).build();

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getMethod()).isEqualTo("GET");
                softAssertions.assertThat(request.getPath()).isEqualTo(
                        "/" + Api.VERSION + "/" + Api.MAIN_TABLE + "/" + freelancerId + "?yql_filter=query");
                try {
                    return new MockResponse().setBody(JsonFormat.printer().print(tDirectService));
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    public void getReviews() {
        TDirectService actual = ugcDbClient.api.getFeedbackList(freelancerId, "query").execute().getSuccess();
        softAssertions.assertThat(actual.getReviewsList()).hasSize(1);
        TDirectReview actualReview = actual.getReviewsList().get(0);
        softAssertions.assertThat(actualReview.getReviewId()).isEqualTo(tDirectReview.getReviewId());
        softAssertions.assertThat(actualReview.getContractorId()).isEqualTo(tDirectReview.getContractorId());
    }
}
