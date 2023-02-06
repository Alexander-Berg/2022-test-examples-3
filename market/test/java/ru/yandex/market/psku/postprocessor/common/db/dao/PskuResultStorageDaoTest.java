package ru.yandex.market.psku.postprocessor.common.db.dao;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.GenerationTaskType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PairState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ProcessingResult;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PairStorage;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.msku_creation.PskuResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PskuResultStorageDaoTest extends BaseDBTest {
    private static final Long CATEGORY_ID = 12345L;

    @Autowired
    PskuResultStorageDao pskuResultStorageDao;

    @Test
    public void upsertInsertsEntries() {
        PairStorage pair = createPair(1L, 1L, PairState.RECEIVED);
        pskuResultStorageDao.updateFromPairStorage(Collections.singletonList(pair));

        List<PskuResultStorage> allEntries = pskuResultStorageDao.findAll();
        Assertions.assertThat(allEntries)
            .extracting(PskuResultStorage::getState)
            .containsExactly(PskuStorageState.FOR_REMAPPING);
    }

    @Test
    public void upsertDeletedNotExistsValidation() {
        PairStorage pair = createPair(1L, 1L, PairState.ALREADY_DELETED);
        pair.setValidationResult(Markup.PartnerMappingValidationTaskResult.newBuilder()
                .setDeleted(true)
                .build());
        pskuResultStorageDao.updateFromPairStorage(Collections.singletonList(pair));

        List<PskuResultStorage> allEntries = pskuResultStorageDao.findAll();
        Assertions.assertThat(allEntries)
                .extracting(PskuResultStorage::getState)
                .containsExactly(PskuStorageState.PSKU_DELETED);
    }

    @Test
    public void upsertDeletedNotExistsClusterization() {
        PskuResponse response =
                createPskuGenerationResponse(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.UNDEFINED,
                        "1", "1", true);
        pskuResultStorageDao.updateFromGenerationResponse(Collections.singletonList(response),
            GenerationTaskType.CLUSTER);

        List<PskuResultStorage> allEntries = pskuResultStorageDao.findAll();
        Assertions.assertThat(allEntries)
                .extracting(PskuResultStorage::getState)
                .containsExactly(PskuStorageState.PSKU_DELETED);
    }

    @Test
    public void upsertDeletedAlreadyExistsValidation() {
        PskuResponse response =
                createPskuGenerationResponse(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.MAPPED,
                        "1", "1", false);
        pskuResultStorageDao.updateFromGenerationResponse(Collections.singletonList(response),
            GenerationTaskType.CLUSTER);

        PairStorage pair = createPair(1L, 1L, PairState.ALREADY_DELETED);
        pair.setValidationResult(Markup.PartnerMappingValidationTaskResult.newBuilder().setDeleted(true).build());
        pskuResultStorageDao.updateFromPairStorage(Collections.singletonList(pair));

        List<PskuResultStorage> allEntries = pskuResultStorageDao.findAll();
        Assertions.assertThat(allEntries)
                .extracting(PskuResultStorage::getState)
                .containsExactly(PskuStorageState.FOR_REMAPPING);
        Assertions.assertThat(allEntries)
                .extracting(PskuResultStorage::getMappingCheckerProcessingResult)
                .containsExactly(ProcessingResult.ALREADY_DELETED);
    }

    @Test
    public void upsertDeletedAlreadyExistsClusterization() {
        PairStorage pair = createPair(1L, 1L, PairState.RECEIVED);
        pair.setValidationResult(Markup.PartnerMappingValidationTaskResult.newBuilder().setDeleted(false).build());
        pskuResultStorageDao.updateFromPairStorage(Collections.singletonList(pair));

        PskuResponse response =
                createPskuGenerationResponse(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.UNDEFINED,
                        "1", "1", true);
        pskuResultStorageDao.updateFromGenerationResponse(Collections.singletonList(response),
            GenerationTaskType.CLUSTER);

        List<PskuResultStorage> allEntries = pskuResultStorageDao.findAll();
        Assertions.assertThat(allEntries)
                .extracting(PskuResultStorage::getState)
                .containsExactly(PskuStorageState.FOR_REMAPPING);
        Assertions.assertThat(allEntries)
                .extracting(PskuResultStorage::getClusterizerProcessingResult)
                .containsExactly(ProcessingResult.ALREADY_DELETED);
    }

    @Test
    public void upsertDoesNotOverrideMskuId() {
        PskuResponse response =
            createPskuGenerationResponse(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.TRASH, "1", "1");
        PairStorage secondPair = createPair(1L, 2L, PairState.RECEIVED);

        pskuResultStorageDao.updateFromGenerationResponse(Collections.singletonList(response),
            GenerationTaskType.CLUSTER);
        pskuResultStorageDao.updateFromPairStorage(Collections.singletonList(secondPair));

        List<PskuResultStorage> allEntries = pskuResultStorageDao.findAll();
        Assertions.assertThat(allEntries)
            .extracting(PskuResultStorage::getMskuMappedId)
            .containsExactly(response.getMskuId());
    }

    @Test
    public void upsertSetsMskuIdWhenItWasNull() {
        PskuResponse response =
            createPskuGenerationResponse(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.TRASH, "1", null);
        PairStorage secondPair = createPair(1L, 2L, PairState.RECEIVED);

        pskuResultStorageDao.updateFromGenerationResponse(Collections.singletonList(response),
            GenerationTaskType.CLUSTER);
        pskuResultStorageDao.updateFromPairStorage(Collections.singletonList(secondPair));

        List<PskuResultStorage> allEntries = pskuResultStorageDao.findAll();
        Assertions.assertThat(allEntries).hasSize(1);

        PskuResultStorage resultEntry = allEntries.get(0);
        Assertions.assertThat(resultEntry.getMskuMappedId()).isEqualTo(2L);
    }

    @Test
    public void upsertUpsertsCorrectly() {
        PskuResponse response =
            createPskuGenerationResponse(Markup.MskuFromPskuGenerationTaskResult.MappingStatus.TRASH, "1", null);

        PairStorage updatePair = createPair(1L, 2L, PairState.RECEIVED);
        PairStorage pairToBeInserted = createPair(2L, 3L, PairState.RECEIVED);

        pskuResultStorageDao.updateFromGenerationResponse(Collections.singletonList(response),
            GenerationTaskType.CLUSTER);
        pskuResultStorageDao.updateFromPairStorage(Arrays.asList(updatePair, pairToBeInserted));

        List<PskuResultStorage> allEntries = pskuResultStorageDao.findAll();
        Assertions.assertThat(allEntries).hasSize(2);

        Assertions.assertThat(allEntries).extracting(
            PskuResultStorage::getPskuId,
            PskuResultStorage::getMskuMappedId,
            PskuResultStorage::getClusterizerProcessingResult,
            PskuResultStorage::getMappingCheckerProcessingResult
        ).containsExactlyInAnyOrder(
            new Tuple(1L, 2L, ProcessingResult.NEED_INFO, ProcessingResult.PENDING_MAPPING),
            new Tuple(2L, 3L, null, ProcessingResult.PENDING_MAPPING));
    }

    @Test
    public void updateFromGenerationResponseWithCorrectProcessingField() {
        PskuResponse pskuResponse1 = createPskuGenerationResponse(
            Markup.MskuFromPskuGenerationTaskResult.MappingStatus.WRONG_CATEGORY, "12", "31");
        PskuResponse pskuResponse2 = createPskuGenerationResponse(
            Markup.MskuFromPskuGenerationTaskResult.MappingStatus.WRONG_CATEGORY, "13", "31");

        pskuResultStorageDao.updateFromGenerationResponse(Arrays.asList(pskuResponse1), GenerationTaskType.CLUSTER);
        pskuResultStorageDao.updateFromGenerationResponse(Arrays.asList(pskuResponse2), GenerationTaskType.TO_MODEL);


        List<PskuResultStorage> results = pskuResultStorageDao.findAll();
        Assertions.assertThat(results).extracting(PskuResultStorage::getPskuId,
            PskuResultStorage::getPskuToModelProcessingResult,
            PskuResultStorage::getClusterizerProcessingResult)
            .containsExactlyInAnyOrder(new Tuple(12L, null, ProcessingResult.WRONG_CATEGORY),
                new Tuple (13L, ProcessingResult.WRONG_CATEGORY, null));
    }

    private PairStorage createPair(Long pskuId, Long mskuId, PairState state) {
        PairStorage pair = new PairStorage();
        pair.setPskuId(pskuId);
        pair.setMskuId(mskuId);
        pair.setPskuCategoryId(CATEGORY_ID);
        pair.setValidationResult(Markup.PartnerMappingValidationTaskResult.newBuilder().build());
        pair.setState(state);
        return pair;
    }

    private PskuResponse createPskuGenerationResponse(
        Markup.MskuFromPskuGenerationTaskResult.MappingStatus mappingStatus, String pskuId,  String mskuId,
        Boolean deleted
        ) {
        Markup.MskuFromPskuGenerationTaskResult.Builder result = Markup.MskuFromPskuGenerationTaskResult.newBuilder()
                .setMappingStatus(mappingStatus)
                .setPskuId(pskuId);
        if (deleted != null) {
            result.setDeleted(deleted);
        }
        if (mskuId != null) {
            result.setMskuId(mskuId);
        }

        return new PskuResponse(result.build(), CATEGORY_ID, null, GenerationTaskType.CLUSTER);
    }

    private PskuResponse createPskuGenerationResponse(
            Markup.MskuFromPskuGenerationTaskResult.MappingStatus mappingStatus, String pskuId,  String mskuId
    ) {
        return createPskuGenerationResponse(mappingStatus, pskuId, mskuId, null);
    }
}
