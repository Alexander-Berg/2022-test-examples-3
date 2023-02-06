package ru.yandex.market.psku.postprocessor.common.db.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ProcessingResult;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorageHistory;
import ru.yandex.market.psku.postprocessor.msku_creation.PskuIdentity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

public class PskuResultStorageHistoryDaoTest extends BaseDBTest {
    private static final Long CATEGORY_ID = 123L;
    private static final PskuStorageState CREATED_STATE = PskuStorageState.WRONG_CATEGORY;
    private static final Long FAKE_PMODEL_ID = 1234L;

    @Autowired
    PskuResultStorageHistoryDao historyDao;
    @Autowired
    PskuResultStorageDao pskuResultStorageDao;

    @Test
    public void shouldRotateOnlyPassedIds() {
        Set<PskuIdentity> allIds = createPskuIdentitiesAndStore();
        Set<PskuIdentity> idsForRotate = allIds.stream()
            .filter(psku -> psku.getPskuId() % 2 == 0)
            .collect(Collectors.toSet());

        historyDao.rotate(idsForRotate, PskuStorageState.WRONG_CATEGORY);

        assertThat(historyDao.findAll())
            .extracting(PskuResultStorageHistory::getPskuId)
            .containsExactlyInAnyOrder(idsForRotate.stream()
                .map(PskuIdentity::getPskuId)
                .toArray(Long[]::new)
            );
        assertThat(pskuResultStorageDao.findAll())
            .extracting(PskuResultStorage::getPskuId)
            .containsExactlyInAnyOrder(allIds.stream()
                .filter(psku -> !idsForRotate.contains(psku))
                .map(PskuIdentity::getPskuId)
                .toArray(Long[]::new)
            );
    }

    @Test
    public void whenRotateShouldSaveAllInformation() {
        Set<PskuIdentity> allIds = createPskuIdentitiesAndStore();
        List<PskuResultStorage> old = pskuResultStorageDao.findAll();

        historyDao.rotate(allIds, PskuStorageState.WRONG_CATEGORY);

        List<PskuResultStorageHistory> saved = historyDao.findAll();

        assertThat(saved.toArray())
            .usingElementComparatorIgnoringFields("modelToHistoryTs", "id")
            .containsExactlyInAnyOrder(old.toArray());
        assertThat(pskuResultStorageDao.findAll())
            .hasSize(0);
    }

    @Test
    public void shouldNotRotatePskuInOtherStates() {
        Set<PskuIdentity> allIds = createPskuIdentitiesAndStore();
        historyDao.rotate(allIds, PskuStorageState.PMODEL_DELETED);
        assertThat(historyDao.findAll()).hasSize(0);
    }

    private Set<PskuIdentity> createPskuIdentitiesAndStore() {
        Set<PskuIdentity> allIds = LongStream.iterate(1, l -> l * 7)
            .mapToObj(SimplePsku::new)
            .limit(15)
            .collect(Collectors.toSet());
        allIds.forEach(this::createPskuResultStorage);
        assertThat(pskuResultStorageDao.findAll())
            .allSatisfy(s -> assertThat(s).hasNoNullFieldsOrProperties());
        return allIds;
    }

    private Long createPskuResultStorage(PskuIdentity simplePsku) {
        final PskuResultStorage psku = new PskuResultStorage();
        psku.setPskuId(simplePsku.getPskuId());
        psku.setMskuMappedId(simplePsku.getPskuId()*2);
        psku.setCategoryId(CATEGORY_ID);
        psku.setState(CREATED_STATE);
        psku.setCreateTime(Timestamp.from(Instant.now()));
        psku.setClusterizerProcessingResult(ProcessingResult.WRONG_CATEGORY);
        psku.setMappingCheckerProcessingResult(ProcessingResult.WRONG_CATEGORY);
        psku.setPskuToModelProcessingResult(ProcessingResult.WRONG_CATEGORY);
        psku.setErrorKinds("some errors for not null check");
        psku.setPmodelIdForDelete(FAKE_PMODEL_ID);
        psku.setUserLogin("a user for not null check");
        pskuResultStorageDao.insert(psku);
        return psku.getId();
    }
}