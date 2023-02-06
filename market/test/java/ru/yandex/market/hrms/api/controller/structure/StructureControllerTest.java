package ru.yandex.market.hrms.api.controller.structure;


import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "StructureControllerTest.before.csv")
class StructureControllerTest extends AbstractApiTest {

    @ParameterizedTest(name = "loadStructure_{index}")
    @DisplayName("Запрос структуры")
    @MethodSource("structureTestArguments")
    void structure(LocalDate date, String expectedJsonFile) throws Exception {
        mockClock(date);
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/structure").param("domainId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile(expectedJsonFile), true));
    }

    private static Stream<Arguments> structureTestArguments() {
        return Stream.of(
                Arguments.of(LocalDate.of(2021, 1, 1), "StructureControllerTest.structureJanuary.json"),
                Arguments.of(LocalDate.of(2021, 2, 1), "StructureControllerTest.structureFebruary.json"),
                Arguments.of(LocalDate.of(2021, 4, 2), "StructureControllerTest.structureApril.json")
        );
    }
}
