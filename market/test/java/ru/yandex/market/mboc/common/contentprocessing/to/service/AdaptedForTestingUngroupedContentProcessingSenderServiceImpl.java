package ru.yandex.market.mboc.common.contentprocessing.to.service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import lombok.SneakyThrows;

import ru.yandex.market.ir.http.OfferContentProcessingServiceGrpc.OfferContentProcessingServiceBlockingStub;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.contentprocessing.log.ContentProcessingLog;
import ru.yandex.market.mboc.common.contentprocessing.to.model.ContentProcessingOffer;
import ru.yandex.market.mboc.common.contentprocessing.to.repository.ContentProcessingQueueRepository;
import ru.yandex.market.mboc.common.datacamp.HashCalculator;
import ru.yandex.market.mboc.common.datacamp.service.DataCampService;
import ru.yandex.market.mboc.common.datacamp.service.DatacampImportService;
import ru.yandex.market.mboc.common.datacamp.service.converter.DataCampConverterService;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.ContextedOfferDestinationCalculator;
import ru.yandex.market.mboc.common.offers.repository.MigrationModelRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mboc.common.services.offers.processing.NeedContentStatusService;
import ru.yandex.market.mboc.common.services.offers.processing.OffersProcessingStatusService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class AdaptedForTestingUngroupedContentProcessingSenderServiceImpl
    extends UngroupedContentProcessingSenderServiceImpl {

    public AdaptedForTestingUngroupedContentProcessingSenderServiceImpl(
        ContentProcessingQueueRepository queue,
        OfferContentProcessingServiceBlockingStub agService,
        TransactionHelper transactionHelper,
        OfferRepository offerRepository,
        MskuRepository mskuRepository,
        CategoryCachingService categoryService,
        SupplierService supplierService,
        OffersProcessingStatusService processingStatusService,
        NeedContentStatusService needContentStatusService,
        DataCampConverterService dataCampConverterService,
        DataCampService dataCampService,
        DatacampImportService datacampImportService,
        StorageKeyValueService keyValueService,
        ContentProcessingLog contentProcessingLog,
        ContextedOfferDestinationCalculator calculator,
        HashCalculator hashCalculator,
        MigrationModelRepository migrationModelRepository
    ) {
        super(queue, agService, transactionHelper, offerRepository, mskuRepository, categoryService, supplierService,
            processingStatusService, needContentStatusService,
            dataCampService, dataCampConverterService, datacampImportService,
            keyValueService, contentProcessingLog, calculator, hashCalculator, migrationModelRepository);
    }

    @Override
    @SneakyThrows
    protected Future<SenderTaskResult> buildAndSubmitTask(Collection<ContentProcessingOffer> offers) {
        var offersCopy = List.copyOf(offers);
        @SuppressWarnings("unchecked")
        var mock = (Future<SenderTaskResult>) mock(Future.class);
        doAnswer(invocation -> executeTask(new SenderTaskContext(offersCopy))).when(mock).get(anyLong(), any());
        return mock;
    }
}
