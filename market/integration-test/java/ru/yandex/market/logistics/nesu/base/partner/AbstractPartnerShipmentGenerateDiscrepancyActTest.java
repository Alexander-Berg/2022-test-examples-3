package ru.yandex.market.logistics.nesu.base.partner;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.common.mds.s3.client.content.ContentConsumer;
import ru.yandex.market.common.mds.s3.client.content.consumer.StreamCopyContentConsumer;
import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.logistics.nesu.model.TMFactory;
import ru.yandex.market.logistics4shops.client.model.MdsFilePath;
import ru.yandex.market.logistics4shops.client.model.Outbound;
import ru.yandex.market.logistics4shops.client.model.OutboundsListDto;
import ru.yandex.market.logistics4shops.client.model.OutboundsSearchRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/partner-shipment/common.xml")
public abstract class AbstractPartnerShipmentGenerateDiscrepancyActTest extends AbstractPartnerShipmentTest {

    private static final String BUCKET = "bucket";
    private static final String FILENAME = "filename";
    private static final byte[] RESPONSE = new byte[10];

    @BeforeEach
    void setup() {
        when(transportManagerClient.getTransportation(TMFactory.SHIPMENT_ID)).thenReturn(TMFactory.transportation(
            TMFactory.defaultOutbound().build(),
            TMFactory.defaultMovement().build()
        ));
        doAnswer(invocation -> {
            var consumer = invocation.getArgument(1);
            var inputStream = new ByteArrayInputStream(RESPONSE);
            ((StreamCopyContentConsumer<OutputStream>) consumer).consume(inputStream);
            return invocation;
        }).when(mdsS3Client).download(any(ResourceLocation.class), any(ContentConsumer.class));
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(mdsS3Client, outboundApi, transportManagerClient);
    }

    @Test
    @DisplayName("Успешная генерация акта расхождений")
    void success() throws Exception {
        var request = createRequest();
        when(outboundApi.searchOutbounds(request)).thenReturn(new OutboundsListDto().outbounds(List.of(createOutbound(
            true,
            createFilePath(BUCKET, FILENAME)
        ))));

        generateDiscrepancyAct()
            .andExpect(status().isOk())
            .andExpect(content().contentType(XLSX_MIME_TYPE))
            .andExpect(content().bytes(RESPONSE));

        verify(outboundApi).searchOutbounds(eq(request));
        verify(mdsS3Client).download(any(ResourceLocation.class), any(ContentConsumer.class));
        verify(transportManagerClient).getTransportation(TMFactory.SHIPMENT_ID);
    }

    @Nonnull
    private static Stream<Arguments> fail() {
        return Stream.of(
            Arguments.of(
                "Акт расхождения не сформирован",
                createOutbound(false, null),
                status().isBadRequest(),
                "Discrepancy act are not generated"
            ),
            Arguments.of(
                "Отгрузка не найдена",
                null,
                status().isNotFound(),
                "Failed to find [TRANSPORTATION_OUTBOUND] with id [TMU300]"
            ),
            Arguments.of(
                "MdsFilePath null",
                createOutbound(true, null),
                status().isBadRequest(),
                "MdsFilePath must be not null"
            ),
            Arguments.of(
                "Bucket null",
                createOutbound(true, createFilePath(null, FILENAME)),
                status().isBadRequest(),
                "Bucket must be not null"
            ),
            Arguments.of(
                "Filename null",
                createOutbound(true, createFilePath(BUCKET, null)),
                status().isBadRequest(),
                "Filename must be not null"
            )
        );
    }

    @MethodSource("fail")
    @DisplayName("Ошибка генерации акта расхождений")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @SuppressWarnings("unused")
    void failTest(String name, @Nullable Outbound mockOutbound, ResultMatcher status, String message) throws Exception {
        var request = createRequest();
        when(outboundApi.searchOutbounds(request)).thenReturn(
            new OutboundsListDto().outbounds(Optional.ofNullable(mockOutbound).map(List::of).orElse(List.of()))
        );

        generateDiscrepancyAct()
            .andExpect(status)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(errorMessage(message));

        verify(outboundApi).searchOutbounds(eq(request));
        verify(transportManagerClient).getTransportation(TMFactory.SHIPMENT_ID);
    }

    @Nonnull
    private OutboundsSearchRequest createRequest() {
        return new OutboundsSearchRequest().yandexIds(List.of(TMFactory.outboundId()));
    }

    @Nonnull
    private static Outbound createOutbound(
        boolean isDiscrepancyReady,
        @Nullable MdsFilePath filePath
    ) {
        var outbound = new Outbound();
        outbound.setDiscrepancyActIsReady(isDiscrepancyReady);
        outbound.setDiscrepancyActPath(filePath);
        return outbound;
    }

    @Nonnull
    private static MdsFilePath createFilePath(@Nullable String bucket, @Nullable String filename) {
        return new MdsFilePath().bucket(bucket).filename(filename);
    }

    @Nonnull
    private ResultActions generateDiscrepancyAct() throws Exception {
        return mockMvc.perform(get(url(TMFactory.SHIPMENT_ID))
            .param("userId", "-1")
            .param("shopId", String.valueOf(SHOP_ID))
        );
    }

    @Nonnull
    protected abstract String url(long shipmentId);
}
