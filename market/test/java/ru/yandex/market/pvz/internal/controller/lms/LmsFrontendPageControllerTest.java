package ru.yandex.market.pvz.internal.controller.lms;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.domain.banner_information.frontend_page.FrontendPage;
import ru.yandex.market.pvz.core.domain.banner_information.frontend_page.FrontendPageRepository;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LmsFrontendPageControllerTest extends BaseShallowTest {

    private final FrontendPageRepository frontendPageRepository;

    @Test
    void getPages() throws Exception {
        var page1 = createPage("Описание 1", "Название 1");
        var page2 = createPage("Описание 2", "Название 2");

        var expected = String.format(
                getFileContent("lms/frontend_page/response.json"),
                page1.getId(), page1.getId(), page1.getDescription(), page1.getPageName(),
                page2.getId(), page2.getId(), page2.getDescription(), page2.getPageName()
        );

        mockMvc.perform(get("/lms/frontend-page")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(expected, false));
    }

    private FrontendPage createPage(String description, String pageName) {
        return frontendPageRepository.save(
                FrontendPage.builder().description(description).pageName(pageName).build()
        );
    }

}
