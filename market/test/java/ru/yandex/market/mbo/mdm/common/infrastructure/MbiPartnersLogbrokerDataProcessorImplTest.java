package ru.yandex.market.mbo.mdm.common.infrastructure;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.SskuToRefreshInfo;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierSalesModel;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.BusinessSwitchTransport;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmBusinessStageSwitcher;
import ru.yandex.market.mbo.mdm.common.masterdata.services.iris.BeruIdMock;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.partner.event.PartnerInfo;

public class MbiPartnersLogbrokerDataProcessorImplTest extends MdmBaseDbTestClass {
    private static final int BUSINESS_A = 1;
    private static final int SERVICE_A1 = 11;
    private static final int SERVICE_A2 = 12;
    private static final int BUSINESS_B = 2;
    private static final int SERVICE_B1 = 21;
    private static final int SERVICE_B2 = 22;
    private static final int WHITE_A1 = 101;
    private static final int WHITE_B1 = 201;

    private static final String CLICK_AND_COLLECT = "CLICK_AND_COLLECT";
    private static final String CROSSDOCK = "CROSSDOCK";
    private static final String DROPSHIP = "DROPSHIP";
    private static final String DROPSHIP_BY_SELLER = "DROPSHIP_BY_SELLER";
    private static final String FULFILLMENT = "FULFILLMENT";

    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmBusinessStageSwitcher mdmBusinessStageSwitcher;
    private MbiPartnersLogbrokerDataProcessor processor;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshRepository;
    @Autowired
    private MdmQueuesManager queuesManager;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    @Before
    public void setup() {
        storageKeyValueService.putValue(MdmProperties.READ_PARTNERS_FROM_MBI_LB, true);
        storageKeyValueService.putValue(MdmProperties.SEND_SSKU_TO_QUEUE_ON_SUPPLIER_CHANGE, true);
        processor = new MbiPartnersLogbrokerDataProcessorImpl(storageKeyValueService, mdmSupplierRepository,
            mdmBusinessStageSwitcher, queuesManager);
        storageKeyValueService.invalidateCache();
    }

    @Test
    public void testNoDataNoOp() throws Exception {
        processor.doProcess(message());
        Assertions.assertThat(mdmSupplierRepository.totalCount()).isZero();
    }

    @Test
    public void testNewSuppliersReceived() {
        MdmSupplier existingDeleted = service(SERVICE_A1, 0).setDeleted(true);
        mdmSupplierRepository.insert(existingDeleted);

        processor.doProcess(message(
            businessEvent(BUSINESS_A),
            serviceEvent(SERVICE_A1, BUSINESS_A),
            serviceEvent(SERVICE_A2, BUSINESS_A),
            serviceEvent(BeruIdMock.DEFAULT_PROD_FP_ID, BeruIdMock.DEFAULT_PROD_BIZ_ID) // проигнорируется
        ));

        MdmSupplier expectedBusinessA = business(BUSINESS_A);
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier expectedServiceA2 = service(SERVICE_A2, BUSINESS_A);
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                expectedBusinessA, expectedServiceA1, expectedServiceA2
            );
    }

    @Test
    public void test1pReceivedIfAllowed() {
        storageKeyValueService.putValue(MdmProperties.ALLOW_1P_CHANGE_FROM_MBI, true);
        storageKeyValueService.invalidateCache();

        processor.doProcess(message(serviceEvent(BeruIdMock.DEFAULT_PROD_FP_ID, BeruIdMock.DEFAULT_PROD_BIZ_ID)));
        MdmSupplier expectedService = service(BeruIdMock.DEFAULT_PROD_FP_ID, BeruIdMock.DEFAULT_PROD_BIZ_ID);
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(expectedService);
    }

    @Test
    public void testOldSuppliersUpdated() throws Exception {
        MdmSupplier existingA1 = service(SERVICE_A1).setDeleted(true);
        MdmSupplier existingA2 = service(SERVICE_A2).setName("oldname2");
        mdmSupplierRepository.insertBatch(existingA1, existingA2);

        processor.doProcess(message(
            serviceEvent(SERVICE_A1).toBuilder().setName("newname1").build(),
            serviceEvent(SERVICE_A2).toBuilder().setName("newname2").build()
        ));

        MdmSupplier expectedServiceA1 = service(SERVICE_A1).setName("newname1");
        MdmSupplier expectedServiceA2 = service(SERVICE_A2).setName("newname2");
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                expectedServiceA1, expectedServiceA2
            );
    }

    @Test
    public void testOldWhiteSuppliersUpdated() throws Exception {
        MdmSupplier existingA1 = service(SERVICE_A1).setDeleted(true).setType(MdmSupplierType.MARKET_SHOP);
        MdmSupplier existingA2 = service(SERVICE_A2).setName("oldname2").setType(MdmSupplierType.MARKET_SHOP);
        mdmSupplierRepository.insertBatch(existingA1, existingA2);

        processor.doProcess(message(
            serviceEvent(SERVICE_A1, PartnerInfo.MbiPartnerType.SHOP, PartnerInfo.SupplierType.REAL, null)
                .toBuilder().setName("newname1").build(),
            serviceEvent(SERVICE_A2, PartnerInfo.MbiPartnerType.SHOP, PartnerInfo.SupplierType.REAL, null)
                .toBuilder().setName("newname2").build()
        ));

        MdmSupplier expectedServiceA1 = service(SERVICE_A1).setName("newname1").setType(MdmSupplierType.MARKET_SHOP);
        MdmSupplier expectedServiceA2 = service(SERVICE_A2).setName("newname2").setType(MdmSupplierType.MARKET_SHOP);
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                expectedServiceA1, expectedServiceA2
            );
    }

    @Test
    public void testBusinessSwitchTransportUpdatedWhenOldWhiteSupplierUpdated() throws Exception {
        MdmSupplier existing = service(SERVICE_A2).setName("oldname2").setType(MdmSupplierType.MARKET_SHOP)
            .setBusinessId(BUSINESS_A);
        MdmSupplier existingBusiness1 = business(BUSINESS_A);
        MdmSupplier existingBusiness2 = business(BUSINESS_B);
        mdmSupplierRepository.insertBatch(existing, existingBusiness1, existingBusiness2);

        processor.doProcess(message(
            serviceEvent(SERVICE_A2, PartnerInfo.MbiPartnerType.SHOP, PartnerInfo.SupplierType.REAL, BUSINESS_B)
                .toBuilder().setName("newname2").build()
        ));

        MdmSupplier expectedService = service(SERVICE_A2).setName("newname2").setType(MdmSupplierType.MARKET_SHOP)
            .setBusinessId(BUSINESS_B);
        List<MdmSupplier> updated = mdmSupplierRepository.findByIds(List.of(SERVICE_A2));
        Assertions.assertThat(updated).hasSize(1);
        Assertions.assertThat(updated)
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(expectedService);
        Assertions.assertThat(updated.get(0).getBusinessSwitchTransports().keySet())
            .containsExactlyInAnyOrder(BusinessSwitchTransport.MBI_LOGBROKER);
    }

    @Test
    public void testRealSupplierWithoutRSIDMustBeIgnored() {
        MdmSupplier existingDeleted = service(SERVICE_A1, 0).setDeleted(true);
        mdmSupplierRepository.insert(existingDeleted);

        processor.doProcess(message(
            businessEvent(BUSINESS_A), serviceEvent(SERVICE_A1, BUSINESS_A),
            serviceEvent(SERVICE_B1, true) //no RSID, must not be saved to mdmSupplierRepository
        ));

        MdmSupplier expectedBusinessA = business(BUSINESS_A);
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A);
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                expectedBusinessA, expectedServiceA1
            );
    }

    @Test
    public void testStageChanged() throws Exception {
        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A).setBusinessEnabled(true);
        mdmSupplierRepository.insertBatch(existingBusinessA, existingServiceA1, existingServiceA2);

        processor.doProcess(message(
            stageChangeEvent(SERVICE_A1, BUSINESS_A, true),
            stageChangeEvent(SERVICE_A2, BUSINESS_A, false)
        ));

        MdmSupplier expectedBusinessA = existingBusinessA;
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A).setBusinessEnabled(true);
        MdmSupplier expectedServiceA2 = service(SERVICE_A2, BUSINESS_A);
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                expectedBusinessA, expectedServiceA1, expectedServiceA2
            );
    }

    @Test
    public void testBusinessChanged() throws Exception {
        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);
        MdmSupplier existingBusinessB = business(BUSINESS_B);
        MdmSupplier existingServiceB1 = service(SERVICE_B1, BUSINESS_B);
        MdmSupplier existingServiceB2 = service(SERVICE_B2, BUSINESS_B);
        mdmSupplierRepository.insertBatch(
            existingBusinessA, existingServiceA1, existingServiceA2,
            existingBusinessB, existingServiceB1, existingServiceB2);

        processor.doProcess(message(
            businessChangeEvent(SERVICE_A1, BUSINESS_B),
            businessChangeEvent(SERVICE_B1, BUSINESS_A)
        ));

        MdmSupplier expectedBusinessA = existingBusinessA;
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_B);
        MdmSupplier expectedServiceA2 = existingServiceA2;
        MdmSupplier expectedBusinessB = existingBusinessB;
        MdmSupplier expectedServiceB1 = service(SERVICE_B1, BUSINESS_A);
        MdmSupplier expectedServiceB2 = existingServiceB2;
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                expectedBusinessA, expectedServiceA1, expectedServiceA2,
                expectedBusinessB, expectedServiceB1, expectedServiceB2
            );
    }

    @Test
    public void testStageChangedOnNewSupplier() throws Exception {
        MdmSupplier existingBusinessA = business(BUSINESS_A);
        mdmSupplierRepository.insertBatch(existingBusinessA);

        processor.doProcess(message(
            stageChangeEvent(SERVICE_A1, BUSINESS_A, true),
            stageChangeEvent(SERVICE_A2, BUSINESS_A, false)
        ));

        MdmSupplier expectedBusinessA = existingBusinessA;
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A).setBusinessEnabled(true);
        MdmSupplier expectedServiceA2 = service(SERVICE_A2, BUSINESS_A);
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                expectedBusinessA, expectedServiceA1, expectedServiceA2
            );
    }

    @Test
    public void testDuplicateMessages() throws Exception {
        MdmSupplier existingDeleted = service(SERVICE_A1, 0).setDeleted(true);
        mdmSupplierRepository.insert(existingDeleted);

        processor.doProcess(message(
            businessEvent(BUSINESS_A), serviceEvent(SERVICE_A1, BUSINESS_A), serviceEvent(SERVICE_A2, BUSINESS_A),
            businessEvent(BUSINESS_A), serviceEvent(SERVICE_A1, BUSINESS_A), serviceEvent(SERVICE_A2, BUSINESS_A)
        ));

        MdmSupplier expectedBusinessA = business(BUSINESS_A);
        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier expectedServiceA2 = service(SERVICE_A2, BUSINESS_A);
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                expectedBusinessA, expectedServiceA1, expectedServiceA2
            );
    }

    @Test
    public void testWhenChangeBlueSupplierThenEnqueue() {
        generateMappingsCache();

        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, 0);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);
        MdmSupplier existingBusinessB = business(BUSINESS_B);
        MdmSupplier existingServiceB1 = service(SERVICE_B1, BUSINESS_B);
        MdmSupplier existingServiceB2 = service(SERVICE_B2, BUSINESS_B);
        mdmSupplierRepository.insertBatch(
            existingBusinessA, existingServiceA1, existingServiceA2,
            existingBusinessB, existingServiceB1, existingServiceB2);

        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A);

        processor.updatePartners(List.of(expectedServiceA1), BusinessSwitchTransport.MBI_LOGBROKER);

        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                existingBusinessA, expectedServiceA1, existingServiceA2,
                existingBusinessB, existingServiceB1, existingServiceB2
            );

        List<Integer> recalcSsku = sskuToRefreshRepository.findAll().stream()
            .map(it -> it.getEntityKey().getSupplierId())
            .collect(Collectors.toList());

        Assertions.assertThat(recalcSsku).containsExactly(SERVICE_A2);
    }

    @Test
    public void testWhenNonChangeBlueSupplierWithForcedTransportThenNotEnqueueAndUpdateTransport() {
        generateMappingsCache();

        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, 0);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);
        MdmSupplier existingBusinessB = business(BUSINESS_B);
        MdmSupplier existingServiceB1 = service(SERVICE_B1, BUSINESS_B);
        MdmSupplier existingServiceB2 = service(SERVICE_B2, BUSINESS_B);
        mdmSupplierRepository.insertBatch(
            existingBusinessA, existingServiceA1, existingServiceA2,
            existingBusinessB, existingServiceB1, existingServiceB2);

        processor.updatePartners(List.of(existingServiceA1), BusinessSwitchTransport.MBI_YT);

        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                existingBusinessA, existingServiceA1, existingServiceA2,
                existingBusinessB, existingServiceB1, existingServiceB2
            );
        // because BusinessSwitchTransport.MBI_YT is force-updated
        // see all force-updated sources in MbiPartnersLogbrokerDataProcessorImpl
        Assertions.assertThat(mdmSupplierRepository.findById(SERVICE_A1).getBusinessSwitchTransports().keySet())
            .containsExactlyInAnyOrder(BusinessSwitchTransport.MBI_YT);

        List<SskuToRefreshInfo> recalcSsku = sskuToRefreshRepository.findAll();

        Assertions.assertThat(recalcSsku).isEmpty();
    }

    @Test
    public void testWhenEnabledNeighbourSwitchedShouldNotEnqueueIfHasNotEoxedSubgroups() {
        generateMappingsCache(List.of(SERVICE_A1, SERVICE_A2));

        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);
        mdmSupplierRepository.insertBatch(
            existingBusinessA, existingServiceA1, existingServiceA2);

        // if hasn't any eoxed services
        processor.updatePartners(List.of(existingServiceA1.setBusinessEnabled(true)), BusinessSwitchTransport.MBI_YT);

        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                existingBusinessA, existingServiceA1, existingServiceA2
            );
        Assertions.assertThat(mdmSupplierRepository.findById(SERVICE_A1).getBusinessSwitchTransports().keySet())
            .containsExactlyInAnyOrder(BusinessSwitchTransport.MBI_YT);
        Assertions.assertThat(mdmSupplierRepository.findById(SERVICE_A2).getBusinessSwitchTransports().keySet())
            .isEmpty();

        List<Integer> recalcSsku = sskuToRefreshRepository.findAll()
            .stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .map(ShopSkuKey::getSupplierId)
            .collect(Collectors.toList());

        Assertions.assertThat(recalcSsku).isEmpty();
    }

    @Test
    public void testWhenEnabledNeighbourSwitchedShouldEnqueueIfHasEoxedSubgroups() {
        var mappings = generateMappingsCache(List.of(SERVICE_A1, SERVICE_A2));

        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);

        // if has at least 1 eoxed service
        existingServiceA2.setBusinessEnabled(true);
        existingServiceA1.setBusinessEnabled(false);
        mdmSupplierRepository.insertBatch(existingServiceA2, existingServiceA1, existingBusinessA);
        var service2Keys = mappings.stream()
            .filter(mapping -> mapping.getSupplierId() == SERVICE_A2)
            .map(MappingCacheDao::getShopSkuKey)
            .collect(Collectors.toList());
        sskuExistenceRepository.markExistence(service2Keys, true);

        processor.updatePartners(List.of(existingServiceA1.setBusinessEnabled(true)), BusinessSwitchTransport.MBI_YT);
        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                existingBusinessA, existingServiceA1, existingServiceA2
            );
        Assertions.assertThat(mdmSupplierRepository.findById(SERVICE_A1).getBusinessSwitchTransports().keySet())
            .containsExactlyInAnyOrder(BusinessSwitchTransport.MBI_YT);
        Assertions.assertThat(mdmSupplierRepository.findById(SERVICE_A2).getBusinessSwitchTransports().keySet())
            .isEmpty();

        List<Integer> recalcSsku = sskuToRefreshRepository.findAll()
            .stream()
            .map(MdmQueueInfoBase::getEntityKey)
            .map(ShopSkuKey::getSupplierId)
            .collect(Collectors.toList());

        Assertions.assertThat(recalcSsku).containsExactly(BUSINESS_A);
    }

    @Test
    public void testWhenChangeWhiteSupplierThenDoesNotEnqueue() {
        generateMappingsCache();

        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);
        MdmSupplier existingWhiteA1 = serviceWhite(WHITE_A1, 0);
        MdmSupplier existingBusinessB = business(BUSINESS_B);
        MdmSupplier existingServiceB1 = service(SERVICE_B1, BUSINESS_B);
        MdmSupplier existingServiceB2 = service(SERVICE_B2, BUSINESS_B);
        mdmSupplierRepository.insertBatch(
            existingBusinessA, existingServiceA1, existingServiceA2, existingWhiteA1,
            existingBusinessB, existingServiceB1, existingServiceB2);

        MdmSupplier expectedWhiteA1 = serviceWhite(WHITE_A1, BUSINESS_A);

        processor.updatePartners(List.of(expectedWhiteA1), BusinessSwitchTransport.MBI_LOGBROKER);

        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                existingBusinessA, existingServiceA1, existingServiceA2, expectedWhiteA1,
                existingBusinessB, existingServiceB1, existingServiceB2
            );

        List<Integer> recalcSsku = sskuToRefreshRepository.findAll().stream()
            .map(it -> it.getEntityKey().getSupplierId())
            .collect(Collectors.toList());

        Assertions.assertThat(recalcSsku).isEmpty();
    }

    @Test
    public void testWhenNonChangeWhiteSupplierWithForcedTransportThenNotEnqueueAndUpdateTransport() {
        generateMappingsCache();

        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);
        MdmSupplier existingWhiteA1 = serviceWhite(WHITE_A1, 0);
        MdmSupplier existingBusinessB = business(BUSINESS_B);
        MdmSupplier existingServiceB1 = service(SERVICE_B1, BUSINESS_B);
        MdmSupplier existingServiceB2 = service(SERVICE_B2, BUSINESS_B);
        mdmSupplierRepository.insertBatch(
            existingBusinessA, existingServiceA1, existingServiceA2, existingWhiteA1,
            existingBusinessB, existingServiceB1, existingServiceB2);

        processor.updatePartners(List.of(existingWhiteA1), BusinessSwitchTransport.MBI_LOGBROKER);

        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                existingBusinessA, existingServiceA1, existingServiceA2, existingWhiteA1,
                existingBusinessB, existingServiceB1, existingServiceB2
            );
        // because BusinessSwitchTransport.MBI_LOGBROKER is force-updated
        // see all force-updated sources in MbiPartnersLogbrokerDataProcessorImpl
        Assertions.assertThat(mdmSupplierRepository.findById(WHITE_A1).getBusinessSwitchTransports().keySet())
            .containsExactlyInAnyOrder(BusinessSwitchTransport.MBI_LOGBROKER);

        List<SskuToRefreshInfo> recalcSsku = sskuToRefreshRepository.findAll();

        Assertions.assertThat(recalcSsku).isEmpty();
    }

    @Test
    public void testWhenNewBlueSupplierThenEnqueue() {
        generateMappingsCache();

        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);

        mdmSupplierRepository.insertBatch(existingBusinessA, existingServiceA2);

        MdmSupplier expectedServiceA1 = service(SERVICE_A1, BUSINESS_A);
        MdmSupplier expectedServiceB2 = service(SERVICE_B2, 0);

        processor.updatePartners(List.of(expectedServiceA1, expectedServiceB2),
            BusinessSwitchTransport.MBI_LOGBROKER);

        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(existingBusinessA, expectedServiceA1, existingServiceA2, expectedServiceB2);

        List<Integer> recalcSsku = sskuToRefreshRepository.findAll().stream()
            .map(it -> it.getEntityKey().getSupplierId())
            .collect(Collectors.toList());

        //для новых партнеров на пересчет попадают именно они, а не вся бизнес группа и только если уже есть мапинги
        Assertions.assertThat(recalcSsku).containsExactlyInAnyOrder(SERVICE_B2);
    }

    @Test
    public void testWhenNewWhiteSupplierThenDoesNotEnqueue() {
        generateMappingsCache();

        MdmSupplier existingBusinessA = business(BUSINESS_A);
        MdmSupplier existingServiceA2 = service(SERVICE_A2, BUSINESS_A);

        mdmSupplierRepository.insertBatch(existingBusinessA, existingServiceA2);

        MdmSupplier expectedWhiteA1 = serviceWhite(WHITE_A1, BUSINESS_A);
        MdmSupplier expectedWhiteB1 = serviceWhite(WHITE_B1, 0);

        processor.updatePartners(List.of(expectedWhiteA1, expectedWhiteB1),
            BusinessSwitchTransport.MBI_LOGBROKER);

        Assertions.assertThat(mdmSupplierRepository.findAll())
            .usingElementComparatorIgnoringFields("businessStateUpdatedTs", "updatedTs", "businessSwitchTransports")
            .containsExactlyInAnyOrder(
                existingBusinessA, existingServiceA2, expectedWhiteA1, expectedWhiteB1
            );

        List<Integer> recalcSsku = sskuToRefreshRepository.findAll().stream()
            .map(it -> it.getEntityKey().getSupplierId())
            .collect(Collectors.toList());

        Assertions.assertThat(recalcSsku).isEmpty();
    }

    @Test
    public void testSupplierSalesModelSuccessfullyImportedFromMbi() {
        Map<String, List<MdmSupplierSalesModel>> map = new LinkedHashMap<>();
        map.put(CLICK_AND_COLLECT, List.of(MdmSupplierSalesModel.CLICK_AND_COLLECT));
        map.put(CROSSDOCK, List.of(MdmSupplierSalesModel.CROSSDOCK));
        map.put(DROPSHIP, List.of(MdmSupplierSalesModel.DROPSHIP));
        map.put(DROPSHIP_BY_SELLER, List.of(MdmSupplierSalesModel.DROPSHIP_BY_SELLER));
        map.put(FULFILLMENT, List.of(MdmSupplierSalesModel.FULFILLMENT));

        List<String> allActualSalesModels = new ArrayList<>();
        List<MdmSupplierSalesModel> allExpectedSalesModels = new ArrayList<>();
        map.forEach((actualSalesModel, expectedSalesModel) -> {
            allActualSalesModels.add(actualSalesModel);
            allExpectedSalesModels.addAll(expectedSalesModel);

            processor.doProcess(message(serviceEvent(SERVICE_A1, allActualSalesModels)));

            MdmSupplier savedSupplier = mdmSupplierRepository.findById(SERVICE_A1);
            Assertions.assertThat(savedSupplier.getSalesModels())
                .containsExactlyInAnyOrderElementsOf(allExpectedSalesModels);

            mdmSupplierRepository.deleteAll();
        });
    }

    @Test
    public void whenSupplierIdOverflowsIntSkipItWithoutException() {
        // given
        var serviceEvent = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(1L << 45)
            .setBusinessId(100)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setUpdateType(PartnerInfo.UpdateType.IS_PUSH)
            .build();

        // when
        processor.doProcess(message(serviceEvent));

        // then
        Assertions.assertThat(mdmSupplierRepository.findAll()).isEmpty();
    }

    private MdmSupplier service(int id) {
        return new MdmSupplier()
            .setId(id)
            .setType(MdmSupplierType.THIRD_PARTY);
    }

    private MdmSupplier service(int id, int businessId) {
        return new MdmSupplier()
            .setId(id)
            .setBusinessId(businessId)
            .setType(id == BeruIdMock.DEFAULT_PROD_FP_ID ? MdmSupplierType.FIRST_PARTY : MdmSupplierType.THIRD_PARTY);
    }

    private MdmSupplier serviceWhite(int id, int businessId) {
        return new MdmSupplier()
            .setId(id)
            .setBusinessId(businessId)
            .setType(MdmSupplierType.MARKET_SHOP);
    }

    private MdmSupplier business(int id) {
        return new MdmSupplier()
            .setId(id)
            .setType(MdmSupplierType.BUSINESS);
    }

    private MessageBatch message(PartnerInfo.PartnerInfoEvent... events) {
        List<MessageData> messageData = new ArrayList<>();
        for (PartnerInfo.PartnerInfoEvent event : events) {
            byte[] data = event.toByteArray();
            messageData.add(new MessageData(data, 0, null));
        }
        return new MessageBatch("", 0, messageData);
    }

    private PartnerInfo.PartnerInfoEvent serviceEvent(int id, PartnerInfo.MbiPartnerType type,
                                                      PartnerInfo.SupplierType supplierType, Integer businessId) {
        var eventBuilder = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setType(type)
            .setSupplierType(supplierType)
            .setUpdateType(PartnerInfo.UpdateType.IS_PUSH); // в протобуфке нет дефолта, возьмём рандомный ненужный
        if (businessId != null) {
            eventBuilder.setBusinessId(businessId);
        }
        return eventBuilder.build();
    }

    private PartnerInfo.PartnerInfoEvent serviceEvent(int id) {
        return serviceEvent(id, PartnerInfo.MbiPartnerType.SUPPLIER, PartnerInfo.SupplierType.THREE_P, null);
    }

    private PartnerInfo.PartnerInfoEvent serviceEvent(int id, int businessId) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setBusinessId(businessId)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(
                id == BeruIdMock.DEFAULT_PROD_FP_ID ? PartnerInfo.SupplierType.ONE_P : PartnerInfo.SupplierType.THREE_P)
            .setUpdateType(PartnerInfo.UpdateType.IS_PUSH) // в протобуфке нет дефолта, возьмём рандомный ненужный
            .build();
    }

    private PartnerInfo.PartnerInfoEvent serviceEvent(int id, boolean isRealSupplier) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(isRealSupplier ? PartnerInfo.SupplierType.REAL : PartnerInfo.SupplierType.THREE_P)
            .setUpdateType(PartnerInfo.UpdateType.IS_PUSH) // в протобуфке нет дефолта, возьмём рандомный ненужный
            .build();
    }

    private PartnerInfo.PartnerInfoEvent serviceEvent(int id, List<String> salesModels) {
        PartnerInfo.PartnerInfoEvent.Builder partnerInfoBuilder = PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setUpdateType(PartnerInfo.UpdateType.IS_PUSH); // в протобуфке нет дефолта, возьмём рандомный ненужный

        if (salesModels.contains(CLICK_AND_COLLECT)) {
            partnerInfoBuilder.setIsClickAndCollect(true);
        }
        if (salesModels.contains(CROSSDOCK)) {
            partnerInfoBuilder.setIsCrossdock(true);
        }
        if (salesModels.contains(DROPSHIP)) {
            partnerInfoBuilder.setIsDropship(true);
        }
        if (salesModels.contains(DROPSHIP_BY_SELLER)) {
            partnerInfoBuilder.setIsDropshipBySeller(true);
        }
        if (salesModels.contains(FULFILLMENT)) {
            partnerInfoBuilder.setIsFullfilment(true);
        }

        return partnerInfoBuilder
            .build();
    }

    private PartnerInfo.PartnerInfoEvent businessEvent(int id) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setUpdateType(PartnerInfo.UpdateType.IS_PUSH) // в протобуфке нет дефолта, возьмём рандомный ненужный
            .build();
    }

    private PartnerInfo.PartnerInfoEvent businessChangeEvent(int id, int businessId) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setBusinessId(businessId)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setUpdateType(PartnerInfo.UpdateType.SERVICE_LINK)
            .build();
    }

    private PartnerInfo.PartnerInfoEvent stageChangeEvent(int id, int businessId, boolean isStage3) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setBusinessId(businessId)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setIsUnitedCatalog(isStage3)
            .setUpdateType(PartnerInfo.UpdateType.UNITED_CATALOG)
            .build();
    }

    private void generateMappingsCache() {
        generateMappingsCache(List.of(WHITE_A1, WHITE_B1, SERVICE_A2, SERVICE_B2));
    }

    private List<MappingCacheDao> generateMappingsCache(Collection<Integer> ids) {
        List<MappingCacheDao> mappings = ids.stream()
            .map(supplierId -> new MappingCacheDao()
                .setCategoryId(1)
                .setMskuId(1L)
                .setShopSku("123")
                .setSupplierId(supplierId))
            .collect(Collectors.toList());

        return mappingsCacheRepository.insertOrUpdateAll(mappings);
    }
}
