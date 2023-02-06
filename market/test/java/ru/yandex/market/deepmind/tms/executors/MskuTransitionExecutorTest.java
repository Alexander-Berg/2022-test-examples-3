package ru.yandex.market.deepmind.tms.executors;

import java.time.LocalDate;

import javax.annotation.Resource;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.BlockReasonKey;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuAvailabilityMatrix;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.MskuAvailabilityMatrixRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper;

import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository
    .EKATERINBURG_ID;
import static ru.yandex.market.deepmind.common.repository.logicstics.warehouse.DeepmindWarehouseRepository
    .SAINT_PETERSBURG_ID;


public class MskuTransitionExecutorTest extends DeepmindBaseDbTestClass {

    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private MskuAvailabilityMatrixRepository mskuAvailabilityMatrixRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;

    private MskuTransitionExecutor executor;

    @Before
    public void setUp() {
        executor = new MskuTransitionExecutor(namedParameterJdbcTemplate, mskuStatusRepository,
            mskuAvailabilityMatrixRepository, deepmindMskuRepository, TransactionHelper.MOCK);
        executor.setBatchSize(2);
    }

    @Test
    public void testCopyStatus() {
        insertMsku(111); // old
        insertMsku(222); // new
        insertStatus(111, MskuStatusValue.REGULAR);
        insertStatus(222, MskuStatusValue.EMPTY);
        insertTransition(111, 222);

        executor.execute();

        Assertions.assertThat(mskuStatusRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "mskuStatus", "comment")
            .containsExactlyInAnyOrder(
                new MskuStatus().setMarketSkuId(111L).setMskuStatus(MskuStatusValue.REGULAR),
                new MskuStatus().setMarketSkuId(222L).setMskuStatus(MskuStatusValue.REGULAR)
                    .setComment("Скопировано с MSKU (111) DEEPMIND-557")
            );
    }

    @Test
    public void testCopyStatusForNull() {
        insertMsku(111); // old
        insertMsku(222); // new
        insertStatus(111, MskuStatusValue.ARCHIVE);
        insertTransition(111, 222);

        executor.execute();

        Assertions.assertThat(mskuStatusRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "mskuStatus", "comment")
            .containsExactlyInAnyOrder(
                new MskuStatus().setMarketSkuId(111L).setMskuStatus(MskuStatusValue.ARCHIVE),
                new MskuStatus().setMarketSkuId(222L).setMskuStatus(MskuStatusValue.ARCHIVE)
                    .setComment("Скопировано с MSKU (111) DEEPMIND-557")
            );
    }

    @Test
    public void testCopyStatusWillSkippedForTheSame() {
        insertMsku(111); // old
        insertMsku(222); // new
        insertStatus(111, MskuStatusValue.REGULAR);
        insertStatus(222, MskuStatusValue.REGULAR);
        insertTransition(111, 222);

        executor.execute();

        Assertions.assertThat(mskuStatusRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "mskuStatus", "comment")
            .containsExactlyInAnyOrder(
                new MskuStatus().setMarketSkuId(111L).setMskuStatus(MskuStatusValue.REGULAR),
                new MskuStatus().setMarketSkuId(222L).setMskuStatus(MskuStatusValue.REGULAR)
            );
    }

    @Test
    public void testCopyStatusWillSkippedForArchive() {
        insertMsku(111); // old
        insertMsku(222); // new
        insertStatus(111, MskuStatusValue.REGULAR);
        insertStatus(222, MskuStatusValue.ARCHIVE);
        insertTransition(111, 222);

        executor.execute();

        Assertions.assertThat(mskuStatusRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "mskuStatus", "comment")
            .containsExactlyInAnyOrder(
                new MskuStatus().setMarketSkuId(111L).setMskuStatus(MskuStatusValue.REGULAR),
                new MskuStatus().setMarketSkuId(222L).setMskuStatus(MskuStatusValue.ARCHIVE)
            );
    }

    @Test
    public void testCopyStatusManyToOne() {
        insertMsku(111); // old
        insertMsku(112); // old
        insertMsku(11); // new
        insertStatus(111, MskuStatusValue.REGULAR);
        insertStatus(112, MskuStatusValue.NPD);
        insertStatus(11, MskuStatusValue.EMPTY);
        insertTransition(111, 11);
        insertTransition(112, 11);

        executor.execute();

        Assertions.assertThat(mskuStatusRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "mskuStatus", "comment")
            .containsExactlyInAnyOrder(
                new MskuStatus().setMarketSkuId(111L).setMskuStatus(MskuStatusValue.REGULAR),
                new MskuStatus().setMarketSkuId(112L).setMskuStatus(MskuStatusValue.NPD),
                new MskuStatus().setMarketSkuId(11L).setMskuStatus(MskuStatusValue.REGULAR)
                    .setComment("Скопировано с MSKU (111) DEEPMIND-557")
            );
    }

    @Test
    public void testCopyAvailability() {
        insertMsku(111); // old
        insertMsku(222); // new
        insertAvailability(111, SAINT_PETERSBURG_ID, false, BlockReasonKey.SSKU_PRIORITIZING_SALES);
        insertAvailability(111, EKATERINBURG_ID, true,
            BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK);
        insertTransition(111, 222);

        executor.execute();

        Assertions.assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "available", "comment", "blockReasonKey")
            .containsExactlyInAnyOrder(
                new MskuAvailabilityMatrix()
                    .setMarketSkuId(111L)
                    .setWarehouseId(SAINT_PETERSBURG_ID)
                    .setAvailable(false)
                    .setBlockReasonKey(BlockReasonKey.SSKU_PRIORITIZING_SALES),
                new MskuAvailabilityMatrix()
                    .setMarketSkuId(111L)
                    .setWarehouseId(EKATERINBURG_ID)
                    .setAvailable(true)
                    .setBlockReasonKey(BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK),

                new MskuAvailabilityMatrix()
                    .setMarketSkuId(222L)
                    .setWarehouseId(SAINT_PETERSBURG_ID)
                    .setAvailable(false)
                    .setComment("Скопировано с MSKU (111) DEEPMIND-557")
                    .setBlockReasonKey(BlockReasonKey.SSKU_PRIORITIZING_SALES),
                new MskuAvailabilityMatrix().setMarketSkuId(222L)
                    .setWarehouseId(EKATERINBURG_ID)
                    .setAvailable(true)
                    .setComment("Скопировано с MSKU (111) DEEPMIND-557")
                    .setBlockReasonKey(BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK)
            );
    }

    @Test
    public void testDontOverrideAvailability() {
        insertMsku(111); // old
        insertMsku(222); // new
        insertAvailability(111, SAINT_PETERSBURG_ID, true, BlockReasonKey.SSKU_PRIORITIZING_SALES);
        insertAvailability(222, SAINT_PETERSBURG_ID, false,
            BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK);
        insertTransition(111, 222);

        executor.execute();

        Assertions.assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "available", "comment", "blockReasonKey")
            .containsExactlyInAnyOrder(
                new MskuAvailabilityMatrix()
                    .setMarketSkuId(111L)
                    .setWarehouseId(SAINT_PETERSBURG_ID)
                    .setAvailable(true)
                    .setBlockReasonKey(BlockReasonKey.SSKU_PRIORITIZING_SALES),
                new MskuAvailabilityMatrix()
                    .setMarketSkuId(222L)
                    .setWarehouseId(SAINT_PETERSBURG_ID)
                    .setAvailable(false)
                    .setBlockReasonKey(BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK)
            );
    }

    @Test
    public void testCopyAvailabilityManyToOne() {
        insertMsku(111); // old
        insertMsku(112); // old
        insertMsku(11);  // new
        insertAvailability(111, EKATERINBURG_ID, false, BlockReasonKey.SSKU_PRIORITIZING_SALES);
        insertAvailability(112, SAINT_PETERSBURG_ID, false,
            BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK);
        insertTransition(111, 11);
        insertTransition(112, 11);

        executor.execute();

        Assertions.assertThat(mskuAvailabilityMatrixRepository.findAll())
            .usingElementComparatorOnFields("marketSkuId", "warehouseId", "available", "comment", "blockReasonKey")
            .containsExactlyInAnyOrder(
                new MskuAvailabilityMatrix()
                    .setMarketSkuId(111L)
                    .setWarehouseId(EKATERINBURG_ID)
                    .setAvailable(false)
                    .setBlockReasonKey(BlockReasonKey.SSKU_PRIORITIZING_SALES),
                new MskuAvailabilityMatrix()
                    .setMarketSkuId(112L)
                    .setWarehouseId(SAINT_PETERSBURG_ID)
                    .setAvailable(false)
                    .setBlockReasonKey(BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK),

                new MskuAvailabilityMatrix()
                    .setMarketSkuId(11L)
                    .setWarehouseId(SAINT_PETERSBURG_ID)
                    .setAvailable(false)
                    .setComment("Скопировано с MSKU (112) DEEPMIND-557")
                    .setBlockReasonKey(BlockReasonKey.SSKU_SUPPLIES_OPTIMIZATION_BLOCK)
            );
    }

    private void insertTransition(long oldId, long newId) {
        jdbcTemplate.update("insert into msku.msku_transition values (?, ?)", oldId, newId);
    }

    private void insertMsku(long id) {
        deepmindMskuRepository.save(TestUtils.newMsku(id));
    }

    private void insertStatus(long mskuId, MskuStatusValue mskuStatusValue) {
        MskuStatus mskuStatus = new MskuStatus()
            .setMarketSkuId(mskuId)
            .setMskuStatus(mskuStatusValue);
        if (mskuStatusValue == MskuStatusValue.NPD) {
            mskuStatus.setNpdStartDate(LocalDate.now());
        }
        mskuStatusRepository.save(mskuStatus);
    }

    private void insertAvailability(long mskuId, long warehouseId, boolean available, BlockReasonKey blockReasonKey) {
        mskuAvailabilityMatrixRepository.save(new MskuAvailabilityMatrix()
            .setMarketSkuId(mskuId)
            .setWarehouseId(warehouseId)
            .setAvailable(available)
            .setBlockReasonKey(blockReasonKey)
        );
    }

}
