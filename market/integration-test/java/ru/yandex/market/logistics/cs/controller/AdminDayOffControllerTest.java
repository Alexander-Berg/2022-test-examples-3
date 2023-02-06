package ru.yandex.market.logistics.cs.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.cs.AbstractIntegrationTest;

@DisplayName("Контроллер дейоффов")
public class AdminDayOffControllerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Устанавливаем manual дейофф")
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @DatabaseSetup("/controller/dayoff/before/basic_counters.xml")
    @ExpectedDatabase(
        value = "/controller/dayoff/after/set_manual.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void testSetManual() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/admin/day-off/manual")
                .param("capacity-value-id", "105")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Устанавливаем manual дейофф при отсутсвии счётчиков")
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @ExpectedDatabase(
        value = "/controller/dayoff/after/set_manual_but_there_are_no_counters.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void testSetManualButThereAreNoCounters() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/admin/day-off/manual")
                .param("capacity-value-id", "105")
                .param("day", "2021-09-30")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Устанавливаем manual дейофф, но там уже стоит manual дейофф")
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @DatabaseSetup("/controller/dayoff/before/set_manual_but_already_manual.xml")
    @ExpectedDatabase(
        value = "/controller/dayoff/after/set_manual.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void testSetManualButThereIsAlreadyManual() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/admin/day-off/manual")
                .param("capacity-value-id", "105")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Устанавливаем manual дейофф, но там уже стоит technical дейофф")
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @DatabaseSetup("/controller/dayoff/before/set_manual_but_already_technical.xml")
    @ExpectedDatabase(
        value = "/controller/dayoff/after/set_manual.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void testSetManualButThereIsAlreadyTechnical() {
        mockMvc.perform(
            MockMvcRequestBuilders.post("/admin/day-off/manual")
                .param("capacity-value-id", "105")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Устанавливаем manual дейофф, но CapacityValue не существует")
    @SneakyThrows
    void testSetManualButThereIsNoCapacityValue() {
        var result = mockMvc.perform(
            MockMvcRequestBuilders.post("/admin/day-off/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .param("capacity-value-id", "105")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andReturn();

        softly.assertThat(result.getResponse().getContentAsString())
            .isEqualTo("CapacityValue with id 105 is not found");
    }

    @Test
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @DisplayName("Устанавливаем manual дейофф, но переданные счётчик и дата конфликтуют")
    @SneakyThrows
    void testSetManualButCapacityValueAndDayConflict() {
        var result = mockMvc.perform(
            MockMvcRequestBuilders.post("/admin/day-off/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .param("capacity-value-id", "102")
                .param("day", "2000-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();

        softly.assertThat(result.getResponse().getContentAsString())
            .isEqualTo("CapacityValue 102 has a day 1997-04-20 that conflicts with provided day 2000-04-20");
    }

    @Test
    @DisplayName("Снимаем manual дейофф, но нужный счётчик не найден")
    @SneakyThrows
    void testUnsetManualButThereIsNoCounter() {
        var result = mockMvc.perform(
            MockMvcRequestBuilders.delete("/admin/day-off/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .param("capacity-value-id", "105")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andReturn();

        softly.assertThat(result.getResponse().getContentAsString())
            .isEqualTo("CapacityValueCounter is not found by CapacityValue 105 and day 1997-04-20");
    }

    @Test
    @DisplayName("Снимаем manual дейофф")
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @DatabaseSetup("/controller/dayoff/before/unset_manual.xml")
    @ExpectedDatabase(
        value = "/controller/dayoff/after/unset_manual.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void testUnsetManual() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/admin/day-off/manual")
                .param("capacity-value-id", "105")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Снимаем manual дейофф на переполненном счётчике")
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @DatabaseSetup("/controller/dayoff/before/unset_manual_but_there_should_be_a_day_off.xml")
    @ExpectedDatabase(
        value = "/controller/dayoff/after/unset_manual_but_there_should_be_a_day_off.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void testUnsetManualButThereShouldBeADayOff() {
        mockMvc.perform(
            MockMvcRequestBuilders.delete("/admin/day-off/manual")
                .param("capacity-value-id", "105")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    @DisplayName("Снимаем manual дейофф, но дейоффа нет")
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @DatabaseSetup("/controller/dayoff/before/basic_counters.xml")
    @SneakyThrows
    void testUnsetManualButThereIsNoDayOff() {
        var result = mockMvc.perform(
            MockMvcRequestBuilders.delete("/admin/day-off/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .param("capacity-value-id", "105")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();

        softly.assertThat(result.getResponse().getContentAsString())
            .isEqualTo("Unable to unset a day off. Counter 1005 doesn't have a day off to unset");
    }

    @Test
    @DisplayName("Снимаем manual дейофф, но дейофф спропагирован со счётчика выше")
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @DatabaseSetup("/controller/dayoff/before/unset_manual.xml")
    @SneakyThrows
    void testUnsetManualButDayOffIsPropagated() {
        var result = mockMvc.perform(
            MockMvcRequestBuilders.delete("/admin/day-off/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .param("capacity-value-id", "108")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();

        softly.assertThat(result.getResponse().getContentAsString())
            .isEqualTo("Unable to unset a propagated day off form counter 1008");
    }

    @Test
    @DisplayName("Снимаем manual дейофф, но дейофф типа technical")
    @DatabaseSetup("/controller/dayoff/before/capacity_tree.xml")
    @DatabaseSetup("/controller/dayoff/before/unset_manual_but_actually_tecnical.xml")
    @SneakyThrows
    void testUnsetManualButDayOffIsTechnical() {
        var result = mockMvc.perform(
            MockMvcRequestBuilders.delete("/admin/day-off/manual")
                .contentType(MediaType.APPLICATION_JSON)
                .param("capacity-value-id", "105")
                .param("day", "1997-04-20")
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();

        softly.assertThat(result.getResponse().getContentAsString())
            .isEqualTo("Can't use MANUAL mode to unset a NOT MANUAL day off");
    }

}
