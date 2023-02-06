package ru.yandex.market.marketpromo.core.data.serialization;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.application.context.CategoryInterfacePromo;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.model.FeedOfferKey;
import ru.yandex.market.marketpromo.model.OfferId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FeedOfferKeySerializationTest extends ServiceTestBase {

    @Autowired
    @CategoryInterfacePromo
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeOfferKey() throws IOException {
        FeedOfferKey expected = FeedOfferKey.of(FeedOfferKey.EMPTY_FEED_ID, OfferId.of("test", 12L));

        String data = objectMapper.writeValueAsString(expected);

        assertThat(objectMapper.readValue(data, FeedOfferKey.class), is(expected));
    }
}
