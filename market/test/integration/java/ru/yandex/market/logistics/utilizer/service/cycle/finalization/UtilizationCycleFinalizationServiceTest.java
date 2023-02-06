package ru.yandex.market.logistics.utilizer.service.cycle.finalization;

import java.util.Map;

import Market.DataCamp.SyncAPI.SyncChangeOffer;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.mbi.api.client.entity.supplier.SupplierBaseDTO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class UtilizationCycleFinalizationServiceTest extends AbstractContextualTest {

    @Autowired
    private UtilizationCycleFinalizationService utilizationCycleFinalizationService;

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/finalization/1/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/finalization/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void finalizeIntervalWithoutItems() {
        utilizationCycleFinalizationService.finalizeUtilizationCycle(1);
        Mockito.verifyZeroInteractions(mbiApiClient);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/finalization/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/finalization/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void finalizeIntervalWithAllItemsWithoutPrice() {
        long supplierId = 100500;
        Mockito.when(mbiApiClient.getSupplierInfo(supplierId))
                .thenReturn(new SupplierBaseDTO(supplierId, "Ромашка", null));
        Mockito.when(dataCampClient.searchOffers(eq(supplierId), any())).thenReturn(createEmptyFullOfferResponse());
        utilizationCycleFinalizationService.finalizeUtilizationCycle(1);

        String expectedMessageData =
                "<data>" +
                        "<supplier-name>Ромашка</supplier-name>" +
                        "<items-count>340</items-count>" +
                        "<deadline>13.01.2021</deadline>" +
                 "</data>";
        Mockito.verify(mbiApiClient).getSupplierInfo(supplierId);
        Mockito.verify(mbiApiClient).sendMessageToSupplier(supplierId, 1606176000, expectedMessageData);
        Mockito.verifyNoMoreInteractions(mbiApiClient);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/finalization/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/finalization/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void finalizeIntervalWithSomeItemsWithPrice() {
        long supplierId = 100500;
        Mockito.when(mbiApiClient.getSupplierInfo(supplierId))
                .thenReturn(new SupplierBaseDTO(supplierId, "Ромашка", null));
        Mockito.when(dataCampClient.searchOffers(eq(supplierId), any())).thenReturn(
                createOfferResponse(Map.of("sku1", 10000L))
        );
        utilizationCycleFinalizationService.finalizeUtilizationCycle(1);

        String expectedMessageData =
                "<data>" +
                        "<supplier-name>Ромашка</supplier-name>" +
                        "<items-count>340</items-count>" +
                        "<items-total-cost>1 100 000</items-total-cost>" +
                        "<deadline>13.01.2021</deadline>" +
                        "</data>";
        Mockito.verify(mbiApiClient).getSupplierInfo(supplierId);
        Mockito.verify(mbiApiClient).sendMessageToSupplier(supplierId, 1606176000, expectedMessageData);
        Mockito.verifyNoMoreInteractions(mbiApiClient);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/service/cycle/finalization/2/before.xml")
    @ExpectedDatabase(value = "classpath:fixtures/service/cycle/finalization/2/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void finalizeIntervalFailsInCaseOfDataCampException() {
        long supplierId = 100500;
        Mockito.when(mbiApiClient.getSupplierInfo(supplierId))
                .thenReturn(new SupplierBaseDTO(supplierId, "Ромашка", null));
        Mockito.when(dataCampClient.searchOffers(eq(supplierId), any()))
                .thenThrow(new RuntimeException("Connection timeout"));
        try {
            utilizationCycleFinalizationService.finalizeUtilizationCycle(1);
            softly.fail("Exception should be thrown");
        } catch (Exception ignored) {
        }

        Mockito.verify(dataCampClient, Mockito.times(3)).searchOffers(eq(supplierId), any());
        Mockito.verifyZeroInteractions(mbiApiClient);
    }

    private SyncChangeOffer.FullOfferResponse createEmptyFullOfferResponse() {
        return SyncChangeOffer.FullOfferResponse.newBuilder().build();
    }
}
