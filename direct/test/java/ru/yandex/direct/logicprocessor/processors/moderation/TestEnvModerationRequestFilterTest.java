package ru.yandex.direct.logicprocessor.processors.moderation;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import ru.yandex.direct.core.entity.moderation.model.ModerationWorkflow;
import ru.yandex.direct.core.entity.moderation.model.text.TextBannerModerationRequest;
import ru.yandex.direct.logicprocessor.processors.moderation.special.flags.ModerationFlagsRequest;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class TestEnvModerationRequestFilterTest {

    private static final Long ACCEPTABLE_CAMPAIGN_ID = 123L;
    private static final Long UNACCEPTABLE_CAMPAIGN_ID = 345L;
    private static final ModerationWorkflow ACCEPTABLE_WORKFLOW = ModerationWorkflow.COMMON;
    private static final ModerationWorkflow UNACCEPTABLE_WORKFLOW = ModerationWorkflow.MANUAL;

    @Test
    public void emptyFilterAcceptsAllRequests() {
        TestEnvModerationRequestFilter filter = new TestEnvModerationRequestFilter(emptyMap());
        List<Object> requests = List.of(getUnacceptableTextBannerRequest(), getUnacceptableModerationFlagsRequest());
        List<Object> result = filter.filter(requests);
        assertThat(result).hasSize(2);
    }

    @Test
    public void acceptRequestsOfOneType() {
        TestEnvModerationRequestFilter filter = new TestEnvModerationRequestFilter(
                Map.of(TextBannerModerationRequest.class, getTextBannerPredicate()));

        TextBannerModerationRequest acceptableRequest1 = getAcceptableTextBannerRequest();
        TextBannerModerationRequest acceptableRequest2 = getAcceptableTextBannerRequest();
        List<Object> requests = List.of(acceptableRequest1, acceptableRequest2);

        List<Object> result = filter.filter(requests);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isSameAs(acceptableRequest1);
        assertThat(result.get(1)).isSameAs(acceptableRequest2);
    }

    @Test
    public void declineRequestsOfOneType() {
        TestEnvModerationRequestFilter filter = new TestEnvModerationRequestFilter(
                Map.of(TextBannerModerationRequest.class, getTextBannerPredicate()));

        TextBannerModerationRequest unacceptableRequest1 = getUnacceptableTextBannerRequest();
        TextBannerModerationRequest unacceptableRequest2 = getUnacceptableTextBannerRequest();
        List<Object> requests = List.of(unacceptableRequest1, unacceptableRequest2);

        List<Object> result = filter.filter(requests);
        assertThat(result).isEmpty();
    }

    @Test
    public void acceptAndDeclineRequestsOfOneType() {
        TestEnvModerationRequestFilter filter = new TestEnvModerationRequestFilter(
                Map.of(TextBannerModerationRequest.class, getTextBannerPredicate()));

        TextBannerModerationRequest acceptableRequest = getAcceptableTextBannerRequest();
        List<Object> requests = List.of(acceptableRequest, getUnacceptableTextBannerRequest());

        List<Object> result = filter.filter(requests);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(acceptableRequest);
    }

    @Test
    public void acceptRequestsOfSeveralTypes() {
        TestEnvModerationRequestFilter filter = new TestEnvModerationRequestFilter(
                Map.of(
                        TextBannerModerationRequest.class, getTextBannerPredicate(),
                        ModerationFlagsRequest.class, getModerationFlagsPredicate()));

        TextBannerModerationRequest acceptableTextRequest = getAcceptableTextBannerRequest();
        ModerationFlagsRequest acceptableFlagsRequest = getAcceptableModerationFlagsRequest();
        List<Object> requests = List.of(acceptableTextRequest, acceptableFlagsRequest);

        List<Object> result = filter.filter(requests);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isSameAs(acceptableTextRequest);
        assertThat(result.get(1)).isSameAs(acceptableFlagsRequest);
    }

    @Test
    public void acceptAndDeclineRequestsOfSeveralTypes() {
        TestEnvModerationRequestFilter filter = new TestEnvModerationRequestFilter(
                Map.of(
                        TextBannerModerationRequest.class, getTextBannerPredicate(),
                        ModerationFlagsRequest.class, getModerationFlagsPredicate()));

        TextBannerModerationRequest acceptableTextRequest = getAcceptableTextBannerRequest();
        ModerationFlagsRequest acceptableFlagsRequest = getAcceptableModerationFlagsRequest();
        List<Object> requests = List.of(acceptableTextRequest, getUnacceptableTextBannerRequest(),
                getUnacceptableModerationFlagsRequest(), acceptableFlagsRequest);

        List<Object> result = filter.filter(requests);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isSameAs(acceptableTextRequest);
        assertThat(result.get(1)).isSameAs(acceptableFlagsRequest);
    }

    @Test
    public void acceptRequestsByDefaultWhenPredicateIsNotDefined() {
        TestEnvModerationRequestFilter filter = new TestEnvModerationRequestFilter(
                Map.of(TextBannerModerationRequest.class, getTextBannerPredicate()));

        TextBannerModerationRequest acceptableTextRequest = getAcceptableTextBannerRequest();
        ModerationFlagsRequest unacceptableFlagsRequest = getUnacceptableModerationFlagsRequest();
        List<Object> requests = List.of(acceptableTextRequest, unacceptableFlagsRequest,
                getUnacceptableTextBannerRequest());

        List<Object> result = filter.filter(requests);
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isSameAs(acceptableTextRequest);
        assertThat(result.get(1)).isSameAs(unacceptableFlagsRequest);
    }

    private Predicate<TextBannerModerationRequest> getTextBannerPredicate() {
        return req -> req.getWorkflow().equals(ACCEPTABLE_WORKFLOW);
    }

    private Predicate<ModerationFlagsRequest> getModerationFlagsPredicate() {
        return req -> req.getCampaignId() == ACCEPTABLE_CAMPAIGN_ID;
    }

    private TextBannerModerationRequest getAcceptableTextBannerRequest() {
        TextBannerModerationRequest request = new TextBannerModerationRequest();
        request.setWorkflow(ACCEPTABLE_WORKFLOW);
        return request;
    }

    private TextBannerModerationRequest getUnacceptableTextBannerRequest() {
        TextBannerModerationRequest request = new TextBannerModerationRequest();
        request.setWorkflow(UNACCEPTABLE_WORKFLOW);
        return request;
    }

    private ModerationFlagsRequest getAcceptableModerationFlagsRequest() {
        ModerationFlagsRequest request = new ModerationFlagsRequest();
        request.setCampaignId(ACCEPTABLE_CAMPAIGN_ID);
        return request;
    }

    private ModerationFlagsRequest getUnacceptableModerationFlagsRequest() {
        ModerationFlagsRequest request = new ModerationFlagsRequest();
        request.setCampaignId(UNACCEPTABLE_CAMPAIGN_ID);
        return request;
    }
}
