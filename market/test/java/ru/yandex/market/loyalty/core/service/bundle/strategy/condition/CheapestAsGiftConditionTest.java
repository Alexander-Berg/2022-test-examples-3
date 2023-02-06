package ru.yandex.market.loyalty.core.service.bundle.strategy.condition;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.spy.memcached.CachedData;
import net.spy.memcached.transcoders.SerializingTranscoder;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class CheapestAsGiftConditionTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final SerializingTranscoder serializingTranscoder = new SerializingTranscoder();

    @Test
    public void shouldSerializeConditionForCache() {
        CachedData cachedData = serializingTranscoder.encode(CheapestAsGiftCondition.builder()
                .withFeedSskuSets(123L, List.of("test 1", "test 2"))
                .build());

        assertThat(cachedData.getData().length, greaterThan(0));
    }

    @Test
    public void shouldDeserializeConditionForCache() {
        CachedData cachedData = serializingTranscoder.encode(CheapestAsGiftCondition.builder()
                .withFeedSskuSets(123L, List.of("test 1", "test 2"))
                .build());

        CheapestAsGiftCondition condition = (CheapestAsGiftCondition) serializingTranscoder.decode(cachedData);

        assertThat(condition.getId(), notNullValue());
        assertThat(condition.getFeedSskuSets(), hasSize(1));
        assertThat(condition.getFeedSskuSets().get(0).getFeedId(), comparesEqualTo(123L));
        assertThat(condition.getFeedSskuSets().get(0).getSskuSet(), hasItems("test 1", "test 2"));
    }

    @Test
    public void shouldParseJsonForBackwardCompatibility() throws IOException {
        CheapestAsGiftCondition condition = mapper.readValue("{" +
                "\"feedId\":123, " +
                "\"shopSkus\":[\"test 1\",\"test 2\"]" +
                "}", CheapestAsGiftCondition.class);

        assertThat(condition.getId(), notNullValue());
        assertThat(condition.getFeedSskuSets(), hasSize(1));
        assertThat(condition.getFeedSskuSets().get(0).getFeedId(), comparesEqualTo(123L));
        assertThat(condition.getFeedSskuSets().get(0).getSskuSet(), hasItems("test 1", "test 2"));
    }

    @Test
    public void shouldParseJsonToCondition() throws IOException {
        CheapestAsGiftCondition condition = mapper.readValue("{" +
                "\"feedSskus\":[{\"feedId\":123, \"sskus\":[\"test 1\",\"test 2\"]}]" +
                "}", CheapestAsGiftCondition.class);

        assertThat(condition.getId(), notNullValue());
        assertThat(condition.getFeedSskuSets(), hasSize(1));
        assertThat(condition.getFeedSskuSets().get(0).getFeedId(), comparesEqualTo(123L));
        assertThat(condition.getFeedSskuSets().get(0).getSskuSet(), hasItems("test 1", "test 2"));
    }

    @Test
    public void shouldWriteJsonVersionFields() throws IOException {
        String conditionStr = mapper.writeValueAsString(CheapestAsGiftCondition.builder()
                .withFeedSskuSets(123L, List.of("test 1", "test 2"))
                .build());
        assertThat(conditionStr, containsString("\"feedSskus\":"));
        assertThat(conditionStr, containsString("\"sskus\":"));
    }
}
