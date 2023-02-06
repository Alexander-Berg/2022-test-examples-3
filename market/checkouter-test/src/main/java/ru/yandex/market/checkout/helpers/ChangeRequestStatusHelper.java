package ru.yandex.market.checkout.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestPatchRequest;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

@WebTestHelper
public class ChangeRequestStatusHelper extends MockMvcAware {

    @Autowired
    private CheckouterAPI client;

    @Autowired
    public ChangeRequestStatusHelper(WebApplicationContext webApplicationContext,
                                     TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public void proceedToStatus(Order order, ChangeRequest changeRequest, ChangeRequestStatus targetStatus) {
        ChangeRequestStatus currentStatus = changeRequest.getStatus();
        while (currentStatus != targetStatus) {
            ChangeRequestStatus nextStatus = nextStatus(currentStatus);
            if (nextStatus == null) {
                throw new IllegalStateException("Cannot proceed to status " + targetStatus + " from status " +
                        currentStatus);
            }
            boolean isSuccess = client.updateChangeRequestStatus(
                    order.getId(),
                    changeRequest.getId(),
                    ClientInfo.SYSTEM.getRole(),
                    ClientInfo.SYSTEM.getUid(),
                    new ChangeRequestPatchRequest(nextStatus, null, null));

            if (!isSuccess) {
                throw new IllegalStateException("Cannot proceed to status " + nextStatus + " from status" +
                        currentStatus);
            }

            currentStatus = nextStatus;
        }
    }

    private static ChangeRequestStatus nextStatus(ChangeRequestStatus status) {
        switch (status) {
            case NEW:
                return ChangeRequestStatus.PROCESSING;
            case PROCESSING:
                return ChangeRequestStatus.APPLIED;
            default:
                return null;
        }
    }
}
