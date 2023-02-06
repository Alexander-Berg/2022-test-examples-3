package ru.yandex.market.mboc.app.copy_assortment;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.BaseMbocAppTest;
import ru.yandex.market.mbo.mdm.common.masterdata.services.SupplierConverterServiceMock;
import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.app.proto.SupplierDocumentServiceMock;
import ru.yandex.market.mboc.app.tool.CopySupplierAssortmentService;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.offers.repository.search.OffersFilter;
import ru.yandex.market.mboc.common.services.modelstorage.ModelStorageCachingServiceMock;
import ru.yandex.market.mboc.common.services.proto.MasterDataHelperService;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

public class CopySupplierAssortmentServiceTest extends BaseMbocAppTest {
    private static final int SUPPLIER_SOURCE = 123;
    private static final int SUPPLIER_TARGET = 234;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private TransactionHelper transactionHelper;

    private MasterDataServiceMock masterDataServiceMock;
    private SupplierDocumentServiceMock supplierDocumentServiceMock;
    private MasterDataHelperService masterDataHelperService;

    private TaskQueueRegistratorMock taskQueueRegistrator;
    private ModelStorageCachingServiceMock modelStorageCachingService;
    private CopySupplierAssortmentService copySupplierAssortmentService;

    @Before
    public void setUp() throws Exception {
        masterDataServiceMock = new MasterDataServiceMock();
        supplierDocumentServiceMock = new SupplierDocumentServiceMock(masterDataServiceMock);
        masterDataHelperService = new MasterDataHelperService(masterDataServiceMock, supplierDocumentServiceMock,
            new SupplierConverterServiceMock(), storageKeyValueService);

        modelStorageCachingService = new ModelStorageCachingServiceMock();
        taskQueueRegistrator = new TaskQueueRegistratorMock();

        copySupplierAssortmentService = new CopySupplierAssortmentService(supplierRepository, offerRepository,
            modelStorageCachingService, masterDataHelperService, transactionHelper, taskQueueRegistrator);

        Supplier source = new Supplier(SUPPLIER_SOURCE,
            "source",
            "source",
            "source",
            MbocSupplierType.THIRD_PARTY);

        Supplier target = new Supplier(SUPPLIER_TARGET,
            "source",
            "source",
            "source",
            MbocSupplierType.THIRD_PARTY);

        supplierRepository.insertBatch(source, target);

        Offer offer = OfferTestUtils.simpleOffer(source).setBusinessId(source.getId())
            .setApprovedSkuMappingInternal(new Offer.Mapping(1234, LocalDateTime.now()))
            .setApprovedSkuMappingConfidence(Offer.MappingConfidence.PARTNER)
            .setCategoryIdForTests(1L, Offer.BindingKind.APPROVED);
        offerRepository.insertOffers(offer);
    }

    @Test
    public void testCopySupplierAssortmentService() {
        Map<Integer, Integer> sourceToTargetSupplierIds = Map.of(SUPPLIER_SOURCE, SUPPLIER_TARGET);

        copySupplierAssortmentService.copyAssortment(sourceToTargetSupplierIds, true, false);

        var filter = new OffersFilter().setBusinessIds(SUPPLIER_TARGET);
        List<Offer> targetOffers = offerRepository.findOffers(filter);
        Assertions.assertThat(targetOffers).hasSize(1);
    }
}
