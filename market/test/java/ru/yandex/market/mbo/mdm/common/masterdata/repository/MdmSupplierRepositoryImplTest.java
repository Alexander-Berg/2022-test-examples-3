package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmBusinessStage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierIdGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierSalesModel;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.BusinessSwitchTransport;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSupplierCachingServiceImpl;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

/**
 * @author dmserebr
 * @date 16/07/2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class MdmSupplierRepositoryImplTest
    extends MdmGenericMapperRepositoryTestBase<MdmSupplierRepository, MdmSupplier, Integer> {

    private static final int SUPPLIER_1_OF_BUSINESS_1 = 101;
    private static final int SUPPLIER_2_OF_BUSINESS_1 = 102;
    private static final int SUPPLIER_OF_BUSINESS_2 = 103;
    private static final int SUPPLIER_WITHOUT_BUSINESS = 105;
    private static final int WHITE_SUPPLIER_OF_BUSINESS_1 = 106;
    private static final int WHITE_SUPPLIER_WITHOUT_BUSINESS = 107;
    private static final int UNKNOWN_SUPPLIER = 108;

    private static final int BUSINESS_1 = 201;
    private static final int BUSINESS_2 = 202;
    private static final int BUSINESS_3 = 203;

    private StorageKeyValueServiceMock storageKeyValueServiceMock;
    private MdmSupplierCachingServiceImpl cachingService;

    @Before
    public void setup() {
        super.setup();
        storageKeyValueServiceMock = new StorageKeyValueServiceMock();
        cachingService = new MdmSupplierCachingServiceImpl(repository, storageKeyValueServiceMock);
    }

    @Override
    protected MdmSupplier randomRecord() {
        return random.nextObject(MdmSupplier.class);
    }

    @Override
    protected String[] getFieldsToIgnore() {
        return new String[]{"updatedTs"};
    }

    @Override
    protected Function<MdmSupplier, Integer> getIdSupplier() {
        return MdmSupplier::getId;
    }

    @Override
    protected List<BiConsumer<Integer, MdmSupplier>> getUpdaters() {
        return List.of(
            (i, record) -> record.setType(MdmSupplierType.REAL_SUPPLIER),
            (i, record) -> record.setType(MdmSupplierType.FMCG),
            (i, record) -> {
                record.setType(MdmSupplierType.THIRD_PARTY);
                record.setCrossdockWarehouseId(12L);
            }
        );
    }

    @Test
    public void testFindByBusinessId() {
        MdmSupplier record1 = randomRecord();
        record1.setBusinessId(10);

        MdmSupplier record2 = randomRecord();
        record2.setBusinessId(10);

        MdmSupplier record3 = randomRecord();
        record3.setBusinessId(15);

        MdmSupplier record4 = randomRecord();
        record4.setBusinessId(20);

        repository.insertBatch(List.of(record1, record2, record3, record4));

        Map<Integer, List<MdmSupplier>> result = repository.findByBusinessIds(List.of(10, 20));
        Assertions.assertThat(result).hasSize(2);
        Assertions.assertThat(result.get(10))
            .usingElementComparatorIgnoringFields("updatedTs")
            .containsExactlyInAnyOrder(record1, record2);
        Assertions.assertThat(result.get(20))
            .usingElementComparatorIgnoringFields("updatedTs")
            .containsExactlyInAnyOrder(record4);
    }

    @Test
    public void testFindByType() {
        MdmSupplier record1 = randomRecord();
        record1.setType(MdmSupplierType.FMCG);

        MdmSupplier record2 = randomRecord();

        MdmSupplier record3 = randomRecord();
        record3.setType(MdmSupplierType.FMCG);

        List<MdmSupplier> records = List.of(record1, record2, record3);
        repository.insertBatch(records);
        List<MdmSupplier> found = repository.findAllOfTypes(MdmSupplierType.FMCG);
        Assertions.assertThat(found)
            .usingElementComparatorIgnoringFields("updatedTs")
            .containsExactlyInAnyOrder(record1, record3);
    }

    @Test
    public void testGetSupplierGroupsFor() {
        MdmSupplier childSupplier1 = createSupplier(SUPPLIER_1_OF_BUSINESS_1, MdmSupplierType.THIRD_PARTY, BUSINESS_1);
        MdmSupplier childSupplier2 = createSupplier(SUPPLIER_2_OF_BUSINESS_1, MdmSupplierType.THIRD_PARTY, BUSINESS_1);
        MdmSupplier childSupplier3 = createSupplier(SUPPLIER_OF_BUSINESS_2, MdmSupplierType.THIRD_PARTY, BUSINESS_2);
        MdmSupplier supplier4 = createSupplier(SUPPLIER_WITHOUT_BUSINESS, MdmSupplierType.THIRD_PARTY, null);
        MdmSupplier business1 = createSupplier(BUSINESS_1, MdmSupplierType.BUSINESS, null);
        MdmSupplier business2 = createSupplier(BUSINESS_2, MdmSupplierType.BUSINESS, null);
        MdmSupplier business3 = createSupplier(BUSINESS_3, MdmSupplierType.BUSINESS, null);
        MdmSupplier whiteChildSupplier1 = createSupplier(WHITE_SUPPLIER_OF_BUSINESS_1,
            MdmSupplierType.MARKET_SHOP, BUSINESS_1);
        MdmSupplier whiteSupplier = createSupplier(WHITE_SUPPLIER_WITHOUT_BUSINESS,
            MdmSupplierType.MARKET_SHOP, null);
        repository.insertBatch(childSupplier1, childSupplier2, childSupplier3, supplier4,
            business1, business2, business3, whiteChildSupplier1, whiteSupplier);
        cachingService.refresh();

        List<MdmSupplierIdGroup> result = cachingService.getSupplierIdGroupsFor(
            List.of(SUPPLIER_1_OF_BUSINESS_1, BUSINESS_2, SUPPLIER_WITHOUT_BUSINESS, BUSINESS_3,
                WHITE_SUPPLIER_WITHOUT_BUSINESS, UNKNOWN_SUPPLIER));

        Assertions.assertThat(result).hasSize(3);
        var group1 = MdmSupplierIdGroup.createBusinessGroup(BUSINESS_1,
            List.of(SUPPLIER_1_OF_BUSINESS_1, SUPPLIER_2_OF_BUSINESS_1));
        var group2 = MdmSupplierIdGroup.createBusinessGroup(BUSINESS_2,
            List.of(SUPPLIER_OF_BUSINESS_2));
        var group3 = MdmSupplierIdGroup.createNoBusinessGroup(SUPPLIER_WITHOUT_BUSINESS);

        Assertions.assertThat(result).containsExactlyInAnyOrder(group1, group2, group3);

        Assertions.assertThat(cachingService.getBusinessEnableMode(WHITE_SUPPLIER_OF_BUSINESS_1))
            .isEqualTo(MdmBusinessStage.NO_DATA);
        Assertions.assertThat(cachingService.getBusinessEnableMode(WHITE_SUPPLIER_WITHOUT_BUSINESS))
            .isEqualTo(MdmBusinessStage.NO_DATA);
        Assertions.assertThat(cachingService.getBusinessEnableMode(UNKNOWN_SUPPLIER))
            .isEqualTo(MdmBusinessStage.NO_DATA);
    }

    @Test
    public void testPartiallyEnabledBusinessGroups() {
        final int rootAId = 1;
        final int childA1Id = 11;
        final int childA2Id = 12;
        final int rootBId = 2;
        final int childB1Id = 21;
        final int childB2Id = 22;
        final int rootCId = 3;
        final int childC1Id = 31;
        final int childC2Id = 32;
        final int lonely1Id = 61;
        final int lonely2Id = 62;

        // 1. Полностью отключённая группа
        MdmSupplier rootA = createBusiness(rootAId, false);
        MdmSupplier childA1 = createSupplier(childA1Id, rootAId, false);
        MdmSupplier childA2 = createSupplier(childA2Id, rootAId, false);

        // 2. Включён один сервис
        MdmSupplier rootB = createBusiness(rootBId, false);
        MdmSupplier childB1 = createSupplier(childB1Id, rootBId, false);
        MdmSupplier childB2 = createSupplier(childB2Id, rootBId, true);

        // 3. Включены все сервисы
        MdmSupplier rootC = createBusiness(rootCId, false);
        MdmSupplier childC1 = createSupplier(childC1Id, rootCId, true);
        MdmSupplier childC2 = createSupplier(childC2Id, rootCId, true);

        // 4. Бизнесов вообще нет
        MdmSupplier lonely1 = createSupplier(lonely1Id, null, false);
        MdmSupplier lonely2 = createSupplier(lonely2Id, null, true);

        repository.insertBatch(
            rootA, rootB, rootC,
            childA1, childA2, childB1, childB2, childC1, childC2,
            lonely1, lonely2);
        cachingService.refresh();

        List<MdmSupplierIdGroup> result = cachingService.getSupplierIdGroupsFor(List.of(
            childA1Id, childB2Id, rootCId, lonely1Id, lonely2Id, childB1Id, rootAId
        ));

        var expectedGroupA11 = MdmSupplierIdGroup.createNoBusinessGroup(childA1Id);

        var expectedGroupB1 = MdmSupplierIdGroup.createBusinessGroup(rootBId, List.of(childB2Id));
        var expectedGroupB11 = MdmSupplierIdGroup.createNoBusinessGroup(childB1Id);

        var expectedGroupC1 = MdmSupplierIdGroup.createBusinessGroup(rootCId, List.of(childC1Id, childC2Id));
        var expectedGroupLone1 = MdmSupplierIdGroup.createNoBusinessGroup(lonely1Id);
        var expectedGroupLone2 = MdmSupplierIdGroup.createNoBusinessGroup(lonely2Id);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            expectedGroupA11,
            expectedGroupB1,
            expectedGroupB11,
            expectedGroupC1,
            expectedGroupLone1,
            expectedGroupLone2
        );

        Assertions.assertThat(cachingService.getBusinessEnableMode(childA1Id))
            .isEqualTo(MdmBusinessStage.BUSINESS_DISABLED);
        Assertions.assertThat(cachingService.getBusinessEnableMode(childA2Id))
            .isEqualTo(MdmBusinessStage.BUSINESS_DISABLED);
        Assertions.assertThat(cachingService.getBusinessEnableMode(childB1Id))
            .isEqualTo(MdmBusinessStage.BUSINESS_DISABLED);
        Assertions.assertThat(cachingService.getBusinessEnableMode(childB2Id))
            .isEqualTo(MdmBusinessStage.BUSINESS_ENABLED);
        Assertions.assertThat(cachingService.getBusinessEnableMode(childC1Id))
            .isEqualTo(MdmBusinessStage.BUSINESS_ENABLED);
        Assertions.assertThat(cachingService.getBusinessEnableMode(childC2Id))
            .isEqualTo(MdmBusinessStage.BUSINESS_ENABLED);
        Assertions.assertThat(cachingService.getBusinessEnableMode(lonely1Id))
            .isEqualTo(MdmBusinessStage.NO_BUSINESS);
        Assertions.assertThat(cachingService.getBusinessEnableMode(lonely1Id))
            .isEqualTo(MdmBusinessStage.NO_BUSINESS);
        Assertions.assertThat(cachingService.getBusinessEnableMode(rootAId))
            .isEqualTo(MdmBusinessStage.NO_DATA);
    }

    @Test
    public void testPartiallyEnabledFlatBusinessGroups() {
        final int rootAId = 1;
        final int childA1Id = 11;
        final int childA2Id = 12;
        final int rootBId = 2;
        final int childB1Id = 21;
        final int childB2Id = 22;
        final int rootCId = 3;
        final int childC1Id = 31;
        final int childC2Id = 32;
        final int lonely1Id = 61;
        final int lonely2Id = 62;
        final int whiteId1 = 71;
        final int whiteId2 = 72;
        final int unknownId1 = 81;

        // 1. Полностью отключённая группа
        MdmSupplier rootA = createBusiness(rootAId, false);
        MdmSupplier childA1 = createSupplier(childA1Id, rootAId, false);
        MdmSupplier childA2 = createSupplier(childA2Id, rootAId, false);

        // 2. Включён один сервис
        MdmSupplier rootB = createBusiness(rootBId, false);
        MdmSupplier childB1 = createSupplier(childB1Id, rootBId, false);
        MdmSupplier childB2 = createSupplier(childB2Id, rootBId, true);

        // 3. Включены все сервисы
        MdmSupplier rootC = createBusiness(rootCId, false);
        MdmSupplier childC1 = createSupplier(childC1Id, rootCId, true);
        MdmSupplier childC2 = createSupplier(childC2Id, rootCId, true);

        // 4. Бизнесов вообще нет
        MdmSupplier lonely1 = createSupplier(lonely1Id, null, false);
        MdmSupplier lonely2 = createSupplier(lonely2Id, null, true);

        MdmSupplier white1 = createSupplier(whiteId1, MdmSupplierType.MARKET_SHOP, rootBId);
        MdmSupplier white2 = createSupplier(whiteId2, MdmSupplierType.MARKET_SHOP, null);

        repository.insertBatch(
            rootA, rootB, rootC,
            childA1, childA2, childB1, childB2, childC1, childC2,
            lonely1, lonely2,
            white1, white2);
        cachingService.refresh();

        List<MdmSupplierIdGroup> result = cachingService.getFlatRelationsFor(List.of(
            childA1Id, childB2Id, rootCId, lonely1Id, lonely2Id, childB1Id, rootAId, whiteId1, whiteId2, unknownId1
        ));

        // При flat-поиске статус подсключённости игнорируется - должны собраться все группы, где есть бизнес.
        var expectedGroupA1 = MdmSupplierIdGroup.createBusinessGroup(rootAId, List.of(childA1Id, childA2Id));
        var expectedGroupB1 = MdmSupplierIdGroup.createBusinessGroup(rootBId, List.of(childB1Id, childB2Id));
        var expectedGroupC1 = MdmSupplierIdGroup.createBusinessGroup(rootCId, List.of(childC1Id, childC2Id));
        var expectedGroupLone1 = MdmSupplierIdGroup.createNoBusinessGroup(lonely1Id);
        var expectedGroupLone2 = MdmSupplierIdGroup.createNoBusinessGroup(lonely2Id);

        Assertions.assertThat(result).containsExactlyInAnyOrder(
            expectedGroupA1,
            expectedGroupB1,
            expectedGroupC1,
            expectedGroupLone1,
            expectedGroupLone2
        );
    }

    @Test
    public void testMarkSuppliersBusinessEnabledSuccessfullyUpdatesRows() {
        int businessId = 1;
        int idWithoutBusiness = 2, idWithBusinessEnabled = 3, idWithBusinessDisabled = 4;
        MdmSupplier businessSupplier = createBusiness(businessId, true);
        MdmSupplier supplierWithoutBusiness = createSupplier(idWithoutBusiness, null, false);
        MdmSupplier supplierWithBusinessEnabled = createSupplier(idWithBusinessEnabled, businessId, true);
        MdmSupplier supplierWithBusinessDisabled = createSupplier(idWithBusinessDisabled, businessId, false);

        repository.insertBatch(businessSupplier,
            supplierWithoutBusiness,
            supplierWithBusinessEnabled,
            supplierWithBusinessDisabled);

        repository.changeBusinessStage(List.of(idWithBusinessDisabled), List.of(businessId),
            MdmBusinessStage.BUSINESS_ENABLED, BusinessSwitchTransport.MBI_LOGBROKER);

        List<MdmSupplier> rows = repository.findAll();
        Assertions.assertThat(rows.size()).isEqualTo(4);

        Map<Integer, MdmSupplier> supplierById = rows.stream().collect(Collectors.toMap(MdmSupplier::getId,
            Function.identity()));

        MdmSupplier justEnabledSupplier = supplierById.get(idWithBusinessDisabled);
        Assertions.assertThat(justEnabledSupplier.isBusinessEnabled()).isEqualTo(true);
        assert justEnabledSupplier.getBusinessStateUpdatedTs().isBefore(Instant.now());

        // Проверяем, что другие объекты не затронуты
        Assertions.assertThat(supplierById.get(businessId).isBusinessEnabled()).isEqualTo(true);
        assert supplierById.get(businessId).getBusinessStateUpdatedTs() == null;

        Assertions.assertThat(supplierById.get(idWithoutBusiness).isBusinessEnabled()).isEqualTo(false);
        assert supplierById.get(idWithoutBusiness).getBusinessStateUpdatedTs() == null;

        Assertions.assertThat(supplierById.get(idWithBusinessEnabled).isBusinessEnabled()).isEqualTo(true);
        assert supplierById.get(idWithBusinessEnabled).getBusinessStateUpdatedTs() == null;
    }

    @Test
    public void testSupplierSalesModelCorrectlySavedAndParsed() {
        Set<MdmSupplierSalesModel> salesModels = Set.of(
            MdmSupplierSalesModel.CLICK_AND_COLLECT,
            MdmSupplierSalesModel.CROSSDOCK,
            MdmSupplierSalesModel.DROPSHIP,
            MdmSupplierSalesModel.DROPSHIP_BY_SELLER,
            MdmSupplierSalesModel.FULFILLMENT);

        List<MdmSupplierSalesModel> supplierSalesModels = new ArrayList<>();
        for (MdmSupplierSalesModel salesModel : salesModels) {
            MdmSupplier supplier = randomRecord();
            supplierSalesModels.add(salesModel);
            supplier.setSalesModels(supplierSalesModels);

            repository.insert(supplier);

            MdmSupplier savedSupplier = repository.findById(supplier.getId());
            Assertions.assertThat(savedSupplier).isEqualToIgnoringGivenFields(supplier, "updatedTs");
            Assertions.assertThat(savedSupplier.getSalesModels()).containsExactlyInAnyOrderElementsOf(supplierSalesModels);
        }
    }

    @Test
    public void findAllDbsServicesReturnsCorrectResult() {
        // given
        MdmSupplier dbsSupplier = randomRecord();
        dbsSupplier.setType(MdmSupplierType.MARKET_SHOP);
        dbsSupplier.setSalesModels(List.of(MdmSupplierSalesModel.DROPSHIP_BY_SELLER));

        MdmSupplier nonMdmCompatibleSupplier = randomRecord();
        nonMdmCompatibleSupplier.setType(MdmSupplierType.MARKET_SHOP);
        nonMdmCompatibleSupplier.setSalesModels(List.of(MdmSupplierSalesModel.CLICK_AND_COLLECT));

        repository.insertBatch(dbsSupplier, nonMdmCompatibleSupplier);

        // when
        Set<Integer> result = repository.findAllDbsServices();

        // then
        Assertions.assertThat(result.size()).isOne();
        MdmSupplier resultDbsSupplier = repository.findByIds(result).get(0);
        Assertions.assertThat(resultDbsSupplier).isEqualToIgnoringGivenFields(dbsSupplier, "id", "updatedTs");
    }

    private static MdmSupplier createSupplier(int id, MdmSupplierType type, Integer businessId) {
        var supplier = new MdmSupplier();
        supplier.setId(id);
        supplier.setType(type);
        supplier.setBusinessId(businessId);
        supplier.setBusinessEnabled(true);
        return supplier;
    }

    private static MdmSupplier createSupplier(int id, Integer businessId, boolean businessEnabled) {
        var supplier = new MdmSupplier();
        supplier.setId(id);
        supplier.setType(MdmSupplierType.THIRD_PARTY);
        supplier.setBusinessId(businessId);
        supplier.setBusinessEnabled(businessEnabled);
        return supplier;
    }

    private static MdmSupplier createBusiness(int id, boolean businessEnabled) {
        var supplier = new MdmSupplier();
        supplier.setId(id);
        supplier.setType(MdmSupplierType.BUSINESS);
        supplier.setBusinessEnabled(businessEnabled);
        return supplier;
    }
}
