package ru.yandex.market.wms.api.service;

import java.math.BigDecimal;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.logistics.iris.client.api.PushApiClient;
import ru.yandex.market.logistics.iris.client.model.entity.Korobyte;
import ru.yandex.market.logistics.iris.client.model.entity.MeasurementDimensions;
import ru.yandex.market.logistics.iris.client.model.entity.UnitId;
import ru.yandex.market.logistics.iris.client.model.request.PushMeasurementDimensionsRequest;
import ru.yandex.market.wms.api.config.IrisClientTestConfiguration;
import ru.yandex.market.wms.api.config.IrisTvmConfiguration;
import ru.yandex.market.wms.api.service.iris.push.IrisPushService;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.config.IntegrationTestConfig;
import ru.yandex.market.wms.common.spring.servicebus.model.dto.PushReferenceItemsResultDto;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {IntegrationTestConfig.class, IrisTvmConfiguration.class, IrisClientTestConfiguration.class})
public class IrisPushServiceTest extends IntegrationTest {

    @Autowired
    protected PushApiClient pushApiClient;

    @Autowired
    private IrisPushService irisPushService;

    @Test
    public void shouldSuccessInvokePush() {
        final Integer warehouseId = 145;

        irisPushService.pushDimensions(createPushSkuRecords(), warehouseId);

        ArgumentCaptor<PushMeasurementDimensionsRequest> requestCaptor =
                ArgumentCaptor.forClass(PushMeasurementDimensionsRequest.class);

        verify(pushApiClient, times(1)).pushMeasurementDimensions(requestCaptor.capture());

        PushMeasurementDimensionsRequest request = requestCaptor.getValue();

        assertions.assertThat(request.getWarehouseId()).isEqualTo(warehouseId);
        assertions.assertThat(request.getDimensions()).isNotNull();
        assertions.assertThat(request.getDimensions().size()).isEqualTo(2);

        assertSoftly(assertions -> {
            MeasurementDimensions firstDimensions = request.getDimensions().get(0);
            assertions.assertThat(firstDimensions.getUnitId()).isEqualTo(new UnitId(null, 4334L, "sku_1"));
            assertions.assertThat(firstDimensions.getKorobyte())
                    .isEqualTo(new Korobyte.KorobyteBuilder()
                            .setWidth(BigDecimal.valueOf(10))
                            .setHeight(BigDecimal.valueOf(20))
                            .setLength(BigDecimal.valueOf(30))
                            .setWeightGross(BigDecimal.valueOf(1220))
                            .build()
                    );

            MeasurementDimensions secondDimensions = request.getDimensions().get(1);
            assertions.assertThat(secondDimensions.getUnitId()).isEqualTo(new UnitId(null, 4334L, "sku_2"));
            assertions.assertThat(secondDimensions.getKorobyte())
                    .isEqualTo(new Korobyte.KorobyteBuilder()
                            .setWidth(BigDecimal.valueOf(40))
                            .setHeight(BigDecimal.valueOf(50))
                            .setLength(BigDecimal.valueOf(60))
                            .setWeightGross(BigDecimal.valueOf(1620))
                            .build()
                    );
        });
    }

    private List<PushReferenceItemsResultDto> createPushSkuRecords() {
        return ImmutableList.of(
                PushReferenceItemsResultDto.builder()
                        .korobyte(new ru.yandex.market.logistic.api.model.fulfillment.Korobyte.KorobyteBuiler(
                                10,
                                20,
                                30,
                                BigDecimal.valueOf(1220)
                        ).build())
                        .unitId(new ru.yandex.market.logistic.api.model.fulfillment.UnitId(
                                null,
                                4334L,
                                "sku_1"
                        ))
                        .build(),
                PushReferenceItemsResultDto.builder()
                        .korobyte(new ru.yandex.market.logistic.api.model.fulfillment.Korobyte.KorobyteBuiler(
                                40,
                                50,
                                60,
                                BigDecimal.valueOf(1620)
                        ).build())
                        .unitId(new ru.yandex.market.logistic.api.model.fulfillment.UnitId(
                                null,
                                4334L,
                                "sku_2"
                        )).build()
        );
    }

}
