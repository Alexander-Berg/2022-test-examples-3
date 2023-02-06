package ru.yandex.market.hrms.api.controller.structure.filters;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;

@DbUnitDataSet(before = "StructureControllerFiltersTest.before.csv")
public class StructureControllerFiltersTest extends AbstractApiTest {

    @Test
    @DbUnitDataSet(before = "StructureControllerFiltersTest.groupRanks.csv")
    void shouldReturnStructure() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/lms/structure/filters")
                        .param("groupId", "2")
        )
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(loadFromFile("structure.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "StructureControllerFiltersTest.groupRanks.csv")
    void shouldReturnAllTree() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/lms/structure/filters")
        )
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(loadFromFile("structure2.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "StructureControllerFiltersTest.groupRanks.csv")
    void shouldReturnTreeByLeafNode() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.get("/lms/structure/filters")
                        .param("groupId", "28")
        )
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(loadFromFile("structure3.json"), true));
    }

    @Test
    @DbUnitDataSet(before = "StructureControllerFiltersTest.groupRanks.pruned.csv")
    public void shouldIgnoreGroupsFromExtraLevels() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/lms/structure/filters")
                )
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(loadFromFile("structure2.pruned.json"), true));
    }

    @Test
    public void shouldNotFailWhenRanksNotPresent() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/lms/structure/filters")
                )
                .andDo(log())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("[]", true));
    }
}
