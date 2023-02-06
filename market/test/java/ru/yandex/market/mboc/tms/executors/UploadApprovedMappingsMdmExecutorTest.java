package ru.yandex.market.mboc.tms.executors;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.app.proto.MasterDataServiceMock;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.masterdata.model.MbocBaseProtoConverter;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.offers.upload.MdmOfferUploadQueueService;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mdm.http.MdmCommon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createNonProcessedOffer;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createOffer;
import static ru.yandex.market.mboc.tms.utils.UploadApprovedMappingsHelper.createUploadToYtOffer;

/**
 * Тесты {@link UploadApprovedMappingsMdmExecutor}.
 *
 * @author s-ermakov
 */
@SuppressWarnings("checkstyle:magicNumber")
public class UploadApprovedMappingsMdmExecutorTest extends BaseDbTestClass {
    private static final int SEED = 42;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private MdmOfferUploadQueueService mdmOfferUploadQueueService;

    private UploadApprovedMappingsMdmExecutor uploadApprovedMappingsMdmExecutor;
    private Offer offer;
    private Offer nonApprovedOffer;
    private MasterDataServiceMock masterDataService;
    private Supplier supplier1;
    private Supplier supplier2;

    @Before
    public void setUp() throws Exception {
        masterDataService = new MasterDataServiceMock();
        uploadApprovedMappingsMdmExecutor = new UploadApprovedMappingsMdmExecutor(
                offerRepository,
                transactionHelper,
                masterDataService,
                mdmOfferUploadQueueService,
                true,
                true
        );
        supplier1 = new Supplier(1, "Test supplier", null, null);
        supplierRepository.insert(supplier1);
        supplier2 = new Supplier(2, "Supplier #2", null, null);
        supplierRepository.insert(supplier2);
        supplierRepository.insert(new Supplier(3, "Supplier #3", null, null));
        supplierRepository.insert(new Supplier(OfferTestUtils.TEST_SUPPLIER_ID, "Test supplier"));
        offer = createOffer(supplier1);
        nonApprovedOffer = createNonProcessedOffer(supplier1);
    }

    private void mdmVerify(List<MdmCommon.ShopSkuKey> skuToSend) {
        List<MdmCommon.ShopSkuKey> logbrokerTopicContent = masterDataService.getLogbrokerTopicContent();
        Assertions.assertThat(logbrokerTopicContent).hasSize(skuToSend.size());
        Assertions.assertThat(logbrokerTopicContent).containsExactlyInAnyOrderElementsOf(skuToSend);
    }

    private void verityNoMdmInteractions() {
        List<MdmCommon.ShopSkuKey> logbrokerTopicContent = masterDataService.getLogbrokerTopicContent();
        Assertions.assertThat(logbrokerTopicContent).hasSize(0);
    }

    @Test
    public void testSuccessScenario() throws Exception {
        // arrange
        offerRepository.insertOffer(offer);
        offerRepository.insertOffer(nonApprovedOffer);
        Offer newNonApprovedOffer = offerRepository.getOfferById(nonApprovedOffer.getId());
        assertThat(offer).matches(this::needsUploadToMdm);
        assertThat(nonApprovedOffer).matches(this::needsUploadToMdm);
        // act
        uploadApprovedMappingsMdmExecutor.execute();
        // assert
        assertEquals(1, masterDataService.getLogbrokerTopicContent().size());
        // Из очереди убрался оффер
        assertThat(offer).matches(this::notNeedsUploadToMdm);
        assertThat(nonApprovedOffer).matches(this::notNeedsUploadToMdm);

        Offer offerFromDb = offerRepository.getOfferById(this.offer.getId());
        assertThat(offerFromDb.getLastVersion()).isGreaterThan(offer.getLastVersion());
        // остальные офферы не должны никак измениться
        assertThat(offerRepository.getOfferById(nonApprovedOffer.getId()))
                .usingRecursiveComparison().isEqualTo(newNonApprovedOffer);
    }

    @Test
    public void testNoRequestsIfNoApprovedOffers() throws Exception {
        offerRepository.insertOffer(nonApprovedOffer);
        // запускаем
        uploadApprovedMappingsMdmExecutor.execute();
        // проверяем
        verityNoMdmInteractions();
        assertThat(offerRepository.getOfferById(nonApprovedOffer.getId())).matches(this::notNeedsUploadToMdm);
    }

    @Test
    public void testNoRequestsIfNothingInQueue() throws Exception {
        offerRepository.insertOffer(offer);
        mdmOfferUploadQueueService.dequeueOfferIds(List.of(offer.getId()));
        assertThat(offerRepository.getOfferById(offer.getId())).matches(this::notNeedsUploadToMdm);
        // запускаем
        uploadApprovedMappingsMdmExecutor.execute();
        // проверяем
        verityNoMdmInteractions();

        assertThat(offerRepository.getOfferById(offer.getId())).matches(this::notNeedsUploadToMdm);
    }

    @Test
    public void verifyMdmPayload() {
        List<MdmCommon.ShopSkuKey> skuToSend = new ArrayList<>();
        int datasetSize = 10;
        for (int i = 0; i < datasetSize; i++) {
            Offer offerForUpload = createUploadToYtOffer(1001,
                Offer.MappingType.APPROVED,
                Offer.MappingDestination.BLUE,
                OfferTestUtils.simpleSupplier(),
                "12345-" + i
            );
            offerForUpload.setUploadToYtStamp((long) i);
            offerRepository.insertOffer(offerForUpload);
            skuToSend.add(MbocBaseProtoConverter.pojoToProto(offerForUpload.getShopSkuKey()));
        }
        uploadApprovedMappingsMdmExecutor.execute();
        mdmVerify(skuToSend);
    }

    @Test
    public void disabledMdmSend() {
        uploadApprovedMappingsMdmExecutor = new UploadApprovedMappingsMdmExecutor(
            offerRepository,
            transactionHelper,
            masterDataService,
            mdmOfferUploadQueueService,
            false,
            true
        );
        offerRepository.insertOffer(offer);

        uploadApprovedMappingsMdmExecutor.execute();

        verityNoMdmInteractions();
        assertThat(offerRepository.getOfferById(offer.getId())).matches(this::notNeedsUploadToMdm);
    }

    private boolean notNeedsUploadToMdm(Offer offer) {
        return !needsUploadToMdm(offer);
    }

    private boolean needsUploadToMdm(Offer offer) {
        return mdmOfferUploadQueueService.areAllOfferIdsInQueue(List.of(offer.getId()));
    }
}
