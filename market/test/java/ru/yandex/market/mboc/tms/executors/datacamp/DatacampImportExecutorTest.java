package ru.yandex.market.mboc.tms.executors.datacamp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;

import Market.DataCamp.API.DatacampMessageOuterClass.DatacampMessage;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer.UnitedOffer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.datacamp.model.DatacampImportQueueItem;
import ru.yandex.market.mboc.common.datacamp.service.DataCampIdentifiersService;
import ru.yandex.market.mboc.common.datacamp.service.DataCampService;
import ru.yandex.market.mboc.common.datacamp.service.DataCampServiceException;
import ru.yandex.market.mboc.common.datacamp.service.DatacampImportService;
import ru.yandex.market.mboc.common.offers.model.BusinessSkuKey;
import ru.yandex.market.mboc.common.offers.repository.OfferRepositoryMock;
import ru.yandex.market.mboc.common.services.datacamp.LogbrokerDatacampOfferMessageHandler;
import ru.yandex.market.mboc.common.services.offers.processing.RemoveOfferService;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DatacampImportExecutorTest {
    private DatacampImportService failedImportService;
    private DataCampService dataCampService;
    private LogbrokerDatacampOfferMessageHandler messageHandler;
    private StorageKeyValueService storageKeyValueService;
    private DatacampImportExecutor datacampImportExecutor;
    private RemoveOfferService removeOfferService;
    private OfferRepositoryMock offerRepositoryMock;

    @Before
    public void setUp() {
        failedImportService = mock(DatacampImportService.class);
        dataCampService = mock(DataCampService.class);
        messageHandler = mock(LogbrokerDatacampOfferMessageHandler.class);
        storageKeyValueService = new StorageKeyValueServiceMock();
        removeOfferService = mock(RemoveOfferService.class);
        offerRepositoryMock = new OfferRepositoryMock();

        var identifiersService = new DataCampIdentifiersService(0, 0, new SupplierConverterServiceMock());
        datacampImportExecutor = new DatacampImportExecutor(
            failedImportService,
            dataCampService,
            identifiersService,
            messageHandler,
            storageKeyValueService,
            removeOfferService,
            offerRepositoryMock) {
            @Override
            protected ExecutorService getExecutorService() {
                ExecutorService res = mock(ExecutorService.class);
                when(res.submit(any(Runnable.class))).thenAnswer(invocation -> {
                    Runnable r = invocation.getArgument(0);
                    r.run();
                    return null;
                });
                return res;
            }
        };
    }

    @Test
    public void onBatchExceptionMarksAsFailedAndProcessAllBatches() {
        doReturn(
            List.of(
                new DatacampImportQueueItem(123, "ssku", now(), now(), null, 0),
                new DatacampImportQueueItem(456, "ssku2", now(), now(), null, 0)
            )
        ).when(failedImportService).getForImport(anyInt());

        storageKeyValueService.putValue(DatacampImportExecutor.MAX_BATCHES_PER_EXECUTION, 3);
        storageKeyValueService.putValue(DatacampImportExecutor.BATCH_SIZE, 1);

        doThrow(new DataCampServiceException("test"))
            .doReturn(List.of(UnitedOffer.newBuilder().build()))
            .when(dataCampService).getUnitedOffersByBusinessSkuKeys(any());

        assertThatThrownBy(() -> datacampImportExecutor.execute())
            .isInstanceOf(DataCampServiceException.class);

        verify(failedImportService, times(1)).getForImport(anyInt());
        verify(dataCampService, times(2)).getUnitedOffersByBusinessSkuKeys(any());
        verify(messageHandler, times(0)).processAs(any(), any());

        var expectedFailed = new HashMap<BusinessSkuKey, String>();
        expectedFailed.put(new BusinessSkuKey(123, "ssku"), null);
        verify(failedImportService, times(1))
            .markForImport(eq(expectedFailed));
    }

    @Test
    public void unmarksAsFailedOffersRemovedFromDataCamp() {
        doReturn(
            List.of(
                new DatacampImportQueueItem(123, "ssku", now(), now(), null, 0),
                new DatacampImportQueueItem(456, "ssku2", now(), now(), null, 0)
            )
        ).when(failedImportService).getForImport(anyInt());

        storageKeyValueService.putValue(DatacampImportExecutor.MAX_BATCHES_PER_EXECUTION, 3);
        storageKeyValueService.putValue(DatacampImportExecutor.BATCH_SIZE, 1);

        doReturn(
            List.of(createUnited(123, "ssku")),
            List.of()
        ).when(dataCampService).getUnitedOffersByBusinessSkuKeys(any());

        datacampImportExecutor.execute();

        verify(failedImportService, times(1)).getForImport(anyInt());
        verify(dataCampService, times(2)).getUnitedOffersByBusinessSkuKeys(any());
        verify(messageHandler, times(1)).processAs(any(), any());

        verify(failedImportService, times(1))
            .markAsImported(eq(new HashSet<>(List.of(new BusinessSkuKey(456, "ssku2")))));
    }

    @Test
    public void correctlyBuildsMessageForLogbrokerHandler() {
        doReturn(
            List.of(new DatacampImportQueueItem(123, "ssku", now(), now(), null, 0))
        ).when(failedImportService).getForImport(anyInt());

        var dcOffer = createUnited(123, "ssku");
        doReturn(List.of(dcOffer), List.of()).when(dataCampService).getUnitedOffersByBusinessSkuKeys(any());

        datacampImportExecutor.execute();

        verify(failedImportService, times(1)).getForImport(anyInt());
        verify(dataCampService, times(1)).getUnitedOffersByBusinessSkuKeys(any());

        @SuppressWarnings("unchecked")
        var messageCaptor = (ArgumentCaptor<List<DatacampMessage>>) (Object) ArgumentCaptor.forClass(List.class);

        verify(messageHandler, times(1)).processAs(messageCaptor.capture(), any());

        var messages = messageCaptor.getValue();
        assertThat(messages).hasSize(1);
        var batches = messages.get(0).getUnitedOffersList();
        assertThat(batches).hasSize(1);
        var offers = batches.get(0).getOfferList();
        assertThat(offers).isEqualTo(List.of(dcOffer));

        verify(failedImportService, times(0)).markAsImported(any());
    }

    @Test
    public void testNotDataCampAnd1PoffersSkipped() {
        var offerNotDC = OfferTestUtils.simpleOffer().setDataCampOffer(false);
        var offer1P = OfferTestUtils.simpleOffer(OfferTestUtils.realSupplier());

        offerRepositoryMock.insertOffers(offerNotDC, offer1P);

        doReturn(List.of(
            new DatacampImportQueueItem(offerNotDC.getBusinessId(), offerNotDC.getShopSku(), now(), now(), null, 0),
            new DatacampImportQueueItem(offer1P.getBusinessId(), offer1P.getShopSku(), now(), now(), null, 0)
        )).when(failedImportService).getForImport(anyInt());

        datacampImportExecutor.execute();

        verify(failedImportService, times(1)).getForImport(anyInt());
        verify(dataCampService, never()).getUnitedOffersByBusinessSkuKeys(any());
        verify(messageHandler, never()).processAs(any(), any());

        verify(failedImportService, times(1)).markAsImported(any());
    }

    private UnitedOffer createUnited(int businessId, String sku) {
        return UnitedOffer.newBuilder().setBasic(DataCampOffer.Offer.newBuilder()
            .setIdentifiers(
                DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(businessId)
                    .setOfferId(sku)
                    .build()
            ).build()
        ).build();
    }
}
