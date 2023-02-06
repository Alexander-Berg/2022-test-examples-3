package ru.yandex.market.logistics.lom.admin;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.embedded.OrderHistoryEventAuthor;
import ru.yandex.market.logistics.lom.jobs.model.MdsFileIdAuthorPayload;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.utils.TestUtils;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi;
import ru.yandex.market.logistics.util.client.tvm.client.TvmServiceTicket;
import ru.yandex.market.logistics.util.client.tvm.client.TvmTicketStatus;
import ru.yandex.market.logistics.util.client.tvm.client.TvmUserTicket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.SERVICE_HEADERS;
import static ru.yandex.market.logistics.lom.controller.order.TvmClientApiTestUtil.USER_HEADERS;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Загрузить файл и создать задачу на изменение опций доставки заказов")
class AdminMultipleFileOperationTest extends AbstractContextualTest {
    private static final long USER_UID = 1010L;
    private static final int SERVICE_ID = 1011;
    private static final String TEST_URL = "http://localhost:8080/data.xlsx";
    private static final byte[] CONTENT = "test-content".getBytes(StandardCharsets.UTF_8);
    private static final MdsFileIdAuthorPayload PAYLOAD = PayloadFactory.mdsFileIdAuthorPayload(
        1,
        new OrderHistoryEventAuthor().setTvmServiceId((long) SERVICE_ID).setYandexUid(BigDecimal.valueOf(USER_UID)),
        "1",
        1L
    );

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    private TvmClientApi tvmClientApi;

    @BeforeEach
    void setUp() throws MalformedURLException {
        when(mdsS3Client.getUrl(any())).thenReturn(new URL(TEST_URL));
        when(tvmClientApi.checkUserTicket("test-user-ticket"))
            .thenReturn(new TvmUserTicket(USER_UID, TvmTicketStatus.OK));
        when(tvmClientApi.checkServiceTicket("test-service-ticket"))
            .thenReturn(new TvmServiceTicket(SERVICE_ID, TvmTicketStatus.OK, ""));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    @DisplayName("Файл загружен, задача создана")
    void createSuccess(QueueType expectedQueueType, String url) throws Exception {
        uploadFile(url)
            .andExpect(status().isOk())
            .andExpect(content().string("1"));
        queueTaskChecker.assertQueueTaskCreated(expectedQueueType, PAYLOAD);
        mockMvc.perform(get("/files/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/order/after/uploaded-file.json"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("arguments")
    @DisplayName("Ошибка загрузки файла")
    void errorUploadingFile(QueueType expectedQueueType, String url) throws Exception {
        doThrow(new IllegalArgumentException("oops")).when(mdsS3Client).upload(any(), any());

        uploadFile(url)
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("oops"));

        queueTaskChecker.assertNoQueueTasksCreated();
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of(
                QueueType.MULTIPLE_CHANGE_ORDER_SHIPMENT_DATES_VIA_FILE,
                "/admin/orders/create-change-shipment-dates-task"
            ),
            Arguments.of(
                QueueType.MULTIPLE_CHANGE_DELIVERY_OPTIONS_VIA_FILE,
                "/admin/orders/create-change-delivery-option-requests-task"
            ),
            Arguments.of(
                QueueType.MULTIPLE_RETRY_BUSINESS_PROCESS_STATES_VIA_FILE,
                "/admin/business-processes/create-multiple-retry-task"
            ),
            Arguments.of(
                QueueType.MULTIPLE_CHANGE_ORDER_RETURN_SEGMENT_VIA_FILE,
                "/admin/orders/create-change-orders-return-segments-task"
            ),
            Arguments.of(
                QueueType.MULTIPLE_RECALL_COURIER_VIA_FILE,
                "/admin/orders/recall-courier-from-file"
            )
        );
    }

    @Nonnull
    private ResultActions uploadFile(String url) throws Exception {
        return mockMvc.perform(
            multipart(url)
                .file(createMultipartFile())
                .headers(TestUtils.toHttpHeaders(USER_HEADERS))
                .headers(TestUtils.toHttpHeaders(SERVICE_HEADERS))
        );
    }

    @Nonnull
    private MockMultipartFile createMultipartFile() {
        return new MockMultipartFile(
            "request",
            "data.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            CONTENT
        );
    }
}
