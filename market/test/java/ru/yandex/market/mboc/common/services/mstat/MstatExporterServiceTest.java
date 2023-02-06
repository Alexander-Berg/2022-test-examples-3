package ru.yandex.market.mboc.common.services.mstat;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.availability.msku.MskuRepository;
import ru.yandex.market.mboc.common.dict.MbocSupplierType;
import ru.yandex.market.mboc.common.dict.Supplier;
import ru.yandex.market.mboc.common.dict.SupplierRepository;
import ru.yandex.market.mboc.common.dict.SupplierService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.msku.TestUtils;
import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.offers.repository.MstatOfferStateRepository;
import ru.yandex.market.mboc.common.offers.repository.OfferRepository;
import ru.yandex.market.mboc.common.services.mstat.yt.CompositeYtMstatTableLoader;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.BaseDbTestClass;

/**
 * @author apluhin
 * @created 2/18/21
 */
public class MstatExporterServiceTest extends BaseDbTestClass {

    private MstatExporterService service;
    @Autowired
    private MstatOfferStateRepository mstatOfferStateRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private MskuRepository mskuRepository;
    @Autowired
    private TransactionHelper transactionHelper;

    private CompositeYtMstatTableLoader tableLoader;
    private MstatOfferStateService mstatOfferStateService;

    @Before
    public void setUp() throws Exception {
        tableLoader = Mockito.mock(CompositeYtMstatTableLoader.class);
        mstatOfferStateService = Mockito.spy(new MstatOfferStateService(mstatOfferStateRepository));
        storageKeyValueService = new StorageKeyValueServiceMock();
        SupplierService supplierService = new SupplierService(supplierRepository);
        service = new MstatExporterService(
            mstatOfferStateService,
            tableLoader,
            offerRepository,
            supplierService,
            mskuRepository,
            transactionHelper
        );
        mskuRepository.save(
            Arrays.asList(
                TestUtils.newMsku(1),
                TestUtils.newMsku(2),
                TestUtils.newMsku(3),
                TestUtils.newMsku(4)
            ));
    }

    @Test
    public void testLoadWithoutState() {
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
        Offer sampleOffer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        sampleOffer.setSuggestSkuMapping(new Offer.Mapping(1, LocalDateTime.now()));
        sampleOffer.setApprovedSkuMappingInternal(new Offer.Mapping(2, LocalDateTime.now()));
        sampleOffer.setContentSkuMapping(new Offer.Mapping(3, LocalDateTime.now()));
        sampleOffer.setSupplierSkuMapping(new Offer.Mapping(4, LocalDateTime.now()));
        offerRepository.insertOffers(sampleOffer);

        Assertions.assertThat(mstatOfferStateRepository.findAll().size()).isEqualTo(0);

        service.updateOfferByIds(Collections.singletonList(sampleOffer.getId()));

        ArgumentCaptor<ExportContext> contextCaptor = ArgumentCaptor.forClass(ExportContext.class);

        Assertions.assertThat(mstatOfferStateRepository.findAll().size()).isEqualTo(1);

        Mockito.verify(tableLoader, Mockito.times(1))
            .insertOrUpdate(contextCaptor.capture());
        ExportContext context = contextCaptor.getValue();
        Assertions.assertThat(context.getOffers().iterator().next()).isEqualTo(sampleOffer);
        Assertions.assertThat(context.getMskus().keySet()).containsExactly(1L, 2L, 3L, 4L);
        Assertions.assertThat(context.getSuppliers().keySet()).isEqualTo(
            sampleOffer.getServiceOffers().stream().map(it -> it.getSupplierId()).collect(Collectors.toSet()));

        Mockito.verify(mstatOfferStateService, Mockito.times(1))
            .findStateByOfferId(Mockito.eq(Collections.singletonList(sampleOffer.getId())));

        ArgumentCaptor<Map> mapArgumentCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(mstatOfferStateService, Mockito.times(1))
            .insertOrUpdateStatesForOffer(Mockito.any(), mapArgumentCaptor.capture());
        Map<Integer, Supplier> value = (Map<Integer, Supplier>) mapArgumentCaptor.getValue();
        Assertions.assertThat(value.get(42)).isEqualTo(supplierRepository.findById(42));
    }

    @Test
    public void testRetryDelete() {
        supplierRepository.insertBatch(
            YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
        Assertions.assertThat(mstatOfferStateRepository.findAll().size()).isEqualTo(0);

        service.updateOfferByIds(Collections.singletonList(1L));

        ArgumentCaptor<ExportContext> contextCaptor = ArgumentCaptor.forClass(ExportContext.class);

        Assertions.assertThat(mstatOfferStateRepository.findAll().size()).isEqualTo(0);

        Mockito.verify(tableLoader, Mockito.times(1)).insertOrUpdate(contextCaptor.capture());
        ExportContext context = contextCaptor.getValue();
        Assertions.assertThat(context.getOffers().size()).isEqualTo(0);

        Mockito.verify(mstatOfferStateService, Mockito.times(1))
            .findStateByOfferId(Mockito.eq(Collections.singletonList(1L)));

        Mockito.verify(mstatOfferStateService, Mockito.times(0))
            .insertOrUpdateStatesForOffer(Mockito.any(), Mockito.any());
    }

    @Test
    public void testSkipEmptyIds() {
        supplierRepository.insertBatch(
            YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
        Assertions.assertThat(mstatOfferStateRepository.findAll().size()).isEqualTo(0);

        service.updateOfferByIds(Collections.emptyList());

        Assertions.assertThat(mstatOfferStateRepository.findAll().size()).isEqualTo(0);

        Mockito.verify(tableLoader, Mockito.times(0)).insertOrUpdate(Mockito.any());


        Mockito.verify(mstatOfferStateService, Mockito.times(0))
            .findStateByOfferId(Mockito.any());

        Mockito.verify(mstatOfferStateService, Mockito.times(0))
            .insertOrUpdateStatesForOffer(Mockito.any(), Mockito.any());
    }

    @Test
    public void testUpdateExportByNewState() {
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
        Offer sampleOffer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        offerRepository.insertOffers(sampleOffer);
        sampleOffer = offerRepository.findAll().get(0);
        Offer finalSampleOffer1 = sampleOffer;
        service.updateOfferByIds(Collections.singletonList(finalSampleOffer1.getId()));

        MstatOfferState beforeState = mstatOfferStateRepository.findAll().get(0);

        sampleOffer.setBusinessId(200);
        sampleOffer.setShopSku("new-name");
        Offer.ServiceOffer serviceOffer = new Offer.ServiceOffer(
            102,
            MbocSupplierType.REAL_SUPPLIER,
            Offer.AcceptanceStatus.NEW);
        sampleOffer.setServiceOffers(serviceOffer);

        offerRepository.updateOffer(sampleOffer);
        Offer finalSampleOffer = sampleOffer;
        service.updateOfferByIds(Collections.singletonList(finalSampleOffer.getId()));

        MstatOfferState newState = mstatOfferStateRepository.findAll().get(0);

        Assertions.assertThat(newState.getServiceOfferRealSupplierIds()).isEqualTo(Map.of(102, "000102"));
        Assertions.assertThat(newState.getServiceOffers()).isEqualTo(sampleOffer.getServiceOffers());
        Assertions.assertThat(newState.getShopSku()).isEqualTo(sampleOffer.getShopSku());
        Assertions.assertThat(newState.getBusinessId()).isEqualTo(sampleOffer.getBusinessId());

        Mockito.verify(mstatOfferStateService, Mockito.times(0)).deleteStates(Mockito.any());

        ArgumentCaptor<Collection> collectionArgumentCaptor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(tableLoader, Mockito.times(2))
            .deleteByCalculatedDiff(collectionArgumentCaptor.capture());

        Collection<DiffState> diffStates2 = collectionArgumentCaptor.getAllValues().get(1); //первый запрос init
        Assertions.assertThat(diffStates2.size()).isEqualTo(1);
        DiffState state2 = diffStates2.iterator().next();
        Assertions.assertThat(state2).isEqualTo(new DiffState(beforeState, newState));
    }

    @Test
    public void testDeleteAndUpdateInOneBatch() {
        supplierRepository.insertBatch(YamlTestUtil.readSuppliersFromResource("suppliers/test-suppliers.yml"));
        Offer sampleOffer = YamlTestUtil.readFromResources("offers/test-offer.yml", Offer.class);
        offerRepository.insertOffers(sampleOffer);
        sampleOffer = offerRepository.findAll().get(0);
        Offer finalSampleOffer = sampleOffer;
        service.updateOfferByIds(Collections.singletonList(finalSampleOffer.getId()));

        mstatOfferStateRepository.insert(
            MstatOfferState.builder()
                .offerId(10L)
                .businessId(1)
                .shopSku("shop").build()
        );

        Assertions.assertThat(mstatOfferStateRepository.findAll().size()).isEqualTo(2);
        sampleOffer.setShopSku("new-name");
        offerRepository.updateOffer(sampleOffer);
        //10 - deleted
        Offer finalSampleOffer1 = sampleOffer;
        service.updateOfferByIds(Arrays.asList(finalSampleOffer1.getId(), 10L));

        Assertions.assertThat(mstatOfferStateRepository.findAll().size()).isEqualTo(1);
        MstatOfferState newState = mstatOfferStateRepository.findAll().get(0);

        Assertions.assertThat(newState.getShopSku()).isEqualTo(sampleOffer.getShopSku());

        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        Mockito.verify(mstatOfferStateService, Mockito.times(1)).deleteStates(captor.capture());
        Collection<MstatOfferState> value = captor.getValue();
        //Старый стейт должны быть удалены
        Assertions.assertThat(value.size()).isEqualTo(1);
    }
}
