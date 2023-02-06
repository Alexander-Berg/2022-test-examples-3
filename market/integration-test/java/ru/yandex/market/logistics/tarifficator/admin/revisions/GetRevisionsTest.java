package ru.yandex.market.logistics.tarifficator.admin.revisions;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение поколений через админку")
class GetRevisionsTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получение всех поколений")
    @DatabaseSetup("/controller/admin/revisions/before/search_prepare.xml")
    void getAllRevisions() throws Exception {
        getRevisions()
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/revisions/response/all.json"));
    }

    @Test
    @DisplayName("Поколения не были сгенерированы")
    void getEmptyRevisions() throws Exception {
        getRevisions()
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/common/empty_response.json"));
    }

    @Nonnull
    private ResultActions getRevisions() throws Exception {
        return mockMvc.perform(get("/admin/revisions"));
    }
}
