package ru.yandex.market.abo.core.resupply;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.resupply.entity.ResupplyEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyItemEntity;
import ru.yandex.market.abo.core.resupply.entity.ResupplyOperator;
import ru.yandex.market.abo.core.resupply.entity.Warehouse;
import ru.yandex.market.abo.core.resupply.fulfillment.FulfillmentResupplyRequestService;
import ru.yandex.market.abo.core.resupply.repo.ResupplyOperatorRepo;
import ru.yandex.market.abo.core.resupply.repo.ResupplyRequestRepo;
import ru.yandex.market.abo.core.resupply.stock.ResupplyStock;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.CustomerReturnInfoDTO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.openMocks;
import static ru.yandex.market.abo.core.resupply.ResupplyServiceTest.orderItem;
import static ru.yandex.market.abo.core.resupply.entity.ResupplyStatus.SUPPLY_REQUESTED;

public class ResupplyRequestServiceTest extends EmptyTest {

    @Autowired
    ResupplyRequestService resupplyRequestService;
    @Autowired
    ResupplyService resupplyService;
    @Autowired
    ResupplyRequestRepo resupplyRequestRepo;
    @Autowired
    ResupplyOperatorRepo resupplyOperatorRepo;

    @InjectMocks
    @Autowired
    private FulfillmentResupplyRequestService fulfillmentService;
    @Mock
    FulfillmentWorkflowClientApi ffClient;

    @BeforeEach
    public void init() {
        openMocks(this);
    }

    @Test
    public void supply() {
        long userId = 3L;
        ResupplyOperator settings = new ResupplyOperator();
        settings.setUserId(userId);
        settings.setWarehouse(Warehouse.TOMILINO);
        resupplyOperatorRepo.save(settings);

        ResupplyEntity resupply = resupplyService.createResupply(userId);
        ResupplyItemEntity resupplyItem = resupplyService.createNewItem(resupply, orderItem(), 1L, 2L);

        resupplyItem.setResupplyStock(ResupplyStock.GOOD);
        resupplyService.saveResupplyItem(resupplyItem);

        resupplyRequestService.requestSupply(Arrays.asList(resupply.getId()), Warehouse.ROSTOV);
        verify(ffClient).sendCustomerReturns(anyLong(), anyString(), any(), any());
        assertEquals(SUPPLY_REQUESTED, resupply.getStatus());
        assertNotNull(resupply.getRequestId());

        CustomerReturnInfoDTO dto = new CustomerReturnInfoDTO();
        dto.setRequestId(123L);
        doReturn(dto).when(ffClient).getCustomerReturn(anyString());
        resupplyRequestService.loadSupplies();
        assertNotNull(resupplyRequestRepo.findByIdOrNull(resupply.getRequestId()).getFfRequestId());
    }

}
