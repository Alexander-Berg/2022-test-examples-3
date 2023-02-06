package ru.yandex.market.deepmind.tms.executors;

import java.time.Instant;
import java.util.List;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.core.delivery.DeliveryServiceType;
import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.config.TestYqlOverPgDatasourceConfig;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.PartnerRelationType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.ChangedSsku;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.PartnerRelation;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.repository.partner_relations.PartnerRelationRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplicaRepository;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.utils.YqlOverPgUtils;

import static ru.yandex.market.core.delivery.DeliveryServiceType.CROSSDOCK;
import static ru.yandex.market.core.delivery.DeliveryServiceType.DROPSHIP;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.CROSSDOCK_ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.ROSTOV_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository.SOFINO_ID;

public class ImportPartnerRelationsExecutorTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {

    private static final long SORTING_CENTER_1 = 12345L;
    private static final long SORTING_CENTER_2 = 54321L;

    @Resource
    private SupplierRepository deepmindSupplierRepository;
    @Resource
    private PartnerRelationRepository partnerRelationRepository;
    @Resource(name = TestYqlOverPgDatasourceConfig.YQL_OVER_PG_NAMED_TEMPLATE)
    private NamedParameterJdbcTemplate namedYqlJdbcTemplate;
    @Resource
    private ServiceOfferReplicaRepository serviceOfferReplicaRepository;

    private ImportPartnerRelationsExecutor importPartnerRelationsExecutor;

    private YPath partnerTable = YPath.simple("//tmp/unit_test/partner");
    private YPath partnerRelationTable = YPath.simple("//tmp/unit_test/partner_relation");
    private YPath partnerServiceLinkTable = YPath.simple("//tmp/unit_test/partner_service_link");
    private YPath mbiSuppliersTable = YPath.simple("//tmp/unit_test/mbi_suppliers_biz");

    @Before
    public void setUp() {
        importPartnerRelationsExecutor = new ImportPartnerRelationsExecutor(
            partnerRelationRepository,
            namedYqlJdbcTemplate,
            "unit-test",
            partnerTable,
            partnerRelationTable,
            partnerServiceLinkTable,
            mbiSuppliersTable
        );

        deepmindSupplierRepository.save(
            new Supplier().setId(1).setName("supplier").setFulfillment(true).setCrossdock(true),
            new Supplier().setId(2).setName("supplier").setDropship(true),
            new Supplier().setId(3).setName("supplier").setDropship(true)
        );

        serviceOfferReplicaRepository.save(
            offer(1, "sku-11"),
            offer(1, "sku-12"),
            offer(2, "sku-21"),
            offer(3, "sku-31")
        );

        YqlOverPgUtils.setTransformYqlToSql(true);

        mockPartnersTable();
        mockPartnersRelation();
        mockPartnerServiceLinkRelation();
        mockMbiSuppliersTable();
        clearQueue();
    }

    @After
    public void tearDown() {
        YqlOverPgUtils.setTransformYqlToSql(false);
    }

    @Test
    public void testSetFromWarehouseId() {
        createServiceLink(1, 100L);
        createServiceLink(2, 200L);
        createRelation(100L, "SUPPLIER", ROSTOV_ID, "FULFILLMENT");
        createRelation(200L, "DROPSHIP", 201L, "SORTING_CENTER");

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(1, 100L, CROSSDOCK_ROSTOV_ID, CROSSDOCK),
                partnerRelation(2, 200L, 201L, DROPSHIP)
            );

        // assert changed_sskus
        execute();
        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(1, "sku-11"),
                changedSsku(1, "sku-12"),
                changedSsku(2, "sku-21")
            );
    }


    @Test
    public void testDontImportDataToUnknownCrossdockWarehouse() {
        createServiceLink(1, 100L);
        createRelation(100L, "SUPPLIER", 101L, "FULFILLMENT");

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation).isEmpty();
    }

    @Test
    public void testImportSeveralWithFromWarehouseId() {
        createServiceLink(2, 200L);
        createServiceLink(2, 202L);
        createRelation(200L, "DROPSHIP", 201L, "SORTING_CENTER");
        createRelation(202L, "DROPSHIP", 201L, "SORTING_CENTER");

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(2, 0, 201L, DROPSHIP)
                    .setFromWarehouseIds(200L, 202L)
            );

        // assert changed_sskus
        execute();
        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(2, "sku-21")
            );
    }

    @Test
    public void testChangeWarehouseId() {
        partnerRelationRepository.save(partnerRelation(1, 200L, CROSSDOCK_ROSTOV_ID, CROSSDOCK));
        taskQueueRepository.deleteAll();

        createServiceLink(1, 100L);
        createRelation(100L, "SUPPLIER", ROSTOV_ID, "FULFILLMENT");

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(1, 100L, CROSSDOCK_ROSTOV_ID, CROSSDOCK)
            );

        // assert changed_sskus
        execute();
        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(1, "sku-11"),
                changedSsku(1, "sku-12")
            );
    }

    @Test
    public void testDeleteRowWarehouseId() {
        partnerRelationRepository.save(partnerRelation(1, 200L, CROSSDOCK_ROSTOV_ID, CROSSDOCK));
        taskQueueRepository.deleteAll();

        createServiceLink(1, 100L);

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(1, 200L, null, CROSSDOCK)
            );

        // assert changed_sskus
        execute();
        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(1, "sku-11"),
                changedSsku(1, "sku-12")
            );
    }

    @Test
    public void testDontImportOtherRows() {
        partnerRelationRepository.save(partnerRelation(1, 100L, CROSSDOCK_ROSTOV_ID, CROSSDOCK));
        taskQueueRepository.deleteAll();

        createServiceLink(1, 100L);
        createServiceLink(2, 200L);
        createServiceLink(3, 300L);
        createRelation(100L, "SUPPLIER", ROSTOV_ID, "FULFILLMENT");
        createRelation(200L, "1", ROSTOV_ID, "2");
        createRelation(300L, "DROPSHIP", ROSTOV_ID, "FULFILLMENT");

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(1, 100L, CROSSDOCK_ROSTOV_ID, CROSSDOCK)
            );

        // assert changed_sskus
        execute();
        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus).isEmpty();
    }

    @Test
    public void importFromWarehouseWontChangeAnythingIfSecondRun() {
        createServiceLink(1, 100L);
        createServiceLink(2, 200L);
        createRelation(100L, "SUPPLIER", 101L, "FULFILLMENT");
        createRelation(200L, "DROPSHIP", 201L, "SORTING_CENTER");

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(2, 200L, 201L, DROPSHIP)
            );

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation2 = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation2).containsExactlyInAnyOrderElementsOf(partnerRelation);
    }

    @Test
    public void importFromWarehouseShouldFailIfInResponseWillBeSeveralEqualData() {
        createServiceLink(1, 100L);
        createServiceLink(1, 101L);
        createServiceLink(1, 103L);
        createRelation(100L, "SUPPLIER", ROSTOV_ID, "FULFILLMENT");
        createRelation(101L, "SUPPLIER", SOFINO_ID, "FULFILLMENT");
        createRelation(103L, "DROPSHIP", 9L, "SORTING_CENTER");

        Assertions.assertThatThrownBy(() -> {
            importPartnerRelationsExecutor.execute();
        })
            .hasMessageContaining("Duplicate partner_relations at 1 supplier (from -> to):" +
                " [100] -> -147; [101] -> -172");
    }

    @Test // DEEPMIND-794
    public void importShouldSaveCrossdockEvenIfDropshipContainsNotValidData() {
        createServiceLink(1, 99L);
        createServiceLink(2, 100L);
        createServiceLink(2, 101L);
        createRelation(99L, "SUPPLIER", ROSTOV_ID, "FULFILLMENT");
        createRelation(100L, "DROPSHIP", 9L, "SORTING_CENTER");
        createRelation(101L, "DROPSHIP", 10L, "SORTING_CENTER");

        Assertions.assertThatThrownBy(() -> {
            importPartnerRelationsExecutor.execute();
        })
            .hasMessageContaining("Duplicate partner_relations at 2 supplier (from -> to):" +
                " [100] -> 9; [101] -> 10");

        // check first relation is saved
        var all = partnerRelationRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(1, 99L, CROSSDOCK_ROSTOV_ID, CROSSDOCK)
            );
    }

    @Test
    public void testReplaceRelationTypeIfItChanged() {
        partnerRelationRepository.save(partnerRelation(1, 200L, SORTING_CENTER_1, DROPSHIP));
        taskQueueRepository.deleteAll();

        createServiceLink(1, 100L);
        createRelation(100L, "SUPPLIER", ROSTOV_ID, "FULFILLMENT");

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(1, 100L, CROSSDOCK_ROSTOV_ID, CROSSDOCK)
            );

        // assert changed_sskus
        execute();
        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(1, "sku-11"),
                changedSsku(1, "sku-12")
            );
    }

    @Test
    public void testChangeToWarehouseIfToWarehouseIsChanged() {
        partnerRelationRepository.save(partnerRelation(2, 200L, SORTING_CENTER_1, DROPSHIP));
        taskQueueRepository.deleteAll();

        createServiceLink(2, 200L);
        createRelation(200L, "DROPSHIP", SORTING_CENTER_2, "SORTING_CENTER");

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(2, 200L, SORTING_CENTER_2, DROPSHIP)
            );

        // assert changed_sskus
        execute();
        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(2, "sku-21")
            );
    }

    @Test
    public void dontSaveNotAffectedRows() {
        PartnerRelation pr1 = partnerRelationRepository.save(partnerRelation(1, 10, CROSSDOCK_ROSTOV_ID, CROSSDOCK));
        PartnerRelation pr2 = partnerRelationRepository.save(partnerRelation(2, 20, null, DROPSHIP));
        PartnerRelation pr3 = partnerRelationRepository.save(partnerRelation(3, 30, 200L, DROPSHIP));

        taskQueueRepository.deleteAll();

        createServiceLink(1, 10L);
        createServiceLink(2, 20L);
        createServiceLink(3, 30L);
        createRelation(10, "SUPPLIER", ROSTOV_ID, "FULFILLMENT");
        createRelation(30, "DROPSHIP", 300L, "SORTING_CENTER");

        importPartnerRelationsExecutor.execute();

        Instant newModificationTime3 = partnerRelationRepository.getById(pr3.getId()).getModifiedTs();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .containsExactlyInAnyOrder(
                pr1,
                pr2,
                pr3.setToWarehouseId(300L).setModifiedTs(newModificationTime3)
            );

        // assert changed_sskus
        execute();
        List<ChangedSsku> changedSskus = changedSskuRepository.findAll();
        Assertions.assertThat(changedSskus)
            .usingElementComparatorOnFields("supplierId", "shopSku")
            .containsExactlyInAnyOrder(
                changedSsku(3, "sku-31")
            );
    }

    @Test
    public void importSeveralSuppliersWithEqualWarehouses() {
        createServiceLink(2, 100L);
        createServiceLink(3, 100L);
        createRelation(100L, "DROPSHIP", 300L, "SORTING_CENTER");

        importPartnerRelationsExecutor.execute();

        List<PartnerRelation> partnerRelation = partnerRelationRepository.findAll();
        Assertions.assertThat(partnerRelation)
            .usingElementComparatorIgnoringFields("id", "modifiedTs")
            .containsExactlyInAnyOrder(
                partnerRelation(2, 100L, 300L, DROPSHIP),
                partnerRelation(3, 100L, 300L, DROPSHIP)
            );
    }

    private ServiceOfferReplica offer(
        int supplierId, String ssku) {
        return new ServiceOfferReplica()
            .setBusinessId(supplierId)
            .setSupplierId(supplierId)
            .setShopSku(ssku)
            .setTitle("title")
            .setCategoryId(99L)
            .setSeqId(0L)
            .setMskuId(1L)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setModifiedTs(Instant.now())
            .setAcceptanceStatus(OfferAcceptanceStatus.OK);
    }

    private void mockPartnersRelation() {
        namedYqlJdbcTemplate.getJdbcOperations().execute(
            "create table `" + partnerRelationTable + "` (" +
                "from_partner bigint," +
                "to_partner bigint, " +
                "enabled boolean " +
                ")"
        );
    }

    private void mockPartnerServiceLinkRelation() {
        namedYqlJdbcTemplate.getJdbcOperations().execute(
            "create table `" + partnerServiceLinkTable + "` (" +
                "partner_id int," +
                "service_id bigint" +
                ")"
        );
    }

    private void mockMbiSuppliersTable() {
        namedYqlJdbcTemplate.getJdbcOperations().execute(
            "create view `" + mbiSuppliersTable + "` as select" +
                " id," +
                " case when fulfillment then 1 else 0 end as is_fullfilment," +
                " case when crossdock then 1 else 0 end   as is_crossdock," +
                " case when dropship then 1 else 0 end    as is_dropship" +
                " from msku.supplier"
        );
    }

    private void mockPartnersTable() {
        namedYqlJdbcTemplate.getJdbcOperations().execute(
            "create table `" + partnerTable + "` (" +
                "id bigint primary key," +
                "type text " +
                ")"
        );
    }

    private PartnerRelation partnerRelation(int supplierId, long fromWarehouse, Long toWarehouse,
                                            DeliveryServiceType type) {
        return new PartnerRelation()
            .setSupplierId(supplierId)
            .setFromWarehouseIds(fromWarehouse)
            .setToWarehouseId(toWarehouse)
            .setRelationType(PartnerRelationType.valueOf(type.name()));
    }

    private void createWarehouseIfNotExist(long warehouseId, String type) {
        namedYqlJdbcTemplate.getJdbcOperations()
            .update("insert into " + partnerTable + " (id, type) values (?, ?) on conflict do nothing",
                warehouseId, type);
    }

    private void createRelation(long from, long to) {
        namedYqlJdbcTemplate.getJdbcOperations()
            .update("insert into " + partnerRelationTable + " (from_partner, to_partner, enabled) values (?, ?, ?) ",
                from, to, true);
    }

    private void createServiceLink(int supplierId, long fromWarehouse) {
        namedYqlJdbcTemplate.getJdbcOperations()
            .update("insert into " + partnerServiceLinkTable + " (partner_id, service_id) values (?, ?) ",
                supplierId, fromWarehouse);
    }

    private void createRelation(long from, String typeFrom, long to, String typeTo) {
        namedYqlJdbcTemplate.getJdbcOperations()
            .update("insert into " + partnerRelationTable + " (from_partner, to_partner, enabled) values (?, ?, ?) ",
                from, to, true);
        createWarehouseIfNotExist(from, typeFrom);
        createWarehouseIfNotExist(to, typeTo);
    }
}
