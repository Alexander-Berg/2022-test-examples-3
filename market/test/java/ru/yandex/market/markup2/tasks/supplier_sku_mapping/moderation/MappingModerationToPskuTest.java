package ru.yandex.market.markup2.tasks.supplier_sku_mapping.moderation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.ir.http.Markup;
import ru.yandex.market.markup2.entries.config.TaskConfigInfo;
import ru.yandex.market.markup2.entries.group.ParameterType;
import ru.yandex.market.markup2.entries.task.TaskInfo;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.SupplierOffer;

import static org.mockito.ArgumentMatchers.eq;

@Deprecated
@Ignore("m3")
public class MappingModerationToPskuTest extends MappingModerationTest {

    private static final long PSKU1 = 1001L;
    private static final long PSKU2 = 1002L;
    private static final long PSKU3 = 1003L;
    private static final long PSKU4 = 1004L;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ReflectionTestUtils.setField(ssmGenerationRequestGenerator, "distinctGenerationToPsku", true);
    }

    @Override
    protected MboCategory.GetOffersPrioritiesRequest getBaseOffersRequest() {
        return MboCategory.GetOffersPrioritiesRequest.newBuilder()
                .setStatusFilter(MboCategory.GetOffersPrioritiesRequest.StatusFilter.IN_MODERATION)
                .setSuggestSkuType(SupplierOffer.SkuType.TYPE_PARTNER)
                .build();
    }

    @Override
    public YangLogStorage.YangTaskType getYangTaskType() {
        return YangLogStorage.YangTaskType.MAPPING_MODERATION;
    }

    @Override
    protected void runGeneration(int offersInTask) throws Exception {
        mboCategoryService.getTicketPriorities()
                .forEach(op -> op.setSkuType(SupplierOffer.SkuType.TYPE_PARTNER));
        runGeneration(offersInTask, builder -> builder.addParameter(ParameterType.FOR_MODERATION_TO_PSKU, true));
    }

    @Override
    public int getTaskType() {
        return Markup.TaskType.SUPPLIER_MAPPING_MODERATION_TO_PSKU_VALUE;
    }

    @Test
    public void testPskuRemovingBarcodes() throws Exception {
        Mockito.when(modelStorageService.getModels(eq((long) CATEGORY_ID), Mockito.anyCollection())).thenAnswer(
                invocation -> {
                    Collection<Long> ids = invocation.getArgument(1);
                    return ids.stream()
                            .map(this::mdl)
                            .map(m -> {
                                if (m.getId() == PSKU1) {
                                    m.addParameterValues(ModelStorage.ParameterValue.newBuilder()
                                            .setXslName(ParamUtils.BARCODE_XSL_NAME)
                                            .build());
                                }
                                if (m.getId() == PSKU3) {
                                    m.addParameterValues(ModelStorage.ParameterValue.newBuilder()
                                            .setXslName(ParamUtils.BAD_PSKU_XSL_NAME)
                                            .setBoolValue(true)
                                            .build());
                                }
                                if (m.getId() == PSKU4) {
                                    m.addParameterValues(ModelStorage.ParameterValue.newBuilder()
                                            .setXslName(ParamUtils.BAD_PSKU_XSL_NAME)
                                            .setBoolValue(false)
                                            .build());
                                }
                                return m.build();
                            })
                            .collect(Collectors.toList());
                }
        );
        Mockito.when(categoryParametersService.getParameters(eq(CATEGORY_ID))).thenReturn(
                Collections.singletonList(
                        MboParameters.Parameter.newBuilder()
                                .setId(1)
                                .setXslName(ParamUtils.BAD_PSKU_XSL_NAME)
                                .setValueType(MboParameters.ValueType.BOOLEAN)
                                .addOption(MboParameters.Option.newBuilder()
                                        .setId(10)
                                        .addName(MboParameters.Word.newBuilder().setName("TRUE").build())
                                        .build())
                                .addOption(MboParameters.Option.newBuilder()
                                        .setId(20)
                                        .addName(MboParameters.Word.newBuilder().setName("FALSE").build())
                                        .build())
                                .build()
                )
        );
        aliasMakerService.clearOffers();
        addOffers(CATEGORY_ID, getBaseOffersRequest(), offer(SUPPLIER_ID),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID),
                offer(SUPPLIER_ID)
        );
        runGeneration(4);

        TaskConfigInfo configInfo = tasksCache.getConfigInfosByCategoryId(CATEGORY_ID, (x) -> true)
                .iterator().next();
        TaskInfo taskInfo = configInfo.getSingleCurrentTask();
        taskProcessManager.processAll();
        processTaskInYang(taskInfo);
        processAllTasksWithUnlock(taskProcessManager);
        Mockito.verify(modelStorageService).getModels(
                eq((long) CATEGORY_ID), eq(new HashSet<>(Arrays.asList(PSKU1, PSKU3, PSKU4))));

        ModelStorage.ParameterValue badPskuVal = ModelStorage.ParameterValue.newBuilder()
                .setBoolValue(true)
                .setModificationDate(MODIFICATION_DATE)
                .setOptionId(10)
                .setValueType(MboParameters.ValueType.BOOLEAN)
                .setUserId(TEST_UID)
                .setTypeId(0)
                .setXslName(ParamUtils.BAD_PSKU_XSL_NAME)
                .setParamId(1)
                .setValueSource(ModelStorage.ModificationSource.AUTO)
                .build();
        List<ModelStorage.Model> pskus = Arrays.asList(
                mdl(PSKU1).addParameterValues(badPskuVal).build(),
                mdl(PSKU4).addParameterValues(badPskuVal).build());

        Mockito.verify(modelStorageService).updateModels(
                eq(pskus), eq(TEST_UID), eq(taskInfo.getId()),
                eq(YangLogStorage.YangTaskType.MAPPING_MODERATION), Mockito.any());
    }

    private ModelStorage.Model.Builder mdl(long id) {
        return ModelStorage.Model.newBuilder().setId(id);
    }

    @Override
    protected Long getMskuIdByIndex(int i) {
        List<Long> allIds = Arrays.asList(PSKU1, PSKU2, PSKU3, PSKU4);
        return allIds.get(i % allIds.size());
    }

    @Override
    protected Boolean isBadCard(long skuId) {
        return skuId != PSKU2;
    }
}
