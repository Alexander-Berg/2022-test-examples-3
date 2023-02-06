package ru.yandex.market.delivery.rupostintegrationapp.functional.getreferencewarehouses;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import steps.pickuppointsteps.PickupPointSteps;
import utils.FixtureRepository;

import ru.yandex.market.delivery.rupostintegrationapp.BaseContextualTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.PickuppointRepository;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GetReferenceWarehousesPositiveTest extends BaseContextualTest {
    // TODO: 31/03/17 выпилить, когда запилим тестовую БД
    @MockBean
    private PickuppointRepository repository;

    @BeforeEach
    void initMock() {
        when(repository.findPickupPoints("bname", "fname", "fvalue", null))
            .thenReturn(PickupPointSteps.getRussianPostPickupPoints());
    }

    @Test
    void testService() throws Exception {
        mockMvc.perform(post(
            "/ds/getReferenceWarehouses?boolean_flag=bname&enum_flag_name=fname&enum_flag_value=fvalue"
        )
            .contentType(MediaType.APPLICATION_XML)
            .content(FixtureRepository.getReferenceWarehousesRequest()))
            .andExpect(status().isOk())
            .andExpect(content().string(Matchers.containsString("type=\"getReferenceWarehouses\"")))
            .andExpect(content().string(Matchers.containsString("<isError>false</isError>")))
            .andExpect(content().string(Matchers.containsString("<warehouse><id><deliveryId>1</deliveryId>")))
            .andExpect(content().string(Matchers.containsString("<warehouse><id><deliveryId>2</deliveryId>")))
            .andExpect(content().string(Matchers.containsString("<warehouse><id><deliveryId>3</deliveryId>")));
    }
}
