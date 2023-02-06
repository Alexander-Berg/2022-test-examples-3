package ru.yandex.market.logistics.nesu.jobs.processor;

import java.io.OutputStream;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.exception.http.BadRequestException;
import ru.yandex.market.logistics.nesu.exception.http.NesuValidationException;
import ru.yandex.market.logistics.nesu.jobs.model.MultipleUpdateLogisticPointAvailabilitiesPayload;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@DisplayName("Массовое изменение доступностей")
class MultipleChangeLogisticPointAvailabilitiesProcessorTest extends AbstractContextualTest {

    @Autowired
    private MultipleUpdateLogisticPointAvailabilitiesProcessor processor;

    @Autowired
    private MdsS3Client mdsS3Client;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    @DisplayName("Успех")
    @DatabaseSetup("/jobs/processor/multiple_update_availabilities/success_before.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/multiple_update_availabilities/success_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void success() {
        mockMdsS3ClientDownload("jobs/processor/multiple_update_availabilities/multiple_update_availabilities.xlsx");
        processor.processPayload(createPayload(1L));
        verify(mdsS3Client).download(any(ResourceLocation.class), any(ContentConsumer.class));
    }

    @Test
    @DisplayName("Частичное изменение доступностей")
    @DatabaseSetup("/jobs/processor/multiple_update_availabilities/partial_success_before.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/multiple_update_availabilities/partial_success_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void partialSuccess() {
        mockMdsS3ClientDownload("jobs/processor/multiple_update_availabilities/multiple_update_availabilities.xlsx");
        processor.processPayload(createPayload(1L));
        verify(mdsS3Client).download(any(ResourceLocation.class), any(ContentConsumer.class));
    }

    @MethodSource
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DatabaseSetup("/jobs/processor/multiple_update_availabilities/success_before.xml")
    void validationError(String name, String filePath) {
        mockMdsS3ClientDownload("jobs/processor/multiple_update_availabilities/" + filePath);
        Assertions.assertThrows(
            NesuValidationException.class,
            () -> processor.processPayload(createPayload(1L))
        );
        verify(mdsS3Client).download(any(ResourceLocation.class), any(ContentConsumer.class));
    }

    private static Stream<Arguments> validationError() {
        return Stream.of(
            Arguments.of(
                "Ошибка валидации logisticPointId",
                "validation_logistic_point_id.xlsx"
            ),
            Arguments.of(
                "Ошибка валидации locationId",
                "validation_location_id.xlsx"
            ),
            Arguments.of(
                "Ошибка валидации shipmentType",
                "validation_shipment_type.xlsx"
            ),
            Arguments.of(
                "Ошибка валидации partnerType",
                "validation_partner_type.xlsx"
            )
        );
    }

    @Test
    @DisplayName("Ошибка обработки задачи")
    @DatabaseSetup("/jobs/processor/multiple_update_availabilities/fail_before.xml")
    @ExpectedDatabase(
        value = "/jobs/processor/multiple_update_availabilities/fail_after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failParseFile() {
        processor.processFinalFailure(
            createPayload(2L),
            new BadRequestException("IOException occurred while processing payload: test")
        );
    }

    public void mockMdsS3ClientDownload(String filepath) {
        when(mdsS3Client.download(any(ResourceLocation.class), any(ContentConsumer.class))).thenAnswer(
            invocation -> invocation.<StreamCopyContentConsumer<OutputStream>>getArgument(1)
                .consume(IntegrationTestUtils.inputStreamFromResource(filepath))
        );
    }

    private MultipleUpdateLogisticPointAvailabilitiesPayload createPayload(long taskId) {
        return new MultipleUpdateLogisticPointAvailabilitiesPayload(REQUEST_ID, taskId);
    }
}
