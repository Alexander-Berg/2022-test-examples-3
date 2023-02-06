package ru.yandex.chemodan.trust.client;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.misc.lang.DefaultObject;

public class DateSerializationTest {

    @Test
    public void testSerialization() throws JsonProcessingException {
        ObjectMapper mapper = TrustClient.buildObjectMapper();
        Instant someDate = Instant.parse("2019-08-02T19:53:57.123+03:00");
        DatesPojo datesPojo = new DatesPojo(someDate, someDate);
        String s = mapper.writeValueAsString(datesPojo);
        Assert.assertEquals("{\"custom\":\"1564764837.123\",\"iso8601\":\"2019-08-02T16:53:57.123+0000\"}", s);
    }

    @Test
    public void testDeserialization() throws IOException {
        ObjectMapper mapper = TrustClient.buildObjectMapper();
        Instant someDate = Instant.parse("2019-08-02T19:53:57.123+03:00");
        DatesPojo datesPojo =
                mapper.readValue("{\"custom\":\"1564764837.123\",\"iso8601\":\"2019-08-02T19:53:57.123+03:00\"}",
                        DatesPojo.class);
        Assert.assertEquals(someDate, datesPojo.getCustom());
        Assert.assertEquals(someDate, datesPojo.getIso8601());
    }

    public static class DatesPojo extends DefaultObject {
        @JsonProperty("custom")
        @JsonSerialize(using = DateAdapter.Serializer.class)
        @JsonDeserialize(using = DateAdapter.Deserializer.class)
        private Instant custom;
        @JsonProperty("iso8601")
        private Instant iso8601;

        public DatesPojo(Instant custom, Instant iso8601) {
            this.custom = custom;
            this.iso8601 = iso8601;
        }

        public DatesPojo() {
        }

        public Instant getCustom() {
            return custom;
        }

        public Instant getIso8601() {
            return iso8601;
        }
    }
}
