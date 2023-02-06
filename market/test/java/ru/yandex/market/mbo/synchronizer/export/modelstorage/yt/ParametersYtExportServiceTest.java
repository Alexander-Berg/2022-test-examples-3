package ru.yandex.market.mbo.synchronizer.export.modelstorage.yt;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.db.params.ParameterProtoConverter;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.synchronizer.export.tree.TovarTreeYtExportService;

@RunWith(MockitoJUnitRunner.class)
public class ParametersYtExportServiceTest {

    public static final int ID = 1;
    public static final long HID = 10;
    public static final String NAME = "name";
    public static final String XSL_NAME = "xsl_name";
    public static final String DESCRIPTION = "description";
    public static final Boolean USE_FOR_GURULIGHT = true;
    public static final MboParameters.ParameterLevel PARAM_TYPE = MboParameters.ParameterLevel.MODEL_LEVEL;
    public static final MboParameters.ValueType VALUE_TYPE = MboParameters.ValueType.ENUM;
    public static final Boolean USE_FORMALIZATION = false;
    public static final int COMMON_FILTER_INDEX = -1;
    public static final int ADV_FILTER_INDEX = -1;
    public static final Boolean PUBLISHED = true;

    public static final Boolean MULTIVALUE = false;
    public static final Boolean USE_FOR_IMAGES = false;
    public static final MboParameters.GuruType GURU_TYPE = MboParameters.GuruType.GURU_TYPE_EMPTY;
    public static final MboParameters.SKUParameterMode SKU_MODE = MboParameters.SKUParameterMode.SKU_NONE;

    @Test
    public void columnTest() throws Exception {
        MboParameters.Parameter.Builder paramBuild = MboParameters.Parameter.newBuilder();
        paramBuild.setId(ID)
                .setXslName(XSL_NAME)
                .setParamType(PARAM_TYPE)
                .setValueType(VALUE_TYPE)
                .setPublished(PUBLISHED)
                .setUseFormalization(USE_FORMALIZATION)
                .setCommonFilterIndex(COMMON_FILTER_INDEX)
                .setAdvFilterIndex(ADV_FILTER_INDEX)
                .setMultivalue(MULTIVALUE)
                .setUseForImages(USE_FOR_IMAGES)
                .setGuruType(GURU_TYPE)
                .setSkuMode(SKU_MODE)
                .setUseForGurulight(USE_FOR_GURULIGHT)
                .setDescription(DESCRIPTION)
                .addName(ParameterProtoConverter.convert(new Word(Word.DEFAULT_LANG_ID, NAME)));

        YTreeMapNode mapNode = ParametersYtExportService.mapParameter(HID, paramBuild.build());

        Assert.assertEquals(paramBuild.getId(), mapNode.get(ParametersYtExportService.ID).get().intValue());

        Assert.assertEquals(HID, mapNode.get(ParametersYtExportService.HID).get().longValue());

        Assert.assertEquals(paramBuild.getXslName(),
                            mapNode.get(ParametersYtExportService.XSL_NAME).get().stringValue());

        Assert.assertEquals(paramBuild.getParamType().name(),
                            mapNode.get(ParametersYtExportService.PARAM_TYPE).get().stringValue());

        Assert.assertEquals(paramBuild.getValueType().name(),
                            mapNode.get(ParametersYtExportService.VALUE_TYPE).get().stringValue());

        Assert.assertEquals(paramBuild.getPublished(),
                            mapNode.get(TovarTreeYtExportService.PUBLISHED).get().boolValue());

        Assert.assertEquals(paramBuild.getUseFormalization(),
                            mapNode.get(ParametersYtExportService.USE_FORMALIZATION).get().boolValue());

        Assert.assertEquals(paramBuild.getCommonFilterIndex(),
                            mapNode.get(ParametersYtExportService.COMMON_FILTER_INDEX).get().intValue());

        Assert.assertEquals(paramBuild.getAdvFilterIndex(),
                            mapNode.get(ParametersYtExportService.ADV_FILTER_INDEX).get().intValue());

        Assert.assertEquals(paramBuild.getMultivalue(),
                            mapNode.get(ParametersYtExportService.MULTIVALUE).get().boolValue());

        Assert.assertEquals(paramBuild.getUseForImages(),
                            mapNode.get(ParametersYtExportService.USE_FOR_IMAGES).get().boolValue());

        Assert.assertEquals(paramBuild.getGuruType().name(),
                            mapNode.get(ParametersYtExportService.GURU_TYPE).get().stringValue());

        Assert.assertEquals(paramBuild.getSkuMode().name(),
                            mapNode.get(ParametersYtExportService.SKU_MODE).get().stringValue());

        Assert.assertEquals(paramBuild.getName(0).getName(),
                            mapNode.get(ParametersYtExportService.NAME).get().stringValue());

        Assert.assertEquals(paramBuild.getName(0).getName(),
                mapNode.get(ParametersYtExportService.NAME).get().stringValue());

        Assert.assertEquals(paramBuild.getUseForGurulight(),
                mapNode.get(ParametersYtExportService.USE_FOR_GURULIGHT).get().boolValue());

        Assert.assertEquals(paramBuild.getDescription(),
                mapNode.get(ParametersYtExportService.DESCRIPTION).get().stringValue());
    }
}
