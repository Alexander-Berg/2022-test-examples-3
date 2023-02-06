package ru.yandex.market.deepdive.domain.controller;

import java.util.HashSet;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.deepdive.domain.pickup_point.PickupPoint;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointMapper;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointRepository;
import ru.yandex.market.deepdive.domain.pickup_point.PickupPointService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(controllers = {PickupPointController.class, PickupPointService.class, PickupPointMapper.class})
@AutoConfigureMockMvc(addFilters = false)
public class PickupPointControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private PickupPointRepository repository;

    @Test
    public void test() throws Exception {
        var repositoryOutput = List.of(
                new PickupPoint(1L, "first", true, new HashSet<>()),
                new PickupPoint(2L, "last", false, new HashSet<>()));
        Mockito.doReturn(repositoryOutput).when(repository).findAll();

        mockMvc.perform(get("/api/pickup-points"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json("[{\"id\":1,\"name\":\"first\"},{\"id\":2,\"name\":\"last\"}]"));
    }
}
