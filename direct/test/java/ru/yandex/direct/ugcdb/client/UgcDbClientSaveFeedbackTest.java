package ru.yandex.direct.ugcdb.client;

import java.nio.charset.Charset;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;

import ru.yandex.kernel.ugc.protos.direct.TDirectReview;

public class UgcDbClientSaveFeedbackTest extends UgcDbClientTestBase {
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
                softAssertions.assertThat(request.getMethod()).isEqualTo("PUT");
                softAssertions.assertThat(request.getPath()).isEqualTo(
                        "/" + Api.VERSION + "/" + Api.MAIN_TABLE + "/" + freelancerId + "/" + Api.REVIEWS_TABLE
                                + "/" + feedbackId);
                try {
                    softAssertions.assertThat(request.getBody().readString(Charset.defaultCharset()))
                            .isEqualTo(JsonFormat.printer().omittingInsignificantWhitespace().print(tDirectReview));
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
                return new MockResponse();
            }
        };
    }

    @Test
    public void getReviews() {
        Void success = ugcDbClient.api
                .saveFeedback(freelancerId, feedbackId, new UgcDbRequest(tDirectReview)).execute().getSuccess();
        softAssertions.assertThat(success).isNull();
    }
}
