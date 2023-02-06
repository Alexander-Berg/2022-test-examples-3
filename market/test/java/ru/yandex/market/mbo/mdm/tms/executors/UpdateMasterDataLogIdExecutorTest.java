package ru.yandex.market.mbo.mdm.tms.executors;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.repository.MasterDataLogIdService;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;
import ru.yandex.market.mboc.common.masterdata.model.MasterData;
import ru.yandex.market.mboc.common.masterdata.repository.MasterDataRepository;

/**
 * @author amaslak
 */
public class UpdateMasterDataLogIdExecutorTest extends MdmBaseDbTestClass {

    private static final long SEED = 2512412;

    @Autowired
    private MasterDataRepository masterDataRepository;

    @Autowired
    private MasterDataLogIdService masterDataLogIdService;

    private UpdateMasterDataLogIdExecutor executor;

    private EnhancedRandom enhancedRandom;

    @Before
    public void setUp() throws Exception {
        executor = new UpdateMasterDataLogIdExecutor(masterDataLogIdService);
        enhancedRandom = TestDataUtils.defaultRandom(SEED);
    }

    @Test
    public void whenNoRecordsShouldPass() {
        Assertions.assertThat(masterDataLogIdService.getNewModifiedSequenceCount()).isEqualTo(0L);
        executor.execute();
        Assertions.assertThat(masterDataLogIdService.getNewModifiedSequenceCount()).isEqualTo(0L);
    }

    @Test
    public void whenHasNewRecordsShouldUpdateAll() {
        int count = 432;
        List<MasterData> masterData = TestDataUtils.generateSskuMsterData(count, enhancedRandom);

        masterDataRepository.insertBatch(masterData);
        Assertions.assertThat(masterDataLogIdService.getNewModifiedSequenceCount()).isEqualTo(count);

        executor.execute();
        Assertions.assertThat(masterDataLogIdService.getNewModifiedSequenceCount()).isEqualTo(0L);
    }
}
