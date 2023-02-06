package ru.yandex.market.pers.grade.api.docs;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.pers.grade.MockedPersGradeTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PageMatcherControllerTest extends MockedPersGradeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testSmoke() throws Exception {
        mockMvc.perform(get("/api/pagematch"))
                .andExpect(status().is2xxSuccessful());
    }

}
