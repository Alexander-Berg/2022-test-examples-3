package ru.yandex.market.tsup.controller.front;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.RatingApiClient;
import ru.yandex.mj.generated.client.carrier.model.EntityRatingDto;
import ru.yandex.mj.generated.client.carrier.model.RatingDto;
import ru.yandex.mj.generated.client.carrier.model.RatingEntityTypeDto;
import ru.yandex.mj.generated.client.carrier.model.RatingTypeDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RatingControllerTest extends AbstractContextualTest {

    @Autowired
    private RatingApiClient ratingApiClient;

    @BeforeEach
    void setUp() {
        ExecuteCall<List<EntityRatingDto>, RetryStrategy> call = Mockito.mock(ExecuteCall.class);
        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(ratings()));
        Mockito.when(ratingApiClient.internalRatingDeliveryServiceGet(Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(call);
    }

    @Test
    @SneakyThrows
    void shouldGetRating() {
        mockMvc.perform(get("/companies/rating")
                        .param("deliveryServiceId", "123")
                        .param("maxValues", "2")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(IntegrationTestUtils.jsonContent("fixture/rating/company_ratings.json", true));
    }

    private List<EntityRatingDto> ratings() {
        return List.of(
                new EntityRatingDto()
                        .ratingType(RatingTypeDto.RUNS_ASSIGNED_DRIVER_AND_TRANSPORT)
                        .ratingEntityType(RatingEntityTypeDto.COMPANY)
                        .entityId(123L)
                        .values(List.of(
                                new RatingDto()
                                        .value(42L)
                                        .status(RatingDto.StatusEnum.CRIT)
                                        .createdAt(OffsetDateTime.of(2022, 2, 2, 22, 22, 22, 22, ZoneOffset.UTC)),
                                new RatingDto()
                                        .value(62L)
                                        .status(RatingDto.StatusEnum.OK)
                                        .createdAt(OffsetDateTime.of(2022, 2, 3, 22, 22, 22, 22, ZoneOffset.UTC))
                        ))
                        .thresholds(List.of(80L, 90L)),
                new EntityRatingDto()
                        .ratingType(RatingTypeDto.RUNS_FINISHED_IN_APPLICATION)
                        .ratingEntityType(RatingEntityTypeDto.COMPANY)
                        .entityId(123L)
                        .values(List.of(
                                new RatingDto()
                                        .value(42L)
                                        .status(RatingDto.StatusEnum.CRIT)
                                        .createdAt(OffsetDateTime.of(2022, 2, 2, 22, 22, 22, 22, ZoneOffset.UTC)),
                                new RatingDto()
                                        .value(62L)
                                        .status(RatingDto.StatusEnum.OK)
                                        .createdAt(OffsetDateTime.of(2022, 2, 3, 22, 22, 22, 22, ZoneOffset.UTC))
                        ))
                        .thresholds(List.of(80L, 90L)),
                new EntityRatingDto()
                        .ratingType(RatingTypeDto.RUNS_PROCESSED_BY_CARRIER)
                        .ratingEntityType(RatingEntityTypeDto.COMPANY)
                        .entityId(123L)
                        .values(List.of(
                                new RatingDto()
                                        .value(42L)
                                        .status(RatingDto.StatusEnum.CRIT)
                                        .createdAt(OffsetDateTime.of(2022, 2, 2, 22, 22, 22, 22, ZoneOffset.UTC)),
                                new RatingDto()
                                        .value(62L)
                                        .status(RatingDto.StatusEnum.OK)
                                        .createdAt(OffsetDateTime.of(2022, 2, 3, 22, 22, 22, 22, ZoneOffset.UTC))
                        ))
                        .thresholds(List.of(80L, 90L))
        );
    }
}
