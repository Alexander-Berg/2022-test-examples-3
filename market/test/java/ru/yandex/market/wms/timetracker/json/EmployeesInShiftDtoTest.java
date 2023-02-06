package ru.yandex.market.wms.timetracker.json;

import java.io.IOException;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.wms.timetracker.dto.EmployeeInShiftDto;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.SamePropertyValuesAs.samePropertyValuesAs;

@JsonTest
@ActiveProfiles("test")
public class EmployeesInShiftDtoTest {

    @Autowired
    private JacksonTester<EmployeeInShiftDto> tester;

    @Test
    public void canDeserialize() throws IOException {
        final EmployeeInShiftDto expected = EmployeeInShiftDto.builder()
                .wmsLogin("test")
                .position("Кладовщик")
                .shiftStart(Instant.parse("2021-12-01T17:15:00Z"))
                .shiftEnd(Instant.parse("2021-12-02T05:15:00Z"))
                .shiftName("СОФЬИНО_2/2 ДЕНЬ\\НОЧЬ ПО 11 ЧАСОВ")
                .build();

        final EmployeeInShiftDto result =
                tester.readObject("/json/employees-in-shift-dto/model.json");

        assertThat(result, samePropertyValuesAs(expected));
    }
}
