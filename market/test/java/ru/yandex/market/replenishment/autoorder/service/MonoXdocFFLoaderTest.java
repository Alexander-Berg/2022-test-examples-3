package ru.yandex.market.replenishment.autoorder.service;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.CreateSupplyRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.replenishment.autoorder.CreateSupplyRequestDTOPriceMatcher;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MonoXdocFFLoaderTest extends FunctionalTest {

    @Autowired
    private FulfillmentWorkflowClientApi clientApi;

    @Autowired
    private MonoXdocFFLoader loader;

    @After
    public void setUp() {
        ReflectionTestUtils.setField(loader, "ffApi", clientApi);
    }

    @Test
    @DbUnitDataSet(before = "MonoXdocFFLoaderTest.load.before.csv", after = "MonoXdocFFLoaderTest.load.after.csv")
    public void loadTest() {
        final FulfillmentWorkflowClientApi mockedClientApi = Mockito.mock(FulfillmentWorkflowClientApi.class);
        Mockito.doAnswer(invocation -> {
            final CreateSupplyRequestDTO request = invocation.getArgument(0);
            final ShopRequestDTO response = new ShopRequestDTO();
            response.setId(request.getServiceId());
            return response;
        }).when(mockedClientApi).createSupplyRequest(Mockito.any());
        ReflectionTestUtils.setField(loader, "ffApi", mockedClientApi);
        loader.load();
    }

    @Test
    @DbUnitDataSet(before = "MonoXdocFFLoaderTest.loadWithDefaultPriceForMonoXDocItems.before.csv", after =
        "MonoXdocFFLoaderTest.loadWithDefaultPriceForMonoXDocItems.after.csv")
    public void loadWithDefaultPriceForMonoXDocItemsTest() {
        FulfillmentWorkflowClientApi mockedClientApi = Mockito.mock(FulfillmentWorkflowClientApi.class);
        Mockito.doAnswer(invocation -> {
            final CreateSupplyRequestDTO request = invocation.getArgument(0);
            final ShopRequestDTO response = new ShopRequestDTO();
            response.setId(request.getServiceId());
            return response;
        }).when(mockedClientApi).createSupplyRequest(Mockito.any());

        ReflectionTestUtils.setField(loader, "ffApi", mockedClientApi);
        loader.load();

        verify(mockedClientApi, times(5)).createSupplyRequest(
            argThat(new CreateSupplyRequestDTOPriceMatcher(BigDecimal.ONE)));
    }

    @Test
    @DbUnitDataSet(before = "MonoXdocFFLoaderTest.load.before.csv", after = "MonoXdocFFLoaderTest.loadFail.after.csv")
    public void loadTestFailed() {
        final FulfillmentWorkflowClientApi mockedClientApi = Mockito.mock(FulfillmentWorkflowClientApi.class);
        Mockito.doAnswer(invocation -> {
            final CreateSupplyRequestDTO request = invocation.getArgument(0);
            if (request.getServiceId() == 147L) {
                throw new HttpTemplateException(400, "Error");
            }
            final ShopRequestDTO response = new ShopRequestDTO();
            response.setId(request.getServiceId());
            return response;
        }).when(mockedClientApi).createSupplyRequest(Mockito.any());
        ReflectionTestUtils.setField(loader, "ffApi", mockedClientApi);
        Assertions.assertThrows(IllegalStateException.class, loader::load);
        // repeat check
        Assertions.assertThrows(IllegalStateException.class, loader::load);
    }

    @Test
    @DbUnitDataSet(before = "MonoXdocFFLoaderTest.load.before.csv", after = "MonoXdocFFLoaderTest.loadFailIdempotency" +
        ".after.csv")
    public void loadTestFailedIdempotency() {
        final FulfillmentWorkflowClientApi mockedClientApi = Mockito.mock(FulfillmentWorkflowClientApi.class);
        Mockito.doAnswer(invocation -> {
            final CreateSupplyRequestDTO request = invocation.getArgument(0);
            if (request.getServiceId() == 147L) {
                throw new HttpTemplateException(409, "Error");
            }
            final ShopRequestDTO response = new ShopRequestDTO();
            response.setId(request.getServiceId());
            return response;
        }).when(mockedClientApi).createSupplyRequest(Mockito.any());
        ReflectionTestUtils.setField(loader, "ffApi", mockedClientApi);
        Assertions.assertThrows(IllegalStateException.class, loader::load);
        // repeat check
        //     Assertions.assertDoesNotThrow(loader::load);
    }

}
