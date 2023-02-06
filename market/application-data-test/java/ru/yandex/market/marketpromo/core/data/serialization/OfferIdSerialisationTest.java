package ru.yandex.market.marketpromo.core.data.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.application.context.CategoryInterfacePromo;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.model.OfferId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class OfferIdSerialisationTest extends ServiceTestBase {

    @Autowired
    @CategoryInterfacePromo
    private ObjectMapper objectMapper;

    @Test
    void shouldSerializeOfferId() throws JsonProcessingException {
        OfferId offerId = OfferId.of("test", 12L);

        assertThat(objectMapper.writeValueAsString(offerId), is("\"test.12\""));
    }

    @Test
    void shouldDeserializeOfferId() throws IOException {
        OfferId offerId = OfferId.of("test", 12L);

        assertThat(objectMapper.readValue("\"test.12\"", OfferId.class), is(offerId));
    }

    @Test()
    void shouldFailOnWrongDeserializeOfferId() throws IOException {
        Assertions.assertThrows(JsonParseException.class, () -> {
            objectMapper.readValue("test", OfferId.class);
        });
    }

}
