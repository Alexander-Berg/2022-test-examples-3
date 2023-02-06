package ru.yandex.market.tpl.dora.api;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.dora.db.jooq.tables.pojos.Course;
import ru.yandex.market.tpl.dora.test.BaseShallowTest;
import ru.yandex.market.tpl.dora.test.WebLayerTest;
import ru.yandex.market.tpl.dora.test.factory.TestCourseFactory;
import ru.yandex.market.tpl.dora.test.factory.TestPlatformFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.dora.test.TestUtils.getFileContent;

@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class KioskApiServiceTest extends BaseShallowTest {

    private final TestCourseFactory courseFactory;
    private final TestPlatformFactory platformFactory;

    @Test
    void assignCourseTest() throws Exception {
        platformFactory.create();
        Course course = courseFactory.create();

        mockMvc.perform(post("/kiosk/complete-course")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(getFileContent("kiosk/request_complete_course.json"),
                                course.getPlatformId())))
                .andExpect(status().is2xxSuccessful());
    }

}
