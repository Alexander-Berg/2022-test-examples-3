package ru.yandex.market.ff.listener.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.listener.RequestStatusChangeListener;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.repository.NewMovementFlowRequestsRepository;
import ru.yandex.market.ff.service.MbiNotificationService;
import ru.yandex.market.ff.service.RequestPostProcessService;
import ru.yandex.market.ff.service.implementation.SendRequestService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.ff.service.util.MbiNotificationTypes.WITHDRAW_IS_READY;

/**
 * Unit тесты для {@link RequestStatusChangeListenerTest}.
 */
public class RequestStatusChangeListenerTest {

    private MbiNotificationService notificationService;
    private RequestPostProcessService requestPostProcessService;
    private RequestStatusChangeListener requestStatusChangeListener;
    private NewMovementFlowRequestsRepository newMovementFlowRequestsRepository;
    private SendRequestService sendRequestService;

    @BeforeEach
    public void init() {
        notificationService = mock(MbiNotificationService.class);
        requestPostProcessService = mock(RequestPostProcessService.class);
        sendRequestService = mock(SendRequestService.class);
        newMovementFlowRequestsRepository = mock(NewMovementFlowRequestsRepository.class);

        requestStatusChangeListener = new RequestStatusChangeListener(
                notificationService,
                requestPostProcessService,
                sendRequestService,
                newMovementFlowRequestsRepository);
    }

    /**
     * Не отпраляем нотификацию мерчанту при отключенной нотификации по изъятиям со стока "брак".
     */
    @Test
    public void shouldNotSendNotificationFromWithdrawAndDefectStock() {
        ShopRequest shopRequest = createShopRequest(RequestType.WITHDRAW, RequestStatus.ACCEPTED_BY_SERVICE);
        RequestStatusChangeEvent event = new RequestStatusChangeEvent(
                shopRequest, null, null, RequestStatus.ACCEPTED_BY_SERVICE, shopRequest.getStatus());

        requestStatusChangeListener.onApplicationEvent(event);

        verify(notificationService, never()).sendNotificationQuietly(anyInt(), any(ShopRequest.class));
    }

    /**
     * Отпраляем нотификацию мерчанту при включенной нотификации по изъятиям со стока "брак".
     */
    @Test
    public void shouldSendNotificationFromWithdrawAndDefectStockWithoutTypeMove() {
        ShopRequest shopRequest = createShopRequest(RequestType.WITHDRAW, RequestStatus.READY_TO_WITHDRAW);
        RequestStatusChangeEvent event = new RequestStatusChangeEvent(
                shopRequest, null, null, RequestStatus.READY_TO_WITHDRAW, shopRequest.getStatus());

        requestStatusChangeListener.onApplicationEvent(event);

        verify(notificationService).sendNotificationQuietly(WITHDRAW_IS_READY, shopRequest);
    }

    /**
     * Отпраляем нотификацию мерчанту при невыставленном флаге отключения нотификации по изъятиям со стока "брак".
     */
    @Test
    public void shouldSendNotificationFromWithdrawAndDefectStockWithoutSetNotificationFlag() {
        ShopRequest shopRequest = createShopRequest(RequestType.WITHDRAW, RequestStatus.READY_TO_WITHDRAW);
        RequestStatusChangeEvent event = new RequestStatusChangeEvent(
                shopRequest, null, null, RequestStatus.READY_TO_WITHDRAW, shopRequest.getStatus());

        requestStatusChangeListener.onApplicationEvent(event);

        verify(notificationService).sendNotificationQuietly(WITHDRAW_IS_READY, shopRequest);
    }
    @Test
    public void generateSendToServiceTask() {
        ShopRequest shopRequest = createShopRequest(RequestType.WITHDRAW, RequestStatus.VALIDATED);
        RequestStatusChangeEvent event = new RequestStatusChangeEvent(
                shopRequest, null, null, RequestStatus.VALIDATED, shopRequest.getStatus());

        requestStatusChangeListener.onApplicationEvent(event);
        verify(sendRequestService).sendIfNeeded(shopRequest);
    }

    private ShopRequest createShopRequest(RequestType requestType, RequestStatus requestStatus) {
        final ShopRequest shopRequest = new ShopRequest();
        shopRequest.setType(requestType);
        shopRequest.setSupplier(new Supplier(10, null, null, null, null, new SupplierBusinessType()));
        shopRequest.setServiceId(1L);
        shopRequest.setStatus(requestStatus);
        return shopRequest;
    }
}
