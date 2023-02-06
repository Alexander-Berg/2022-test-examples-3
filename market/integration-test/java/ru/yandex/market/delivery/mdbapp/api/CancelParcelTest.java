package ru.yandex.market.delivery.mdbapp.api;

import java.text.MessageFormat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.parcel.CancellationRequestStatus;
import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.service.crm.client.OrderCommands;
import ru.yandex.market.delivery.mdbclient.model.request.CancelStatus;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

public class CancelParcelTest extends AllMockContextualTest {

    private static final long ORDER_ID = 1L;
    private static final long PARCEL_ID = 2L;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private HealthManager healthManager;

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private OrderCommands orderCommands;

    @Before
    public void beforeTest() {
        when(healthManager.isHealthyEnough()).thenReturn(true);
    }

    @Test
    public void cancelSuccessDsOrderHandler() throws Exception {
        doReturn(null).when(checkouterAPI).updateParcelCancellationRequestStatus(
            Mockito.eq(ORDER_ID),
            Mockito.eq(PARCEL_ID),
            Mockito.eq(CancellationRequestStatus.CONFIRMED),
            Mockito.eq(ClientRole.SYSTEM),
            Mockito.any()
        );

        performRequestWithStatus(makeDsCancelUrl(), CancelStatus.SUCCESS);

        Mockito.verify(checkouterAPI).updateParcelCancellationRequestStatus(
            Mockito.eq(ORDER_ID),
            Mockito.eq(PARCEL_ID),
            Mockito.eq(CancellationRequestStatus.CONFIRMED),
            Mockito.eq(ClientRole.SYSTEM),
            Mockito.any()
        );
        Mockito.verify(orderCommands, Mockito.never()).cancelOrder(Mockito.anyLong());
    }

    @Test
    public void cancelErrorDsOrderHandler() throws Exception {
        doReturn(null).when(checkouterAPI).updateParcelCancellationRequestStatus(
            Mockito.eq(ORDER_ID),
            Mockito.eq(PARCEL_ID),
            Mockito.eq(CancellationRequestStatus.REJECTED),
            Mockito.eq(ClientRole.SYSTEM),
            Mockito.any()
        );

        performRequestWithStatus(makeDsCancelUrl(), CancelStatus.FAIL);

        Mockito.verify(checkouterAPI).updateParcelCancellationRequestStatus(
            Mockito.eq(ORDER_ID),
            Mockito.eq(PARCEL_ID),
            Mockito.eq(CancellationRequestStatus.REJECTED),
            Mockito.eq(ClientRole.SYSTEM),
            Mockito.any()
        );

        Mockito.verify(orderCommands).cancelOrder(Mockito.anyLong());
    }

    @Test
    public void cancelSuccessFfOrderHandler() throws Exception {
        performRequestWithStatus(makeFfCancelUrl(), CancelStatus.SUCCESS);
        performRequestWithStatus(makeFfCancelUrl(), CancelStatus.FAIL);
        Mockito.verifyZeroInteractions(checkouterAPI);
        Mockito.verifyZeroInteractions(orderCommands);
    }

    private void performRequestWithStatus(String url, CancelStatus cancelStatus) throws Exception {
        mvc.perform(
                MockMvcRequestBuilders.post(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(MessageFormat.format("\"{0}\"", cancelStatus.toString()))
            )
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("OK;"));
    }

    private String makeDsCancelUrl() {
        return String.format("/orders/%d/parcels/%d/cancel", ORDER_ID, PARCEL_ID);
    }

    private String makeFfCancelUrl() {
        return String.format("/orders/%d/cancelFulfillment", ORDER_ID);
    }
}
