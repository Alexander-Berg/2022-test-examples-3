package ru.yandex.market.delivery.mdbapp.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.request.ParcelPatchRequest;
import ru.yandex.market.delivery.mdbapp.MockContextualTest;
import ru.yandex.market.delivery.mdbapp.api.order.scenario.OrderLabelWasSetScenario;
import ru.yandex.market.delivery.mdbapp.components.health.HealthManager;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterServiceClient;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.CompoundOrderId;
import ru.yandex.market.delivery.mdbapp.scheduled.HealthScheduler;
import ru.yandex.market.delivery.mdbapp.scheduled.checkouter.order.OrderHistoryEventScheduler;
import ru.yandex.market.delivery.mdbclient.model.delivery.ResourceId;
import ru.yandex.market.delivery.mdbclient.model.request.SetOrderDeliveryShipmentLabel;
import ru.yandex.market.logistic.pechkin.client.PechkinHttpClient;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.mbi.api.client.MbiApiClient;
import ru.yandex.market.sc.internal.client.ScIntClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static steps.orderSteps.OrderSteps.getFilledOrder;

@RunWith(Parameterized.class)
@MockBean({
    CheckouterAPI.class,
    MbiApiClient.class,
    OrderHistoryEventScheduler.class,
    HealthScheduler.class,
    LMSClient.class,
    PechkinHttpClient.class,
    ScIntClient.class,
})
@DirtiesContext
public class OrderLabelTest extends MockContextualTest {

    private static final long ORDER_ID = 1L;
    private static final String LABEL_URL = "url";

    @Autowired
    @Qualifier("commonJsonMapper")
    private ObjectMapper objectMapper;

    @MockBean
    private HealthManager healthManager;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CheckouterServiceClient checkouterServiceClient;

    private ArgumentCaptor<ParcelPatchRequest> requestCaptor;

    @Parameterized.Parameter
    public CompoundOrderId compoundOrderId;

    @Parameterized.Parameters
    public static Collection<CompoundOrderId> parameters() {
        return Arrays.asList(new CompoundOrderId(), new CompoundOrderId(123L, null));
    }

    @Before
    public void beforeTest() {
        requestCaptor = ArgumentCaptor.forClass(ParcelPatchRequest.class);
        when(healthManager.isHealthyEnough()).thenReturn(true);
    }

    @Test
    public void orderLabelWasSetSuccessTest() throws Exception {
        OrderLabelWasSetScenario scenario = createOrderLabelWasSetScenario(compoundOrderId);

        when(checkouterServiceClient.getFirstParcel(anyLong())).thenReturn(scenario.getOrderParcels().get(0));
        when(checkouterServiceClient.getOrderParcel(anyLong(), any())).thenReturn(scenario.getOrderParcels().get(0));

        String jsn = toJson(scenario.getSetOrderDeliveryShipmentLabel());
        mvc.perform(post("/orders/" + ORDER_ID + "/label")
            .accept(MediaType.APPLICATION_JSON)
            .content(jsn)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());

        verify(checkouterServiceClient, times(1))
            .updateParcel(eq(ORDER_ID), eq(scenario.getOrderParcels().get(0).getId()), requestCaptor.capture());

        softly.assertThat(requestCaptor.getValue())
            .as("ParcelPatchRequest label should be correct")
            .extracting(ParcelPatchRequest::getLabelUrl)
            .isEqualTo(LABEL_URL);
    }

    private OrderLabelWasSetScenario createOrderLabelWasSetScenario(CompoundOrderId compoundOrderId) {
        ResourceId parcelId = Optional.ofNullable(compoundOrderId.getParcelId())
            .map(id -> new ResourceId(String.valueOf(id), null)).orElse(null);

        SetOrderDeliveryShipmentLabel label = new SetOrderDeliveryShipmentLabel(ORDER_ID, parcelId, LABEL_URL);

        Order order = getFilledOrder(ORDER_ID);

        Parcel parcel = new Parcel();
        parcel.setId(order.getDelivery().getParcels().get(0).getId());

        return new OrderLabelWasSetScenario(label, order, parcel);
    }

    private String toJson(Object o) throws JsonProcessingException {
        return objectMapper.writeValueAsString(o);
    }
}
