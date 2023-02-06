package ru.yandex.market.mbo.synchronizer.export.tree;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.db.params.ParameterProtoConverter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class TovarTreeYtExportServiceTest {

    private static final int TOVAR_ID = 1;
    private static final int HID = 10;
    private static final int PARENT_HID = 11;
    private static final int GURU_ID = 100;
    private static final int VISUAL_CATEGORY_ID = 1000;

    private static final Boolean PUBLISHED = true;
    private static final Boolean CLUSTERIZE = false;
    private static final Boolean GROUPED = false;
    private static final Boolean LEAF = true;

    private static final String NAME = "Category Name";
    private static final String UNIQUE_NAME = "Unique Category Name";

    @Test
    public void columnTest() throws Exception {
        MboParameters.Category.Builder catBuild = MboParameters.Category.newBuilder();
        catBuild.setTovarId(TOVAR_ID)
                .setHid(HID)
                .setGuruId(GURU_ID)
                .setParentHid(PARENT_HID)
                .setVisualCategoryId(VISUAL_CATEGORY_ID)
                .setPublished(PUBLISHED)
                .setClusterize(CLUSTERIZE)
                .setGrouped(GROUPED)
                .setLeaf(LEAF)
                .setOutputType(MboParameters.OutputType.GURU)
                .addName(ParameterProtoConverter.convert(new Word(Word.DEFAULT_LANG_ID, NAME)))
                .addUniqueName(ParameterProtoConverter.convert(new Word(Word.DEFAULT_LANG_ID, UNIQUE_NAME)));

        YTreeMapNode mapNode = TovarTreeYtExportService.mapCategory(catBuild.build());

        Assert.assertEquals(catBuild.getTovarId(), mapNode.get(TovarTreeYtExportService.TOVAR_ID).get().intValue());
        Assert.assertEquals(catBuild.getHid(), mapNode.get(TovarTreeYtExportService.HID).get().intValue());
        Assert.assertEquals(catBuild.getGuruId(), mapNode.get(TovarTreeYtExportService.GURU_ID).get().intValue());
        Assert.assertEquals(catBuild.getParentHid(), mapNode.get(TovarTreeYtExportService.PARENT_HID).get().intValue());
        Assert.assertEquals(catBuild.getVisualCategoryId(),
                            mapNode.get(TovarTreeYtExportService.VISUAL_CATEGORY_ID).get().intValue());

        Assert.assertEquals(catBuild.getPublished(), mapNode.get(TovarTreeYtExportService.PUBLISHED).get().boolValue());
        Assert.assertEquals(catBuild.getClusterize(),
                            mapNode.get(TovarTreeYtExportService.CLUSTERIZE).get().boolValue());

        Assert.assertEquals(catBuild.getGrouped(), mapNode.get(TovarTreeYtExportService.GROUPED).get().boolValue());
        Assert.assertEquals(catBuild.getLeaf(), mapNode.get(TovarTreeYtExportService.LEAF).get().boolValue());

        Assert.assertEquals(catBuild.getOutputType().name(),
                            mapNode.get(TovarTreeYtExportService.OUTPUT_TYPE).get().stringValue());
        Assert.assertEquals(catBuild.getName(0).getName(),
                            mapNode.get(TovarTreeYtExportService.NAME).get().stringValue());
        Assert.assertEquals(catBuild.getUniqueName(0).getName(),
                            mapNode.get(TovarTreeYtExportService.UNIQUE_NAME).get().stringValue());
    }

    /**
     *  In column Name and Unique Name we save only one russian name/unique name.
     *  all another names saved in "data" column.
     * @throws Exception
     */
    @Test
    public void nameTest() throws Exception {
        MboParameters.Category.Builder catBuild = MboParameters.Category.newBuilder();

        List<Word> names = new ArrayList<>();
        names.add(new Word(Word.DEFAULT_LANG_ID + 1, "Another name"));
        names.add(new Word(Word.DEFAULT_LANG_ID, NAME));
        names.add(new Word(1, "Yet Another name"));

        List<Word> uniqueName = new ArrayList<>();
        uniqueName.add(new Word(Word.DEFAULT_LANG_ID + 1, "Another unique name"));
        uniqueName.add(new Word(1, "Yet Another Unique name"));
        uniqueName.add(new Word(Word.DEFAULT_LANG_ID, UNIQUE_NAME));

        catBuild.setTovarId(TOVAR_ID)
                .setHid(HID)
                .addAllName(ParameterProtoConverter.convertWords(names))
                .addAllUniqueName(ParameterProtoConverter.convertWords(uniqueName));

        YTreeMapNode mapNode = TovarTreeYtExportService.mapCategory(catBuild.build());

        Assert.assertEquals(NAME, mapNode.get(TovarTreeYtExportService.NAME).get().stringValue());
        Assert.assertEquals(UNIQUE_NAME, mapNode.get(TovarTreeYtExportService.UNIQUE_NAME).get().stringValue());

        String data = mapNode.get(TovarTreeYtExportService.DATA).get().stringValue();
        Assert.assertEquals(data.contains(NAME), true);
        Assert.assertEquals(data.contains("Another name"), true);
        Assert.assertEquals(data.contains("Yet Another name"), true);

        Assert.assertEquals(data.contains(UNIQUE_NAME), true);
        Assert.assertEquals(data.contains("Another unique name"), true);
        Assert.assertEquals(data.contains("Yet Another Unique name"), true);
    }
}
