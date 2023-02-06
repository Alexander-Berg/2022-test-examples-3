package ru.yandex.travel.api.models.hotels;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.Test;

import ru.yandex.travel.hotels.offercache.api.EBadgeTheme;

import static org.assertj.core.api.Assertions.assertThat;

public class BadgeTest {
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SnakeCaseStrategy.SNAKE_CASE);

    @Test
    public void testSerialization() throws JsonProcessingException {
        Badge badge = Badge.builder()
                .id("some_id")
                .text("some_text")
                .theme(Badge.BadgeTheme.BLACK)
                .build();

        String json = mapper.writeValueAsString(badge);
        assertThat(json).contains("\"id\":\"some_id\"");
        assertThat(json).contains("\"text\":\"some_text\"");
        assertThat(json).contains("\"theme\":\"BLACK\"");
    }

    @Test
    public void testBadgeThemeMapping() {
        assertThat(Badge.BadgeTheme.map(null)).isNull();
        assertThat(Badge.BadgeTheme.map(EBadgeTheme.BT_UNUSED)).isNull();
        assertThat(Badge.BadgeTheme.map(EBadgeTheme.BT_GREEN)).isEqualTo(Badge.BadgeTheme.GREEN);
    }
}
