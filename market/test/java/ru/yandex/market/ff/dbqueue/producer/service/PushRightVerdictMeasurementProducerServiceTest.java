package ru.yandex.market.ff.dbqueue.producer.service;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import ru.yandex.market.ff.dbqueue.producer.PushRightVerdictMeasurementQueueProducer;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.dbqueue.PushRightVerdictMeasurementPayload;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PushRightVerdictMeasurementProducerServiceTest {

    private static final long REQUEST_ID = 3L;

    private final SupplierSkuKey firstKey = new SupplierSkuKey(2L, "sku1");
    private final SupplierSkuKey secondKey = new SupplierSkuKey(2L, "sku2");

    private PushRightVerdictMeasurementProducerService service;

    private PushRightVerdictMeasurementQueueProducer verdictProducer;

    private SoftAssertions assertions;

    @BeforeEach
    public void init() {
        verdictProducer = mock(PushRightVerdictMeasurementQueueProducer.class);
        service = new PushRightVerdictMeasurementProducerService(verdictProducer);
        assertions = new SoftAssertions();
    }

    @Test
    public void shouldSuccessProducerOnePushTask() {
        service.push(REQUEST_ID, ImmutableList.of(firstKey, secondKey));

        ArgumentCaptor<PushRightVerdictMeasurementPayload> captor =
                ArgumentCaptor.forClass(PushRightVerdictMeasurementPayload.class);

        verify(verdictProducer).produceSingle(captor.capture());
        PushRightVerdictMeasurementPayload payload = captor.getValue();
        assertions.assertThat(payload.getEntityId()).isEqualTo(REQUEST_ID);

        assertions.assertThat(payload.getIdentifiers()).isNotEmpty();
        assertions.assertThat(payload.getIdentifiers().get(0)).isEqualTo(firstKey);
        assertions.assertThat(payload.getIdentifiers().get(1)).isEqualTo(secondKey);
    }
}
