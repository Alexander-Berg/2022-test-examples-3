package ru.yandex.direct.audience.client;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.audience.client.model.AudienceSegment;
import ru.yandex.direct.audience.client.model.CreateExperimentRequest;
import ru.yandex.direct.audience.client.model.CreateExperimentResponse;
import ru.yandex.direct.audience.client.model.CreateExperimentResponseEnvelope;
import ru.yandex.direct.audience.client.model.ExperimentSegmentRequest;
import ru.yandex.direct.audience.client.model.ExperimentSegmentResponse;
import ru.yandex.direct.audience.client.model.SegmentContentType;
import ru.yandex.direct.audience.client.model.SegmentStatus;
import ru.yandex.direct.audience.client.model.SetExperimentGrantRequest;
import ru.yandex.direct.audience.client.model.SetExperimentGrantResponse;
import ru.yandex.direct.audience.client.model.SetExperimentGrantResponseEnvelope;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class YaAudienceClientMockedTest {

    @Rule
    public final MockedYaAudience mockedYaAudience = new MockedYaAudience();

    private YaAudienceClient client;

    private static final long EXPERIMENT_ID = 945L;
    private static final String EXPERIMENT_PERMISSION = "view";
    private static final String BRAND_LIFT_LOGIN = "brand-lift-login";
    private static final long COUNTER_ID = 56089156L;
    private static final long SEGMENT_A = 3031L;
    private static final long SEGMENT_B = 3032L;
    private static final String DIRECT_LOGIN = "direct-client";

    @Before
    public void setup() {
        client = mockedYaAudience.createClient();
    }

    @Test
    public void testCreateExperiment() {
        ExperimentSegmentRequest segmentA = new ExperimentSegmentRequest()
                .withName("A")
                .withStart(0)
                .withEnd(90);

        ExperimentSegmentRequest segmentB = new ExperimentSegmentRequest()
                .withName("B")
                .withStart(90)
                .withEnd(100);

        CreateExperimentRequest createExperimentRequest = new CreateExperimentRequest()
                .withExperimentName("Brand-lift 123337516")
                .withExperimentSegmentRequests(List.of(segmentA, segmentB))
                .withCounterIds(List.of(COUNTER_ID));

        CreateExperimentResponseEnvelope actualResponse =
                client.createExperiment(DIRECT_LOGIN, createExperimentRequest);

        CreateExperimentResponseEnvelope expectedResponse = new CreateExperimentResponseEnvelope()
                .withCreateExperimentResponse(new CreateExperimentResponse()
                        .withExperimentId(EXPERIMENT_ID)
                        .withExperimentSegments(List.of(
                                new ExperimentSegmentResponse().withSegmentId(SEGMENT_A),
                                new ExperimentSegmentResponse().withSegmentId(SEGMENT_B))
                        )
                );

        assertThat(actualResponse, beanDiffer(expectedResponse));
    }

    @Test
    public void testSetExperimentGrant() {
        SetExperimentGrantRequest setExperimentGrantRequest = new SetExperimentGrantRequest()
                .withUserLogin(BRAND_LIFT_LOGIN)
                .withPermission(EXPERIMENT_PERMISSION);

        SetExperimentGrantResponseEnvelope actualResponse = client.setExperimentGrant(EXPERIMENT_ID,
                setExperimentGrantRequest);

        SetExperimentGrantResponseEnvelope expectedResponse = new SetExperimentGrantResponseEnvelope()
                .withSetExperimentGrantResponse(new SetExperimentGrantResponse()
                        .withPermission(EXPERIMENT_PERMISSION)
                );

        assertThat(actualResponse, beanDiffer(expectedResponse));
    }

    @Test
    public void testConfirmSegment() {
        AudienceSegment actualResponse = client.confirmSegment(
                DIRECT_LOGIN, SEGMENT_A, "A", SegmentContentType.YUID);
        AudienceSegment expectedResponse = new AudienceSegment()
                .withId(SEGMENT_A)
                .withStatus(SegmentStatus.UPLOADED)
                .withName("A");

        assertThat(actualResponse, beanDiffer(expectedResponse));
    }
}
