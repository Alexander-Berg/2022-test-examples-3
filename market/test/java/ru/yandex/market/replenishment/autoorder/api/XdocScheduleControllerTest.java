package ru.yandex.market.replenishment.autoorder.api;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.api.dto.XdocScheduleDTO;
import ru.yandex.market.replenishment.autoorder.config.ControllerTest;
import ru.yandex.market.replenishment.autoorder.model.entity.postgres.XdocSchedule;
import ru.yandex.market.replenishment.autoorder.repository.postgres.XdocScheduleRepository;
import ru.yandex.market.replenishment.autoorder.security.WithMockLogin;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockLogin
public class XdocScheduleControllerTest extends ControllerTest {

    @Autowired
    private XdocScheduleRepository repository;

    @Test
    @DbUnitDataSet(before = "XdocScheduleControllerTest.before.csv")
    public void testGet() throws Exception {
        mockMvc.perform(get("/api/v2/xdoc-schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].warehouseId").value(147L))
                .andExpect(jsonPath("$[0].xdocId").value(47723L))
                .andExpect(jsonPath("$[0].schedule[0]").value(1))
                .andExpect(jsonPath("$[0].schedule[1]").value(2))
                .andExpect(jsonPath("$[0].schedule[2]").value(3))
                .andExpect(jsonPath("$[0].schedule[3]").value(4))
                .andExpect(jsonPath("$[0].schedule[4]").value(5))
                .andExpect(jsonPath("$[0].schedule[5]").value(6))
                .andExpect(jsonPath("$[0].schedule[6]").value(7));
    }

    @Test
    @DbUnitDataSet(before = "XdocScheduleControllerTest.before.csv")
    public void testSave() throws Exception {
        Integer[] arrayWithNull = new Integer[]{1, 2, 3, 10, 5, 6, null};
        final List<XdocScheduleDTO> dtos = List.of(
                new XdocScheduleDTO(47723L, 147L, arrayWithNull),
                new XdocScheduleDTO(47723L, 145L, new Integer[]{11, 12, 13, 14, 15, 16, 17})
        );
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/xdoc-schedule")
                        .content(dtoToString(dtos))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());
        final Collection<XdocSchedule> result = repository.findAll();
        assertEquals(2, result.size());
        result.forEach(schedule -> {
            assertNotNull(schedule);
            assertNotNull(schedule.getXdocId());
            assertNotNull(schedule.getWarehouseId());
            assertNotNull(schedule.getSchedule());

            if (schedule.getWarehouseId() == 147L) {
                assertArrayEquals(arrayWithNull, schedule.getSchedule());
            }
        });
    }

    @Test
    @DbUnitDataSet(before = "XdocScheduleControllerTest.before.csv")
    public void testSaveXdocNotExists() throws Exception {
        final List<XdocScheduleDTO> dtos = List.of(
                new XdocScheduleDTO(47723L, 147L, new Integer[]{1, 2, 3, 10, 5, 6, 7}),
                new XdocScheduleDTO(666L, 145L, new Integer[]{11, 12, 13, 14, 15, 16, 17})
        );
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/xdoc-schedule")
                        .content(dtoToString(dtos))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.message")
                        .value("Склад с указанным id не существует 666 и типом склада XDOC"));
    }

    @Test
    @DbUnitDataSet(before = "XdocScheduleControllerTest.before.csv")
    public void testSaveFFNotExists() throws Exception {
        final List<XdocScheduleDTO> dtos = List.of(
                new XdocScheduleDTO(47723L, 666L, new Integer[]{1, 2, 3, 10, 5, 6, 7}),
                new XdocScheduleDTO(47723L, 145L, new Integer[]{11, 12, 13, 14, 15, 16, 17})
        );
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/xdoc-schedule")
                        .content(dtoToString(dtos))
                        .contentType(APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.message").value("Склад с указанным id не существует 666"));
    }

    private <T> String dtoToString(T dto) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
        ObjectWriter ow = mapper.writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(dto);
    }

    @Test
    @DbUnitDataSet(before = "XdocScheduleControllerTest.before.csv")
    public void testDelete() throws Exception {
        final XdocScheduleDTO toDelete = new XdocScheduleDTO(47723L, 147L, new Integer[]{1});
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v2/xdoc-schedule")
                        .param("xdocId", Objects.requireNonNull(toDelete.getXdocId()).toString())
                        .param("warehouseId", Objects.requireNonNull(toDelete.getWarehouseId()).toString()))
                .andExpect(status().isOk());
        final Collection<XdocSchedule> result = repository.findAll();
        assertEquals(0, result.size());
    }

}
