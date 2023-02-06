package ru.yandex.market.hrms.api.controller.operation;

import javax.servlet.http.Cookie;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.api.AbstractApiTest;
import ru.yandex.market.hrms.core.domain.operation.repo.OperationGroupRepo;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DbUnitDataSet(before = "OperationControllerTest.before.csv")
public class OperationControllerTest extends AbstractApiTest {

    @Autowired
    private OperationGroupRepo operationRepo;

    @Test
    public void shouldReturnOperationGroups() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/operation-groups")
                .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OperationControllerTestGetOperationGroups.json")));
    }

    @Test
    public void shouldReturnEnableForNpoOperationGroups() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/operation-groups")
                        .queryParam("nonProductionOnly", "true")
                        .cookie(new Cookie("yandex_login", "alex-fill")))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OperationControllerTestGetNpoOnlyOperationGroups.json")));
    }

    @Test
    @DbUnitDataSet(after = "OperationControllerTestCreateGroup.after.csv")
    public void shouldCreateOperationGroup() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.post("/lms/operation-groups")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .content("""
                        {
                          "groupName": "Тестовая группа 1",
                          "operationsIds": [111]
                         }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OperationControllerTestCreateOperationGroup.json")));
    }

    @Test
    @DbUnitDataSet(after = "OperationControllerTestUpdateOperationGroup.after.csv")
    public void shouldUpdateOperationGroup() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.put("/lms/operation-groups")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .content("""
                        {
                          "groupId": 5227,
                          "groupName": "Изменённая тестовая группа 1",
                          "operationsIds": [112]
                         }
                        """))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OperationControllerTestUpdateOperationGroup.json")));
    }

    @Test
    public void shouldReturn400WhenGroupDoesntExist() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.put("/lms/operation-groups")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .cookie(new Cookie("yandex_login", "magomedovgh"))
                .content("""
                        {
                          "groupId": 5229,
                          "groupName": "Изменённая тестовая группа 1",
                          "operationsIds": [112]
                         }
                        """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void shouldSuggestOperations() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/operation-groups/suggest")
                .queryParam("search", "переме")
                .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OperationControllerTestSuggestOperations.json")));
    }

    @Test
    public void shouldSuggestNoOperations() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/operation-groups/suggest")
                .queryParam("search", "")
                .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    public void shouldSuggestOperationGroups() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/operation-groups/suggest-group")
                .queryParam("search", "кон")
                .cookie(new Cookie("yandex_login", "alex-fill")))
                .andExpect(status().isOk())
                .andExpect(content().json(loadFromFile("OperationControllerTestSuggestOperationGroups.json")));
    }

    @Test
    public void shouldSuggestNoOperationGroups() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.get("/lms/operation-groups/suggest-group")
                .queryParam("search", "конь")
                .cookie(new Cookie("yandex_login", "alex-fill")))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    public void shouldDelete() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.delete("/lms/operation-groups/5227")
                .cookie(new Cookie("yandex_login", "magomedovgh")))
                .andExpect(status().isOk());

        var group = operationRepo.findById(5227L);
        MatcherAssert.assertThat(
                group.orElse(null),
                Matchers.hasProperty("deletedAt", Matchers.notNullValue()));
    }
}
