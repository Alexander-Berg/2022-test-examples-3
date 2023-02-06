package ru.yandex.market.wms.timetracker.json;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.model.AreaModel;
import ru.yandex.market.wms.timetracker.response.EmployeeRelocationRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

@JsonTest
@ActiveProfiles("test")
class EmployeeRelocationRequestTest {

    @Autowired
    private JacksonTester<EmployeeRelocationRequest> tester;

    @Test
    public void canDeserialize() throws IOException {
        final EmployeeRelocationRequest expected = EmployeeRelocationRequest.builder()
                .wmsLogin("test")
                .position("Кладовщик")
                .eventTime(Instant.parse("2021-11-01T00:00:00Z"))
                .isEntry(false)
                .area(AreaModel.builder().name("area-test").build())
                .build();

        final EmployeeRelocationRequest result =
                tester.readObject("/json/employee-relocation-dto/model.json");

        assertThat(result, samePropertyValuesAs(expected));
    }

}
