package steps.orderSteps;

import java.time.Instant;

import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.changerequest.AbstractChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryDatesChangeRequestPayload;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryOptionChangeRequestPayload;

public class ChangeRequestSteps {

    private ChangeRequestSteps() {
    }

    public static ChangeRequest createChangeRequest(AbstractChangeRequestPayload payload) {
        return new ChangeRequest(
            1L,
            123L,
            payload,
            ChangeRequestStatus.PROCESSING,
            Instant.now(),
            "test",
            ClientRole.SYSTEM
        );
    }

    public static ChangeRequest deliveryOptionChangeRequest() {
        return createChangeRequest(new DeliveryOptionChangeRequestPayload());
    }

    public static ChangeRequest deliveryDatesChangeRequest() {
        return createChangeRequest(new DeliveryDatesChangeRequestPayload());
    }
}
