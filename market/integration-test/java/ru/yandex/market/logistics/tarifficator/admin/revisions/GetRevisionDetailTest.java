package ru.yandex.market.logistics.tarifficator.admin.revisions;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение детальной информации о поколении через админку")
@DatabaseSetup("/controller/admin/revisions/before/search_prepare.xml")
class GetRevisionDetailTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получение детальной информации — успешно")
    void getRevisionDetail() throws Exception {
        getRevisionDetail(1)
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/revisions/response/id_1_details.json"));
    }

    @Test
    @DisplayName("Получение детальной информации — поколение не найдено")
    void getRevisionDetailNotFound() throws Exception {
        getRevisionDetail(4)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [REVISION] with ids [[4]]"));
    }

    @Nonnull
    private ResultActions getRevisionDetail(long revisionId) throws Exception {
        return mockMvc.perform(get("/admin/revisions/" + revisionId));
    }
}
