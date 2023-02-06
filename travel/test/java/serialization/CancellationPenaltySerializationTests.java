package serialization;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.expedia.DefaultExpediaClient;
import ru.yandex.travel.hotels.common.CancellationPenalty;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CancellationPenaltySerializationTests {
    private ObjectMapper mapper = DefaultExpediaClient.createObjectMapper();

    @Test
    public void testSerializeNoCents() throws JsonProcessingException {
        CancellationPenalty penalty = CancellationPenalty.somePenalty(null, null, BigDecimal.valueOf(42), null);
        String res = mapper.writer().writeValueAsString(penalty);
        assertThat(res).contains("\"amount\":\"42.00\"");
    }

    @Test
    public void testSerializeSomeCents() throws JsonProcessingException {
        CancellationPenalty penalty = CancellationPenalty.somePenalty(null, null, BigDecimal.valueOf(42.56), null);
        String res = mapper.writer().writeValueAsString(penalty);
        assertThat(res).contains("\"amount\":\"42.56\"");
    }

    @Test
    public void testSerializeLongCentsRounding() throws JsonProcessingException {
        CancellationPenalty penalty = CancellationPenalty.somePenalty(null, null, BigDecimal.valueOf(42.5699), null);
        String res = mapper.writer().writeValueAsString(penalty);
        assertThat(res).contains("\"amount\":\"42.57\"");
    }


    @Test
    public void testDeserilizeCents() throws IOException {
        String data = "{\"starts_at\":null,\"ends_at\":null,\"type\":null,\"amount\":\"42.57\",\"currency\":null}";
        CancellationPenalty penalty = mapper.readerFor(CancellationPenalty.class).readValue(data);
        assertThat(penalty.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(42.57));
    }

    @Test
    public void testDeserilizeNoCents() throws IOException {
        String data = "{\"starts_at\":null,\"ends_at\":null,\"type\":null,\"amount\":\"42.00\",\"currency\":null}";
        CancellationPenalty penalty = mapper.readerFor(CancellationPenalty.class).readValue(data);
        assertThat(penalty.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(42));
    }
}
