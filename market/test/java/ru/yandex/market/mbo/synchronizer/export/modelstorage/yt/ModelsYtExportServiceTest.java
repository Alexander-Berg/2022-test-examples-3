package ru.yandex.market.mbo.synchronizer.export.modelstorage.yt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.http.ModelStorage;

import java.time.Instant;

@RunWith(MockitoJUnitRunner.class)
public class ModelsYtExportServiceTest {

    public static final int CATEGORY_ID = 1;
    public static final int VENDOR_ID = 10;
    public static final int MODEL_ID = 100;
    public static final int SKU_PARENT_MODEL_ID = 200;
    public static final int PARENT_ID = 1000;

    public static final String TITLE = "title";

    public static final Boolean PUBLISHED = true;
    public static final Boolean BLUE_PUBLISHED = false;

    public static final Instant CREATE_DATE = Instant.now().minusSeconds(10);
    public static final Instant DELETED_DATE = Instant.now();

    @Test
    public void simpleTest() throws Exception {
        // NPE free method
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
        builder.setCategoryId(CATEGORY_ID).setVendorId(VENDOR_ID).setId(MODEL_ID);

        YTreeMapNode node = ModelsYtExportService.row(builder.build());

        Assert.assertEquals(MODEL_ID, node.get(ModelsYtExportService.MODEL_ID).get().intValue());
        Assert.assertEquals(CATEGORY_ID, node.get(ModelsYtExportService.CATEGORY_ID).get().intValue());
        Assert.assertEquals(VENDOR_ID, node.get(ModelsYtExportService.VENDOR_ID).get().intValue());
    }

    @Test
    public void guruModelTest() throws Exception {
        ModelStorage.ModelType type = ModelStorage.ModelType.GURU;

        YTreeMapNode node = ModelsYtExportService.row(createModel(type));
        assertParams(node, type);

        // sku empty
        Assert.assertEquals(false, node.get(ModelsYtExportService.IS_SKU).get().boolValue());
        Assert.assertEquals(null, node.get(ModelsYtExportService.SKU_PARENT_MODEL_ID).orElse(null));
    }

    @Test
    public void skuModelTest() throws Exception {
        ModelStorage.ModelType type = ModelStorage.ModelType.SKU;

        YTreeMapNode node = ModelsYtExportService.row(createModel(type));
        assertParams(node, type);

        // sku info exist
        Assert.assertEquals(true, node.get(ModelsYtExportService.IS_SKU).get().boolValue());
        Assert.assertEquals(SKU_PARENT_MODEL_ID, node.get(ModelsYtExportService.SKU_PARENT_MODEL_ID).get().intValue());

    }

    private void assertParams(YTreeMapNode node, ModelStorage.ModelType type) {
        Assert.assertEquals(MODEL_ID, node.get(ModelsYtExportService.MODEL_ID).get().intValue());
        Assert.assertEquals(CATEGORY_ID, node.get(ModelsYtExportService.CATEGORY_ID).get().intValue());
        Assert.assertEquals(VENDOR_ID, node.get(ModelsYtExportService.VENDOR_ID).get().intValue());
        Assert.assertEquals(PARENT_ID, node.get(ModelsYtExportService.PARENT_ID).get().intValue());
        Assert.assertEquals(type.name(), node.get(ModelsYtExportService.SOURCE_TYPE).get().stringValue());
        Assert.assertEquals(type.name(), node.get(ModelsYtExportService.CURRENT_TYPE).get().stringValue());
        Assert.assertEquals(TITLE, node.get(ModelsYtExportService.TITLE).get().stringValue());
        Assert.assertEquals(PUBLISHED, node.get(ModelsYtExportService.PUBLISHED).get().boolValue());
        Assert.assertEquals(BLUE_PUBLISHED, node.get(ModelsYtExportService.BLUE_PUBLISHED).get().boolValue());
        Assert.assertEquals(ModelsYtExportService.formatDate(CREATE_DATE.toEpochMilli()),
                            node.get(ModelsYtExportService.CREATE_DATE).get().stringValue());
        Assert.assertEquals(ModelsYtExportService.formatDate(DELETED_DATE.toEpochMilli()),
                            node.get(ModelsYtExportService.DELETED_DATE).get().stringValue());
    }

    private ModelStorage.Model createModel(ModelStorage.ModelType type) {
        ModelStorage.Model.Builder builder = ModelStorage.Model.newBuilder();
        builder.setCategoryId(CATEGORY_ID)
                .setVendorId(VENDOR_ID)
                .setId(MODEL_ID)
                .setParentId(PARENT_ID)
                .setSourceType(type.name())
                .setCurrentType(type.name())
                .addTitles(ModelStorage.LocalizedString.newBuilder().setValue(TITLE).build())
                .setPublished(PUBLISHED)
                .setBluePublished(BLUE_PUBLISHED)
                .setCreatedDate(CREATE_DATE.toEpochMilli())
                .addRelations(ModelStorage.Relation.newBuilder().setId(SKU_PARENT_MODEL_ID)
                                                                .setType(ModelStorage.RelationType.SKU_PARENT_MODEL)
                                                                .build())
                .setDeletedDate(DELETED_DATE.toEpochMilli());

        return builder.build();
    }
}
