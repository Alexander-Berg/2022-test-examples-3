package ru.yandex.market.billing.tasks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link TablePartitionAddService}.
 */
class TablePartitionAddServiceTest {

    private static final int CONFIGURED_VALUE = 100;
    private static final int ADD_ARGUMENT_VALUE = 10;
    private static final int REMAINING_DAYS_VALUE = 70;
    private static final String ENV_ACTION_TABLE_DAYS_TO_ADD = "mbi.billing.table.partition.remaining.days";
    private static final String ENV_MAX_PARTITIONS_TO_ADD = "mbi.billing.add.partition.max.batch.size";
    private static final int DEFAULT_REMAINING_DAYS = 90;
    private static final int DEFAULT_AMOUNT_PARTITIONS_TO_ADD = 10;
    private static final int SMALLER_THAN_BATCH_REMAIN_DAYS = 5;
    private static final String TABLE_NAME = "action";
    private static final String PARTITION_PREFIX = "p_action_";
    private static final String SCHEMA_NAME = "shops_web";
    private static final int MORE_THAN_ALLOWED_PARTITIONS_TO_ADD = 20;

    private EnvironmentService environmentService = mock(EnvironmentService.class);

    private TablePartitionAddDbService mockDao = mock(TablePartitionAddDbService.class);

    private TablePartitionAddService tablePartitionAddService =
            new TablePartitionAddService(mockDao, environmentService);

    @DisplayName("Проверяем, что сервис не добавляем партиции, когда их достаточно.")
    @Test
    void test_shouldNotAddPartitionsIfHaveEnough() {
        when(environmentService.getIntValue(anyString())).thenReturn(CONFIGURED_VALUE);
        when(mockDao.getRemainingDaysCount(SCHEMA_NAME, TABLE_NAME, PARTITION_PREFIX)).thenReturn(CONFIGURED_VALUE);
        tablePartitionAddService.addPartitions(SCHEMA_NAME, TABLE_NAME, PARTITION_PREFIX);
        verify(mockDao, never()).addPartition(SCHEMA_NAME, TABLE_NAME, PARTITION_PREFIX);
    }

    @DisplayName("Проверяем, что сервис добавляет количество партиций не больше разрешенного размера.")
    @Test
    void test_shouldCreateBatchNumberOfPartitions() {
        when(environmentService.getIntValue(eq(ENV_ACTION_TABLE_DAYS_TO_ADD), eq(DEFAULT_REMAINING_DAYS)))
                .thenReturn(CONFIGURED_VALUE);
        when(environmentService.getIntValue(eq(ENV_MAX_PARTITIONS_TO_ADD), eq(DEFAULT_AMOUNT_PARTITIONS_TO_ADD)))
                .thenReturn(DEFAULT_AMOUNT_PARTITIONS_TO_ADD);
        when(mockDao.getRemainingDaysCount(SCHEMA_NAME, TABLE_NAME, PARTITION_PREFIX)).thenReturn(REMAINING_DAYS_VALUE);
        tablePartitionAddService.addPartitions(SCHEMA_NAME, TABLE_NAME, PARTITION_PREFIX);
        verify(mockDao, times(ADD_ARGUMENT_VALUE)).addPartition(SCHEMA_NAME, TABLE_NAME, PARTITION_PREFIX);
    }

    @DisplayName("Проверяем, что сервис добавляет недостающее количество партиций, если оно меньше размера батча.")
    @Test
    void test_shouldAddDifferenceIfItsSmallerThanBatch() {
        when(environmentService.getIntValue(eq(ENV_ACTION_TABLE_DAYS_TO_ADD), eq(DEFAULT_REMAINING_DAYS)))
                .thenReturn(CONFIGURED_VALUE);
        when(environmentService.getIntValue(eq(ENV_MAX_PARTITIONS_TO_ADD), eq(DEFAULT_AMOUNT_PARTITIONS_TO_ADD)))
                .thenReturn(DEFAULT_AMOUNT_PARTITIONS_TO_ADD);
        when(mockDao.getRemainingDaysCount(SCHEMA_NAME, TABLE_NAME, PARTITION_PREFIX))
                .thenReturn(CONFIGURED_VALUE - SMALLER_THAN_BATCH_REMAIN_DAYS);
        tablePartitionAddService.addPartitions(SCHEMA_NAME, TABLE_NAME, PARTITION_PREFIX);
        verify(mockDao, times(SMALLER_THAN_BATCH_REMAIN_DAYS)).addPartition(SCHEMA_NAME, TABLE_NAME, PARTITION_PREFIX);
    }

    @Test
    @DisplayName("Проверяем ограничение на максимальное количество добавляемых партиций.")
    void test_shouldReturnNoMoreThanMaxSize() {
        when(environmentService.getIntValue(eq(ENV_ACTION_TABLE_DAYS_TO_ADD), eq(DEFAULT_REMAINING_DAYS)))
                .thenReturn(CONFIGURED_VALUE);
        when(environmentService.getIntValue(eq(ENV_MAX_PARTITIONS_TO_ADD), eq(DEFAULT_AMOUNT_PARTITIONS_TO_ADD)))
                .thenReturn(MORE_THAN_ALLOWED_PARTITIONS_TO_ADD);
        int result = tablePartitionAddService.getAmountToAdd(REMAINING_DAYS_VALUE, CONFIGURED_VALUE);

        assertEquals(result, DEFAULT_AMOUNT_PARTITIONS_TO_ADD);
    }
}