package ru.yandex.market.markup2.tasks.fill_picture_params;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.markup2.utils.model.ModelStorageService;
import ru.yandex.market.markup2.utils.param.ParamUtils;
import ru.yandex.market.markup2.workflow.general.TaskDataItem;
import ru.yandex.market.markup2.workflow.generation.RequestGeneratorContext;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.market.markup2.utils.Markup2TestUtils.createBasicTaskInfo;
import static ru.yandex.market.markup2.utils.Markup2TestUtils.createBasicUniqueContext;
import static ru.yandex.market.markup2.utils.Markup2TestUtils.createGenerationContext;
import static ru.yandex.market.markup2.utils.Markup2TestUtils.mockIdGenerator;
import static ru.yandex.market.markup2.utils.ModelTestUtils.createModel;
import static ru.yandex.market.markup2.utils.ModelTestUtils.createModelWithPictures;
import static ru.yandex.market.markup2.utils.ModelTestUtils.createParameterValue;
import static ru.yandex.market.markup2.utils.ModelTestUtils.createPicture;
import static ru.yandex.market.markup2.utils.ModelTestUtils.mockModelStorageConditionalProcessModels;

/**
 * @author anmalysh
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class FillPictureParamsTaskGeneratorTest {
    private FillPictureParamsRequestGenerator generator;

    static final int CATEGORY_ID = 100500;
    static final Long VENDOR_ID = 500L;

    @Mock
    private ModelStorageService modelStorageService;
    @Mock
    private ParamUtils paramUtils;

    ModelStorage.Model model1 = createModelWithPictures(CATEGORY_ID, VENDOR_ID, 1L, "GURU", false,
        createPicture("XL-Picture", "http://image.url").build(),
        createPicture("XL-Picture_2", "http://image2.url").build()
        );
    ModelStorage.Model model2 = createModel(CATEGORY_ID, VENDOR_ID, 2L, "GURU", true);
    ModelStorage.Model model3 = createModelWithPictures(CATEGORY_ID, VENDOR_ID, 3L, "GURU", false,
        createPicture("XL-Picture", "http://image.url").build(),
        createPicture("XL-Picture_2", "http://image2.url",
            createParameterValue(
                FillPictureParamsRequestGenerator.COLOR_PARAM_ID,
                MboParameters.ValueType.ENUM,
                "vendor_color").build()).build()
    );
    ModelStorage.Model model4 = createModel(CATEGORY_ID, VENDOR_ID, 4L, "CLUSTER", true);
    List<ModelStorage.Model> models = new ArrayList<>();
    {
        models.addAll(Arrays.asList(model1, model2, model3));
    }

    Long2ObjectMap<String> vendors = new Long2ObjectOpenHashMap<>();
    {
        vendors.put(VENDOR_ID, "Vendor");
    }

    Map<Long, List<VendorColor>> modelVendorColors = new HashMap<>();
    {
        modelVendorColors.computeIfAbsent(1L, k -> new ArrayList<>())
            .add(new VendorColor(1L, "Blue"));
        modelVendorColors.computeIfAbsent(1L, k -> new ArrayList<>())
            .add(new VendorColor(2L, "Red"));
    }

    @Before
    public void setup() throws Exception {
        mockModelStorageConditionalProcessModels(modelStorageService, models);

        when(paramUtils.getVendors(CATEGORY_ID)).thenReturn(vendors);

        FillPictureParamsRequestGenerator generatorInstance = new FillPictureParamsRequestGenerator();
        generatorInstance.setModelStorageService(modelStorageService);
        generatorInstance.setParamUtils(paramUtils);
        generator = spy(generatorInstance);
        doReturn(modelVendorColors).when(generator).loadModelVendorColors();
    }

    @Test
    public void testGenerator() throws IOException, SQLException {
        Collection<TaskDataItem<FillPictureParamsPayload, FillPictureParamsResponse>> tasks = generateTasks();
        assertEquals(2, tasks.size());
        Iterator<TaskDataItem<FillPictureParamsPayload, FillPictureParamsResponse>> taskIter = tasks.iterator();
        assertPayload(taskIter.next().getInputData(), 1L, "http://image.url");
        assertPayload(taskIter.next().getInputData(), 1L, "http://image2.url");
    }

    private void assertPayload(FillPictureParamsPayload payload, long modelId, String imageUrl) {
        assertEquals(modelId, payload.getModelId());
        assertEquals(imageUrl, payload.getImageUrl());
        assertEquals(CATEGORY_ID, payload.getAttributes().getCategoryId());
        assertEquals("Model1", payload.getAttributes().getModelName());
        assertEquals(VENDOR_ID.longValue(), payload.getAttributes().getVendorId());
        assertEquals("Vendor", payload.getAttributes().getVendorName());
        assertEquals(Arrays.asList(1L, 2L), payload.getAttributes().getVendorColorIds());
        assertEquals(modelVendorColors.get(modelId), payload.getAttributes().getVendorColors());
    }

    private Collection<TaskDataItem<FillPictureParamsPayload, FillPictureParamsResponse>> generateTasks() {
        RequestGeneratorContext<FillPictureParamsTaskIdentity, FillPictureParamsPayload,
                FillPictureParamsResponse> context =
            createGenerationContext(
                createBasicTaskInfo(CATEGORY_ID, 5, Collections.emptyMap()),
                createBasicUniqueContext(),
                mockIdGenerator());

        generator.generateRequests(context);

        return context.getTaskDataItems();
    }
}
