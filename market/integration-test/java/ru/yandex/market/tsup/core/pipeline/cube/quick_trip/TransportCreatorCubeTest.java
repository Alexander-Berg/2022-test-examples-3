package ru.yandex.market.tsup.core.pipeline.cube.quick_trip;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.retrofit.ExecuteCall;
import ru.yandex.market.common.retrofit.RetryStrategy;
import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.cube.CarrierTransportCreatorCube;
import ru.yandex.market.tsup.core.pipeline.data.quick_trip.TransportCreationData;
import ru.yandex.market.tsup.core.pipeline.data.quick_trip.TransportId;
import ru.yandex.mj.generated.client.carrier.api.TransportApiClient;
import ru.yandex.mj.generated.client.carrier.model.TransportCreateDto;
import ru.yandex.mj.generated.client.carrier.model.TransportDto;

public class TransportCreatorCubeTest extends AbstractContextualTest {
    @Autowired
    private CarrierTransportCreatorCube cube;

    @Autowired
    private TransportApiClient transportApiClient;

    @Test
    void testExisting() {
        TransportCreationData data = new TransportCreationData().setExistingTransportId(1L);

        var result = cube.execute(data);

        Mockito.verify(transportApiClient, Mockito.times(0))
            .internalTransportCreatePost(Mockito.any());

        softly.assertThat(result).isEqualTo(new TransportId(1L));
    }

    @Test
    void testCreateNew() {
        TransportCreationData data = new TransportCreationData()
            .setData(new TransportCreationData.CreationData()
                .setBrand("brand")
                .setModel("model")
                .setCapacity(12D)
                .setPalletsCapacity(12)
                .setCompanyId(1L)
                .setNumber("TX456T")
            );

        ExecuteCall<TransportDto, RetryStrategy> call = Mockito.mock(ExecuteCall.class);

        Mockito.when(call.schedule()).thenReturn(CompletableFuture.completedFuture(
            new TransportDto().id(12L)
        ));

        Mockito.when(
                transportApiClient.internalTransportCreatePost(
                    new TransportCreateDto()
                        .companyId(1L)
                        .name("brand model TX456T")
                        .brand("brand")
                        .model("model")
                        .capacity(12D)
                        .palletsCapacity(12)
                        .number("TX456T")
                )
            )
            .thenReturn(call);
        var result = cube.execute(data);

        softly.assertThat(result).isEqualTo(new TransportId(12L));
    }
}
