package ru.yandex.market.pvz.internal.controller.logistics;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.pvz.core.test.factory.TestOrderFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestReturnRequestFactory;
import ru.yandex.market.pvz.internal.BaseShallowTest;
import ru.yandex.market.pvz.internal.WebLayerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pvz.core.TestUtils.getFileContent;

/**
 * @author valeriashanti
 * @date 3/4/21
 */
@WebLayerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LogisticsReturnRequestControllerTest extends BaseShallowTest {

    private final TestPickupPointFactory pickupPointFactory;
    private final TestReturnRequestFactory returnRequestFactory;
    private final TestOrderFactory orderFactory;

    @Test
    @SneakyThrows
    void createReturnRequest() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build());
        var createDtoJson = String.format(
                getFileContent("return_request/return_request_create.json"),
                pickupPoint.getId(), order.getExternalId());

        mockMvc.perform(post("/logistics/return-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDtoJson))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent("return_request/return_request_create_response.json"), false)
                );
    }

    @Test
    @SneakyThrows
    void createReturnRequestWithoutReason() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var order = orderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                        .pickupPoint(pickupPoint)
                        .build());
        var createDtoJson = String.format(
                getFileContent("return_request/return_request_create_null_reason.json"),
                pickupPoint.getId(), order.getExternalId());

        mockMvc.perform(post("/logistics/return-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDtoJson))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent(
                                "return_request/return_request_create_response_null_reason.json"), false)
                );
    }

    @Test
    @SneakyThrows
    void createReturnWhenReturnAlreadyExist() {
        var returnRequest = returnRequestFactory.createReturnRequest();
        var createDtoJson = String.format(
                getFileContent("return_request/return_request_create_with_params.json"),
                returnRequest.getPickupPointId(), returnRequest.getReturnId(), returnRequest.getBarcode(),
                returnRequest.getOrderId());

        mockMvc.perform(post("/logistics/return-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDtoJson))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent("return_request/return_request_create_response.json"), false)
                );
    }

    @Test
    @SneakyThrows
    void tryToCreateReturnOnInvalidDto() {
        var pickupPoint = pickupPointFactory.createPickupPoint();
        var createDtoJson = String.format(
                getFileContent("return_request/return_request_create_with_params.json"),
                pickupPoint.getId(), "", "", "");

        mockMvc.perform(post("/logistics/return-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDtoJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SneakyThrows
    void cancelReturnRequest() {
        var returnRequest = returnRequestFactory.createReturnRequest();

        mockMvc.perform(patch("/logistics/return-request/cancel/" + returnRequest.getReturnId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent("return_request/return_request_cancel_response.json"), false)
                );
    }

    @Test
    @SneakyThrows
    void getReturnRequest() {
        var returnRequest = returnRequestFactory.createReturnRequest();

        mockMvc.perform(get("/logistics/return-request/" + returnRequest.getReturnId()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent("return_request/return_request_get_response.json"), false)
                );
    }

    @Test
    @SneakyThrows
    void getReturnRequests() {
        var returnRequest1 = returnRequestFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .params(TestReturnRequestFactory.ReturnRequestTestParams.builder().returnId("ret1").build())
                        .build()
        );
        var returnRequest2 = returnRequestFactory.createReturnRequest(
                TestReturnRequestFactory.CreateReturnRequestBuilder.builder()
                        .params(TestReturnRequestFactory.ReturnRequestTestParams.builder().returnId("ret2").build())
                        .build()
        );

        var createDtoJson = String.format(
                getFileContent("return_request/return_request_ids.json"),
                returnRequest1.getReturnId(),
                returnRequest2.getReturnId()
        );

        mockMvc.perform(put("/logistics/return-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createDtoJson))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content()
                        .json(getFileContent("return_request/return_requests_get_response.json"), false)
                );
    }

}
