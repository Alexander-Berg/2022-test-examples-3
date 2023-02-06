package ru.yandex.market.mbo.mdm.common.masterdata.services.iris;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.ir.http.MdmIrisPayload.MasterDataSource;
import ru.yandex.market.ir.http.MdmIrisPayload.ReferenceInformation;
import ru.yandex.market.ir.http.MdmIrisPayload.RemainingLifetime;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamOption;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.SskuSilverParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.queue.MdmQueueInfoBase;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverCommonSsku;
import ru.yandex.market.mbo.mdm.common.masterdata.model.ssku.SilverSskuKey;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.warehouse.MdmWarehouse;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.FromIrisItemRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.SskuExistenceRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.SilverSskuRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.FromIrisItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapper;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.ItemWrapperTestUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.proto.SourceItemKey;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.BatchProcessingProperties;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.SskuToRefreshRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.business.MdmSskuGroupManager;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.ServiceSskuConverter;
import ru.yandex.market.mbo.mdm.common.masterdata.services.warehouse.MdmWarehouseService;
import ru.yandex.market.mbo.mdm.common.masterdata.validator.WeightDimensionBlockValidationService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

@SuppressWarnings("checkstyle:magicnumber")
public class AddNewItemsServiceImplTest extends MdmBaseDbTestClass {
    @Autowired
    private MdmSupplierRepository supplierRepository;
    @Autowired
    private SilverSskuRepository silverSskuRepository;
    @Autowired
    private SskuToRefreshRepository sskuToRefreshQueue;
    @Autowired
    private FromIrisItemRepository fromIrisItemRepository;
    @Autowired
    private MdmWarehouseService mdmWarehouseService;
    @Autowired
    private AddNewItemsService addNewItemsService;
    @Autowired
    private TransactionHelper transactionHelper;
    @Autowired
    private WeightDimensionBlockValidationService weightDimensionBlockValidationService;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    @Autowired
    private MdmSskuGroupManager mdmSskuGroupManager;
    @Autowired
    private ServiceSskuConverter serviceSskuConverter;
    @Autowired
    private BeruId beruId;
    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private SskuExistenceRepository sskuExistenceRepository;

    private StorageKeyValueService storageKeyValueService;

    @Before
    public void setUp() {
        storageKeyValueService = new StorageKeyValueServiceMock();

        addNewItemsService = new AddNewItemsServiceImpl(
            transactionHelper,
            fromIrisItemRepository,
            mdmWarehouseService,
            weightDimensionBlockValidationService,
            mdmQueuesManager,
            silverSskuRepository,
            storageKeyValueService,
            mdmSskuGroupManager,
            serviceSskuConverter
        );
    }

    @Test
    public void whenUpdateToNewerItemShouldUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        ShopSkuKey businessKey = new ShopSkuKey(businessId, key.getShopSku());
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.Item item1 = generateItem(key, "original", 1);
        MdmIrisPayload.Item item2 = generateItem(key, "updated", 2);

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(businessKey));

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item2));
        verifyEnqueue(List.of(businessKey));
    }

    @Test
    public void whenUpdateToNewerEqualItemShouldNotUpdate() {
        SourceItemKey key = getSourceItemKey();
        MdmIrisPayload.Item item1 = generateItem(key, "original", 1);
        MdmIrisPayload.Item item2 = generateItem(key, "original", 2);

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        mdmSupplierRepository.insertOrUpdate(
            new MdmSupplier()
                .setId((int) key.getSupplierId())
                .setType(MdmSupplierType.THIRD_PARTY)
                .setBusinessEnabled(false));

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(key.getShopSkuKey()));

        processAllQueueItems();

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateToOlderItemShouldNotUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        ShopSkuKey businessKey = new ShopSkuKey(businessId, key.getShopSku());
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.Item item1 = generateItem(key, "original", 2);
        MdmIrisPayload.Item item2 = generateItem(key, "updated", 1);

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(businessKey));

        processAllQueueItems();

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateToNewerCompleteItemShouldUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        ShopSkuKey businessKey = new ShopSkuKey(businessId, key.getShopSku());
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.CompleteItem item1 = generateCompleteItem(key, "original", 1);
        MdmIrisPayload.CompleteItem item2 = generateCompleteItem(key, "updated", 2);

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(businessKey));

        processAllQueueItems();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item2));
        verifyEnqueue(List.of(businessKey));
    }

    @Test
    public void whenUpdateToNewerEqualCompleteItemShouldNotUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        ShopSkuKey businessKey = new ShopSkuKey(businessId, key.getShopSku());
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.CompleteItem item1 = generateCompleteItem(key, "original", 1);
        MdmIrisPayload.CompleteItem item2 = generateCompleteItem(key, "original", 2);

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(businessKey));

        processAllQueueItems();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateToOlderCompleteItemShouldNotUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        ShopSkuKey businessKey = new ShopSkuKey(businessId, key.getShopSku());
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.CompleteItem item1 = generateCompleteItem(key, "original", 2);
        MdmIrisPayload.CompleteItem item2 = generateCompleteItem(key, "updated", 1);

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(businessKey));

        processAllQueueItems();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateToZeroCompleteItemShouldNotUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        ShopSkuKey businessKey = new ShopSkuKey(businessId, key.getShopSku());
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.CompleteItem item1 = generateCompleteItem(key, "original", 2);
        MdmIrisPayload.CompleteItem.Builder builder = generateCompleteItem(key, "updated", 4).toBuilder();
        builder.getRemainingInformationBuilder(0)
            .getItemShippingUnitBuilder()
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(0).build())
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(0).build())
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(0).build())
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(0).build());

        MdmIrisPayload.CompleteItem item2 = builder.build();

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(businessKey));

        processAllQueueItems();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateToPartiallyZeroCompleteItemShouldNotUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        ShopSkuKey businessKey = new ShopSkuKey(businessId, key.getShopSku());
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.CompleteItem item1 = generateCompleteItem(key, "original", 2);
        MdmIrisPayload.CompleteItem.Builder builder = generateCompleteItem(key, "updated", 4).toBuilder();
        builder.getRemainingInformationBuilder(0)
            .getItemShippingUnitBuilder()
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(100000).build())
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(200000).build())
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(300000).build())
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(0).build());

        MdmIrisPayload.CompleteItem item2 = builder.build();

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(businessKey));

        processAllQueueItems();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateToExtremelySmallCompleteItemShouldNotUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.CompleteItem item1 = generateCompleteItem(key, "original", 2);
        MdmIrisPayload.CompleteItem.Builder builder = generateCompleteItem(key, "updated", 4).toBuilder();
        builder.getRemainingInformationBuilder(0)
            .getItemShippingUnitBuilder()
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(1).build())
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(2).build())
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(3).build())
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(1000).build());

        MdmIrisPayload.CompleteItem item2 = builder.build();

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(new ShopSkuKey(businessId, key.getShopSku())));

        processAllQueueItems();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateToExtremelyHeavyCompleteItemShouldNotUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.CompleteItem item1 = generateCompleteItem(key, "original", 2);
        MdmIrisPayload.CompleteItem.Builder builder = generateCompleteItem(key, "updated", 4).toBuilder();
        builder.getRemainingInformationBuilder(0)
            .getItemShippingUnitBuilder()
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(10000).build())
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(20000).build())
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(30000).build())
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder()
                .setUpdatedTs(4).setValue(2_000_000_000).build()); // 2 tons

        MdmIrisPayload.CompleteItem item2 = builder.build();

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(new ShopSkuKey(businessId, key.getShopSku())));

        processAllQueueItems();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateToEmptyItemShippingUnitShouldNotFailWithException() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        MdmIrisPayload.CompleteItem item1 = generateCompleteItem(key, "original", 2);
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.CompleteItem.Builder builder = generateCompleteItem(key, "updated", 4).toBuilder();
        builder.getRemainingInformationBuilder(0)
            .setItemShippingUnit(MdmIrisPayload.ShippingUnit.newBuilder()
                .clearWidthMicrometer()
                .clearHeightMicrometer()
                .clearLengthMicrometer()
                .clearWeightGrossMg()
            );
        MdmIrisPayload.CompleteItem item2 = builder.build();

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(new ShopSkuKey(businessId, key.getShopSku())));

        processAllQueueItems();

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateExistingZeroCompleteItemToNonzeroShouldUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.Item.Builder builder = generateItem(key, "original", 4).toBuilder();
        builder.getInformationBuilder(0)
            .getItemShippingUnitBuilder()
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(0).build())
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(0).build())
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(0).build())
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setUpdatedTs(4).setValue(0).build());

        MdmIrisPayload.Item item1 = builder.build();
        MdmIrisPayload.CompleteItem item2 = generateCompleteItem(key, "updated", 2);

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        // insert invalid data directly to repository
        fromIrisItemRepository.insertOrUpdate(toItemWrapper(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue(); // no enqueue when new item is invalid

        addNewItemsService.addAndUpdateCompleteItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item2));
        verifyEnqueue(List.of(new ShopSkuKey(businessId, key.getShopSku())));
    }

    @Test
    public void whenUpdateToNewerByRslOnlyShouldNotUpdate() {
        SourceItemKey key = getSourceItemKey();
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.Item item1 = generateItem(key, "original", 1);
        MdmIrisPayload.Item item2;

        // Пусть новый item2 не отличается ничем от item1 кроме ОСГ.
        item2 = item1.toBuilder().setInformation(0, item1.getInformation(0)
            .toBuilder()
            .addMinInboundLifetimeDay(RemainingLifetime.newBuilder().setUpdatedTs(3).setValue(25).build())
            .addMinOutboundLifetimeDay(RemainingLifetime.newBuilder().setUpdatedTs(3).setValue(65).build())
        ).build();

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item1));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyEnqueue(List.of(new ShopSkuKey(businessId, key.getShopSku())));

        processAllQueueItems();

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item2));
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item1));
        verifyNoEnqueue();
    }

    @Test
    public void whenDropshipShouldReplaceWithSupplier() {
        int supplierId = 42;
        int businessId = 43;
        String shopSku = "anything";
        String dropshipSourceId = "I AM A DROPSHIP";
        mdmWarehouseService.addOrUpdate(new MdmWarehouse().setId(dropshipSourceId).setLmsType(PartnerType.DROPSHIP));
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, List.of(shopSku));

        SourceItemKey key = new SourceItemKey(supplierId, shopSku, MasterDataSource.WAREHOUSE, dropshipSourceId);
        MdmIrisPayload.Item item = generateItem(key, "original", 1);

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item));

        // Change type and source id to supplier
        FromIrisItemWrapper expectedItem = toItemWrapper(item);
        ReferenceInformation.Builder info = expectedItem.getReferenceInformation().toBuilder();
        info.getSourceBuilder().setType(MasterDataSource.SUPPLIER);
        expectedItem.setSingleInformationItem(expectedItem.getItem().toBuilder().setInformation(0, info).build());

        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(expectedItem);
        verifyEnqueue(List.of(new ShopSkuKey(businessId, shopSku)));
    }

    @Test
    public void testShouldNotUpdateDropshipEveryTime() {
        int supplierId = 42;
        int businessId = 43;
        String shopSku = "anything";
        String dropshipSourceId = "I AM A DROPSHIP";
        mdmWarehouseService
            .addOrUpdate(new MdmWarehouse().setId(dropshipSourceId).setLmsType(PartnerType.DROPSHIP));
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, List.of(shopSku));

        SourceItemKey key = new SourceItemKey(supplierId, shopSku, MasterDataSource.WAREHOUSE, dropshipSourceId);
        MdmIrisPayload.Item item = generateItem(key, "original", 1);

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item));

        // Change type and source id to supplier
        FromIrisItemWrapper expectedItem = toItemWrapper(item);
        ReferenceInformation.Builder info = expectedItem.getReferenceInformation().toBuilder();
        info.getSourceBuilder().setType(MasterDataSource.SUPPLIER);
        expectedItem.setSingleInformationItem(expectedItem.getItem().toBuilder().setInformation(0, info).build());

        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(expectedItem);
        verifyEnqueue(List.of(new ShopSkuKey(businessId, shopSku)));

        // simulate processing by job
        processAllQueueItems();
        expectedItem.setProcessed(true);
        fromIrisItemRepository.insertOrUpdate(expectedItem);

        // add item again
        addNewItemsService.addAndUpdateItems(Collections.singletonList(item));

        // now the processed flag should not be reset
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(expectedItem);
        verifyNoEnqueue();
    }

    @Test
    public void updateToItemWithIncorrectShopSkuShouldNotUpdate() {
        List<String> invalidShopSkus = List.of("", "$anything", "example@example.com", "ё", "пробелы тоже нельзя");
        List<String> validShopSkus = List.of("312", "abc/4543", "[что-то]=(something)");
        int businessId = 14;
        int serviceId = 12;
        generateSupplierRelationsAndMarkExistence(businessId, serviceId, validShopSkus);
        List<MdmIrisPayload.Item> items = Stream.concat(validShopSkus.stream(), invalidShopSkus.stream())
            .map(sku -> new SourceItemKey(serviceId, sku, MasterDataSource.WAREHOUSE, "146"))
            .map(key -> generateItem(key, "anyName", 1))
            .collect(Collectors.toList());
        addNewItemsService.addAndUpdateItems(items);
        List<String> allRepositoryItemsShopSkus = fromIrisItemRepository.findAll().stream()
            .map(ItemWrapper::getShopSku)
            .collect(Collectors.toList());
        Assertions.assertThat(allRepositoryItemsShopSkus).containsOnlyElementsOf(validShopSkus);
        verifyEnqueue(validShopSkus.stream().map(ss -> new ShopSkuKey(businessId, ss)).collect(Collectors.toList()));
    }

    @Test
    public void testSaveToFromIrisAndSilverEnabledShouldSaveSameValues() {
        //given
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);

        SourceItemKey key = getSourceItemKey();

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d, 12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        // when
        addNewItemsService.addAndUpdateItems(List.of(item));

        // then
        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item));

        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(key.toSilverSskuKey()).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(4);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("11"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("12"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("13"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenFiltrationEnabledNotSaveWarehouseItems() {
        //given
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);

        SourceItemKey key = getSourceItemKey();

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d, 12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        // when
        addNewItemsService.addAndUpdateItems(List.of(item));

        // then
        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();
        Assertions.assertThat(silverSskuRepository.findAll()).isEmpty();
        verifyNoEnqueue();
    }

    @Test
    public void whenSaveToFromIrisDisabledAndSilverFilterEnabledShouldFilterInvalidData() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, true);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146"
        );

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10000000000d, 10000000000d,
                10000000000d, 10000000000d, null, null, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        var savedSilver =
            silverSskuRepository.findSsku(key.toSilverSskuKey());
        Assertions.assertThat(savedSilver).isEmpty();

        verifyNoEnqueue();
    }

    @Test
    public void whenSaveToFromIrisDisabledAndSilverFilterDisabledShouldSaveAllData() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146"
        );

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10000000000d, 10000000000d,
                10000000000d, 10000000000d, null, null, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(key.toSilverSskuKey()).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(4);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10000000000"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10000000000"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10000000000"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10000000000"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenSaveToSilverShouldFilterWarehouse() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);

        SourceItemKey key = getSourceItemKey();

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d, 12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();
        Assertions.assertThat(silverSskuRepository.findAll()).isEmpty();
        verifyNoEnqueue();
    }

    @Test
    public void whenSaveShelfLifeShouldCorrectlySaveToSilver() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146"
        );

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d,
                12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.Lifetime.Builder lifeTime = MdmIrisPayload.Lifetime.newBuilder()
            .setTimeInUnits(MdmIrisPayload.TimeInUnits.newBuilder()
                .setValue(10)
                .setUnitValue(1)
                .build())
            .setSource(source);
        MdmIrisPayload.ReferenceInformation info = ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setLifetime(lifeTime)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(key.toSilverSskuKey()).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(6);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("11"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("12"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("13"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE_UNIT))
            .flatMap(MdmParamValue::getOption)
            .contains(new MdmParamOption().setId(3).setRenderedValue("дни"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenSaveShelfLifeShouldCorrectlySaveToSilverForEoxed1P() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        SourceItemKey key = new SourceItemKey(
            beruId.getId(), "1132341.765", MasterDataSource.MEASUREMENT, "146"
        );

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.FIRST_PARTY)
            .setId(Math.toIntExact(beruId.getId()))
            .setBusinessEnabled(true)
            .setBusinessId(beruId.getBusinessId());
        MdmSupplier business = new MdmSupplier()
            .setType(MdmSupplierType.BUSINESS)
            .setId(beruId.getBusinessId());
        supplierRepository.insertOrUpdateAll(List.of(supplier, business));

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d,
                12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.Lifetime.Builder lifeTime = MdmIrisPayload.Lifetime.newBuilder()
            .setTimeInUnits(MdmIrisPayload.TimeInUnits.newBuilder()
                .setValue(10)
                .setUnitValue(1)
                .build())
            .setSource(source);
        MdmIrisPayload.ReferenceInformation info = ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setLifetime(lifeTime)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));

        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();

        var savedSilver =
            silverSskuRepository.findSsku(toBizKey(key.toSilverSskuKey(), beruId.getBusinessId())).orElseThrow();
        var savedServiceSsku = savedSilver.getServiceSskus().get(beruId.getId());
        Assertions.assertThat(savedSilver.getServiceSskus()).isNotEmpty();
        Assertions.assertThat(savedServiceSsku.getValues()).hasSize(6);
        Assertions.assertThat(savedServiceSsku.getValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedServiceSsku.getParamValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedServiceSsku.getParamValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("11"));
        Assertions.assertThat(savedServiceSsku.getParamValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("12"));
        Assertions.assertThat(savedServiceSsku.getParamValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("13"));
        Assertions.assertThat(savedServiceSsku.getParamValue(KnownMdmParams.SHELF_LIFE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedServiceSsku.getParamValue(KnownMdmParams.SHELF_LIFE_UNIT))
            .flatMap(MdmParamValue::getOption)
            .contains(new MdmParamOption().setId(3).setRenderedValue("дни"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenSaveToSilverShouldReplaceDropshipMDSourceType() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);

        int supplierId = 42;
        String shopSku = "anything";
        String dropshipSourceId = "I AM A DROPSHIP";
        mdmWarehouseService.addOrUpdate(new MdmWarehouse().setId(dropshipSourceId).setLmsType(PartnerType.DROPSHIP));

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(supplierId));
        supplierRepository.insertOrUpdate(supplier);

        SourceItemKey key = new SourceItemKey(supplierId, shopSku, MasterDataSource.WAREHOUSE, dropshipSourceId);
        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d, 12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));
        var replacedKey = new SilverSskuKey(supplierId, shopSku, MasterDataSourceType.SUPPLIER, dropshipSourceId);
        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();
        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(replacedKey).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(4);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("11"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("12"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("13"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenSaveShelfLifeShouldCorrectlySaveToFromIris() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, true);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146"
        );

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d,
                12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.Lifetime.Builder lifeTime = MdmIrisPayload.Lifetime.newBuilder()
            .setTimeInUnits(MdmIrisPayload.TimeInUnits.newBuilder()
                .setValue(10)
                .setUnitValue(1)
                .build())
            .setSource(source);
        MdmIrisPayload.ReferenceInformation info = ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setLifetime(lifeTime)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));

        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item));

        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(key.toSilverSskuKey()).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(6);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("11"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("12"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("13"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE_UNIT))
            .flatMap(MdmParamValue::getOption)
            .contains(new MdmParamOption().setId(3).setRenderedValue("дни"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenShelfLifeDoesntHaveSourceInheritFromReferenceInfo() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, true);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146");

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d,
                12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.Lifetime.Builder lifeTime = MdmIrisPayload.Lifetime.newBuilder()
            .setTimeInUnits(MdmIrisPayload.TimeInUnits.newBuilder()
                .setValue(10)
                .setUnitValue(1)
                .build());
        MdmIrisPayload.ReferenceInformation info = ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setLifetime(lifeTime)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));

        Assertions.assertThat(fromIrisItemRepository.findAll())
            .containsExactly(toItemWrapper(item));

        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(key.toSilverSskuKey()).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(6);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("11"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("12"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("13"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE_UNIT))
            .flatMap(MdmParamValue::getOption)
            .contains(new MdmParamOption().setId(3).setRenderedValue("дни"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void shouldSaveShelfLifeToSilverOnlyWhenOptionEnabled() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, true);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, false);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146");

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d,
                12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.Lifetime.Builder lifeTime = MdmIrisPayload.Lifetime.newBuilder()
            .setTimeInUnits(MdmIrisPayload.TimeInUnits.newBuilder()
                .setValue(10)
                .setUnitValue(1)
                .build());
        MdmIrisPayload.ReferenceInformation info = ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setLifetime(lifeTime)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));

        // replace lifeTime
        info = info.toBuilder().clearLifetime().build();
        item = item.toBuilder().clearInformation().addInformation(info).build();

        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item));

        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(key.toSilverSskuKey()).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(4);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("11"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("12"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("13"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenDrophipShouldReplaceSourceInLifetime() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        int supplierId = 42;
        String shopSku = "anything";
        String dropshipSourceId = "I AM A DROPSHIP";
        mdmWarehouseService
            .addOrUpdate(new MdmWarehouse().setId(dropshipSourceId).setLmsType(PartnerType.DROPSHIP));

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(supplierId));
        supplierRepository.insertOrUpdate(supplier);

        SourceItemKey key = new SourceItemKey(supplierId, shopSku, MasterDataSource.WAREHOUSE, dropshipSourceId);
        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d, 12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.Lifetime.Builder lifeTime = MdmIrisPayload.Lifetime.newBuilder()
            .setTimeInUnits(MdmIrisPayload.TimeInUnits.newBuilder()
                .setValue(10)
                .setUnitValue(1)
                .build())
            .setSource(source);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setLifetime(lifeTime)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info)
            .build();

        addNewItemsService.addAndUpdateItems(List.of(item));

        var replacedKey = new SilverSskuKey(supplierId, shopSku, MasterDataSourceType.SUPPLIER, dropshipSourceId);
        Assertions.assertThat(fromIrisItemRepository.findAll()).isEmpty();
        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(replacedKey).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(6);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("11"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("12"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("13"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE_UNIT))
            .flatMap(MdmParamValue::getOption)
            .contains(new MdmParamOption().setId(3).setRenderedValue("дни"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenUpdateToNewerItemShouldUpdateSilver() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, false);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146");

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Item item1 = generateCustomItem(key, 10., 11., 12., 13., 100L, 10);

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item1));
        checkSilver(key.toSilverSskuKey(), 10, 11, 12, 13, 10);
        verifyEnqueue(List.of(key.getShopSkuKey()));
        sskuToRefreshQueue.deleteAll();

        MdmIrisPayload.Item item2 = generateCustomItem(key, 10., 11., 100., 13.,
            Instant.now().toEpochMilli(), 10);
        addNewItemsService.addAndUpdateItems(Collections.singletonList(item2));
        checkSilver(key.toSilverSskuKey(), 10, 11, 100, 13, 10);
        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenUpdateToNewerEqualItemShouldNotUpdateSilver() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, false);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146");
        int supplierId = (int) key.getSupplierId();
        int businessId = supplierId + 1;
        generateSupplierRelationsAndMarkExistence(businessId, supplierId, key.getShopSku());

        MdmIrisPayload.Item item1 = generateCustomItem(key, 10., 11., 12., 13., 100L, 10);

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item1));
        var businessSilverKey =
            new SilverSskuKey(new ShopSkuKey(businessId, key.getShopSku()),
                key.toSilverSskuKey().getMasterDataSource());
        checkSilver(businessSilverKey, 10, 11, 12, 13, 10);
        verifyEnqueue(List.of(new ShopSkuKey(businessId, key.getShopSku())));
        sskuToRefreshQueue.deleteAll();

        MdmIrisPayload.Item item2 = generateCustomItem(key, 10., 11., 12., 13.,
            Instant.now().toEpochMilli(), 10);
        addNewItemsService.addAndUpdateItems(Collections.singletonList(item2));
        checkSilver(businessSilverKey, 10, 11, 12, 13, 10);
        verifyNoEnqueue();
    }

    @Test
    public void whenUpdateToNewerOlderItemShouldNotUpdateSilver() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, false);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, false);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146");

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Item item1 = generateCustomItem(key, 10., 11., 12., 13., 100L, 10);

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item1));
        checkSilver(key.toSilverSskuKey(), 10, 11, 12, 13, 10);
        verifyEnqueue(List.of(key.getShopSkuKey()));
        sskuToRefreshQueue.deleteAll();

        MdmIrisPayload.Item item2 = generateCustomItem(key, 10., 11., 12., 13., 110L, 10);
        addNewItemsService.addAndUpdateItems(Collections.singletonList(item2));
        checkSilver(key.toSilverSskuKey(), 10, 11, 12, 13, 10);
        verifyNoEnqueue();
    }

    @Test
    public void whenReceiveItemWithShelfLifeAndEmptyShippingUnitShouldCorrectlyReplace() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, true);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, true);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146");

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Item item1 = generateCustomItem(key, 10., 11., 12., 13., 100L, 10);

        addNewItemsService.addAndUpdateItems(Collections.singletonList(item1));
        checkSilver(key.toSilverSskuKey(), 10, 11, 12, 13, 10);
        verifyEnqueue(List.of(key.getShopSkuKey()));
        sskuToRefreshQueue.deleteAll();

        MdmIrisPayload.Item item2 = generateCustomEmptyShippingUnitItem(key, Instant.now().toEpochMilli(), 20);
        addNewItemsService.addAndUpdateItems(Collections.singletonList(item2));
        checkSilver(key.toSilverSskuKey(), 10, 11, 12, 13, 20);
        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenReceiveItemWithShelfLifeAndEmptyShippingUnitShouldCorrectlyImport() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_FROM_IRIS, true);
        storageKeyValueService.putValue(MdmProperties.FILTER_NOT_VALID_DATA_WHEN_SAVING_SILVER_FROM_IRIS, true);
        storageKeyValueService.putValue(MdmProperties.FILTER_FROM_IRIS_DATA_BY_SOURCE, true);
        storageKeyValueService.putValue(MdmProperties.SAVE_SHELF_LIFE_FROM_IRIS_ENABLED, true);

        SourceItemKey key = new SourceItemKey(
            1, "1132341", MasterDataSource.MEASUREMENT, "146");

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Item item = generateCustomEmptyShippingUnitItem(key, Instant.now().toEpochMilli(), 20);
        addNewItemsService.addAndUpdateItems(Collections.singletonList(item));
        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(key.toSilverSskuKey()).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(2);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal(20));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE_UNIT))
            .flatMap(MdmParamValue::getOption)
            .contains(new MdmParamOption().setId(3).setRenderedValue("дни"));
        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    @Test
    public void whenRecieveNewFormatSourceIdShouldCorrectlyParse() {
        storageKeyValueService.putValue(MdmProperties.SAVE_FROM_IRIS_ITEMS_TO_SILVER, true);

        SourceItemKey key = getSourceItemKey("146:unknownOperator666", MasterDataSource.MEASUREMENT);

        MdmSupplier supplier = new MdmSupplier()
            .setType(MdmSupplierType.THIRD_PARTY)
            .setId(Math.toIntExact(key.getSupplierId()));
        supplierRepository.insertOrUpdate(supplier);

        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.ShippingUnit.Builder shippingUnit =
            ItemWrapperTestUtil.generateShippingUnitWithDqScore(10d, 11d, 12d, 13d, null, null, 100L, 100);
        MdmIrisPayload.ReferenceInformation info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setItemShippingUnit(shippingUnit)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build())
            .build();
        MdmIrisPayload.Item.Builder item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info);

        addNewItemsService.addAndUpdateItems(List.of(item.build()));

        source = source.toBuilder()
            .setId("146")
            .build();
        info = info.toBuilder().setSource(source).build();
        item.clearInformation().addInformation(info);

        Assertions.assertThat(fromIrisItemRepository.findAll()).containsExactly(toItemWrapper(item.build()));

        var expectedMdSource = new ru.yandex.market.mbo.mdm.common.masterdata.model.MasterDataSource(
            key.toSilverSskuKey().getMasterDataSource().getSourceType(),
            "146");
        SourceItemKey expectedKey = getSourceItemKey("146", MasterDataSource.MEASUREMENT);
        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(expectedKey.toSilverSskuKey()).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(4);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValues().stream()
                .map(spv -> spv.getSilverSskuKey().getMasterDataSource())
                .collect(Collectors.toList()))
            .allMatch(mdSource -> mdSource.equals(expectedMdSource));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("10"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("11"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("12"));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal("13"));

        verifyEnqueue(List.of(key.getShopSkuKey()));
    }

    private MdmIrisPayload.Item generateCustomEmptyShippingUnitItem(SourceItemKey key, long ts, int lifeTimeValue) {
        return generateCustomItem(key, null, null, null, null, ts, lifeTimeValue, false);
    }

    private MdmIrisPayload.Item generateCustomItem(SourceItemKey key, double length, double width, double height,
                                                   double weightGross, long ts, int lifeTimeValue) {
        return generateCustomItem(key, length, width, height, weightGross, ts, lifeTimeValue, true);
    }

    private MdmIrisPayload.Item generateCustomItem(SourceItemKey key, Double length, Double width, Double height,
                                                   Double weightGross, long ts, int lifeTimeValue,
                                                   boolean generateShippingUnit) {
        MdmIrisPayload.Associate source = MdmIrisPayload.Associate.newBuilder()
            .setId(key.getSourceId())
            .setType(key.getSourceType())
            .build();
        MdmIrisPayload.Lifetime.Builder lifeTime = MdmIrisPayload.Lifetime.newBuilder()
            .setTimeInUnits(MdmIrisPayload.TimeInUnits.newBuilder()
                .setValue(lifeTimeValue)
                .setUnitValue(1)
                .build())
            .setSource(source)
            .setUpdatedTs(ts);
        MdmIrisPayload.ReferenceInformation.Builder info = MdmIrisPayload.ReferenceInformation.newBuilder()
            .setSource(source)
            .setLifetime(lifeTime)
            .setSurplusHandleInfo(MdmIrisPayload.SurplusHandleInfo.newBuilder()
                .setValue(MdmIrisPayload.SurplusHandleMode.ACCEPT)
                .build());
        if (generateShippingUnit) {
            MdmIrisPayload.ShippingUnit.Builder shippingUnit =
                ItemWrapperTestUtil.generateShippingUnitWithDqScore(length, width, height, weightGross, null, null,
                    ts, 100);
            info.setItemShippingUnit(shippingUnit);
        }
        MdmIrisPayload.Item item = MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku()))
            .addInformation(info.build())
            .build();
        return item;
    }

    private void checkSilver(SilverSskuKey key, int length, int width, int height, int weightGross, int shelfLife) {
        SilverCommonSsku savedSilver =
            silverSskuRepository.findSsku(key).orElseThrow();
        Assertions.assertThat(savedSilver.getServiceSskus()).isEmpty();
        Assertions.assertThat(savedSilver.getBaseValues()).hasSize(6);
        Assertions.assertThat(savedSilver.getBaseValues())
            .allMatch(sspv -> sspv.getSskuSilverTransport() == SskuSilverParamValue.SskuSilverTransportType.IRIS);
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.LENGTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal(length));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WIDTH))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal(width));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.HEIGHT))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal(height));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.WEIGHT_GROSS))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal(weightGross));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE))
            .flatMap(MdmParamValue::getNumeric)
            .contains(new BigDecimal(shelfLife));
        Assertions.assertThat(savedSilver.getBaseValue(KnownMdmParams.SHELF_LIFE_UNIT))
            .flatMap(MdmParamValue::getOption)
            .contains(new MdmParamOption().setId(3).setRenderedValue("дни"));
    }

    private SourceItemKey getSourceItemKey() {
        return getSourceItemKey("146", MdmIrisPayload.MasterDataSource.WAREHOUSE);
    }

    private SourceItemKey getSourceItemKey(String sourceId, MdmIrisPayload.MasterDataSource sourceType) {
        return new SourceItemKey(
            1, "1132341", sourceType, sourceId
        );
    }

    private FromIrisItemWrapper toItemWrapper(MdmIrisPayload.Item item) {
        return new FromIrisItemWrapper(item);
    }

    private FromIrisItemWrapper toItemWrapper(MdmIrisPayload.CompleteItem completeItem) {
        MdmIrisPayload.Item.Builder builder = MdmIrisPayload.Item.newBuilder()
            .setItemId(completeItem.getItemId());
        completeItem.getRemainingInformationList().forEach(builder::addInformation);
        return new FromIrisItemWrapper(builder.build());
    }

    private MdmIrisPayload.Item generateItem(SourceItemKey key, String name, long updatedTs) {
        MdmIrisPayload.ReferenceInformation information = generateReferenceInformation(key, name, updatedTs);

        return MdmIrisPayload.Item.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku())
                .build())
            .addInformation(information)
            .build();
    }

    private MdmIrisPayload.CompleteItem generateCompleteItem(SourceItemKey key, String name, long updatedTs) {
        MdmIrisPayload.ReferenceInformation information = generateReferenceInformation(key, name, updatedTs);

        return MdmIrisPayload.CompleteItem.newBuilder()
            .setItemId(MdmIrisPayload.MdmIdentifier.newBuilder()
                .setSupplierId(key.getSupplierId())
                .setShopSku(key.getShopSku())
                .build())
            .addRemainingInformation(information)
            .build();
    }

    private MdmIrisPayload.ReferenceInformation generateReferenceInformation(SourceItemKey key,
                                                                             String name,
                                                                             long updatedTs) {
        MdmIrisPayload.ReferenceInformation.Builder builder = MdmIrisPayload.ReferenceInformation.newBuilder();
        builder.setSource(
            MdmIrisPayload.Associate.newBuilder()
                .setId(key.getSourceId())
                .setType(key.getSourceType())
        );

        builder.setName(MdmIrisPayload.StringValue.newBuilder().setValue(name).setUpdatedTs(updatedTs).build());

        builder.setItemShippingUnit(MdmIrisPayload.ShippingUnit.newBuilder()
            .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(updatedTs))
            .setWeightNetMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(updatedTs))
            .setWeightTareMg(MdmIrisPayload.Int64Value.newBuilder().setValue(1000).setUpdatedTs(updatedTs))
            .setWidthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(updatedTs))
            .setHeightMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(updatedTs))
            .setLengthMicrometer(MdmIrisPayload.Int64Value.newBuilder().setValue(10000).setUpdatedTs(updatedTs))
        );

        builder.setBoxCount(MdmIrisPayload.Int32Value.newBuilder().setValue(100).setUpdatedTs(updatedTs));

        return builder.build();
    }

    private void verifyEnqueue(Collection<ShopSkuKey> shopSkuKeys) {
        Set<ShopSkuKey> uniqueKeys = new LinkedHashSet<>(shopSkuKeys);

        Assertions.assertThat(sskuToRefreshQueue.getUnprocessedBatch(uniqueKeys.size() + 10))
            .map(MdmQueueInfoBase::getEntityKey)
            .containsExactlyInAnyOrderElementsOf(uniqueKeys);
    }

    private void verifyNoEnqueue() {
        Assertions.assertThat(sskuToRefreshQueue.getUnprocessedItemsCount()).isZero();
    }

    private void processAllQueueItems() {
        BatchProcessingProperties processingProperties = BatchProcessingProperties.BatchProcessingPropertiesBuilder
            .constantBatchProperties(1000)
            .deleteProcessed(true)
            .build();
        sskuToRefreshQueue.processUniqueEntitiesInBatches(processingProperties, infos -> true);
    }

    private SilverSskuKey toBizKey(SilverSskuKey silverServiceKey, int bizId) {
        return new SilverSskuKey(
            bizId,
            silverServiceKey.getShopSku(),
            silverServiceKey.getSourceType(),
            silverServiceKey.getSourceId()
        );
    }

    private void generateSupplierRelationsAndMarkExistence(int businessId, int serviceId, String shopSku) {
        generateSupplierRelationsAndMarkExistence(businessId, serviceId, List.of(shopSku));
    }

    private void generateSupplierRelationsAndMarkExistence(int businessId, int serviceId,
                                                           Collection<String> shopSkus) {
        mdmSupplierRepository.insertOrUpdateAll(
            List.of(
                new MdmSupplier()
                    .setBusinessId(businessId)
                    .setType(MdmSupplierType.THIRD_PARTY)
                    .setDeleted(false)
                    .setBusinessEnabled(true)
                    .setId(serviceId),
                new MdmSupplier()
                    .setId(businessId)
                    .setDeleted(false)
                    .setType(MdmSupplierType.BUSINESS)));
        shopSkus.forEach(ssku -> sskuExistenceRepository.markExistence(new ShopSkuKey(serviceId, ssku), true));
    }
}
