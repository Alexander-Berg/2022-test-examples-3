package ru.yandex.market.logistics.lom.controller.order.processing;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.delivery.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.properties.ClientRequestMeta;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.jobs.consumer.GetCourierConsumer;
import ru.yandex.market.logistics.lom.jobs.model.QueueType;
import ru.yandex.market.logistics.lom.jobs.model.WaybillSegmentIdPayload;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lom.utils.jobs.TaskFactory;
import ru.yandex.money.common.dbqueue.api.Task;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
@DatabaseSetup({
    "/controller/order/get_courier/before/order.xml",
    "/controller/order/get_courier/before/waybill_segment.xml",
    "/controller/order/get_courier/before/process_get_courier.xml"
})
@DisplayName("Обработчик задач очереди PROCESS_GET_COURIER")
class GetCourierProcessorTest extends AbstractContextualTest {
    private final Task<WaybillSegmentIdPayload> task = TaskFactory.createTask(
        QueueType.PROCESS_GET_COURIER,
        PayloadFactory.createWaybillSegmentIdPayload(4, "1001")
    );

    @Autowired
    private GetCourierConsumer getCourierConsumer;

    @Autowired
    private DeliveryClient deliveryClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(deliveryClient);
    }

    @Test
    @DisplayName("Успешная обработка задачи")
    @ExpectedDatabase(
        value = "/controller/order/get_courier/after/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessing() {
        getCourierConsumer.execute(task);

        verify(deliveryClient).getCourier(
            ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-4").build(),
            new Partner(1000004L),
            new ClientRequestMeta("1001")
        );

        callAsyncResponseMethod(
            "/orders/ds/getCourier/success",
            "controller/order/get_courier/request/get_courier_success.json"
        );
    }

    @Test
    @DisplayName("Успешная обработка задачи, ответ без кодов не затирает старые значения")
    @DatabaseSetup(
        value = "/controller/order/get_courier/before/waybill_segment_transfer_codes.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/get_courier/after/success_transfer_codes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingNoTransferCodes() {
        getCourierConsumer.execute(task);

        verify(deliveryClient).getCourier(
            ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-4").build(),
            new Partner(1000004L),
            new ClientRequestMeta("1001")
        );

        callAsyncResponseMethod(
            "/orders/ds/getCourier/success",
            "controller/order/get_courier/request/get_courier_success_transfer_codes.json"
        );
    }

    @Test
    @DisplayName("Успешная обработка задачи, ответ без кодов не падает")
    @DatabaseSetup(
        value = "/controller/order/get_courier/before/waybill_segment_transfer_codes.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/get_courier/after/success_transfer_codes.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingNoTransferCodesNode() {
        getCourierConsumer.execute(task);

        verify(deliveryClient).getCourier(
            ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-4").build(),
            new Partner(1000004L),
            new ClientRequestMeta("1001")
        );

        callAsyncResponseMethod(
            "/orders/ds/getCourier/success",
            "controller/order/get_courier/request/get_courier_success_codes_null.json"
        );
    }

    @Test
    @DisplayName("Успешная обработка задачи, склеился description для машины из model и color")
    @DatabaseSetup(
        value = "/controller/order/get_courier/before/waybill_segment_express_tag.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/get_courier/after/success_with_car_built_description.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void successProcessingCourierCarDescription() {
        getCourierConsumer.execute(task);

        verify(deliveryClient).getCourier(
            ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-4").build(),
            new Partner(1000004L),
            new ClientRequestMeta("1001")
        );

        callAsyncResponseMethod(
            "/orders/ds/getCourier/success",
            "controller/order/get_courier/request/get_courier_success_built_description.json"
        );
    }

    @Test
    @DisplayName("Неуспешный асинхронный ответ")
    @ExpectedDatabase(
        value = "/controller/order/get_courier/after/error_response.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @SneakyThrows
    void errorResponseFromLgw() {
        getCourierConsumer.execute(task);

        verify(deliveryClient).getCourier(
            ResourceId.builder().setYandexId("1001").setPartnerId("test-external-id-4").build(),
            new Partner(1000004L),
            new ClientRequestMeta("1001")
        );

        callAsyncResponseMethod(
            "/orders/ds/getCourier/error",
            "controller/order/get_courier/request/get_courier_error.json"
        );
    }

    @Test
    @DisplayName("Неверный ApiType у сегмента - бизнес-процесс переведется в статус UNPROCESSED")
    @DatabaseSetup(
        value = "/controller/order/get_courier/before/waybill_segment_fulfillment.xml",
        type = DatabaseOperation.UPDATE
    )
    @ExpectedDatabase(
        value = "/controller/order/get_courier/after/unprocessed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void wrongSegmentApiTypeTaskWillUnprocessed() {
        getCourierConsumer.execute(task);
    }

    @Test
    @DisplayName("Несуществующий partnerId при вызове ручки асинхронного ответа")
    @SneakyThrows
    void wrongPartnerId() {
        mockMvc.perform(
            put("/orders/ds/getCourier/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/get_courier/request/get_courier_wrong_partner.json"))
        )
            .andExpect(status().isInternalServerError())
            .andExpect(jsonContent("controller/order/get_courier/response/get_courier_wrong_partner.json"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Корректная обработка, если в ответе отсутствует courier")
    void courierIsNull() {
        mockMvc.perform(
            put("/orders/ds/getCourier/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent("controller/order/get_courier/request/courier_is_null.json"))
        )
            .andExpect(status().isOk());
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource
    @SneakyThrows
    @DisplayName("Ошибки валидации в запросе на успешное получение информации о курьере")
    void validationErrors(
        String displayName,
        String requestBodyPath,
        String expectedErrorMessage
    ) {
        mockMvc.perform(
            put("/orders/ds/getCourier/success")
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestBodyPath))
        )
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage(expectedErrorMessage));
    }

    @Nonnull
    private static Stream<Arguments> validationErrors() {
        return Stream.of(
            Arguments.of(
                "Не заполнен идентификатор заказа",
                "controller/order/get_courier/request/barcode_is_null.json",
                "Following validation errors occurred:\nField: 'barcode', message: 'must not be null'"
            ),
            Arguments.of(
                "Не заполнен идентификатор партнёра",
                "controller/order/get_courier/request/partner_id_is_null.json",
                "Following validation errors occurred:\nField: 'partnerId', message: 'must not be null'"
            )
        );
    }

    private void callAsyncResponseMethod(String path, String requestPath) throws Exception {
        mockMvc.perform(
            put(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content(extractFileContent(requestPath))
        )
            .andExpect(status().isOk())
            .andExpect(noContent());
    }
}
