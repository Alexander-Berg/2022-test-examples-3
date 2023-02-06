package ru.yandex.market.tsum.pipelines.common.jobs.github.commit;

import java.util.Arrays;

import com.github.tomakehurst.wiremock.client.ScenarioMappingBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import ru.yandex.market.tsum.clients.github.model.Review;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * @author Alexander Kedrik <a href="mailto:alkedr@yandex-team.ru"></a>
 * @date 30.05.2019
 */
class WireMockRequests {
    private static final Gson GSON = new Gson();

    static final String FULL_JOB_ID = "some-job/id.with%weird{symbols)123";
    static final String JOB_LAUNCH_DETAILS_URL = "JOB_LAUNCH_DETAILS_URL";
    static final String REPOSITORY_ORGANIZATION = "org";
    static final String REPOSITORY_NAME = "repo";
    private static final String FULL_REPOSITORY_NAME = REPOSITORY_ORGANIZATION + '/' + REPOSITORY_NAME;
    private static final String HEAD_BRANCH_NAME = "some_job_id_with_weird_symbols_123";
    private static final String HEAD_BRANCH_SHA1 = "HEAD_BRANCH_SHA1";
    static final String BASE_BRANCH_NAME = "BASE_BRANCH_NAME";
    private static final String BASE_BRANCH_SHA1 = "BASE_BRANCH_SHA1";
    private static final int PULL_REQUEST_NUMBER = 123;
    static final String PULL_REQUEST_TITLE = "PULL_REQUEST_TITLE";
    static final String FILE_PATH = "FILE_PATH";
    static final String FILE_CONTENTS = "FILE_CONTENTS";
    private static final String REQUIRED_CHECK = "REQUIRED_CHECK";
    private static final String NON_REQUIRED_CHECK = "NON_REQUIRED_CHECK";
    private static final String SCENARIO_NAME = "SCENARIO_NAME";

    private WireMockRequests() {
    }

    static JsonElement openPullRequest() {
        return pullRequestJson("open", false);
    }

    static JsonElement mergedPullRequest() {
        return pullRequestJson("closed", true);
    }

    static JsonElement closedPullRequest() {
        return pullRequestJson("closed", false);
    }

    private static JsonElement pullRequestJson(String state, boolean merged) {
        return GSON.toJsonTree(ImmutableMap.of(
            "number", PULL_REQUEST_NUMBER,
            "state", state,
            "merged", merged,
            "base", ImmutableMap.of(
                "ref", BASE_BRANCH_NAME,
                "sha", BASE_BRANCH_SHA1
            ),
            "head", ImmutableMap.of(
                "ref", HEAD_BRANCH_NAME,
                "sha", HEAD_BRANCH_SHA1
            )
        ));
    }


    static JsonElement requiredCheck(String status) {
        return checkJson(REQUIRED_CHECK, status);
    }

    static JsonElement nonRequiredCheck(String status) {
        return checkJson(NON_REQUIRED_CHECK, status);
    }

    private static JsonElement checkJson(String context, String status) {
        return GSON.toJsonTree(ImmutableMap.of(
            "context", context,
            "state", status
        ));
    }


    static JsonElement approvedReview() {
        return reviewJson(Review.State.APPROVED);
    }

    static JsonElement changesRequestedReview() {
        return reviewJson(Review.State.CHANGES_REQUESTED);
    }

    private static JsonElement reviewJson(Review.State state) {
        return GSON.toJsonTree(ImmutableMap.of(
            "state", state
        ));
    }


    static ScenarioMappingBuilder getBaseRefToReturnBaseBranch() {
        return get("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/git/refs/heads/" + BASE_BRANCH_NAME)
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{\"object\": {\"sha\":\"" + BASE_BRANCH_SHA1 + "\"}}"));
    }

    static ScenarioMappingBuilder deleteHeadRefToReturn(int status) {
        return delete("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/git/refs/heads/" + HEAD_BRANCH_NAME)
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withStatus(status)
                .withHeader("Content-Type", "application/json")
                .withBody("{}")
            );
    }

    static ScenarioMappingBuilder createHeadRefToReturnOk() {
        return post("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/git/refs")
            .withRequestBody(equalToJson("{\"ref\":\"refs/heads/" + HEAD_BRANCH_NAME +
                "\",\"sha\":\"" + BASE_BRANCH_SHA1 + "\"}"))
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{}"));
    }

    static ScenarioMappingBuilder getHeadBranchToReturnOk() {
        return get("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/branches/refs/heads/" + HEAD_BRANCH_NAME)
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{}"));
    }

    static ScenarioMappingBuilder putFileToReturnOk() {
        return put("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/contents/" + FILE_PATH)
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{}"));
    }

    static ScenarioMappingBuilder getBaseBranchToReturnEmptyList() {
        return getBaseBranchToReturn();
    }

    static ScenarioMappingBuilder getBaseBranchToReturnRequiredChecks() {
        return getBaseBranchToReturn(REQUIRED_CHECK);
    }

    private static ScenarioMappingBuilder getBaseBranchToReturn(String... checks) {
        return get("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/branches/" + BASE_BRANCH_NAME)
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(GSON.toJson(ImmutableMap.of(
                    "protection", ImmutableMap.of(
                        "required_status_checks", ImmutableMap.of(
                            "contexts", checks
                        )
                    )
                ))));
    }

    static ScenarioMappingBuilder getMatchingPullRequestsToReturn(JsonElement... pullRequests) {
        return get(urlPathEqualTo("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/pulls"))
            .withQueryParam("head", equalTo(REPOSITORY_ORGANIZATION + ':' + HEAD_BRANCH_NAME))
            .withQueryParam("base", equalTo(BASE_BRANCH_NAME))
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(GSON.toJson(Arrays.asList(pullRequests)))
            );
    }

    static ScenarioMappingBuilder createPullRequestToReturnOk() {
        return post("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/pulls")
            .withRequestBody(equalToJson(GSON.toJson(ImmutableMap.of(
                "head", HEAD_BRANCH_NAME,
                "title", PULL_REQUEST_TITLE,
                "base", BASE_BRANCH_NAME,
                "body", "Этот пулл-реквест был создан и будет смёржен [вот этой джобой](" + JOB_LAUNCH_DETAILS_URL +
                    ")."
            ))))
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("{}"));
    }

    static ScenarioMappingBuilder putMergeToReturn(int statusCode) {
        return put("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/pulls/" + PULL_REQUEST_NUMBER + "/merge")
            .withRequestBody(equalToJson(GSON.toJson(ImmutableMap.of(
                "merge_method", "squash",
                "sha", HEAD_BRANCH_SHA1
            ))))
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withStatus(statusCode)
                .withHeader("Content-Type", "application/json")
                .withBody("{}"));
    }

    static ScenarioMappingBuilder getReviewsToReturn(JsonElement... reviews) {
        return get(urlPathEqualTo("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/pulls/" + PULL_REQUEST_NUMBER +
            "/reviews"))
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(GSON.toJson(reviews))
            );
    }

    static ScenarioMappingBuilder getHeadBranchStatusesToReturn(JsonElement... checks) {
        return get(urlPathEqualTo("/api/v3/repos/" + FULL_REPOSITORY_NAME + "/statuses/" + HEAD_BRANCH_SHA1))
            .inScenario(SCENARIO_NAME)
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody(GSON.toJson(checks))
            );
    }
}
