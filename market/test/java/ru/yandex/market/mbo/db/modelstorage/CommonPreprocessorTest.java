package ru.yandex.market.mbo.db.modelstorage;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors.CommonPreprocessor;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;

import java.io.IOException;
import java.util.Date;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CommonPreprocessorTest {
    private static final int LAST_MODIFICATION_DATE = 10;
    private static final long PARAM_ID3 = 3L;
    private static final int VALUE3 = 3;
    private static final int VALUE4 = 4;
    private static final int VALUE5 = 5;
    private static final long MODIFICATION_UID2 = 2L;
    private static final long MODIFICATION_UID3 = 3L;
    private static final long MODIFICATION_UID10 = 10L;
    private CommonModelBuilder<Object> data;

    private CommonPreprocessor commonPreprocessor;

    @Before
    public void before() {
        data = ParametersBuilder
            .startParameters(CommonModelBuilder::model)
                .startParameter()
                    .xsl("num1").type(Param.Type.NUMERIC)
                .endParameter()
                .startParameter()
                    .xsl("num2").type(Param.Type.NUMERIC)
                .endParameter()
                .startParameter()
                    .xsl("num2").type(Param.Type.NUMERIC)
                .endParameter()
            .endParameters();

        commonPreprocessor = new CommonPreprocessor();
    }

    @Test
    public void preprocessingMatchXslNameTest() throws IOException {
        data
            .startModel()
                .id(1).category(1)
                .param(1L).setNumeric(1).lastModificationUid(1L).lastModificationDate(new Date(LAST_MODIFICATION_DATE))
                .param(2L).setNumeric(VALUE3).lastModificationUid(2L)
                    .lastModificationDate(new Date(LAST_MODIFICATION_DATE))
                .param(PARAM_ID3).setNumeric(VALUE4).lastModificationUid(MODIFICATION_UID3)
                    .lastModificationDate(new Date(LAST_MODIFICATION_DATE))
            .endModel();
        CommonModel before = data.getModel();

        data
            .startModel()
                .id(1).category(1)
                .param(1L).setNumeric(1).lastModificationUid(1L).lastModificationDate(new Date(LAST_MODIFICATION_DATE))
                .param(2L).setNumeric(VALUE3).lastModificationUid(2L)
                    .lastModificationDate(new Date(LAST_MODIFICATION_DATE))
                .param(PARAM_ID3).setNumeric(VALUE5).lastModificationUid(MODIFICATION_UID3)
                    .lastModificationDate(new Date(LAST_MODIFICATION_DATE))
            .endModel();
        CommonModel current = data.getModel();

        ModelSaveGroup modelSaveGroup = ModelSaveGroup.fromModelsCopy(
            ImmutableList.of(current),
            ImmutableList.of(before));
        commonPreprocessor.preprocess(modelSaveGroup, new ModelSaveContext(MODIFICATION_UID10));

        CommonModel model = modelSaveGroup.getById(current.getId());

        Assert.assertEquals(1L, model.getSingleParameterValue(1).getLastModificationUid().longValue());
        Assert.assertEquals(2L, model.getSingleParameterValue(2).getLastModificationUid().longValue());
        Assert.assertEquals(MODIFICATION_UID10,
            model.getSingleParameterValue(PARAM_ID3).getLastModificationUid().longValue());

        Assert.assertEquals(LAST_MODIFICATION_DATE,
            model.getSingleParameterValue(1).getLastModificationDate().getTime());
        Assert.assertEquals(LAST_MODIFICATION_DATE,
            model.getSingleParameterValue(2).getLastModificationDate().getTime());
        Assert.assertTrue(model.getSingleParameterValue(PARAM_ID3)
            .getLastModificationDate().getTime() > LAST_MODIFICATION_DATE);
    }
}
