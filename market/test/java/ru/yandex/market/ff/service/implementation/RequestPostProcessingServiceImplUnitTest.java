package ru.yandex.market.ff.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.dbqueue.producer.GetRequestDetailsQueueProducer;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.StockService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class RequestPostProcessingServiceImplUnitTest extends IntegrationTest {

    private final StockService stockService = Mockito.mock(StockService.class);
    private final GetRequestDetailsQueueProducer getRequestDetailsQueueProducer = mock(
        GetRequestDetailsQueueProducer.class);

    @Autowired
    private RequestSubTypeService subTypeService;

    private final ShopRequestRepository shopRequestRepository = mock(ShopRequestRepository.class);

    private RequestPostProcessServiceImpl requestPostProcessService;

    @BeforeEach
    public void beforeAll() {
        requestPostProcessService = new RequestPostProcessServiceImpl(
            stockService,
            subTypeService,
            shopRequestRepository,
            getRequestDetailsQueueProducer
        );
    }

    @Test
    public void afterCompleted() {
        requestPostProcessService.afterCompleted(createRequest(1, RequestType.SUPPLY, null));
        requestPostProcessService.afterCompleted(createRequest(2, RequestType.WITHDRAW, null));
        requestPostProcessService.afterCompleted(createRequest(3, RequestType.SHADOW_WITHDRAW, null));
        requestPostProcessService.afterCompleted(createRequest(4, RequestType.WITHDRAW, 5L));
        requestPostProcessService
                .afterCompleted(createTransfer(6, StockType.EXPIRED, StockType.PLAN_UTILIZATION, null));
        requestPostProcessService.afterCompleted(createTransfer(7, StockType.SURPLUS, StockType.FIT, null));
        requestPostProcessService.afterCompleted(createTransfer(8, StockType.CIS_QUARANTINE, StockType.FIT, null));
        requestPostProcessService.afterCompleted(createTransfer(9, StockType.CIS_QUARANTINE, StockType.DEFECT, null));

        verify(stockService).unfreeze(2);
        verify(stockService).unfreeze(5);
        verify(stockService).unfreeze(7);

        verifyNoMoreInteractions(stockService);
    }

    @Test
    public void afterCancelled() {
        requestPostProcessService.afterCancelled(createRequest(1, RequestType.SUPPLY, null));
        requestPostProcessService.afterCancelled(createRequest(2, RequestType.WITHDRAW, null));
        requestPostProcessService.afterCancelled(createRequest(3, RequestType.SHADOW_WITHDRAW, null));
        requestPostProcessService.afterCancelled(createRequest(4, RequestType.WITHDRAW, 5L));
        requestPostProcessService
                .afterCancelled(createTransfer(6, StockType.EXPIRED, StockType.PLAN_UTILIZATION, null));
        requestPostProcessService.afterCancelled(createTransfer(7, StockType.SURPLUS, StockType.FIT, null));
        requestPostProcessService.afterCancelled(createTransfer(8, StockType.CIS_QUARANTINE, StockType.FIT, null));
        requestPostProcessService.afterCancelled(createTransfer(9, StockType.CIS_QUARANTINE, StockType.DEFECT, null));

        verify(stockService).checkFreezeExistsAndUnfreeze(2);
        verify(stockService).checkFreezeExistsAndUnfreeze(3);
        verify(stockService).checkFreezeExistsAndUnfreeze(5);
        verify(stockService).checkFreezeExistsAndUnfreeze(6);
        verify(stockService).checkFreezeExistsAndUnfreeze(7);

        verifyNoMoreInteractions(stockService);
    }

    @Test
    public void afterRejectedByService() {
        requestPostProcessService.afterRejectedByService(createRequest(1, RequestType.SUPPLY, null));
        requestPostProcessService.afterRejectedByService(createRequest(2, RequestType.WITHDRAW, null));
        requestPostProcessService.afterRejectedByService(createRequest(4, RequestType.WITHDRAW, 5L));
        requestPostProcessService
                .afterRejectedByService(createTransfer(6, StockType.EXPIRED, StockType.PLAN_UTILIZATION, null));
        requestPostProcessService.afterRejectedByService(createTransfer(7, StockType.SURPLUS, StockType.FIT, null));
        requestPostProcessService
                .afterRejectedByService(createTransfer(8, StockType.CIS_QUARANTINE, StockType.FIT, null));
        requestPostProcessService
                .afterRejectedByService(createTransfer(9, StockType.CIS_QUARANTINE, StockType.DEFECT, null));

        verify(stockService).unfreeze(2);
        verify(stockService).unfreeze(5);
        verify(stockService).unfreeze(6);
        verify(stockService).unfreeze(7);

        verifyNoMoreInteractions(stockService);
    }

    @Test
    public void afterInvalid() {
        requestPostProcessService.afterInvalid(createRequest(1, RequestType.SUPPLY, null));
        requestPostProcessService.afterInvalid(createRequest(2, RequestType.WITHDRAW, null));
        requestPostProcessService.afterInvalid(createRequest(3, RequestType.SHADOW_WITHDRAW, null));
        requestPostProcessService.afterInvalid(createRequest(4, RequestType.WITHDRAW, 5L));
        requestPostProcessService.afterInvalid(createTransfer(6, StockType.EXPIRED, StockType.PLAN_UTILIZATION, 20L));
        requestPostProcessService.afterInvalid(createTransfer(7, StockType.SURPLUS, StockType.FIT, null));
        requestPostProcessService.afterInvalid(createTransfer(8, StockType.CIS_QUARANTINE, StockType.FIT, null));
        requestPostProcessService.afterInvalid(createTransfer(9, StockType.CIS_QUARANTINE, StockType.DEFECT, null));

        verify(stockService).checkFreezeExistsAndUnfreeze(2);
        verify(stockService).checkFreezeExistsAndUnfreeze(3);
        verify(stockService).checkFreezeExistsAndUnfreeze(5);
        verify(stockService).checkFreezeExistsAndUnfreeze(6);
        verify(stockService).checkFreezeExistsAndUnfreeze(7);

        verifyNoMoreInteractions(stockService);
    }

    @Test
    public void afterProcessed() {
        requestPostProcessService.afterProcessed(createRequest(1, RequestType.SUPPLY, null));
        requestPostProcessService.afterProcessed(createRequest(2, RequestType.WITHDRAW, null));
        requestPostProcessService.afterProcessed(createRequest(3, RequestType.SHADOW_WITHDRAW, null));
        requestPostProcessService.afterProcessed(createRequest(4, RequestType.WITHDRAW, 5L));
        requestPostProcessService.afterProcessed(createTransfer(6, StockType.DEFECT, StockType.PLAN_UTILIZATION, null));
        requestPostProcessService.afterProcessed(createTransfer(7, StockType.SURPLUS, StockType.FIT, null));
        requestPostProcessService.afterProcessed(createTransfer(8, StockType.CIS_QUARANTINE, StockType.FIT, null));
        requestPostProcessService.afterProcessed(createTransfer(9, StockType.CIS_QUARANTINE, StockType.DEFECT, null));

        verify(stockService).unfreeze(6);

        verifyNoMoreInteractions(stockService);
    }

    @Test
    public void afterReadyForWithdraw() {
        requestPostProcessService.afterReadyForWithdraw(createRequest(1, RequestType.WITHDRAW, null));
        requestPostProcessService.afterReadyForWithdraw(createRequest(2, RequestType.MOVEMENT_WITHDRAW, null));
        requestPostProcessService.afterReadyForWithdraw(createRequest(
            3,
            RequestType.MOVEMENT_WITHDRAW,
            null,
            "BREAK_BULK_XDOCK"
        ));
        requestPostProcessService.afterReadyForWithdraw(createRequest(3, RequestType.MOVEMENT_SUPPLY, null));

        verifyNoMoreInteractions(getRequestDetailsQueueProducer);
    }

    private ShopRequest createRequest(long id, RequestType type, Long parentRequestId) {
        return createRequest(id, type, parentRequestId, "DEFAULT");
    }

    private ShopRequest createRequest(long id, RequestType type, Long parentRequestId, String subtype) {
        ShopRequest request = new ShopRequest();
        request.setId(id);
        request.setType(type);
        request.setSubtype(subtype);
        request.setParentRequestId(parentRequestId);
        return request;
    }


    private ShopRequest createTransfer(long id, StockType fromStock, StockType toStock, Long parentRequestId) {
        ShopRequest request = createRequest(id, RequestType.TRANSFER, parentRequestId);
        request.setStockType(fromStock);
        request.setStockTypeTo(toStock);
        return request;
    }
}
