package ru.yandex.reminders.util.jsonld;

import lombok.val;
import org.junit.Test;
import ru.yandex.bolts.collection.Cf;
import ru.yandex.commune.json.JsonObject;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaOrgEventDataExtractorTest {
    @Test
    public void simple() {
        val json = JsonObject.parseObject("{\n" +
                "  \"@context\": \"http://schema.org\",\n" +
                "  \"@type\": \"Event\",\n" +
                "  \"location\": {\n" +
                "    \"@type\": \"Place\",\n" +
                "    \"address\": {\n" +
                "      \"@type\": \"PostalAddress\",\n" +
                "      \"addressLocality\": \"Philadelphia\",\n" +
                "      \"addressRegion\": \"PA\"\n" +
                "    },\n" +
                "    \"url\": \"wells-fargo-center.html\"\n" +
                "  },\n" +
                "  \"offers\": {\n" +
                "    \"@type\": \"AggregateOffer\",\n" +
                "    \"lowPrice\": \"$35\",\n" +
                "    \"offerCount\": \"1938\"\n" +
                "  },\n" +
                "  \"startDate\": \"2016-04-21T20:00\",\n" +
                "  \"endDate\": \"2016-04-22\",\n" +
                "  \"url\": \"nba-miami-philidelphia-game3.html\"\n" +
                "}");
        val data = SchemaOrgEventDataExtractor.extractUnsafe(json);

        assertThat(data.getUrl()).hasValue("nba-miami-philidelphia-game3.html");
        assertThat(data.getStart().map(v -> v.fold(Cf.Object.toStringF(), Cf.Object.toStringF()))).hasValue("2016-04-21T20:00:00.000");
        assertThat(data.getEnd().map(v -> v.fold(Cf.Object.toStringF(), Cf.Object.toStringF()))).hasValue("2016-04-22");
    }
}
