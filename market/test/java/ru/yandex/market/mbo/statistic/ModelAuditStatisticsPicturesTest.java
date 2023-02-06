package ru.yandex.market.mbo.statistic;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.market.mbo.core.audit.AuditServiceMock;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditContext;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditService;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditServiceImpl;
import ru.yandex.market.mbo.db.modelstorage.health.SaveStats;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;
import ru.yandex.market.mbo.gwt.models.audit.AuditActionBuilder;
import ru.yandex.market.mbo.gwt.models.audit.AuditFilter;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.PictureBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.statistic.model.SquashedUserActions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author york
 * @since 17.04.2020
 */
@SuppressWarnings("checkstyle:magicNumber")
public class ModelAuditStatisticsPicturesTest {
    private static final Logger log = LoggerFactory.getLogger(ModelAuditStatisticsPicturesTest.class);
    private static final long USER_ID = 1;
    private static final long INSPECTOR_USER_ID = 2;
    private static long timestampSeq = System.currentTimeMillis();

    private AuditServiceMock auditServiceMock;
    private ModelAuditService modelAuditService;
    private ModelAuditContext modelAuditContext;
    private ModelAuditStatisticsServiceImpl service;
    private int idSeq;

    @Before
    public void setUp() throws Exception {
        idSeq = 1;
        auditServiceMock = new AuditServiceMock();
        modelAuditService = new ModelAuditServiceImpl(auditServiceMock);
        modelAuditContext = Mockito.mock(ModelAuditContext.class);
        Mockito.when(modelAuditContext.getSource()).thenReturn(AuditAction.Source.YANG_TASK);
        Mockito.when(modelAuditContext.getSourceId()).thenReturn("12345");
        Mockito.when(modelAuditContext.isBilledOperation()).thenReturn(true);
        Mockito.when(modelAuditContext.getStats()).thenReturn(new SaveStats());
        service = new ModelAuditStatisticsServiceImpl(auditServiceMock, null, null, null);
    }

    @Test
    public void testValuesDiffGuru() {
        new PicturesManager(guru())
            .uploadPic(PictureId.xsl("XL-Picture"), "url1")
            .updateState()
            .uploadPic(PictureId.xsl("XL-Picture"), "url2") //2 actions
            .uploadPicToEnd("url3") // 1 action
            .save()
            .removePic(PictureId.xsl("XL-Picture_1")) //2 actions
            .save();

        List<AuditAction> actions = getAudit();
        assertThat(actions).hasSize(5);
        SquashingService.ValuesDiff picturesDiff = new SquashingService.ValuesDiff(actions);
        assertThat(picturesDiff.getAdditionsByValue().keySet()).containsExactlyInAnyOrder("url2", "url3");
        assertThat(picturesDiff.getRemovesByValue().keySet()).containsExactly("url1");
    }

    @Test
    public void testValuesDiffGuru2() {
        new PicturesManager(guru())
            .uploadPicToEnd("url1") // 1 action
            .save()
            .uploadPicToEnd("url2") // 1 action
            .save()
            .removePic(PictureId.index(0)) //2 actions
            .save()
            .removePic(PictureId.index(0)) //1 action
            .save();

        List<AuditAction> actions = getAudit();
        assertThat(actions).hasSize(5);
        SquashingService.ValuesDiff picturesDiff = new SquashingService.ValuesDiff(actions);
        assertThat(picturesDiff.isEmpty());
    }

    @Test
    public void testValuesDiffSku() {
        new PicturesManager(sku())
            .uploadPicToEnd("url1")
            .uploadPicToEnd("url2")
            .uploadPicToEnd("url3")
            .updateState()
            .swapPictures(PictureId.index(0), PictureId.index(2)) // 2 action
            .save()
            .swapPictures(PictureId.index(0), PictureId.index(1)) // 2 action
            .save()
            .swapPictures(PictureId.index(1), PictureId.index(2)) // 2 action
            .save();
        List<AuditAction> actions = getAudit();
        assertThat(actions).hasSize(6);
        SquashingService.ValuesDiff picturesDiff = new SquashingService.ValuesDiff(actions);
        assertThat(picturesDiff.isEmpty());
    }

    @Test
    public void testUploadedPictures() {
        PicturesManager picManager = new PicturesManager(guru(), sku())
            .withModel(1)
            .uploadPicToEnd("url1")
            .uploadPicToEnd("url2")
            .withModel(2)
            .uploadPicToEnd("url3")
            .updateState() // u1 u2 | u3
            .withModel(1)
            .uploadPicToEnd("url4")
            .copyPicture(PictureId.xsl("XL-Picture"), 2, PictureId.index(1))
            .save() // u1 u2 u4 | u3 u1
            .swapPictures(PictureId.xsl("XL-Picture"), PictureId.xsl("XL-Picture_2"))
            .save() // u4 u2 u1 | u3 u1
            .removePic(PictureId.xsl("XL-Picture_2"))
            .movePicture(PictureId.xsl("XL-Picture"), 2, PictureId.index(2))
            .save(); // u2 | u3 u1 u4
        SquashedUserActions squashedUserActions = SquashingService.squashedUserActions(
            MboParameters.Category.getDefaultInstance(), getAudit(), (x) -> picManager.modelMap);
        assertThat(squashedUserActions.getUploadedUrls()).containsExactly("url4");
    }

    @Test
    public void testMoveNotBilled() {
        PicturesManager picturesManager = new PicturesManager(guru(), sku(), sku(), sku(), sku())
            .withModel(1).uploadPicToEnd("url1")
            .withModel(2).uploadPicToEnd("url1")
            .updateState()
            .withModel(1)
            .movePicture(PictureId.xsl("XL-Picture"), 3, PictureId.index(0))
            .withModel(2)
            .copyPicture(PictureId.index(0), 4, PictureId.index(0))
            .movePicture(PictureId.index(0), 5, PictureId.index(0))
            .save();
        // was 2 pictures, now 3 pictures in diff models => billing 1 copy
        applyAndCheck(picturesManager, new CheckingState()
            .setContractorCopy("url1")
        );
    }

    @Test
    public void testBillingNoInspectorComplex() {
        PicturesManager picturesManager = new PicturesManager(guru(), sku(), sku())
            .withModel(1).uploadPicToEnd("url1")
            .withModel(2).uploadPicToEnd("url2")
            .updateState()
            .withModel(1)
            .copyPicture(PictureId.xsl("XL-Picture"), 2, PictureId.index(1))
            .copyPicture(PictureId.xsl("XL-Picture"), 3, PictureId.index(0))
            .save() // u1 | u2 u1 | u1
            .withModel(2)
            .swapPictures(PictureId.index(0), PictureId.index(1))
            .save() // u1 | u1 u2 | u1
            .movePicture(PictureId.index(1), 3, PictureId.index(1))
            .save() // u1 | u1 | u1 u2
            .uploadPic(PictureId.index(0), "url3")
            .save() // u1 | u3 u1 | u1 u2
            .copyPicture(PictureId.index(0), 1, PictureId.xsl("XL-Picture_1"))
            .save(); // u1 u3 | u3 u1 | u1 u2

        applyAndCheck(picturesManager, new CheckingState()
                .setContractorUpload("url3")
                .setContractorCopy("url1", "url1", "url3")
        );
    }

    @Test
    public void testBillingInspectorRemoveNotBilled() {
        PicturesManager picturesManager = new PicturesManager(guru())
            .uploadPicToEnd("url1")
            .uploadPicToEnd("url2")
            .save()
            .startInspection()
            .removePic(PictureId.xsl("XL-Picture"))
            .save();
        applyAndCheck(picturesManager, new CheckingState()
                .setContractorUpload("url2"));
    }

    @Test
    public void testBillingInspectorRemoveAndUploadBilled() {
        PicturesManager picturesManager = new PicturesManager(guru())
            .uploadPicToEnd("url1")
            .uploadPicToEnd("url2")
            .save()
            .startInspection()
            .removePic(PictureId.xsl("XL-Picture"))
            .uploadPic(PictureId.xsl("XL-Picture"), "url4")
            .save();
        applyAndCheck(picturesManager, new CheckingState()
            .setContractorUpload("url2")
            .setCorrectionsUpload("url4")
        );
    }

    @Test
    public void testBillingInspectorRemoveAndUploadBilled2() {
        PicturesManager picturesManager = new PicturesManager(guru())
            .uploadPicToEnd("url1")
            .save()
            .uploadPicToEnd("url2")
            .uploadPicToEnd("url3")
            .save()
            .uploadPicToEnd("url4")
            .save() // u1 u2 u3 u4
            .startInspection()
            .removePic(PictureId.xsl("XL-Picture"))
            .removePic(PictureId.xsl("XL-Picture"))
            .save() // u3 u4
            .removePic(PictureId.xsl("XL-Picture"))
            .save() // u4
            .uploadPic(PictureId.xsl("XL-Picture_1"), "url5")
            .uploadPic(PictureId.xsl("XL-Picture_1"), "url6")
            .save(); //u4 u5 u6
        applyAndCheck(picturesManager, new CheckingState()
            .setContractorUpload("url4")
            .setCorrectionsUpload("url5", "url6")
        );
    }

    @Test
    public void testBillingInspectorAdditionAfterOperatorRemove() {
        PicturesManager picturesManager = new PicturesManager(guru())
            .uploadPicToEnd("url1")
            .uploadPicToEnd("url2")
            .updateState()
            .removePic(PictureId.xsl("XL-Picture"))
            .save()
            .startInspection()
            .uploadPic(PictureId.xsl("XL-Picture_1"), "url1")
            .save();
        applyAndCheck(picturesManager, new CheckingState()
            .setCorrectionsCopy("url1")
        );
    }

    @Test
    public void testBillingInspectorAdditionAfterOperatorRemove2() {
        PicturesManager picturesManager = new PicturesManager(guru())
            .uploadPicToEnd("url1")
            .uploadPicToEnd("url2")
            .updateState()
            .removePic(PictureId.xsl("XL-Picture"))
            .save()
            .startInspection()
            .uploadPic(PictureId.xsl("XL-Picture_1"), "url3")
            .save();
        applyAndCheck(picturesManager, new CheckingState()
            .setInspectorUpload("url3")
        );
    }

    @Test
    public void testBillingInspectionComplex() {
        PicturesManager picturesManager = new PicturesManager(guru(), sku(), sku(), sku())
            .withModel(1).uploadPicToEnd("url1")
            .withModel(2).uploadPicToEnd("url1").uploadPicToEnd("url2")
            .withModel(3).uploadPicToEnd("url3")
            .updateState() // u1 | u1 u2 | u3 | -
            .withModel(2)
            .copyPicture(PictureId.index(0), 3, PictureId.index(1))
            .movePicture(PictureId.index(0), 4, PictureId.index(0))
            .save() // u1 | u2 | u3 u1 | u1
            .withModel(1)
            .uploadPic(PictureId.xsl("XL-Picture_1"), "url4")
            .withModel(2)
            .removePic(PictureId.index(0))
            .save() // u1 u4 | - | u3 u1 | u1
            .uploadPic(PictureId.index(0), "url6")
            .withModel(1)
            .swapPictures(PictureId.xsl("XL-Picture"), PictureId.xsl("XL-Picture_1"))
            .save(); // u4 u1 | u6 | u3 u1 | u1
        applyAndCheck(picturesManager, new CheckingState()
            .setContractorUpload("url4", "url6")
            .setContractorCopy("url1")
        );
        log.debug("Start inspection");
        //-----------
        picturesManager.startInspection()
            .withModel(1)
            .removePic(PictureId.xsl("XL-Picture"))
            .uploadPic(PictureId.xsl("XL-Picture_1"), "url5")
            .copyPicture(PictureId.xsl("XL-Picture"), 2, PictureId.index(1))
            .withModel(2)
            .removePic(PictureId.index(0))
            .withModel(3)
            .removePic(PictureId.index(0))
            .removePic(PictureId.index(0))
            .withModel(4)
            .removePic(PictureId.index(0))
            .save() // u1 u5 | u1 | - | -
            .withModel(1)
            .copyPicture(PictureId.xsl("XL-Picture"), 3, PictureId.index(0))
            .withModel(2)
            .removePic(PictureId.index(0))
            .uploadPic(PictureId.index(0), "url2")
            .uploadPic(PictureId.index(0), "url4")
            .withModel(3)
            .uploadPic(PictureId.index(0), "url7")
            .withModel(4)
            .uploadPic(PictureId.index(0), "url3")
            .uploadPic(PictureId.index(0), "url1")
            .save(); // u1 u5 | u2 u4 | u7 u1 | u3 u1
        applyAndCheck(picturesManager, new CheckingState()
            .setContractorCopy("url1")
            .setInspectorUpload("url7")
            .setCorrectionsUpload("url5")
            .setCorrectionsCopy("url2")
        );
    }

    @Test
    public void testUpdatePicturesBillingModeManyUpload() {
        long i = 0;
        List<AuditAction> actions = Arrays.asList(
            createPictureAction("", "u1", AuditAction.BillingMode.BILLING_MODE_COPY, 1),
            createPictureAction("", "u5", AuditAction.BillingMode.BILLING_MODE_FILL, 1),
            createPictureAction("u1", "u2", AuditAction.BillingMode.BILLING_MODE_COPY, 2),
            createPictureAction("", "u5", AuditAction.BillingMode.BILLING_MODE_MOVE, 2),
            createPictureAction("", "u2", AuditAction.BillingMode.BILLING_MODE_COPY, 3),
            createPictureAction("u5", "u3", AuditAction.BillingMode.BILLING_MODE_FILL, 3),
            createPictureAction("", "u1", AuditAction.BillingMode.BILLING_MODE_COPY, 3),
            createPictureAction("", "u4", AuditAction.BillingMode.BILLING_MODE_COPY, 3),
            createPictureAction("u1", "u4", AuditAction.BillingMode.BILLING_MODE_FILL, 4),
            createPictureAction("u3", "u4", AuditAction.BillingMode.BILLING_MODE_FILL, 4),
            createPictureAction("", "u1", AuditAction.BillingMode.BILLING_MODE_COPY, 5)
        );
        // u1: -2 +3   copy,copy,copy
        // u2: +2    copy,copy
        // u3: -1 +1  fill
        // u4: +3 fill,fill,copy
        // u5: -1 +2 no upload  fill,move
        SquashedUserActions squashedUserActions = createActions(actions, "u1", "u2", "u3");
        Map<String, List<AuditAction>> actionMap = actions.stream()
            .collect(Collectors.groupingBy(a -> a.getNewValue()));

        testSetBillingMode(1, 0, actionMap.get("u1"));
        testSetBillingMode(1, 1, actionMap.get("u2"));
        testSetBillingMode(0, 0, actionMap.get("u3"));
        testSetBillingMode(1, 2, actionMap.get("u4"));
        testSetBillingMode(0, 1, actionMap.get("u5"));
    }

    private SquashedUserActions createActions(Collection<AuditAction> actions, String... uploaded) {
        Map<Long, List<AuditAction>> actionMap = actions.stream()
            .collect(Collectors.groupingBy(a -> a.getEntityId()));

        SquashedUserActions squashedUserActions = new SquashedUserActions();
        squashedUserActions.setUploadedUrls(new HashSet<>(Arrays.asList(uploaded)));
        actionMap.forEach((modelId, modelAuditActions) -> {
            CommonModel model = new CommonModel();
            model.setId(modelId);
            SquashedUserActions.ModelActions modelActions = new SquashedUserActions.ModelActions(model);
            modelActions.createPicturesDiff(modelAuditActions);
            squashedUserActions.addModelActions(modelActions);
        });
        service.updatePicturesBillingMode(squashedUserActions);
        return squashedUserActions;
    }

    private AuditAction createPictureAction(String before, String after, AuditAction.BillingMode mode, long modelId) {
        return AuditActionBuilder.newBuilder()
            .setActionId(System.nanoTime())
            .setEventId(1L)
            .setDate(new Date())
            .setOldValue(before)
            .setNewValue(after)
            .setPropertyName("prop")
            .setBillingMode(mode)
            .setEntityId(modelId)
            .create();
    }

    private void testSetBillingMode(int uploadCount, int copiedCount, Collection<AuditAction> actions) {
        Map<AuditAction.BillingMode, Integer> counts = new HashMap<>();
        actions.forEach(a -> counts.merge(a.getBillingMode(), 1, Integer::sum));
        Assertions.assertThat(counts.getOrDefault(AuditAction.BillingMode.BILLING_MODE_FILL, 0)).isEqualTo(uploadCount);
        Assertions.assertThat(counts.getOrDefault(AuditAction.BillingMode.BILLING_MODE_COPY, 0)).isEqualTo(copiedCount);
        Assertions.assertThat(counts.getOrDefault(AuditAction.BillingMode.BILLING_MODE_MOVE, 0))
            .isEqualTo(actions.size() - uploadCount - copiedCount);
    }

    private void applyAndCheck(PicturesManager picturesManager,
                               CheckingState state) {
        SquashedUserActions contractorActions = SquashingService.squashedUserActions(
            MboParameters.Category.getDefaultInstance(), getAudit(USER_ID), x -> picturesManager.modelMap);
        SquashedUserActions inspectorActions = SquashingService.squashedUserActions(
            MboParameters.Category.getDefaultInstance(), getAudit(INSPECTOR_USER_ID), x -> picturesManager.modelMap);

        ModelAuditStatisticsServiceImpl.ComputeChangedRequestHelper helper = service.new ComputeChangedRequestHelper();
        helper.setContractorActions(contractorActions);
        helper.setInspectorActions(inspectorActions);
        helper.process(MboParameters.Category.getDefaultInstance());
        List<YangLogStorage.ModelStatistic> resp = helper.getModelStatistics(USER_ID, INSPECTOR_USER_ID);
        assertStats(resp, YangLogStorage.ModelStatistic::getContractorActions,
            state.contractorUpload, state.contractorCopy);
        assertStats(resp, YangLogStorage.ModelStatistic::getInspectorActions,
            state.inspectorUpload, state.inspectorCopy);
        assertStats(resp, YangLogStorage.ModelStatistic::getCorrectionsActions,
            state.correctionsUpload, state.correctionsCopy);
    }

    private void assertStats(List<YangLogStorage.ModelStatistic> resp,
                             Function<YangLogStorage.ModelStatistic, YangLogStorage.ModelActions> getter,
                             List<String> needUploaded, List<String> needCopied) {
        Map<Long, AuditAction> actionMap = getAllAudit().stream()
            .collect(Collectors.toMap(a -> a.getActionId(), a -> a));

        List<String> uploaded = new ArrayList<>();
        List<String> copied = new ArrayList<>();
        resp.forEach(ac -> {
            YangLogStorage.ModelActions stata = getter.apply(ac);
            stata.getPictureUploadedList().forEach(action -> {
                AuditAction auditAction = actionMap.get(action.getAuditActionId());
                uploaded.add(auditAction.getNewValue());
            });
            stata.getPictureCopiedList().forEach(action -> {
                AuditAction auditAction = actionMap.get(action.getAuditActionId());
                copied.add(auditAction.getNewValue());
            });
        });
        assertThat(uploaded).containsExactlyInAnyOrderElementsOf(needUploaded);
        assertThat(copied).containsExactlyInAnyOrderElementsOf(needCopied);
    }

    private List<AuditAction> getAllAudit() {
        return getAudit(null);
    }

    private List<AuditAction> getAudit() {
        return getAudit(USER_ID);
    }

    private List<AuditAction> getAudit(Long uid) {
        AuditFilter auditFilter = new AuditFilter()
            .setEntityType(AuditAction.EntityType.MODEL_PICTURE);
        if (uid != null) {
            auditFilter.setUserId(uid);
        }
        return copy(auditServiceMock.loadAudit(0, Integer.MAX_VALUE, auditFilter));
    }

    private List<AuditAction> copy(List<AuditAction> actions) {
        return actions.stream().map(a -> AuditActionBuilder.newBuilder()
            .setPropertyName(a.getPropertyName())
            .setActionId(a.getActionId())
            .setUserId(a.getUserId())
            .setStaffLogin(a.getStaffLogin())
            .setNewValue(a.getNewValue())
            .setOldValue(a.getOldValue())
            .setEntityType(a.getEntityType())
            .setEntityId(a.getEntityId())
            .setEntityName(a.getEntityName())
            .setDate(a.getDate())
            .setActionType(a.getActionType())
            .setCategoryId(a.getCategoryId())
            .setEventId(a.getEventId())
            .setParameterId(a.getParameterId())
            .setBillingMode(a.getBillingMode())
            .setSource(a.getSource())
            .setSourceId(a.getSourceId())
            .create()
        ).collect(Collectors.toList());
    }

    private CommonModel sku() {
        return model(CommonModel.Source.SKU);
    }

    private CommonModel guru() {
        return model(CommonModel.Source.GURU);
    }

    private CommonModel model(CommonModel.Source type) {
        CommonModel model = new CommonModel();
        model.setId(idSeq++);
        model.setCurrentType(type);
        return model;
    }

    private class PicturesManager {
        private final Map<Long, CommonModel> modelMap = new HashMap<>();
        private final Map<Long, CommonModel> prevMap = new HashMap<>();

        private Long currentModelId;
        private Long userId = USER_ID;

        PicturesManager(CommonModel... models) {
            Arrays.stream(models).forEach(m -> {
                 modelMap.put(m.getId(), m);
                 prevMap.put(m.getId(), new CommonModel(m));
            });
            currentModelId = models[0].getId();
        }

        public PicturesManager newModel(CommonModel model) {
            modelMap.put(model.getId(), model);
            if (currentModelId == null) {
                currentModelId = model.getId();
            }
            return this;
        }

        public PicturesManager withModel(long modelId) {
            currentModelId = modelId;
            return this;
        }

        public PicturesManager startInspection() {
            userId = INSPECTOR_USER_ID;
            return this;
        }

        public PicturesManager uploadPicToEnd(String url) {
            CommonModel model = getModel();
            PictureBuilder builder = pictureBuilder(url);
            if (model.getCurrentType().equals(CommonModel.Source.GURU)) {
                int index = 0;
                String xslName;
                while (true) {
                   xslName = xslNameForIndex(index++);
                   if (model.getPicture(xslName) == null) {
                       break;
                   }
                }
                PictureId picId = PictureId.xsl(xslName);
                insertPicture(model, builder.build(), picId);
            } else {
                Picture picture = builder.build();
                model.addPicture(picture);
                model.setModifiedUserId(userId);
            }
            return this;
        }

        public PicturesManager uploadPic(PictureId picId, String url) {
            CommonModel model = getModel();
            PictureBuilder builder = pictureBuilder(url);
            builder.setXslName(picId.xslName);
            insertPicture(model, builder.build(), picId);
            return this;
        }

        public PicturesManager removePic(PictureId picId) {
            CommonModel model = getModel();
            Picture picture = getPicture(model, picId);
            model.getPictures().remove(picture);
            if (model.getCurrentType().equals(CommonModel.Source.GURU)) {
                String xslName = picture.getXslName();
                model.getPictures().stream()
                    .filter(p -> xslName.compareTo(p.getXslName()) <= 0)
                    .forEach(p -> p.setXslName(shiftXslName(p.getXslName(), -1)));
            }
            model.setModifiedUserId(userId);
            return this;
        }

        public PicturesManager swapPictures(PictureId picId, PictureId picId1) {
            CommonModel model = getModel();
            Picture picture = getPicture(model, picId);
            Picture picture1 = getPicture(model, picId1);
            if (picId.index != null) {
                model.getPictures().set(picId.index, picture1);
                model.getPictures().set(picId1.index, picture);
            } else {
                picture.setXslName(picId1.xslName);
                picture1.setXslName(picId.xslName);
            }
            updateTs(picture);
            updateTs(picture1);
            model.setModifiedUserId(userId);
            return this;
        }

        public PicturesManager movePicture(PictureId picId, long targetModelId, PictureId targetPicId) {
            CommonModel model = getModel();
            CommonModel targetModel = modelMap.get(targetModelId);
            Picture picture = getPicture(model, picId);
            model.getPictures().remove(picture);
            insertPicture(targetModel, picture, targetPicId);
            return this;
        }

        public PicturesManager copyPicture(PictureId picId, long targetModelId, PictureId targetPicId) {
            CommonModel model = getModel();
            CommonModel targetModel = modelMap.get(targetModelId);
            Picture picture = getPicture(model, picId);
            Picture copy = new Picture(picture);
            insertPicture(targetModel, copy, targetPicId);
            return this;
        }

        private CommonModel getModel() {
            return modelMap.get(currentModelId);
        }

        private String shiftXslName(String xslName, int delta) {
            int curIndex = 0;
            if (!xslName.equals(XslNames.XL_PICTURE)) {
                curIndex = Integer.parseInt(xslName.substring(xslName.indexOf("_") + 1));
            }
            curIndex += delta;
            if (curIndex < 0) {
                throw new IllegalStateException("index = " + curIndex);
            }
            return xslNameForIndex(curIndex);
        }

        private String xslNameForIndex(int index) {
            if (index == 0) {
                return XslNames.XL_PICTURE;
            } else {
                return XslNames.XL_PICTURE + "_" + index;
            }
        }

        private PictureBuilder pictureBuilder(String picUrl) {
            return PictureBuilder.newBuilder()
                .setUrl(picUrl)
                .setHeight(1)
                .setWidth(1)
                .setUrlOrig(picUrl)
                .setModificationSource(ModificationSource.OPERATOR_FILLED);
        }

        private void insertPicture(CommonModel model, Picture picture, PictureId picId) {
            if (picId.xslName != null) {
                String xslName = picId.xslName;
                picture.setXslName(xslName);
                if (model.getPicture(xslName) != null) {
                    model.getPictures().stream()
                        .filter(p -> xslName.compareTo(p.getXslName()) <= 0)
                        .forEach(p -> p.setXslName(shiftXslName(p.getXslName(), 1)));
                }
                model.addPicture(picture);
            } else {
                model.getPictures().add(picId.index, picture);
            }
            updateTs(picture);
            model.setModifiedUserId(userId);
        }

        private void updateTs(Picture picture) {
            picture.setLastModificationDate(new Date(timestampSeq++));
            picture.setLastModificationUid(userId);
        }

        private Picture getPicture(CommonModel model, PictureId pictureId) {
            if (pictureId.index != null) {
                return model.getPictures().get(pictureId.index);
            }
            return model.getPicture(pictureId.xslName);
        }

        private PicturesManager save() {
            applyCopyLogic();
            modelAuditService.auditModels(modelMap.values(), prevMap, modelAuditContext);
            return updateState();
        }

        private PicturesManager updateState() {
            modelMap.values().forEach(m -> prevMap.put(m.getId(), new CommonModel(m)));
            return this;
        }

        private void applyCopyLogic() {
            Multimap<String, Picture> picturesByUrl = ArrayListMultimap.create();
            modelMap.values().stream().flatMap(m -> m.getPictures().stream())
                .forEach(p -> picturesByUrl.put(p.getUrl(), p));

            picturesByUrl.asMap().forEach((u, pics) -> {
                List<Picture> picsList = new ArrayList<>(pics);
                Collections.sort(picsList, Comparator.comparing(Picture::getLastModificationDate));
                picsList.stream()
                    .skip(1)
                    .forEach(p -> p.setModificationSource(ModificationSource.OPERATOR_COPIED));
            });
        }
    }

    private static class PictureId {
        final String xslName;
        final Integer index;

        private PictureId(String xslName, Integer index) {
            this.xslName = xslName;
            this.index = index;
        }

        static PictureId xsl(String xslName) {
            return new PictureId(xslName, null);
        }

        static PictureId index(Integer index) {
            return new PictureId(null, index);
        }
    }

    private static class CheckingState {
        private List<String> contractorUpload = Collections.emptyList();
        private List<String> contractorCopy = Collections.emptyList();
        private List<String> inspectorUpload = Collections.emptyList();
        private List<String> inspectorCopy = Collections.emptyList();
        private List<String> correctionsUpload = Collections.emptyList();
        private List<String> correctionsCopy = Collections.emptyList();

        public CheckingState setContractorUpload(String... contractorUploads) {
            this.contractorUpload = Arrays.asList(contractorUploads);
            return this;
        }

        public CheckingState setContractorCopy(String... contractorCopies) {
            this.contractorCopy = Arrays.asList(contractorCopies);
            return this;
        }

        public CheckingState setInspectorUpload(String... inspectorUploads) {
            this.inspectorUpload = Arrays.asList(inspectorUploads);
            return this;
        }

        public CheckingState setInspectorCopy(String... inspectorCopies) {
            this.inspectorCopy = Arrays.asList(inspectorCopies);
            return this;
        }

        public CheckingState setCorrectionsUpload(String... correctionsUploads) {
            this.correctionsUpload = Arrays.asList(correctionsUploads);
            return this;
        }

        public CheckingState setCorrectionsCopy(String... correctionsCopies) {
            this.correctionsCopy = Arrays.asList(correctionsCopies);
            return this;
        }
    }
}
