package ru.yandex.market.tsum.analytics;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @author Nikolay Firov <a href="mailto:firov@yandex-team.ru"></a>
 * @date 18.01.18
 */
public class DurationHelperTest {
    @Test
    public void calculatesSecondsToActionFromRequest() {
        List<Instant> requests = Arrays.asList(
            Instant.ofEpochSecond(1),
            Instant.ofEpochSecond(2),
            Instant.ofEpochSecond(7),
            Instant.ofEpochSecond(8)
        );

        List<Instant> actions = Arrays.asList(
            Instant.ofEpochSecond(3),
            Instant.ofEpochSecond(5),
            Instant.ofEpochSecond(17)
        );

        List<Long> result = DurationHelper.getSecondsToActionFromRequest(actions, requests);

        assertThat(result, org.hamcrest.Matchers.equalTo(Arrays.asList(2L, 10L)));
    }
}
