package ru.yandex.market.psku.postprocessor.common.db.dao;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuToModelState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuToModelBatch;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuToModelInBatchInfo;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

public class PskuToModelInBatchInfoDaoTest extends BaseDBTest {
    @Autowired
    PskuToModelInBatchInfoDao pskuToModelInBatchInfoDao;
    @Autowired
    PskuToModelBatchDao pskuToModelBatchDao;

    @Test
    public void getsPskuInProcessCountByCategory() {
        pskuToModelBatchDao.insert(new PskuToModelBatch(1L, Timestamp.from(Instant.now()), 1L, 1, ""));
        pskuToModelBatchDao.insert(new PskuToModelBatch(2L, Timestamp.from(Instant.now()), 1L, 2, ""));
        pskuToModelBatchDao.insert(new PskuToModelBatch(3L, Timestamp.from(Instant.now()), 2L, 3, ""));
        pskuToModelBatchDao.insert(new PskuToModelBatch(4L, Timestamp.from(Instant.now()), 2L, 4, ""));
        pskuToModelBatchDao.insert(new PskuToModelBatch(5L, Timestamp.from(Instant.now()), 2L, 5, ""));

        pskuToModelInBatchInfoDao.insert(
            createBasicPskuToModelInBatchInfo(1L, 1L, 1L, PskuToModelState.NEW),
            createBasicPskuToModelInBatchInfo(2L, 1L, 2L, PskuToModelState.IN_PROCESS),
            createBasicPskuToModelInBatchInfo(3L, 2L, 3L, PskuToModelState.IN_PROCESS),
            createBasicPskuToModelInBatchInfo(4L, 2L, 4L, PskuToModelState.IN_PROCESS),
            createBasicPskuToModelInBatchInfo(5L, 2L, 4L, PskuToModelState.PROCESSED),
            createBasicPskuToModelInBatchInfo(6L, 2L, 5L, PskuToModelState.PROCESSED)
        );

        Map<Long, Integer> categoryCount = pskuToModelInBatchInfoDao.getInProcessBatchCountByCategory();
        Assertions.assertThat(categoryCount.get(1L)).isEqualTo(1);
        Assertions.assertThat(categoryCount.get(2L)).isEqualTo(2);
    }

    private PskuToModelInBatchInfo createBasicPskuToModelInBatchInfo(long id, long categoryId, long batchId, PskuToModelState state) {
        return new PskuToModelInBatchInfo(id, 1L, categoryId, batchId, state, 1, 1L, null, null, null,
            Markup.MskuFromPskuGenerationTaskResult.getDefaultInstance());
    }
}
