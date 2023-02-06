package ru.yandex.market.tsup.service.data_provider.primitive.external.tpl_planner.location;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.mj.generated.client.carrier.api.LocationApiClient;
import ru.yandex.mj.generated.client.carrier.model.TransportDto;
import ru.yandex.mj.generated.client.carrier.model.TransportLocationDto;

import static org.hamcrest.MatcherAssert.assertThat;

public class LocationProviderTest extends AbstractContextualTest {
    @Autowired
    private LocationProvider locationProvider;
    @Autowired
    private LocationApiClient locationClient;

    @Test
    void provide() {
        ExecuteCall<List<TransportLocationDto>, RetryStrategy> executeCall = Mockito.mock(ExecuteCall.class);
        Mockito.when(executeCall.schedule())
                        .thenReturn(CompletableFuture.completedFuture(
                                List.of(
                                        new TransportLocationDto()
                                                .transport(new TransportDto()
                                                        .id(1L)
                                                        .name("car1")
                                                        .number("111")
                                                        .brand("brand1")
                                                        .model("model1")
                                                        .trailerNumber("123")
                                                )
                                                .latitude(BigDecimal.valueOf(53.22))
                                                .longitude(BigDecimal.valueOf(23.12)),
                                        new TransportLocationDto()
                                                .transport(new TransportDto()
                                                        .id(2L)
                                                        .name("car2")
                                                        .number("222")
                                                        .brand("brand2")
                                                        .model("model2")
                                                        .trailerNumber("123")
                                                )
                                                .latitude(BigDecimal.valueOf(13.22))
                                                .longitude(BigDecimal.valueOf(43.32))
                                )
                        ));
        Mockito.when(locationClient.internalLocationsGet(Mockito.nullable(List.class), Mockito.nullable(List.class)))
            .thenReturn(executeCall);


        Map<Long, LocationDto> result =
            locationProvider.provide(LocationFilter.builder().transportIds(Set.of(1L, 2L)).build(), null);

        assertThat(result, Is.is(Map.of(
            1L, new LocationDto(BigDecimal.valueOf(53.22), BigDecimal.valueOf(23.12)),
            2L, new LocationDto(BigDecimal.valueOf(13.22), BigDecimal.valueOf(43.32))
        )));
    }

    @Test
    void provideNullable() {
        ExecuteCall<List<TransportLocationDto>, RetryStrategy> executeCall = Mockito.mock(ExecuteCall.class);
        Mockito.when(executeCall.schedule())
                .thenReturn(CompletableFuture.completedFuture(
                        List.of(
                                new TransportLocationDto()
                                        .transport(new TransportDto()
                                                .id(1L)
                                                .name("car1")
                                                .number("111")
                                                .brand("brand1")
                                                .model("model1")
                                                .trailerNumber("123")
                                        )
                        )
                ));
        Mockito.when(locationClient.internalLocationsGet(Mockito.nullable(List.class), Mockito.nullable(List.class)))
                .thenReturn(executeCall);


        Map<Long, LocationDto> result =
            locationProvider.provide(LocationFilter.builder().transportIds(Set.of(1L)).build(), null);

        assertThat(result, Is.is(Map.of(
            1L, new LocationDto(null, null)
        )));
    }

}
