package ru.yandex.market.logistics.nesu.jobs.consumer;

import java.net.URL;
import java.time.ZoneId;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.lom.client.LomClient;
import ru.yandex.market.logistics.lom.model.dto.OrderLabelRequestDto;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.client.enums.PageSize;
import ru.yandex.market.logistics.nesu.jobs.model.LabelsFileGenerationPayload;
import ru.yandex.market.logistics.nesu.jobs.retry.SimpleRetryPolicy;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Генерация файлов с ярлыками заказов")
@DatabaseSetup("/jobs/executors/generate_labels_file_setup.xml")
class LabelsFileGenerationConsumerTest extends AbstractContextualTest {
    private static final byte[] GENERATED_FILE_CONTENT = "generated fie content".getBytes();
    private static final String FILE_URL = "http://localhost:8080/labels_1.pdf";

    @Autowired
    private LomClient lomClient;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private LabelsFileGenerationConsumer labelsFileGenerationConsumer;

    @Test
    @DisplayName("Генерация файла с ярлыками заказов")
    @ExpectedDatabase(
        value = "/jobs/executors/generate_labels_file_result.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateLabelsFileLabel() throws Exception {
        when(lomClient.generateLabelsFile(getOrderLabelRequestDto())).thenReturn(GENERATED_FILE_CONTENT);
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(FILE_URL));

        labelsFileGenerationConsumer.execute(getTask(1));
    }

    @Test
    @DisplayName("Ошибка при генерации файла с ярлыками заказов при первой попытке")
    @ExpectedDatabase(
        value = "/jobs/executors/generate_labels_file_result_error_first_attempt.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateLabelsFileLabelError() {
        when(lomClient.generateLabelsFile(getOrderLabelRequestDto())).thenThrow(new RuntimeException("error"));

        labelsFileGenerationConsumer.execute(getTask(1));
        verifyZeroInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Ошибка при генерации файла с ярлыками заказов при последней попытке")
    @ExpectedDatabase(
        value = "/jobs/executors/generate_labels_file_result_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void generateLabelsFileLabelErrorLastAttempt() {
        when(lomClient.generateLabelsFile(getOrderLabelRequestDto())).thenThrow(new RuntimeException("error"));

        labelsFileGenerationConsumer.execute(getTask((int) SimpleRetryPolicy.DEFAULT_ATTEMPTS_NUMBER));
    }

    @Nonnull
    private OrderLabelRequestDto getOrderLabelRequestDto() {
        return OrderLabelRequestDto.builder()
            .ordersIds(Set.of(1L, 2L))
            .pageSize(ru.yandex.market.logistics.lom.model.enums.PageSize.A4)
            .build();
    }

    @Nonnull
    private Task<LabelsFileGenerationPayload> getTask(int attemptsCount) {
        return new Task<>(
            new QueueShardId("1"),
            getLabelsFileGenerationPayload(),
            attemptsCount,
            clock.instant().atZone(ZoneId.systemDefault()),
            null,
            null
        );
    }

    @Nonnull
    private LabelsFileGenerationPayload getLabelsFileGenerationPayload() {
        return new LabelsFileGenerationPayload(
            "123",
            Set.of(1L, 2L),
            PageSize.A4,
            1
        );
    }
}
