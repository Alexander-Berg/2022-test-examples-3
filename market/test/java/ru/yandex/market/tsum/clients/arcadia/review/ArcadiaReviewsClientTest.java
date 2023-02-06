package ru.yandex.market.tsum.clients.arcadia.review;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.io.Resources;
import com.google.gson.JsonArray;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.market.tsum.clients.arcadia.review.model.activity.Activity;
import ru.yandex.market.tsum.clients.arcadia.review.model.activity.ChangeDescription;
import ru.yandex.market.tsum.clients.arcadia.review.model.activity.ChangedValue;
import ru.yandex.market.tsum.clients.arcadia.review.model.activity.Comment;
import ru.yandex.market.tsum.clients.arcadia.review.model.activity.ReviewActivity;
import ru.yandex.market.tsum.clients.arcadia.review.model.activity.ShipItActivity;
import ru.yandex.market.tsum.clients.arcadia.review.model.activity.User;

public class ArcadiaReviewsClientTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WireMockConfiguration.wireMockConfig().dynamicPort());
    ArcadiaReviewsClient arcadiaReviewsClient;

    @Before
    public void setup() {
        arcadiaReviewsClient = new ArcadiaReviewsClient("http://localhost:" + wireMockRule.port(), "");
    }

    @Test
    public void processSomeBigActivitiesListShouldNotThrowsException() throws Exception {
        getActivity("clients/arcadia_reviews/some_big_activities_list.json", 1663036);
    }

    @Test
    public void commentDeserializationShouldWorksCorrectly() throws Exception {
        checkActivity(
            new ReviewActivity(
                Collections.singletonList(
                    new Activity(
                        "commented",
                        Instant.parse("2021-03-01T14:39:47.563201Z"),
                        Arrays.asList(
                            new Comment(
                                new User("trishlex"),
                                null,
                                Instant.parse("2021-03-01T14:41:11.017200Z")
                            ),
                            new Comment(
                                new User("disproper"),
                                "resolved",
                                Instant.parse("2021-03-01T14:39:47.563201Z")
                            )
                        ),
                        null,
                        null
                    )
                )
            ),
            "clients/arcadia_reviews/comment.json"
        );
    }

    @Test
    public void changeDescriptionDeserializationShouldWorksCorrectly() throws Exception {
        JsonArray assignees = new JsonArray();
        assignees.add("mishunin");
        checkActivity(
            new ReviewActivity(
                Collections.singletonList(
                    new Activity(
                        "description_changed",
                        Instant.parse("2021-03-02T08:42:46.187054Z"),
                        null,
                        new ChangeDescription(
                            Instant.parse("2021-03-02T08:42:46.187054Z"),
                            Arrays.asList(
                                new ChangedValue(
                                    "status",
                                    "submitted"
                                ),
                                new ChangedValue(
                                    "assignees",
                                    assignees
                                )
                            )
                        ),
                        null
                    )
                )
            ),
            "clients/arcadia_reviews/change_description.json"
        );
    }

    @Test
    public void approvedDeserializationShouldWorksCorrectly() throws Exception {
        checkActivity(
            new ReviewActivity(
                Collections.singletonList(
                    new Activity(
                        "approved",
                        Instant.parse("2021-03-02T08:41:31.005426Z"),
                        null,
                        null,
                        new ShipItActivity("disproper")
                    )
                )
            ),
            "clients/arcadia_reviews/approved.json"
        );
    }

    private void checkActivity(ReviewActivity expectedReviewActivity, String resourceName) throws IOException {
        ReviewActivity actualReviewActivity = getActivity(resourceName, 0);

        Assertions.assertThat(actualReviewActivity)
            .usingRecursiveComparison()
            .isEqualTo(expectedReviewActivity);
    }

    private ReviewActivity getActivity(String resourceName, int reviewId) throws IOException {
        ResponseDefinitionBuilder responseDefBuilder = WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(loadResource(resourceName));
        wireMockRule.stubFor(WireMock.get(String.format("/api/review/review-request/%d/activity", reviewId))
            .willReturn(responseDefBuilder));
        return arcadiaReviewsClient.getActivity(reviewId);
    }

    private String loadResource(String resourceName) throws IOException {
        return Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8);
    }

}
