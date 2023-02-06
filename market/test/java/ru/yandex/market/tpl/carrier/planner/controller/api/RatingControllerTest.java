package ru.yandex.market.tpl.carrier.planner.controller.api;


import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.rating.RatingHelper;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.market.tpl.carrier.planner.controller.util.DeliveryServiceHelper;
import ru.yandex.market.tpl.carrier.planner.controller.util.DsCreateCommand;
import ru.yandex.mj.generated.server.model.RatingEntityTypeDto;
import ru.yandex.mj.generated.server.model.RatingTypeDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RatingControllerTest extends BasePlannerWebTest {


    private final TestUserHelper testUserHelper;
    private final DeliveryServiceHelper dsHelper;
    private final RatingHelper ratingHelper;

    private Company company;

    @BeforeEach
    void setUp() {
        company = testUserHelper.findOrCreateCompany("Рога и копыта");
        ratingHelper.createRatings(company.getId());
    }

    @Test
    @SneakyThrows
    void shouldGetRating() {
        mockMvc.perform(get("/internal/rating")
                        .param("rating_type", "RUNS_ASSIGNED_DRIVER_AND_TRANSPORT")
                        .param("rating_entity_type", RatingEntityTypeDto.COMPANY.toString())
                        .param("entity_id", String.valueOf(company.getId()))
                        .param("maxResults", String.valueOf(2))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingType").value(RatingTypeDto.RUNS_ASSIGNED_DRIVER_AND_TRANSPORT.getValue()))
                .andExpect(jsonPath("$.ratingEntityType").value(RatingEntityTypeDto.COMPANY.getValue()))
                .andExpect(jsonPath("$.entityId").value(company.getId()))
                .andExpect(jsonPath("$.values").isArray())
                .andExpect(jsonPath("$.values").value(Matchers.hasSize(2)))
                .andExpect(jsonPath("$.values[0].value").value(47L))
                .andExpect(jsonPath("$.values[1].value").value(48L))
                .andExpect(jsonPath("$.thresholds").value(Matchers.hasSize(2)));
    }


    @Test
    @SneakyThrows
    void shouldGetRatingForDs() {
        dsHelper.createDeliveryService(DsCreateCommand.builder()
                    .companyId(company.getId())
                    .dsId(123L)
                    .name("delivery service")
                    .token("token")
                .build());

        mockMvc.perform(get("/internal/rating/delivery-service")
                        .param("entity_id", String.valueOf(123L))
                        .param("maxResults", String.valueOf(2))
                )
                .andDo(print())
                .andExpect(status().isOk());
    }


    @Test
    @SneakyThrows
    void shouldGetRatingForDsWhenTooMuchRequested() {
        dsHelper.createDeliveryService(DsCreateCommand.builder()
                .companyId(company.getId())
                .dsId(123L)
                .name("delivery service")
                .token("token")
                .build());

        mockMvc.perform(get("/internal/rating/delivery-service")
                        .param("entity_id", String.valueOf(123L))
                        .param("maxResults", String.valueOf(200))
                )
                .andDo(print())
                .andExpect(status().isOk());
    }

}
