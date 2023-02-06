package ru.yandex.market.delivery.mdbapp.api;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.testutils.ResourceUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UpdateRequestTest extends MockContextualTest {

    private static final long ORDER_ID = 10L;
    private static final long UPDATE_REQUEST_ID = 111L;
    private static final String ERROR_MESSAGE = "error message";

    @Autowired
    private HealthManager healthManager;

    @MockBean
    private CheckouterAPI checkouterAPI;

    @Autowired
    private MockMvc mockMvc;

    @Before
    public void beforeTest() {
        when(healthManager.isHealthyEnough()).thenReturn(true);
    }

    @Test
    public void testUpdateDeliveryDateSuccess() throws Exception {
        performApplyDeliveryDate();

        verifyCheckouter(ChangeRequestStatus.APPLIED);
    }

    @Test
    public void testUpdateDeliveryDateError() throws Exception {
        performRejectDeliveryDate(status().isOk());

        verifyCheckouter(ChangeRequestStatus.REJECTED);
    }

    @Test
    public void testUpdateDeliveryDateIncorrectStatusFailed() throws Exception {
        mockCheckouterRespondException("some error code");

        performRejectDeliveryDate(status().is4xxClientError());

        verifyCheckouter(ChangeRequestStatus.REJECTED);
    }

    private void performRejectDeliveryDate(ResultMatcher expectedStatus) throws Exception {
        performByOrder("data/controller/request/update-delivery-date-error.json", "lgwUpdateDeliveryDateError")
            .andExpect(expectedStatus);
    }

    private void performApplyDeliveryDate() throws Exception {
        performByOrder("data/controller/request/update-delivery-date-success.json", "lgwUpdateDeliveryDateSuccess")
            .andExpect(status().isOk());
    }

    private ResultActions performByOrder(String contentPath, String subOrderUrl) throws Exception {
        return mockMvc.perform(
            post("/orders/" + ORDER_ID + "/" + subOrderUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .content(ResourceUtils.getFileContent(contentPath))
        );
    }

    private void mockCheckouterRespondException(String errorCode) {
        when(checkouterAPI.updateChangeRequestStatus(anyLong(), anyLong(), any(), any(), any()))
            .thenThrow(new ErrorCodeException(
                    errorCode,
                    "exception",
                    HttpStatus.BAD_REQUEST.value()
                )
            );
    }

    private void verifyCheckouter(ChangeRequestStatus status) {
        ArgumentCaptor<ChangeRequestPatchRequest> patchRequestArgumentCaptor = ArgumentCaptor.forClass(
            ChangeRequestPatchRequest.class);

        verify(checkouterAPI).updateChangeRequestStatus(
            eq(ORDER_ID),
            eq(UPDATE_REQUEST_ID),
            any(),
            any(),
            patchRequestArgumentCaptor.capture()
        );

        softly.assertThat(patchRequestArgumentCaptor.getValue().getStatus())
            .isEqualTo(status);

        if (status == ChangeRequestStatus.REJECTED) {
            softly.assertThat(patchRequestArgumentCaptor.getValue().getMessage())
                .isEqualTo(ERROR_MESSAGE);
        }
    }
}
