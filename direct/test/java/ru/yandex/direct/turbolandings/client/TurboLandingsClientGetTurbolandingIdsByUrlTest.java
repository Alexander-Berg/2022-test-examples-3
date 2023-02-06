package ru.yandex.direct.turbolandings.client;

import java.util.Collections;
import java.util.List;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Test;

import ru.yandex.direct.turbolandings.client.model.GetIdByUrlResponseItem;
import ru.yandex.direct.utils.JsonUtils;

import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class TurboLandingsClientGetTurbolandingIdsByUrlTest extends TurboLandingsClientTestBase {

    private static final Long TEST_CLIENT_ID = 56218217L;
    private static final Long TEST_TL_ID = 12345678L;
    private static final String TEST_TL_URL = "https://yandex.ru/turbo?text=c4nv45-e4789b05-f867-45b4-8038-966bdf7ba683";

    private GetIdByUrlResponseItem expected = new GetIdByUrlResponseItem()
            .withClientId(TEST_CLIENT_ID)
            .withLandingId(TEST_TL_ID)
            .withUrl(TEST_TL_URL);

    @Test
    public void testSuccessfulRequest() {
        List<GetIdByUrlResponseItem> result = turboLandingsClient.getTurbolandingIdsByUrl(Collections.singletonList(TEST_TL_URL));
        softAssertions.assertThat(result)
                .as("Result contains only one item").hasSize(1);
        softAssertions.assertThat(result.stream().findFirst().orElse(null))
                .as("Given expected item").is(matchedBy(beanDiffer(expected)));
    }

    @Override
    protected Dispatcher dispatcher() {
        return new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                softAssertions.assertThat(request.getPath())
                        .as("Expected path").isEqualTo("/api/direct/getIdByUrl");
                return new MockResponse().setBody(JsonUtils
                        .toJson(Collections.singletonList(expected)));
            }
        };
    }
}
