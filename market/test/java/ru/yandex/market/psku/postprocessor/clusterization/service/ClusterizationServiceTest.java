package ru.yandex.market.psku.postprocessor.clusterization.service;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.psku.postprocessor.clusterization.PskuDao;
import ru.yandex.market.psku.postprocessor.clusterization.pojo.Psku;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuKnowledgeDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ProcessingResult;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuKnowledge;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
public class ClusterizationServiceTest extends BaseDBTest {
    @Mock
    PskuDao pskuDao;
    @Autowired
    PskuResultStorageDao pskuResultStorageDao;
    @Autowired
    PskuKnowledgeDao pskuKnowledgeDao;
    ClusterizationService clusterizationService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        clusterizationService = new ClusterizationService(
            Collections.emptyList(),
            pskuDao,
            pskuKnowledgeDao,
            pskuResultStorageDao,
            null,
            null,
            null,
            null,
            result -> result
        );
    }

    private Psku createPsku(long id) {
        return new Psku(10, 123, id, "Test title", Collections.emptyList(),
            Arrays.asList("123"), "", "Test vendor");
    }

    private PskuKnowledge createPskuKnowledge(long id) {
        return new PskuKnowledge(id, "", 1L, null, null, null, null, null, null, 1L);
    }

    @Test
    public void notEligiblePskusAreFiltered() {
        Psku testPsku1 = createPsku(1);
        Psku testPsku2 = createPsku(2);
        Psku testPsku3 = createPsku(3);
        Psku testPsku4 = createPsku(4);
        Mockito.when(pskuDao.getPskus()).thenReturn(Arrays.asList(testPsku1, testPsku2, testPsku3, testPsku4));
        pskuResultStorageDao.insert(createPskuResultStorage(1L, ProcessingResult.NEED_INFO));
        pskuKnowledgeDao.insert(new PskuKnowledge(1L, "", 1L, null, null, null, null, null, null, 1L));
        pskuKnowledgeDao.insert(new PskuKnowledge(2L, "", 1L, null, null, null, null, null, null, 1L));
        pskuKnowledgeDao.insert(new PskuKnowledge(3L, "", 1L, null, null, null, null, null, null, 1L));
        pskuKnowledgeDao.insert(new PskuKnowledge(4L, "", 1L, null, null, null, null, null, null, 2L));
        pskuKnowledgeDao.insert(new PskuKnowledge(5L, "", 1L, null, null, null, null, null, null, null));

        List<Psku> pskus = clusterizationService.getFilteredPskus();

        Assertions.assertThat(pskus).containsExactlyInAnyOrder(testPsku2, testPsku3);
    }

    private PskuResultStorage createPskuResultStorage(long l, ProcessingResult result) {
        PskuResultStorage pskuResultStorage = new PskuResultStorage();
        pskuResultStorage.setPskuId(l);
        pskuResultStorage.setCategoryId(10L);
        pskuResultStorage.setClusterizerProcessingResult(result);
        pskuResultStorage.setMappingCheckerProcessingResult(result);
        pskuResultStorage.setCreateTime(Timestamp.from(Instant.now()));
        pskuResultStorage.setMskuMappedId(13L);
        pskuResultStorage.setState(PskuStorageState.FOR_REMAPPING);
        return pskuResultStorage;
    }
}
