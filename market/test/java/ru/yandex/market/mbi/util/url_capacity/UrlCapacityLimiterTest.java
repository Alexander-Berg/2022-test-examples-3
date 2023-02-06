package ru.yandex.market.mbi.util.url_capacity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class UrlCapacityLimiterTest {
    private UrlCapacityLimiter urlCapacityCounter;


    @Test
    public void testSimpleAdd() {
        urlCapacityCounter = new UrlCapacityLimiter( 4, () -> 1, () -> 50);

        String action = "/campaigns/get";
        String action2 = "/partners/get";

        assertThat(urlCapacityCounter.tryProcessOneMoreRequest(action)).isTrue();
        assertThat(urlCapacityCounter.tryProcessOneMoreRequest(action)).isTrue();
        assertThat(urlCapacityCounter.tryProcessOneMoreRequest(action2)).isTrue();
        assertThat(urlCapacityCounter.tryProcessOneMoreRequest(action)).isFalse();
    }

    @Test
    public void testAddAndRemove() {
        urlCapacityCounter = new UrlCapacityLimiter( 4, () -> 1, () -> 50);

        String action = "/campaigns/get";

        assertThat(urlCapacityCounter.tryProcessOneMoreRequest(action)).isTrue();
        assertThat(urlCapacityCounter.tryProcessOneMoreRequest(action)).isTrue();
        urlCapacityCounter.requestProcessed(action);

        assertThat(urlCapacityCounter.tryProcessOneMoreRequest(action)).isTrue();
        assertThat(urlCapacityCounter.tryProcessOneMoreRequest(action)).isFalse();

        urlCapacityCounter.requestProcessed(action);
        assertThat(urlCapacityCounter.tryProcessOneMoreRequest(action)).isTrue();
    }

}
