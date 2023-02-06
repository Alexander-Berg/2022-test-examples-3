package ru.yandex.market.psku.postprocessor.service.deleter;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.util.ModelBuilder;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.dao.PskuResultStorageDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.TransitionDao;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.ModelType;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.PskuStorageState;
import ru.yandex.market.psku.postprocessor.common.db.jooq.enums.TransitionSource;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.PskuResultStorage;
import ru.yandex.market.psku.postprocessor.common.db.jooq.tables.pojos.Transition;
import ru.yandex.market.psku.postprocessor.service.ModelKey;
import ru.yandex.market.psku.postprocessor.service.ModelWithSkuMappings;
import ru.yandex.market.psku.postprocessor.service.SkuToModelMapping;
import ru.yandex.market.psku.postprocessor.service.yt.YtDataService;
import ru.yandex.market.psku.postprocessor.service.yt.session.SessionParam;
import ru.yandex.market.psku.postprocessor.service.yt.session.YtPModelDeleterSessionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static ru.yandex.market.psku.postprocessor.common.db.jooq.Tables.TRANSITION;

public class RemappedPModelDeleterServiceTest extends BaseDBTest {
    private static final int HID = 91491;
    private static final int SUPPLIER_ID = 91491;
    private static final long EXISTING_PSKU_ID1 = 101L;
    private static final long EXISTING_PSKU_ID2 = 102L;
    private static final long EXISTING_PSKU_ID3 = 103L;
    private static final long EXISTING_PMODEL_ID1 = 100501L;
    private static final long EXISTING_MSKU_ID1 = 1001L;
    private static final long EXISTING_MSKU_ID2 = 1002L;
    private static final long EXISTING_MSKU_ID3 = 1003L;
    private static final long EXISTING_MSKU_ID4 = 1004L;
    private static final long EXISTING_GURU_ID1 = 1000501L;
    private static final long EXISTING_GURU_ID2 = 1000502L;
    private static final long EXISTING_GURU_ID3 = 1000503L;
    private static final String EXISTING_PSKU_SHOP_SKU2 = "EXISTING_PSKU_SHOP_SKU2";
    private static final String MBO_SESSION_NAME = "mbo_test_session";
    private static final String SESSION_NAME = "test_session";

    @Autowired
    private PskuResultStorageDao pskuResultStorageDao;
    @Autowired
    private TransitionDao transitionDao;
    private RemappedPModelDeleterService remappedPModelDeleterService;
    private ModelStorageHelper modelStorageHelper;
    @Mock
    private YtPModelDeleterSessionService ytPModelDeleterSessionService;
    @Mock
    private YtDataService ytDataService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(ytDataService.getRecentExportCreationDate())
                .thenReturn(MBO_SESSION_NAME);
        when(ytPModelDeleterSessionService.startNewPModelDeleterSession())
                .thenReturn(new SessionParam(null, SESSION_NAME));

        ModelStorageServiceMock modelStorageServiceMock = new ModelStorageServiceMock();
        modelStorageHelper = new ModelStorageHelper(modelStorageServiceMock, modelStorageServiceMock);
        remappedPModelDeleterService = new RemappedPModelDeleterService(
                ytPModelDeleterSessionService,
                transitionDao,
                modelStorageHelper,
                pskuResultStorageDao,
                ytDataService
        );

        modelStorageServiceMock.putModels(
                ModelBuilder.newBuilder(EXISTING_PSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER_SKU)
                        .withSkuParentRelation(HID, EXISTING_PMODEL_ID1)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_PSKU_ID2, HID)
                        .currentType(ModelStorage.ModelType.PARTNER_SKU)
                        .withSkuParentRelation(HID, EXISTING_PMODEL_ID1)
                        .supplierId(SUPPLIER_ID)
                        .shopSku(EXISTING_PSKU_SHOP_SKU2)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_PSKU_ID3, HID)
                        .currentType(ModelStorage.ModelType.PARTNER_SKU)
                        .withSkuParentRelation(HID, EXISTING_PMODEL_ID1)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_PMODEL_ID1, HID)
                        .currentType(ModelStorage.ModelType.PARTNER)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_MSKU_ID1, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .withSkuParentRelation(HID, EXISTING_GURU_ID1)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_GURU_ID1, HID)
                        .currentType(ModelStorage.ModelType.GURU)
                        .withSkuRelations(HID, EXISTING_MSKU_ID1)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_MSKU_ID2, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .withSkuParentRelation(HID, EXISTING_GURU_ID2)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_MSKU_ID3, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .withSkuParentRelation(HID, EXISTING_GURU_ID2)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_MSKU_ID4, HID)
                        .currentType(ModelStorage.ModelType.SKU)
                        .withSkuParentRelation(HID, EXISTING_GURU_ID3)
                        .build(),
                ModelBuilder.newBuilder(EXISTING_GURU_ID2, HID)
                        .currentType(ModelStorage.ModelType.GURU)
                        .withSkuRelations(HID, EXISTING_MSKU_ID2, EXISTING_MSKU_ID3)
                        .build()
        );
    }

    @Test
    public void whenOneGuruHasMoreMSkuThenChooseThisGuru() {
        /*
         * --PMODEL1
         *      PSKU1 -> MSKU2 - GURU2  *
         *      PSKU2 -> MSKU3 - GURU2  *
         *      PSKU3 -> MSKU1 - GURU1
         *
         *  => choose GURU 2
         */
        createPskuResultStorage(EXISTING_PSKU_ID1);
        createPskuResultStorage(EXISTING_PSKU_ID2);
        createPskuResultStorage(EXISTING_PSKU_ID3);

        mockYtDataService(
                new SkuToModelMapping(new ModelKey(EXISTING_PSKU_ID1, null), new ModelKey(EXISTING_GURU_ID2, null)),
                new SkuToModelMapping(new ModelKey(EXISTING_PSKU_ID2, null), new ModelKey(EXISTING_GURU_ID2, null)),
                new SkuToModelMapping(new ModelKey(EXISTING_PSKU_ID3, null), new ModelKey(EXISTING_GURU_ID1, null))
        );

        remappedPModelDeleterService.doDelete();

        assertTransition(EXISTING_PMODEL_ID1, EXISTING_GURU_ID2, ModelType.MODEL);
        assertThat(pskuResultStorageDao.findAll())
            .extracting(PskuResultStorage::getState)
            .containsOnly(PskuStorageState.PMODEL_DELETED);

        Optional<ModelStorage.Model> pModelId = modelStorageHelper.findModel(EXISTING_PMODEL_ID1, true);
        assertThat(pModelId).isPresent().get().extracting(ModelStorage.Model::getDeleted).isEqualTo(true);
    }

    @Test
    public void whenOneMskuIsMissingDoNotDeletePModel() {
        /*
         * --PMODEL1
         *      PSKU1 -> MSKU2 - GURU2
         *      PSKU2 -> NON_EXISTING_MSKU_ID
         *      PSKU3 -> MSKU1 - GURU1
         *
         *  => do not delete model
         */
        createPskuResultStorage(EXISTING_PSKU_ID1);
        createPskuResultStorage(EXISTING_PSKU_ID2);
        createPskuResultStorage(EXISTING_PSKU_ID3);

        mockYtDataService(
                new SkuToModelMapping(new ModelKey(EXISTING_PSKU_ID1, null), new ModelKey(EXISTING_GURU_ID2, null)),
                new SkuToModelMapping(new ModelKey(EXISTING_PSKU_ID2, null), null),
                new SkuToModelMapping(new ModelKey(EXISTING_PSKU_ID3, null), new ModelKey(EXISTING_GURU_ID1, null))
        );

        remappedPModelDeleterService.doDelete();

        assertNoTransition(EXISTING_PMODEL_ID1);
        assertThat(pskuResultStorageDao.findAll())
            .extracting(PskuResultStorage::getState)
            .containsOnly(PskuStorageState.PSKU_DELETED);

        Optional<ModelStorage.Model> pModelId = modelStorageHelper.findModel(EXISTING_PMODEL_ID1);
        assertThat(pModelId).isPresent().get().extracting(ModelStorage.Model::getDeleted).isEqualTo(false);
    }

    @Test
    public void whenGurusHasSameMSkuCountThenChooseGuruByMinId() {
        /*
         * --PMODEL1
         *      PSKU1 -> MSKU1 - GURU1  *
         *      PSKU2 -> MSKU4 - GURU3
         *
         *  => choose GURU 1
         */
        createPskuResultStorage(EXISTING_PSKU_ID1);
        createPskuResultStorage(EXISTING_PSKU_ID2);

        mockYtDataService(
                new SkuToModelMapping(new ModelKey(EXISTING_PSKU_ID1, null), new ModelKey(EXISTING_GURU_ID1, null)),
                new SkuToModelMapping(new ModelKey(EXISTING_PSKU_ID2, null), new ModelKey(EXISTING_GURU_ID3, null))
        );

        remappedPModelDeleterService.doDelete();

        assertTransition(EXISTING_PMODEL_ID1, EXISTING_GURU_ID1, ModelType.MODEL);
        assertThat(pskuResultStorageDao.findAll())
                .extracting(PskuResultStorage::getState)
                .containsOnly(PskuStorageState.PMODEL_DELETED);
        Optional<ModelStorage.Model> pModelId = modelStorageHelper.findModel(EXISTING_PMODEL_ID1, true);
        assertThat(pModelId).isPresent().get().extracting(ModelStorage.Model::getDeleted).isEqualTo(true);
    }

    private void mockYtDataService(SkuToModelMapping... skuToModelMappings) {
        Mockito.doAnswer(invocation -> {
            Consumer<ModelWithSkuMappings> consumer = invocation.getArgument(1);
            ModelWithSkuMappings modelWithSkuMappings = new ModelWithSkuMappings(
                    new ModelKey(EXISTING_PMODEL_ID1, null),
                    Arrays.asList(skuToModelMappings)
            );
            consumer.accept(modelWithSkuMappings);
            return null;
        })
                .when(ytDataService).processPmodelsDataForDeletion(anyString(), any());
    }

    private void assertTransition(long oldId, long newId, ModelType modelType) {
        final Optional<Transition> existingPSkuTransitionOpt = transitionDao.fetchOptional(
                TRANSITION.OLD_ID,
                oldId);

        final Transition expectedTransition = new Transition();
        expectedTransition.setOldId(oldId);
        expectedTransition.setNewId(newId);
        expectedTransition.setType(modelType);
        expectedTransition.setIsRemoved(true);
        expectedTransition.setSource(TransitionSource.REMAPPING);

        assertThat(existingPSkuTransitionOpt).isPresent().get().isEqualToIgnoringGivenFields(expectedTransition, "id");
    }

    private void assertNoTransition(long oldId) {
        final Optional<Transition> pSkuTransitionOpt = transitionDao.fetchOptional(
                TRANSITION.OLD_ID,
                oldId);

        assertThat(pSkuTransitionOpt).isEmpty();
    }


    private Long createPskuResultStorage(long pskuId) {
        final PskuResultStorage psku = new PskuResultStorage();
        psku.setPskuId(pskuId);
        psku.setCategoryId((long) HID);
        psku.setState(PskuStorageState.PSKU_DELETED);
        psku.setCreateTime(Timestamp.from(Instant.now()));
        pskuResultStorageDao.insert(psku);
        return psku.getId();
    }

}
