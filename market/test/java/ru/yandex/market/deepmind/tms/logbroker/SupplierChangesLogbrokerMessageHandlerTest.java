package ru.yandex.market.deepmind.tms.logbroker;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseAvailabilitiesTaskQueueTestClass;
import ru.yandex.market.deepmind.common.availability.task_queue.events.SupplierAvailabilityChangedTask;
import ru.yandex.market.deepmind.common.availability.task_queue.handlers.SupplierAvailabilityChangedHandler;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.OfferAcceptanceStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.mbo_category.enums.SupplierType;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Supplier;
import ru.yandex.market.deepmind.common.mocks.StorageKeyValueServiceMock;
import ru.yandex.market.deepmind.common.repository.DeepmindOfferRepository;
import ru.yandex.market.deepmind.common.repository.service_offer.ServiceOfferReplica;
import ru.yandex.market.deepmind.common.repository.supplier.SupplierRepository;
import ru.yandex.market.deepmind.common.services.MboCategoryOfferChangesServiceHelper;
import ru.yandex.market.deepmind.tms.services.ImportSuppliersService;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;
import ru.yandex.market.partner.event.PartnerInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class SupplierChangesLogbrokerMessageHandlerTest extends DeepmindBaseAvailabilitiesTaskQueueTestClass {
    @Autowired
    private SupplierRepository deepmindSupplierRepository;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource
    private DeepmindOfferRepository deepmindOfferRepository;
    @Resource
    private TransactionHelper transactionHelper;

    private SupplierAvailabilityChangedHandler supplierChangedHandler;
    private ImportSuppliersService importSuppliersService;
    private SupplierChangesLogbrokerMessageHandler handler;
    private final MboCategoryOfferChangesServiceHelper offerChangesServiceHelper
        = Mockito.mock(MboCategoryOfferChangesServiceHelper.class);

    @Before
    public void setUp() throws Exception {
        supplierChangedHandler = new SupplierAvailabilityChangedHandler(changedSskuRepository, taskQueueRegistrator);
        importSuppliersService = new ImportSuppliersService(namedParameterJdbcTemplate, supplierChangedHandler,
            deepmindSupplierRepository, offerChangesServiceHelper, deepmindOfferRepository, transactionHelper);
        handler = new SupplierChangesLogbrokerMessageHandler(importSuppliersService, new StorageKeyValueServiceMock());
    }

    @Test
    public void process() {
        handler.process(List.of(
            partner(1, "Business 1"),
            partner(2, "Business 2"),
            partner(3, "Business 3"),
            partner1p(100, "Supplier 1", 1, "000100", false, false, false),
            partner1p(200, "Supplier 2", 1, "000200", true, false, false),
            partner3p(300, "Supplier 3", 1, true, false, false),
            partner3p(400, "Supplier 4", 2, false, true, false),
            partner3p(500, "Supplier 5", 2, true, true, false),
            partner3p(600, "Supplier 6", 2, false, false, false),
            partner3p(700, "Supplier 7", 2, false, false, true)
        ));

        var all = deepmindSupplierRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorIgnoringFields("dropship", "fulfillment", "crossdock")
            .containsExactlyInAnyOrder(
                business(1, "Business 1"),
                business(2, "Business 2"),
                business(3, "Business 3"),
                supplier1p(100, "Supplier 1", 1, "000100"),
                supplier1p(200, "Supplier 2", 1, "000200"),
                supplier3p(300, "Supplier 3", 1),
                supplier3p(400, "Supplier 4", 2),
                supplier3p(500, "Supplier 5", 2)
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(1),
                changedTask(2),
                changedTask(3),
                changedTask(100),
                changedTask(200),
                changedTask(300),
                changedTask(400),
                changedTask(500)
            );
    }

    @Test
    public void insertUpdate() {
        deepmindSupplierRepository.save(
            business(1, "Business 1"),
            supplier1p(100, "SUPPLIER!!!", 1, "000000"),
            supplier3p(400, "Supplier 4", 1),
            supplier3p(666, "Supplier 666", 1)
        );


        handler.process(List.of(
            partner(1, "Business 1"),
            partner(2, "Business 2"),
            partner(3, "Business 3"),
            partner1p(100, "Supplier 1", 1, "000100", false, false, false),
            partner1p(200, "Supplier 2", 1, "000200", true, false, false),
            partner3p(300, "Supplier 3", 1, true, false, false),
            partner3p(400, "Supplier 4", 2, false, true, false),
            partner3p(500, "Supplier 5", 2, true, true, false),
            partner3p(600, "Supplier 6", 2, false, false, false),
            partner3p(700, "Supplier 7", 2, false, false, true)
        ));

        var all = deepmindSupplierRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorIgnoringFields("dropship", "fulfillment", "crossdock")
            .containsExactlyInAnyOrder(
                business(1, "Business 1"),
                business(2, "Business 2"),
                business(3, "Business 3"),
                supplier1p(100, "Supplier 1", 1, "000100"),
                supplier1p(200, "Supplier 2", 1, "000200"),
                supplier3p(300, "Supplier 3", 1),
                supplier3p(400, "Supplier 4", 2),
                supplier3p(500, "Supplier 5", 2),
                // 666 is not deleted, because currently suppliers are not deleted
                supplier3p(666, "Supplier 666", 1)
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(2),
                changedTask(3),
                changedTask(100),
                changedTask(200),
                changedTask(300),
                changedTask(400),
                changedTask(500)
            );
    }


    @Test
    public void triggerOfferUpdateTest() {
        deepmindSupplierRepository.save(
            business(1, "Business 1"),
            supplier1p(100, "SUPPLIER!!!", 1, "000000"),
            supplier1p(200, "Supplier 2", 123, "000200"),
            supplier3p(300, "Supplier 3", 1),
            supplier3p(400, "Supplier 4", 2)
        );


        handler.process(List.of(
            partner(1, "Business 1"),
            partner(2, "Business 2"),
            partner(3, "Business 3"),
            partner1p(100, "Supplier 1", 1, "000100", false, false, false),
            partner1p(200, "Supplier 2", 0, "000200", true, false, false),
            partner3p(300, "Supplier 3", 0, true, false, false),
            partner3p(400, "Supplier 4", 0, false, true, false),
            partner3p(500, "Supplier 5", 2, true, true, false),
            partner3p(600, "Supplier 6", 2, false, false, false),
            partner3p(700, "Supplier 7", 2, false, false, true)
        ));

        var all = deepmindSupplierRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorIgnoringFields("dropship", "fulfillment", "crossdock")
            .containsExactlyInAnyOrder(
                business(1, "Business 1"),
                business(2, "Business 2"),
                business(3, "Business 3"),
                supplier1p(100, "Supplier 1", 1, "000100"),
                supplier1p(200, "Supplier 2", null, "000200"),
                supplier3p(300, "Supplier 3", null),
                supplier3p(400, "Supplier 4", null),
                supplier3p(500, "Supplier 5", 2)
            );

        ArgumentCaptor<Set<Integer>> argCapture = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(offerChangesServiceHelper).syncMskuOffersByBusinessId(argCapture.capture(),
            Mockito.anyString(), Mockito.anyBoolean());
        assertThat(argCapture.getValue()).contains(1, 2);

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(2),
                changedTask(3),
                changedTask(100),
                changedTask(200),
                changedTask(300),
                changedTask(400),
                changedTask(500)
            );
    }

    @Test
    public void updateDeepmindOfferSupplierTypeTest() {
        deepmindSupplierRepository.save(
            business(1, "Business 1"),
            supplier1p(100, "SUPPLIER!!!", 1, "000000"),
            supplier1p(200, "Supplier 2", 123, "000200"),
            supplier3p(300, "Supplier 3", 1),
            supplier3p(400, "Supplier 4", 2),
            supplier1p(500, "Supplier 5", 2, "000500")
        );

        deepmindOfferRepository.save(
            offer(300, "sku-333", 1, SupplierType.THIRD_PARTY),
            offer(500, "sku-555", 5, SupplierType.REAL_SUPPLIER)
        );

        handler.process(List.of(
            partner(1, "Business 1"),
            partner(2, "Business 2"),
            partner(3, "Business 3"),
            partner1p(100, "Supplier 1", 1, "000100", false, false, false),
            partner1p(200, "Supplier 2", 0, "000200", true, false, false),
            partner3p(300, "Supplier 3", 0, true, false, false),
            partner3p(400, "Supplier 4", 0, false, true, false),
            partner3p(500, "Supplier 5", 2, true, true, false),
            partner3p(600, "Supplier 6", 2, false, false, false),
            partner3p(700, "Supplier 7", 2, false, false, true)
        ));

        var all = deepmindSupplierRepository.findAll();
        Assertions.assertThat(all)
            .usingElementComparatorIgnoringFields("dropship", "fulfillment", "crossdock")
            .containsExactlyInAnyOrder(
                business(1, "Business 1"),
                business(2, "Business 2"),
                business(3, "Business 3"),
                supplier1p(100, "Supplier 1", 1, "000100"),
                supplier1p(200, "Supplier 2", null, "000200"),
                supplier3p(300, "Supplier 3", null),
                supplier3p(400, "Supplier 4", null),
                supplier3p(500, "Supplier 5", 2)
            );

        Assertions.assertThat(deepmindOfferRepository.findAll())
            .usingElementComparatorOnFields("businessId", "supplierId", "shopSku", "supplierType")
            .containsExactlyInAnyOrder(
                offer(300, "sku-333", 1, SupplierType.THIRD_PARTY),
                offer(500, "sku-555", 5, SupplierType.THIRD_PARTY)
            );

        List<SupplierAvailabilityChangedTask> queueTasks = getQueueTasks();
        Assertions.assertThat(queueTasks)
            .containsExactlyInAnyOrder(
                changedTask(2),
                changedTask(3),
                changedTask(100),
                changedTask(200),
                changedTask(300),
                changedTask(400),
                changedTask(500)
            );
    }


    private static PartnerInfo.PartnerInfoEvent partner3p(int id, String name, int businessId,
                                                          boolean fulfilment, boolean crossdock, boolean dropship) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setName(name)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.THREE_P)
            .setBusinessId(businessId)
            .setIsFullfilment(fulfilment)
            .setIsCrossdock(crossdock)
            .setIsDropship(dropship)
            .build();
    }

    private static PartnerInfo.PartnerInfoEvent partner1p(int id, String name, int businessId, String rsId,
                                                          boolean fulfilment, boolean crossdock, boolean dropship) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setName(name)
            .setType(PartnerInfo.MbiPartnerType.SUPPLIER)
            .setSupplierType(PartnerInfo.SupplierType.REAL)
            .setBusinessId(businessId)
            .setRealSupplierId(rsId)
            .setIsFullfilment(fulfilment)
            .setIsCrossdock(crossdock)
            .setIsDropship(dropship)
            .build();
    }

    private static PartnerInfo.PartnerInfoEvent partner(int id, String name) {
        return PartnerInfo.PartnerInfoEvent.newBuilder()
            .setId(id)
            .setName(name)
            .setType(PartnerInfo.MbiPartnerType.BUSINESS)
            .build();
    }

    private static Supplier supplier3p(int id, String name, Integer businessId) {
        return new Supplier()
            .setId(id)
            .setName(name)
            .setSupplierType(SupplierType.THIRD_PARTY)
            .setBusinessId(businessId);
    }

    private static Supplier supplier1p(int id, String name, Integer businessId, String rsId) {
        return new Supplier()
            .setId(id)
            .setName(name)
            .setSupplierType(SupplierType.REAL_SUPPLIER)
            .setBusinessId(businessId)
            .setRealSupplierId(rsId);
    }

    private static Supplier business(int id, String name) {
        return new Supplier()
            .setId(id)
            .setName(name)
            .setSupplierType(SupplierType.BUSINESS);
    }

    private static SupplierAvailabilityChangedTask changedTask(int supplierId) {
        return new SupplierAvailabilityChangedTask(supplierId, "", Instant.now());
    }

    private ServiceOfferReplica offer(int supplierId, String shopSku, int businessId, SupplierType supplierType) {
        return new ServiceOfferReplica()
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setBusinessId(businessId)
            .setTitle("title")
            .setMskuId(123L)
            .setCategoryId(123L)
            .setSeqId(1L)
            .setSupplierType(supplierType)
            .setAcceptanceStatus(OfferAcceptanceStatus.OK)
            .setModifiedTs(Instant.now());
    }
}
