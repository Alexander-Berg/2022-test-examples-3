package ru.yandex.market.deepdive.domain.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.configuration.TestConfiguration;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;


@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = TestConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
public class PickupPointControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PickupPointRepository repo;

    @BeforeEach
    public void clearDB() {
        repo.deleteAll();
    }

    @Test
    @DisplayName("BeanCheck")
    public void getPickupPoints() throws Exception {
        Assertions.assertNotNull(repo);
//        PickupPoint point = new PickupPoint(7L, "Benya", true);
//        String jsonExp = "[ { \\\"id\\\" : 7, \\\"name\\\": \\\"Benya\\\", \\\"active\\\": \\\"true\\\"} ]";
        //mockMvc.perform(post("/api/pickup-points").contentType(MediaType.APPLICATION_JSON_UTF8).content(jsonExp));
        //mockMvc.perform(get("/api/pickup-points")).andExpect(status().isOk());

    }




}
