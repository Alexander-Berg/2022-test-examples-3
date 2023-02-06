package ru.yandex.market.logistics.tarifficator.jobs.tms;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.exception.MdsS3Exception;
import ru.yandex.market.logistics.tarifficator.service.shop.DeliveryPaymentService;
import ru.yandex.market.logistics.tarifficator.service.shop.DeliveryTariffService;

class DeliveryPaymentTypesExecutorTest extends AbstractMbiMdsS3Test {
    private static final String REGIONAL_PAYMENT_TYPES =
        "regional-payment-types/current_regional-payment-types.json";
    @Autowired
    private DeliveryPaymentService deliveryPaymentService;
    @Autowired
    private DeliveryTariffService deliveryTariffService;

    private DeliveryPaymentTypesExecutor executor;

    @Override
    protected String getMdsPath() {
        return REGIONAL_PAYMENT_TYPES;
    }

    @BeforeEach
    void onBefore() {
        executor = new DeliveryPaymentTypesExecutor(deliveryPaymentService,
            deliveryTariffService, mdsS3Service);
    }

    @Test
    @DisplayName("Тест джобы DeliveryPaymentTypesExecutor с пустыми таблицами")
    void testEmptyDeliveryPaymentTypes() throws IOException {
        mockMdsClientWithResult();
        executor.doJob(null);
        softly.assertThat(objectMapper.readTree(result)).isEqualTo(objectMapper.readTree("[]"));
    }

    @Test
    @DisplayName("Тест джобы DeliveryPaymentTypesExecutor с заполненными данными таблицами")
    @DatabaseSetup("testExportDeliveryPaymentTypes.before.xml")
    void testConfiguredDeliveryPaymentTypes() throws IOException {
        mockMdsClientWithResult();
        executor.doJob(null);

        try (var stream = getClass().getResourceAsStream("testExportDeliveryPaymentTypes.json")) {
            softly.assertThat(objectMapper.readTree(result)).isEqualTo(objectMapper.readTree(stream));
        }
    }

    @Test
    @DisplayName("Тест джобы DeliveryPaymentTypesExecutor с ошибкой от MdsS3Client")
    void testDeliveryPaymentTypesError() {
        var message = "someTestError";
        mockMdsClientWithError(message);

        softly.assertThatThrownBy(() -> executor.doJob(null))
            .isInstanceOf(RuntimeException.class)
            .hasCause(new MdsS3Exception(message));
    }
}
