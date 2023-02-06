package ru.yandex.market.logistics.nesu.controller.location;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.base.AbstractLocationSearchTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@DisplayName("Поиск локации по строке")
class BackOfficeLocationSearchTest extends AbstractLocationSearchTest {

    @Override
    protected ResultActions search(String term) throws Exception {
        return mockMvc.perform(get("/back-office/location").param("term", term));
    }
}
