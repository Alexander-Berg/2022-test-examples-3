package ru.yandex.market.wms.timetracker.json;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.dto.IndirectActivityRequest;
import ru.yandex.market.wms.timetracker.model.enums.UserActivityStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

@JsonTest
@ActiveProfiles("test")
class IndirectActivityRequestTest {

    @Autowired
    private JacksonTester<IndirectActivityRequest> tester;

    @Test
    public void canDeserialize() throws IOException {
        final IndirectActivityRequest expected = IndirectActivityRequest.builder()
                .userName("test")
                .activityName("Обед")
                .assigner("assigner")
                .status(UserActivityStatus.PENDING)
                .eventTime(Instant.parse("2021-11-01T00:00:00Z"))
                .endTime(Instant.parse("2021-11-01T00:00:00Z"))
                .build();

        final IndirectActivityRequest result =
                tester.readObject("/json/indirect-activity-request/model.json");

        assertThat(result, samePropertyValuesAs(expected));
    }
}
