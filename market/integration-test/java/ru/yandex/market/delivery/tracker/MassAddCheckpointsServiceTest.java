package ru.yandex.market.delivery.tracker;

import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.delivery.tracker.domain.dto.MassAddCheckpointsDto;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;
import ru.yandex.market.delivery.tracker.service.checkpoint.MassAddCheckpointsService;
import ru.yandex.market.delivery.tracker.service.tracking.CheckpointsProcessingService;
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SpyBean(CheckpointsProcessingService.class)
@DatabaseSetup("/database/states/tracks_to_mass_add_checkpoints.xml")
public class MassAddCheckpointsServiceTest extends AbstractContextualTest {

    @Autowired
    private MassAddCheckpointsService massAddCheckpointsService;

    @Autowired
    private CheckpointsProcessingService checkpointsProcessingService;

    @Autowired
    private ResourceLocationFactory resourceLocationFactory;

    @Autowired
    private MdsS3Client mdsS3Client;

    @Captor
    private ArgumentCaptor<List<DeliveryTrackCheckpoint>> deliveryTrackCheckpointCaptor;

    @AfterEach
    public void tearDown() {
        verify(mdsS3Client).download(any(ResourceLocation.class), any(ContentConsumer.class));
        verifyNoMoreInteractions(mdsS3Client);
    }

    @Test
    void downloadFailure() {
        RuntimeException e = new RuntimeException("Download failure");
        mockMdsS3Download(whenDownloaded -> whenDownloaded.thenThrow(e));

        assertions()
            .assertThatThrownBy(() -> massAddCheckpointsService.addOrderCheckpoints(new MassAddCheckpointsDto(1L)))
            .isEqualTo(e);
    }

    @Test
    void emptyFile() {
        mockMdsS3Download(whenDownloaded -> whenDownloaded.thenAnswer(
            invocation -> invocation.<StreamCopyContentConsumer<OutputStream>>getArgument(1).consume(
                IntegrationTestUtils.inputStreamFromResource("spreadsheets/mass_add_checkpoints_empty.xlsx")
            )
        ));

        massAddCheckpointsService.addOrderCheckpoints(new MassAddCheckpointsDto(1L));
        verify(checkpointsProcessingService, never()).addNewFakeCheckpoints(anyList());
    }

    @ParameterizedTest
    @MethodSource
    void validation(
        @SuppressWarnings("unused") String displayName,
        String filePath,
        List<Pair<Long, Integer>> trackIdsAndStatuses
    ) {
        mockMdsS3Download(whenDownloaded -> whenDownloaded.thenAnswer(
            invocation -> invocation.<StreamCopyContentConsumer<OutputStream>>getArgument(1).consume(
                IntegrationTestUtils.inputStreamFromResource(filePath)
            )
        ));

        massAddCheckpointsService.addOrderCheckpoints(new MassAddCheckpointsDto(1L));
        verify(checkpointsProcessingService).addNewFakeCheckpoints(deliveryTrackCheckpointCaptor.capture());

        List<DeliveryTrackCheckpoint> deliveryCheckpoints = deliveryTrackCheckpointCaptor.getValue();
        assertions().assertThat(deliveryCheckpoints).hasSameSizeAs(trackIdsAndStatuses);
        IntStream.range(0, deliveryCheckpoints.size()).forEach(
            i -> {
                DeliveryTrackCheckpoint checkpoint = deliveryCheckpoints.get(i);
                Pair<Long, Integer> trackIdAndStatus = trackIdsAndStatuses.get(i);
                assertions.assertThat(checkpoint.getTrackId()).isEqualTo(trackIdAndStatus.getFirst());
                assertions.assertThat(checkpoint.getDeliveryCheckpointStatus().getId())
                    .isEqualTo(trackIdAndStatus.getSecond());
            }
        );
    }

    @Nonnull
    private static Stream<Arguments> validation() {
        return Stream.of(
            Arguments.of(
                "Add single checkpoint",
                "spreadsheets/mass_add_checkpoints_single.xlsx",
                List.of(
                    Pair.of(0L, 40)
                )
            ),
            Arguments.of(
                "Add multiple checkpoints",
                "spreadsheets/mass_add_checkpoints_multiple.xlsx",
                List.of(
                    Pair.of(0L, 40),
                    Pair.of(1L, 50)
                )
            ),
            Arguments.of(
                "Checkpoint not found by order_id",
                "spreadsheets/mass_add_checkpoints_not_found_by_order_id.xlsx",
                List.of(
                    Pair.of(1L, 50)
                )
            ),
            Arguments.of(
                "Checkpoint not found by delivery_service_id",
                "spreadsheets/mass_add_checkpoints_not_found_by_delivery_service_id.xlsx",
                List.of(
                    Pair.of(1L, 50)
                )
            ),
            Arguments.of(
                "Missing fields",
                "spreadsheets/mass_add_checkpoints_missing_fields.xlsx",
                List.of(
                    Pair.of(0L, 40)
                )
            )
        );
    }

    @ParameterizedTest
    @MethodSource
    @ExpectedDatabase(
        value = "/database/expected/tracks_to_mass_add_checkpoints.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void checkpointsAdded(
        @SuppressWarnings("unused") String displayName,
        String filePath
    ) {
        mockMdsS3Download(whenDownloaded -> whenDownloaded.thenAnswer(
            invocation -> invocation.<StreamCopyContentConsumer<OutputStream>>getArgument(1).consume(
                IntegrationTestUtils.inputStreamFromResource(filePath)
            )
        ));

        massAddCheckpointsService.addOrderCheckpoints(new MassAddCheckpointsDto(1L));
    }

    @Nonnull
    private static Stream<Arguments> checkpointsAdded() {
        return Stream.of(
            Arguments.of(
                "All created",
                "spreadsheets/mass_add_checkpoints_multiple.xlsx"
            ),
            Arguments.of(
                "Skipped one with invalid delivery status",
                "spreadsheets/mass_add_checkpoints_invalid_delivery_status.xlsx"
            )
        );
    }

    private void mockMdsS3Download(Consumer<OngoingStubbing<Object>> whenDownloaded) {
        ResourceLocation resourceLocation = ResourceLocation.create("delivery-tracker-testing", "1");
        when(resourceLocationFactory.createLocation(eq("1"))).thenReturn(resourceLocation);
        whenDownloaded.accept(when(mdsS3Client.download(eq(resourceLocation), any(ContentConsumer.class))));
    }
}
