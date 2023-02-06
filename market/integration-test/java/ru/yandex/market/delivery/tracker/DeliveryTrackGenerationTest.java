package ru.yandex.market.delivery.tracker;

import java.time.Clock;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.tracker.domain.dto.GenerateDeliveryTracksRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Import(DeliveryTrackGenerationTest.ClockConfiguration.class)
class DeliveryTrackGenerationTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DatabaseSetup("/database/states/track_generation/before_tracks_generation.xml")
    @ExpectedDatabase(
        value = "/database/expected/track_generation/after_tracks_generation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void generateTracks() throws Exception {

        GenerateDeliveryTracksRequest request = GenerateDeliveryTracksRequest.builder()
            .deliveryServiceId(1)
            .baseOrderId(1)
            .historyDelay(0)
            .statusDelay(0)
            .nextRequestDate(LocalDateTime.parse("2021-01-01T01:01:01"))
            .numberOfTracks(5)
            .tracksPerTransaction(2)
            .trackCodeType("A")
            .consumerId(1)
            .build();

        mockMvc.perform(
            post("/track/perf-test/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
    }

    @TestConfiguration
    public static class ClockConfiguration {
        @Bean
        public Clock clock() {
            return Clock.systemDefaultZone();
        }
    }
}
