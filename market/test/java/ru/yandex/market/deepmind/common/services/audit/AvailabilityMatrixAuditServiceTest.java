package ru.yandex.market.deepmind.common.services.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.assertj.core.api.Assertions;
import org.jooq.JSONB;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.audit.enums.ChangeType;
import ru.yandex.market.deepmind.common.db.jooq.generated.audit.enums.EntityType;
import ru.yandex.market.deepmind.common.db.jooq.generated.audit.tables.pojos.Audit;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.CategoryAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.SupplierAvailabilityMatrix;
import ru.yandex.market.deepmind.common.repository.AuditRepository;
import ru.yandex.market.deepmind.common.services.audit.pojo.DisplayCategoryAvailabilityMatrixAudit;
import ru.yandex.market.deepmind.common.services.audit.pojo.DisplayMskuAvailabilityMatrixAudit;
import ru.yandex.market.deepmind.common.services.audit.pojo.DisplaySskuAvailabilityMatrixAudit;
import ru.yandex.market.deepmind.common.services.audit.pojo.DisplaySupplierAvailabilityMatrixAudit;
import ru.yandex.market.deepmind.common.services.audit.pojo.parsing.BaseAvailabilityMatrixChange;
import ru.yandex.market.deepmind.common.services.audit.pojo.parsing.CategoryAvailabilityMatrixAudit;
import ru.yandex.market.deepmind.common.services.audit.pojo.parsing.MskuAvailabilityMatrixAudit;
import ru.yandex.market.deepmind.common.services.audit.pojo.parsing.SupplierAvailabilityMatrixAudit;

import static ru.yandex.market.deepmind.common.services.audit.pojo.DisplayBaseAvailabilityMatrixAudit.AuditAvailability;

public class AvailabilityMatrixAuditServiceTest extends DeepmindBaseDbTestClass {

    @Autowired
    private AuditRepository auditRepository;
    private AvailabilityMatrixAuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    @Before
    public void setUpd() {
        auditService = new AvailabilityMatrixAuditServiceImpl(auditRepository);
    }

    @Test
    public void emptyAudit() {
        int supplierId = 111222333;
        long warehouseId = 333444555;
        var result = auditService.getSupplierAvailabilityMatrixAudit(supplierId, warehouseId, null, 10);
        Assertions
            .assertThat(result)
            .hasSize(0);
    }

    @Test
    public void getSupplierAvailabilityMatrixAuditTest() throws JsonProcessingException {
        var entityType = EntityType.supplier_availability_matrix;
        int supplierId = 111222333;
        long warehouseId = 333444555;
        var auditKeys = objectMapper.writeValueAsString(
            new SupplierAvailabilityMatrixAudit.Key(supplierId, warehouseId));
        auditRepository.save(
            audit(Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.INSERT,
                entityType, "", auditKeys,
                objectMapper.writeValueAsString(
                    supplierAvailabilityMatrix(supplierId, warehouseId, false, "comment1",
                        BlockReasonKey.SUPPLIER_DEBT))),
            audit(Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.UPDATE,
                entityType, "", auditKeys,
                objectMapper.writeValueAsString(
                    baseAvailabilityMatrixChange(List.of(false, true), List.of("comment1", "comment2"),
                        List.of(BlockReasonKey.SUPPLIER_DEBT, BlockReasonKey.SUPPLIER_LOW_SL)))),
            audit(Instant.now().minus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.UPDATE,
                entityType, "", auditKeys,
                objectMapper.writeValueAsString(baseAvailabilityMatrixChange(null, List.of("comment2", "comment3"),
                    null))),
            audit(Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.DELETE,
                entityType, "", auditKeys,
                objectMapper.writeValueAsString(
                    supplierAvailabilityMatrix(supplierId, warehouseId, null, "comment4",
                        BlockReasonKey.SUPPLIER_DEBT)))
        );
        var result = auditService.getSupplierAvailabilityMatrixAudit(supplierId, warehouseId, null, 10);
        Assertions
            .assertThat(result)
            .hasSize(3)
            .usingElementComparatorOnFields("supplierId", "warehouseId", "eventTs", "availability", "comment",
                "blockReasonKey")
            .containsExactly(
                displaySupplierAvailabilityMatrix(supplierId, warehouseId,
                    Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.EMPTY, "comment4", BlockReasonKey.SUPPLIER_DEBT),
                displaySupplierAvailabilityMatrix(supplierId, warehouseId,
                    Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.PERMITTED, "comment2", BlockReasonKey.SUPPLIER_LOW_SL),
                displaySupplierAvailabilityMatrix(supplierId, warehouseId,
                    Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.LOCKED, "comment1", BlockReasonKey.SUPPLIER_DEBT)
            );

        result = auditService.getSupplierAvailabilityMatrixAudit(supplierId, warehouseId,
            Instant.now().minus(10, ChronoUnit.DAYS), 10);
        Assertions
            .assertThat(result)
            .hasSize(2)
            .usingElementComparatorOnFields("supplierId", "warehouseId", "eventTs", "availability", "comment",
                "blockReasonKey")
            .containsExactly(
                displaySupplierAvailabilityMatrix(supplierId, warehouseId,
                    Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.EMPTY, "comment4", BlockReasonKey.SUPPLIER_DEBT),
                displaySupplierAvailabilityMatrix(supplierId, warehouseId,
                    Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.PERMITTED, "comment2", BlockReasonKey.SUPPLIER_LOW_SL)
            );
    }

    @Test
    public void getCategoryAvailabilityMatrixAuditTest() throws JsonProcessingException {
        var entityType = EntityType.category_availability_matrix;
        long categoryId = 111222333;
        long warehouseId = 333444555;
        var auditKeys = objectMapper.writeValueAsString(
            new CategoryAvailabilityMatrixAudit.Key(categoryId, warehouseId));
        auditRepository.save(
            audit(Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.INSERT,
                entityType, "", auditKeys,
                objectMapper.writeValueAsString(
                    categoryAvailabilityMatrix(categoryId, warehouseId, false, "comment1",
                        BlockReasonKey.CATEGORY_TOP_UP_SCHEME))),
            audit(Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.UPDATE,
                entityType, "", auditKeys,
                objectMapper.writeValueAsString(
                    baseAvailabilityMatrixChange(List.of(false, true), List.of("comment1", "comment2"),
                        List.of(BlockReasonKey.CATEGORY_TOP_UP_SCHEME, BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS)))),
            audit(Instant.now().minus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.UPDATE,
                entityType, "", auditKeys,
                objectMapper.writeValueAsString(baseAvailabilityMatrixChange(null, List.of("comment2", "comment3"),
                    null))),
            audit(Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.DELETE,
                entityType, "", auditKeys,
                objectMapper.writeValueAsString(
                    categoryAvailabilityMatrix(categoryId, warehouseId, null, "comment4",
                        BlockReasonKey.CATEGORY_TOP_UP_SCHEME)))
        );
        var result = auditService.getCategoryAvailabilityMatrixAudit(categoryId, warehouseId, null, 10);
        Assertions
            .assertThat(result)
            .hasSize(3)
            .usingElementComparatorOnFields("categoryId", "warehouseId", "eventTs", "availability", "comment")
            .containsExactly(
                displayCategoryAvailabilityMatrix(categoryId, warehouseId,
                    Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.EMPTY, "comment4", BlockReasonKey.CATEGORY_TOP_UP_SCHEME),
                displayCategoryAvailabilityMatrix(categoryId, warehouseId,
                    Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.PERMITTED, "comment2", BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS),
                displayCategoryAvailabilityMatrix(categoryId, warehouseId,
                    Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.LOCKED, "comment1", BlockReasonKey.CATEGORY_TOP_UP_SCHEME)
            );

        result = auditService.getCategoryAvailabilityMatrixAudit(categoryId, warehouseId,
            Instant.now().minus(10, ChronoUnit.DAYS), 10);
        Assertions
            .assertThat(result)
            .hasSize(2)
            .usingElementComparatorOnFields("categoryId", "warehouseId", "eventTs", "availability", "comment")
            .containsExactly(
                displayCategoryAvailabilityMatrix(categoryId, warehouseId,
                    Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.EMPTY, "comment4", BlockReasonKey.CATEGORY_TOP_UP_SCHEME),
                displayCategoryAvailabilityMatrix(categoryId, warehouseId,
                    Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.PERMITTED, "comment2", BlockReasonKey.CATEGORY_LEGAL_REQUIREMENTS)
            );
    }

    @Test
    public void getSskuAvailabilityMatrixAuditTest() throws JsonProcessingException {
        var entityType = EntityType.ssku_availability_matrix;
        int supplierId = 111222333;
        long warehouseId = 333444555;
        String shopSku = "666777888";
        auditRepository.save(
            audit(Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.INSERT,
                entityType, supplierId + " " + shopSku + " " + warehouseId, null,
                objectMapper.writeValueAsString(
                    sskuAvailabilityMatrix(supplierId, shopSku, warehouseId, false, "comment1",
                        BlockReasonKey.SSKU_STOP_WORD_BLOCK))),
            audit(Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.UPDATE,
                entityType, supplierId + " " + shopSku + " " + warehouseId, null,
                objectMapper.writeValueAsString(
                    baseAvailabilityMatrixChange(List.of(false, true), List.of("comment1", "comment2"),
                        List.of(BlockReasonKey.SSKU_STOP_WORD_BLOCK, BlockReasonKey.SSKU_FOR_TEST_UNBLOCK_TMP)))),
            audit(Instant.now().minus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.UPDATE,
                entityType, supplierId + " " + shopSku + " " + warehouseId, null,
                objectMapper.writeValueAsString(baseAvailabilityMatrixChange(null, List.of("comment2", "comment3"),
                    null))),
            audit(Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.DELETE,
                entityType, supplierId + " " + shopSku + " " + warehouseId, null,
                objectMapper.writeValueAsString(
                    sskuAvailabilityMatrix(supplierId, shopSku, warehouseId, null, "comment4",
                        BlockReasonKey.SSKU_STOP_WORD_BLOCK)))
        );
        var result = auditService.getSskuAvailabilityMatrixAudit(shopSku, supplierId, warehouseId, null, 10);
        Assertions
            .assertThat(result)
            .hasSize(3)
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId",
                "eventTs", "availability", "comment")
            .containsExactly(
                displaySskuAvailabilityMatrix(supplierId, shopSku, warehouseId,
                    Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.EMPTY, "comment4", BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                displaySskuAvailabilityMatrix(supplierId, shopSku, warehouseId,
                    Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.PERMITTED, "comment2", BlockReasonKey.SSKU_FOR_TEST_UNBLOCK_TMP),
                displaySskuAvailabilityMatrix(supplierId, shopSku, warehouseId,
                    Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.LOCKED, "comment1", BlockReasonKey.SSKU_STOP_WORD_BLOCK)
            );

        result = auditService.getSskuAvailabilityMatrixAudit(shopSku, supplierId, warehouseId,
            Instant.now().minus(10, ChronoUnit.DAYS), 10);
        Assertions
            .assertThat(result)
            .hasSize(2)
            .usingElementComparatorOnFields("supplierId", "shopSku", "warehouseId",
                "eventTs", "availability", "comment")
            .containsExactly(
                displaySskuAvailabilityMatrix(supplierId, shopSku, warehouseId,
                    Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.EMPTY, "comment4", BlockReasonKey.SSKU_STOP_WORD_BLOCK),
                displaySskuAvailabilityMatrix(supplierId, shopSku, warehouseId,
                    Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.PERMITTED, "comment2", BlockReasonKey.SSKU_FOR_TEST_UNBLOCK_TMP)
            );
    }

    @Test
    public void getMskuAvailabilityMatrixAuditTest() throws JsonProcessingException {
        var entityType = EntityType.msku_availability_matrix;
        long mskuId = 111222333;
        long warehouseId = 333444555;
        var auditKeys = objectMapper.writeValueAsString(new MskuAvailabilityMatrixAudit.Key(warehouseId, mskuId));
        auditRepository.save(
            audit(Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.INSERT,
                entityType, Long.toString(mskuId), auditKeys,
                objectMapper.writeValueAsString(mskuAvailabilityMatrix(mskuId, warehouseId, false, "comment1",
                    BlockReasonKey.MSKU_TOP_UP_SCHEME))),
            audit(Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.UPDATE,
                entityType, Long.toString(mskuId), auditKeys,
                objectMapper.writeValueAsString(
                    baseAvailabilityMatrixChange(List.of(false, true), List.of("comment1", "comment2"),
                        List.of(BlockReasonKey.MSKU_TOP_UP_SCHEME, BlockReasonKey.MSKU_SAFETY_REQUIREMENTS)))),
            audit(Instant.now().minus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.UPDATE,
                entityType, Long.toString(mskuId), auditKeys,
                objectMapper.writeValueAsString(baseAvailabilityMatrixChange(null, List.of("comment2", "comment3"),
                    null))),
            audit(Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS), ChangeType.DELETE,
                entityType, Long.toString(mskuId), auditKeys,
                objectMapper.writeValueAsString(mskuAvailabilityMatrix(mskuId, warehouseId, null, "comment4",
                    BlockReasonKey.MSKU_TOP_UP_SCHEME)))
        );
        var result = auditService.getMskuAvailabilityMatrixAudit(mskuId, warehouseId, null, 10);
        Assertions
            .assertThat(result)
            .hasSize(3)
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "eventTs", "availability", "comment")
            .containsExactly(
                displayMskuAvailabilityMatrix(mskuId, warehouseId,
                    Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.EMPTY, "comment4", BlockReasonKey.MSKU_TOP_UP_SCHEME),
                displayMskuAvailabilityMatrix(mskuId, warehouseId,
                    Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.PERMITTED, "comment2", BlockReasonKey.MSKU_SAFETY_REQUIREMENTS),
                displayMskuAvailabilityMatrix(mskuId, warehouseId,
                    Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.LOCKED, "comment1", BlockReasonKey.MSKU_TOP_UP_SCHEME)
            );

        result = auditService.getMskuAvailabilityMatrixAudit(mskuId, warehouseId,
            Instant.now().minus(10, ChronoUnit.DAYS), 10);
        Assertions
            .assertThat(result)
            .hasSize(2)
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "eventTs", "availability", "comment")
            .containsExactly(
                displayMskuAvailabilityMatrix(mskuId, warehouseId,
                    Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.EMPTY, "comment4", BlockReasonKey.MSKU_TOP_UP_SCHEME),
                displayMskuAvailabilityMatrix(mskuId, warehouseId,
                    Instant.now().minus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS),
                    AuditAvailability.PERMITTED, "comment2", BlockReasonKey.MSKU_SAFETY_REQUIREMENTS)
            );
    }

    private Audit audit(Instant eventTs, ChangeType changeType, EntityType entityType, String entityKey, String keys,
                        String changes) {
        return new Audit()
            .setEntityType(entityType)
            .setChangeType(changeType)
            .setEventTs(eventTs)
            .setEntityKey(entityKey)
            .setKeys(JSONB.valueOf(keys))
            .setEventId(111L)
            .setChanges(JSONB.valueOf(changes));
    }

    private DisplayMskuAvailabilityMatrixAudit displayMskuAvailabilityMatrix(
        long mskuId, long warehouseId, Instant eventTs, AuditAvailability availability,
        String comment, BlockReasonKey blockReasonKey) {
        return (DisplayMskuAvailabilityMatrixAudit) new DisplayMskuAvailabilityMatrixAudit()
            .setMarketSkuId(mskuId)
            .setWarehouseId(warehouseId)
            .setAvailability(availability)
            .setEventTs(eventTs)
            .setComment(comment)
            .setBlockReasonKey(blockReasonKey);
    }

    private MskuAvailabilityMatrix mskuAvailabilityMatrix(long mskuId, long warehouseId, Boolean available,
                                                          String comment, BlockReasonKey blockReasonKey) {
        return new MskuAvailabilityMatrix()
            .setId(123L)
            .setMarketSkuId(mskuId)
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setComment(comment)
            .setBlockReasonKey(blockReasonKey);
    }

    private BaseAvailabilityMatrixChange baseAvailabilityMatrixChange(List<Boolean> availableChange,
                                                                      List<String> commentChange,
                                                                      List<BlockReasonKey> blockReasonKey) {
        var result =  new BaseAvailabilityMatrixChange()
            .setComment(commentChange)
            .setBlockReasonKey(blockReasonKey);
        return availableChange == null ? result : result.setAvailable(availableChange);
    }

    private DisplaySskuAvailabilityMatrixAudit displaySskuAvailabilityMatrix(
        int supplierId, String shopSku, long warehouseId, Instant eventTs,
        AuditAvailability availability, String comment, BlockReasonKey blockReasonKey) {
        return (DisplaySskuAvailabilityMatrixAudit) new DisplaySskuAvailabilityMatrixAudit()
            .setShopSku(shopSku)
            .setSupplierId(supplierId)
            .setWarehouseId(warehouseId)
            .setAvailability(availability)
            .setEventTs(eventTs)
            .setComment(comment)
            .setBlockReasonKey(blockReasonKey);
    }

    private SskuAvailabilityMatrix sskuAvailabilityMatrix(int supplierId, String shopSku, long warehouseId,
                                                          Boolean available, String comment,
                                                          BlockReasonKey blockReasonKey) {
        return new SskuAvailabilityMatrix()
            .setId(123L)
            .setSupplierId(supplierId)
            .setShopSku(shopSku)
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setComment(comment)
            .setBlockReasonKey(blockReasonKey);
    }

    private CategoryAvailabilityMatrix categoryAvailabilityMatrix(long categoryId, long warehouseId,
                                                                  Boolean available, String comment,
                                                                  BlockReasonKey blockReasonKey) {
        return new CategoryAvailabilityMatrix()
            .setId(123L)
            .setCategoryId(categoryId)
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setComment(comment)
            .setBlockReasonKey(blockReasonKey);
    }

    private DisplayCategoryAvailabilityMatrixAudit displayCategoryAvailabilityMatrix(
        long categoryId, long warehouseId, Instant eventTs,
        AuditAvailability availability, String comment, BlockReasonKey blockReasonKey) {
        return (DisplayCategoryAvailabilityMatrixAudit) new DisplayCategoryAvailabilityMatrixAudit()
            .setCategoryId(categoryId)
            .setWarehouseId(warehouseId)
            .setAvailability(availability)
            .setEventTs(eventTs)
            .setComment(comment)
            .setBlockReasonKey(blockReasonKey);
    }

    private SupplierAvailabilityMatrix supplierAvailabilityMatrix(int supplierId, long warehouseId,
                                                                  Boolean available, String comment,
                                                                  BlockReasonKey blockReasonKey) {
        return new SupplierAvailabilityMatrix()
            .setId(123L)
            .setSupplierId(supplierId)
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setComment(comment)
            .setBlockReasonKey(blockReasonKey);
    }

    private DisplaySupplierAvailabilityMatrixAudit displaySupplierAvailabilityMatrix(
        int supplierId, long warehouseId, Instant eventTs,
        AuditAvailability availability, String comment, BlockReasonKey blockReasonKey) {
        return (DisplaySupplierAvailabilityMatrixAudit) new DisplaySupplierAvailabilityMatrixAudit()
            .setSupplierId(supplierId)
            .setWarehouseId(warehouseId)
            .setAvailability(availability)
            .setEventTs(eventTs)
            .setComment(comment)
            .setBlockReasonKey(blockReasonKey);
    }

}
