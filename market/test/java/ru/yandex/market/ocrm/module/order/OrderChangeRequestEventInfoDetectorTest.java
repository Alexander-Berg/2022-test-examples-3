package ru.yandex.market.ocrm.module.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryDatesChangeRequestPayload;
import ru.yandex.market.jmf.catalog.items.CatalogItemService;
import ru.yandex.market.ocrm.module.order.domain.CheckouterClientRole;
import ru.yandex.market.ocrm.module.order.domain.OrderHistoryEventReason;
import ru.yandex.market.ocrm.module.order.impl.change.ChangeRequestEventInfo;
import ru.yandex.market.ocrm.module.order.impl.change.OrderChangeRequestEventInfoDetector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.eq;

public class OrderChangeRequestEventInfoDetectorTest {


    private OrderChangeRequestEventInfoDetector roleDetector;

    @Mock
    private CatalogItemService catalogItemService;

    @Captor
    private ArgumentCaptor<String> checkouterClientRoleCodeCaptor;


    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        roleDetector = new OrderChangeRequestEventInfoDetector(catalogItemService);
    }

    @Test
    public void shippingDelayed() {
        final HistoryEventReason reason = HistoryEventReason.SHIPPING_DELAYED;

        assertInfo(CheckouterClientRole.WAREHOUSE,
                detectRoleInfo(
                        ClientRole.SYSTEM,
                        reason));

        for (ClientRole role : ClientRole.values()) {
            if (role != ClientRole.SYSTEM) {
                assertDefaultInfo(detectRoleInfo(role, reason));
            }
        }
    }

    @Test
    public void deliveryServiceDelayedAndSystemRole__waitDeliveryServiceRole() {
        final HistoryEventReason reason = HistoryEventReason.DELIVERY_SERVICE_DELAYED;

        assertInfo(CheckouterClientRole.DELIVERY_SERVICE,
                detectRoleInfo(
                        ClientRole.SYSTEM,
                        reason));

        for (ClientRole role : ClientRole.values()) {
            if (role != ClientRole.SYSTEM) {
                assertDefaultInfo(detectRoleInfo(role, reason));
            }
        }
    }

    @Test
    public void externalConditionsAndSystemRole__waitDeliveryServiceRole() {
        final HistoryEventReason reason = HistoryEventReason.DELAYED_DUE_EXTERNAL_CONDITIONS;

        assertInfo(CheckouterClientRole.DELIVERY_SERVICE,
                detectRoleInfo(
                        ClientRole.SYSTEM,
                        reason));

        for (ClientRole role : ClientRole.values()) {
            if (role != ClientRole.SYSTEM) {
                assertDefaultInfo(detectRoleInfo(role, reason));
            }
        }
    }

    @Test
    public void userMovedDeliveryDates() {
        assertInfo(CheckouterClientRole.DELIVERY_SERVICE,
                detectRoleInfo(
                        ClientRole.SYSTEM,
                        HistoryEventReason.USER_MOVED_DELIVERY_DATES));
        assertInfo(CheckouterClientRole.CLIENT,
                detectRoleInfo(
                        ClientRole.USER,
                        HistoryEventReason.USER_MOVED_DELIVERY_DATES));
        assertInfo(CheckouterClientRole.CALL_CENTER,
                detectRoleInfo(
                        ClientRole.CALL_CENTER_OPERATOR,
                        HistoryEventReason.USER_MOVED_DELIVERY_DATES));
    }

    private void assertInfo(String expectedRoleCode, ChangeRequestEventInfo ignored) {
        Mockito.verify(catalogItemService, Mockito.atLeastOnce())
                .get(eq(OrderHistoryEventReason.FQN), checkouterClientRoleCodeCaptor.capture());

        Mockito.verify(catalogItemService, Mockito.atLeastOnce())
                .get(eq(CheckouterClientRole.FQN), checkouterClientRoleCodeCaptor.capture());
        List<String> captured = checkouterClientRoleCodeCaptor.getAllValues();
        Mockito.reset(catalogItemService);

        assertThat(captured, hasItem(expectedRoleCode));
    }

    private void assertDefaultInfo(ChangeRequestEventInfo ignored) {
        Mockito.verify(catalogItemService, Mockito.times(0))
                .get(eq(OrderHistoryEventReason.FQN), checkouterClientRoleCodeCaptor.capture());

        Mockito.reset(catalogItemService);
    }

    private ChangeRequest getDeliveryChangeRequest(ClientRole clientRole,
                                                   HistoryEventReason reason) {
        return getTestRequest(clientRole,
                () -> {
                    final DeliveryDatesChangeRequestPayload payload =
                            new DeliveryDatesChangeRequestPayload();
                    payload.setReason(reason);
                    return payload;
                });
    }

    private ChangeRequest getTestRequest(
            ClientRole clientRole,
            Supplier<AbstractChangeRequestPayload> payloadProvider) {
        final long id = 1L;
        final long orderId = 1L;
        return new ChangeRequest(
                id,
                orderId,
                payloadProvider.get(),
                ChangeRequestStatus.APPLIED,
                OffsetDateTime.now().toInstant(),
                "message",
                clientRole);
    }

    private ChangeRequestEventInfo detectRoleInfo(ClientRole clientRole,
                                                  HistoryEventReason reason) {
        return roleDetector.getInfo(
                getDeliveryChangeRequest(
                        clientRole,
                        reason));
    }

}
