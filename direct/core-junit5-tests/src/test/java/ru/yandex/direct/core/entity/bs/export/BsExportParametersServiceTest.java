package ru.yandex.direct.core.entity.bs.export;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.common.db.PpcPropertyParseException;
import ru.yandex.direct.core.entity.bs.export.model.WorkerType;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.direct.core.entity.bs.export.BsExportParametersService.DEFAULT_WORKERS_NUM;

class BsExportParametersServiceTest {
    // не связываемся по коду со свойствами, чтобы проверить что не опечатались в их написании
    private static final String ROLLING_WORK = "ALLOW_FULL_LB_EXPORT_ROLLING_WORK";
    private static final String CHUNK_PER_WORKER = "FULL_LB_EXPORT_CHUNK_PER_WORKER";
    private static final String MAX_LB_CAMPS = "MAX_FULL_LB_EXPORT_CAMPAIGNS_IN_QUEUE";
    private static final String CHUNK_PER_ITERATION = "FULL_LB_EXPORT_MAX_CHUNK_PER_ITERATION";

    private static final int SHARDS_NUM = 4;

    @Mock
    private PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    private ShardHelper shardHelper;

    private BsExportParametersService service;


    @BeforeEach
    void prepareMocks() {
        initMocks(this);

        //noinspection unchecked
        when(ppcPropertiesSupport.get(any(PpcPropertyName.class))).thenCallRealMethod();

        List<Integer> shards = IntStream.rangeClosed(1, SHARDS_NUM).boxed().collect(Collectors.toList());
        when(shardHelper.dbShards()).thenReturn(shards);

        service = new BsExportParametersService(ppcPropertiesSupport, shardHelper);
    }

    @Test
    void getWorkersNum_Std_PropertyIsNotSet_ReturnsDefaultValue() {
        when(ppcPropertiesSupport.getByNames(anyCollection()))
                .thenReturn(singletonMap("bs_export_std_workers_num_shard_3", null));

        assertEquals(DEFAULT_WORKERS_NUM, service.getWorkersNum(WorkerType.STD, 3));
    }

    @Test
    void getWorkersNum_Std_PropertySetToValue_ReturnsValue() {
        int value = randomPositiveInt();
        when(ppcPropertiesSupport.getByNames(anyCollection()))
                .thenReturn(singletonMap("bs_export_std_workers_num_shard_1", String.valueOf(value)));

        assertEquals(value, service.getWorkersNum(WorkerType.STD, 1));
    }

    @Test
    void getWorkersNum_FullLbExport_PropertyIsNotSet_ReturnsDefaultValue() {
        when(ppcPropertiesSupport.getByNames(anyCollection()))
                .thenReturn(singletonMap("bs_full_lb_export_workers_num_shard_2", null));

        assertEquals(DEFAULT_WORKERS_NUM, service.getWorkersNum(WorkerType.FULL_LB_EXPORT, 2));
    }

    @Test
    void getWorkersNum_FullLbExport_PropertySetToValue_ReturnsValue() {
        int value = randomPositiveInt();
        when(ppcPropertiesSupport.getByNames(anyCollection()))
                .thenReturn(singletonMap("bs_full_lb_export_workers_num_shard_4", String.valueOf(value)));

        assertEquals(value, service.getWorkersNum(WorkerType.FULL_LB_EXPORT, 4));
    }

    @Test
    void getWorkersNum_Heavy_PropertyIsNotSet_ReturnsDefaultValue() {
        when(ppcPropertiesSupport.getByNames(anyCollection()))
                .thenReturn(singletonMap("bs_export_heavy_workers_num_shard_6", null));

        assertEquals(DEFAULT_WORKERS_NUM, service.getWorkersNum(WorkerType.HEAVY, 6));
    }

    @Test
    void getWorkersNum_Heavy_PropertySetToValue_ReturnsValue() {
        int value = randomPositiveInt();
        when(ppcPropertiesSupport.getByNames(anyCollection()))
                .thenReturn(singletonMap("bs_export_heavy_workers_num_shard_5", String.valueOf(value)));

        assertEquals(value, service.getWorkersNum(WorkerType.HEAVY, 5));
    }

    @Test
    void getWorkersNum_Buggy_PropertyIsNotSet_ReturnsDefaultValue() {
        when(ppcPropertiesSupport.getByNames(anyCollection()))
                .thenReturn(singletonMap("bs_export_buggy_workers_num_shard_7", null));

        assertEquals(DEFAULT_WORKERS_NUM, service.getWorkersNum(WorkerType.BUGGY, 7));
    }

    @Test
    void getWorkersNum_Buggy_PropertySetToValue_ReturnsValue() {
        int value = randomPositiveInt();
        when(ppcPropertiesSupport.getByNames(anyCollection()))
                .thenReturn(singletonMap("bs_export_buggy_workers_num_shard_9", String.valueOf(value)));

        assertEquals(value, service.getWorkersNum(WorkerType.BUGGY, 9));
    }

    @Test
    void setWorkersNumInShard_ForStdValue_WritesValue() {
        int value = randomPositiveInt();

        service.setWorkersNumInShard(WorkerType.STD, 2, value);

        verify(ppcPropertiesSupport).set("bs_export_std_workers_num_shard_2", String.valueOf(value));
    }

    @Test
    void setWorkersNumInShard_ForHeavyValue_WritesValue() {
        int value = randomPositiveInt();

        service.setWorkersNumInShard(WorkerType.HEAVY, 3, value);

        verify(ppcPropertiesSupport).set("bs_export_heavy_workers_num_shard_3", String.valueOf(value));
    }

    @Test
    void setWorkersNumInShard_ForFullLbExportValue_WritesValue() {
        int value = randomPositiveInt();

        service.setWorkersNumInShard(WorkerType.FULL_LB_EXPORT, 1, value);

        verify(ppcPropertiesSupport).set("bs_full_lb_export_workers_num_shard_1", String.valueOf(value));
    }

    @Test
    void setWorkersNumInShard_ForBuggyLbExportValue_WritesValue() {
        int value = randomPositiveInt();

        service.setWorkersNumInShard(WorkerType.BUGGY, 4, value);

        verify(ppcPropertiesSupport).set("bs_export_buggy_workers_num_shard_4", String.valueOf(value));
    }

    @Test
    void setWorkersNumForAllShards_ForStdValue_WriteValueForAllShards() {
        int value = randomPositiveInt();
        String expected = String.valueOf(value);

        Mockito.clearInvocations(ppcPropertiesSupport);
        service.setWorkersNumForAllShards(WorkerType.STD, value);

        verify(ppcPropertiesSupport).set("bs_export_std_workers_num_shard_1", expected);
        verify(ppcPropertiesSupport).set("bs_export_std_workers_num_shard_2", expected);
        verify(ppcPropertiesSupport).set("bs_export_std_workers_num_shard_3", expected);
        verify(ppcPropertiesSupport).set("bs_export_std_workers_num_shard_4", expected);
        verify(ppcPropertiesSupport, never()).set(eq("bs_export_std_workers_num_shard_5"), anyString());
    }

    @Test
    void canFullExportRollingWork_PropertyIsNotSet_ReturnsFalse() {
        assertFalse(service.canFullExportRollingWork());
    }

    @Test
    void canFullExportRollingWork_PropertySetToZero_ReturnsFalse() {
        when(ppcPropertiesSupport.get(ROLLING_WORK)).thenReturn("0");
        assertFalse(service.canFullExportRollingWork());
    }

    @Test
    void canFullExportRollingWork_PropertySetToInvalidValue_ReturnsFalse() {
        when(ppcPropertiesSupport.get(ROLLING_WORK)).thenReturn("42");
        assertFalse(service.canFullExportRollingWork());
    }

    @Test
    void canFullExportRollingWork_PropertySetToOne_ReturnsTrue() {
        when(ppcPropertiesSupport.get(ROLLING_WORK)).thenReturn("1");
        assertTrue(service.canFullExportRollingWork());
    }

    @Test
    void canFullExportRollingWork_PropertySetToTrue_ReturnsTrue() {
        when(ppcPropertiesSupport.get(ROLLING_WORK)).thenReturn("true");
        assertTrue(service.canFullExportRollingWork());
    }

    @Test
    void setFullExportRollingWork_True_WritesOne() {
        service.setFullExportRollingWork(true);
        verify(ppcPropertiesSupport).set(ROLLING_WORK, "1");
    }

    @Test
    void setFullExportRollingWork_False_WritesZero() {
        service.setFullExportRollingWork(false);
        verify(ppcPropertiesSupport).set(ROLLING_WORK, "0");
    }

    @Test
    void getFullExportChunkPerWorker_PropertyIsNotSet_ReturnsZero() {
        assertEquals(0, service.getFullExportChunkPerWorker());
    }

    @Test
    void getFullExportChunkPerWorker_PropertyDefined_ReturnsValue() {
        int chunkSize = randomPositiveInt();
        when(ppcPropertiesSupport.get(CHUNK_PER_WORKER)).thenReturn(String.valueOf(chunkSize));

        assertEquals(chunkSize, service.getFullExportChunkPerWorker());
    }

    @Test
    void getFullExportChunkPerWorker_PropertySetToString_ThrowsException() {
        when(ppcPropertiesSupport.get(CHUNK_PER_WORKER)).thenReturn(RandomStringUtils.random(10));

        assertThrows(PpcPropertyParseException.class, service::getFullExportChunkPerWorker);
    }

    @Test
    void setFullExportChunkPerWorker_Value_WritesValue() {
        int chunkSize = randomPositiveInt();
        service.setFullExportChunkPerWorker(chunkSize);

        verify(ppcPropertiesSupport).set(CHUNK_PER_WORKER, String.valueOf(chunkSize));
    }

    @Test
    void getFullExportMaximumCampaignsInQueue_PropertyIsNotSet_ReturnsZero() {
        assertEquals(0, service.getFullExportMaximumCampaignsInQueue());
    }

    @Test
    void getFullExportMaximumCampaignsInQueue_PropertyDefined_ReturnsValue() {
        int chunkSize = randomPositiveInt();
        when(ppcPropertiesSupport.get(MAX_LB_CAMPS)).thenReturn(String.valueOf(chunkSize));

        assertEquals(chunkSize, service.getFullExportMaximumCampaignsInQueue());
    }

    @Test
    void getFullExportMaximumCampaignsInQueue_PropertySetToString_ThrowsException() {
        when(ppcPropertiesSupport.get(MAX_LB_CAMPS)).thenReturn(RandomStringUtils.random(10));

        assertThrows(PpcPropertyParseException.class, service::getFullExportMaximumCampaignsInQueue);
    }

    @Test
    void setFullExportMaximumCampaignsInQueue_Value_WritesValue() {
        int chunkSize = randomPositiveInt();
        service.setFullExportMaximumCampaignsInQueue(chunkSize);

        verify(ppcPropertiesSupport).set(MAX_LB_CAMPS, String.valueOf(chunkSize));
    }

    @Test
    void getFullExportMaximumChunkPerIteration_PropertyIsNotSet_ReturnsZero() {
        assertEquals(0, service.getFullExportMaximumChunkPerIteration());
    }

    @Test
    void getFullExportMaximumChunkPerIteration_PropertyDefined_ReturnsValue() {
        int chunkSize = randomPositiveInt();
        when(ppcPropertiesSupport.get(CHUNK_PER_ITERATION)).thenReturn(String.valueOf(chunkSize));

        assertEquals(chunkSize, service.getFullExportMaximumChunkPerIteration());
    }

    @Test
    void getFullExportMaximumChunkPerIteration_PropertySetToString_ThrowsException() {
        when(ppcPropertiesSupport.get(CHUNK_PER_ITERATION)).thenReturn(RandomStringUtils.random(10));

        assertThrows(PpcPropertyParseException.class, service::getFullExportMaximumChunkPerIteration);
    }

    @Test
    void setFullExportMaximumChunkPerIteration_Value_WritesValue() {
        int chunkSize = randomPositiveInt();
        service.setFullExportMaximumChunkPerIteration(chunkSize);

        verify(ppcPropertiesSupport).set(CHUNK_PER_ITERATION, String.valueOf(chunkSize));
    }


    private int randomPositiveInt() {
        return RandomUtils.nextInt(1, Integer.MAX_VALUE);
    }
}
