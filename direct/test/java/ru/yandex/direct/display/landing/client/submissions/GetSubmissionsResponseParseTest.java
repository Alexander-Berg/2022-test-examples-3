package ru.yandex.direct.display.landing.client.submissions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class GetSubmissionsResponseParseTest {
    private static final String realLifeJson = "{"
            + "\"Submissions\": ["
            + "{"
            + "\"Data\": ["
            + "{"
            + "\"FieldName\": \"phone\","
            + "\"Value\": \"+1 234 567 89 00\""
            + "},"
            + "{"
            + "\"FieldName\": \"name\","
            + "\"Value\": \"Konstantin\""
            + "}"
            + "],"
            + "\"Id\": \"5a09be61da6f5d0039921614\","
            + "\"SubmittedAt\": \"2017-11-13T15:46:41Z\","
            + "\"TurboPageId\": 1000005,"
            + "\"TurboPageName\": \"Акция помоем апишечку для Директа\""
            + "},"
            + "{"
            + "\"Data\": ["
            + "{"
            + "\"FieldName\": \"phone\","
            + "\"Value\": \"+1 234 567 89 00\""
            + "},"
            + "{"
            + "\"FieldName\": \"name\","
            + "\"Value\": \"Konstantin\""
            + "}"
            + "],"
            + "\"Id\": \"5a09c1bfda6f5d003113cf2b\","
            + "\"SubmittedAt\": \"2017-11-13T16:01:03Z\","
            + "\"TurboPageId\": 1000005,"
            + "\"TurboPageName\": \"Акция помоем апишечку для Директа\""
            + "}"
            + "]"
            + "}";

    @Test
    public void deserialize_smokeTest() {
        assertThatCode(() -> GetSubmissionsResponse.deserialize(realLifeJson)).doesNotThrowAnyException();
    }

    @Test
    public void deserialize_SubmissionsIsDeserialized() {
        GetSubmissionsResponse response = GetSubmissionsResponse.deserialize(realLifeJson);
        assertThat(response.getSubmissions()).isNotNull().isNotEmpty();
    }

    @Test
    public void deserialize_SubmissionDataIsDeserialized() {
        GetSubmissionsResponse response = GetSubmissionsResponse.deserialize(realLifeJson);
        assertThat(response.getSubmissions().get(0).getData()).isNotNull().isNotEmpty();
    }

    @Test
    public void deserialize_SubmissionDataFieldNameIsDeserialized() {
        GetSubmissionsResponse response = GetSubmissionsResponse.deserialize(realLifeJson);
        assertThat(response.getSubmissions().get(0).getData().get(0).getFieldName()).isEqualTo("phone");
    }

    @Test
    public void deserialize_SubmissionDataValueIsDeserialized() {
        GetSubmissionsResponse response = GetSubmissionsResponse.deserialize(realLifeJson);
        assertThat(response.getSubmissions().get(0).getData().get(0).getValue()).isEqualTo("+1 234 567 89 00");
    }

    @Test
    public void deserialize_SubmissionIdIsDeserialized() {
        GetSubmissionsResponse response = GetSubmissionsResponse.deserialize(realLifeJson);
        assertThat(response.getSubmissions().get(0).getId()).isEqualTo("5a09be61da6f5d0039921614");
    }

    @Test
    public void deserialize_SubmissionsSubmittedAtIsDeserialized() {
        GetSubmissionsResponse response = GetSubmissionsResponse.deserialize(realLifeJson);
        assertThat(response.getSubmissions().get(0).getSubmittedAt()).isEqualTo(
                OffsetDateTime.of(2017, 11, 13, 15, 46, 41, 0,
                        ZoneOffset.UTC));
    }

    @Test
    public void deserialize_SubmissionTurboPageIdIsDeserialized() {
        GetSubmissionsResponse response = GetSubmissionsResponse.deserialize(realLifeJson);
        assertThat(response.getSubmissions().get(0).getTurboPageId()).isEqualTo(1000005L);
    }

    @Test
    public void deserialize_SubmissionTurboPageNameIsDeserialized() {
        GetSubmissionsResponse response = GetSubmissionsResponse.deserialize(realLifeJson);
        assertThat(response.getSubmissions().get(0).getTurboPageName())
                .isEqualTo("Акция помоем апишечку для Директа");
    }


}
