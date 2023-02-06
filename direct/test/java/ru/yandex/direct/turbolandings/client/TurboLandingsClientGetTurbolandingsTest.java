package ru.yandex.direct.turbolandings.client;

import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.turbolandings.client.model.DcTurboLanding;
import ru.yandex.direct.turbolandings.client.model.GetTurboLandingsResponse;
import ru.yandex.direct.turbolandings.client.model.TurboLandingCounter;
import ru.yandex.direct.utils.JsonUtils;

import static com.google.common.primitives.Longs.asList;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class TurboLandingsClientGetTurbolandingsTest extends TurboLandingsClientTestBase {

    private static final Long TEST_CLIENT_ID = 56218217L;
    private DcTurboLanding expected = new DcTurboLanding()
            .withId(46353858L)
            .withName("turboname")
            .withUrl("https://yandex.ru/turbo?text=c4nv45-e4789b05-f867-45b4-8038-966bdf7ba683")
            .withCounters(StreamEx.of(1L, 2L, 3L)
                    .map(id -> new TurboLandingCounter()
                            .withId(id)
                            .withGoals(asList(4L, 5L))
                            .withIsUserCounter(true)
                    )
                    .toList()
            );

    @Test
    public void testSuccessfulRequest() {
        List<DcTurboLanding> result = turboLandingsClient.getTurboLandings(TEST_CLIENT_ID, asList());
        softAssertions.assertThat(result).hasSize(1);
        softAssertions.assertThat(result.stream().findFirst().orElse(null)).is(matchedBy(beanDiffer(expected)));
    }

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getPath())
                        .isEqualTo("/api/direct/landings?client_id=" + TEST_CLIENT_ID + "&limit=150&offset=0");
                return new MockResponse().setBody(JsonUtils
                        .toJson(new GetTurboLandingsResponse().withItems(Collections.singletonList(expected))));
            }
        };
    }
}
