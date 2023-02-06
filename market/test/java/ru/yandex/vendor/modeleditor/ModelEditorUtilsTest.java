package ru.yandex.vendor.modeleditor;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.vendor.modeleditor.mbo.MboParam;
import ru.yandex.vendor.modeleditor.model.ModelParameter;
import ru.yandex.vendor.modeleditor.model.ModelParameterValueOption;
import ru.yandex.vendor.modeleditor.sku.ModelSku;

import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static ru.yandex.vendor.modeleditor.mbo.MboParam.*;

public class ModelEditorUtilsTest {

    private ModelParameterValueOption option1;
    private ModelParameterValueOption option2;
    private ModelParameterValueOption option3;
    private ModelParameterValueOption expectedOption;

    @Before
    public void setUp() {
        option1 = new ModelParameterValueOption();
        option1.setId(1);
        option1.setName("option1");

        option2 = new ModelParameterValueOption();
        option2.setId(2);
        option2.setName("option2");

        option3 = new ModelParameterValueOption();
        option3.setId(3);
        option3.setName("option3");

        expectedOption = new ModelParameterValueOption();
        expectedOption.setId(4);
        expectedOption.setName("optionExpected");
    }

    @Test
    public void filterOutWrongBrandLines_AllBrandLinesInFullList() {
        List<ModelParameterValueOption> fullList = asList(option1, option2, expectedOption, option3);
        List<Integer> brandList = singletonList(expectedOption.getId());

        ModelParameter parameter = new ModelParameter();
        parameter.setParamId(VENDOR_LINES.paramId());
        parameter.setXslName(VENDOR_LINES.xslName());
        parameter.setOptions(fullList);

        List<ModelParameter> filteredParameters = ModelEditorUtils.filterOutWrongOptionsForParam(VENDOR_LINES.xslName(), singletonList(parameter), brandList);
        assertThat("Parameters list has been dropped.", filteredParameters, is(not(empty())));

        List<ModelParameterValueOption> actual = emptyList();
        for (ModelParameter modelParameter : filteredParameters) {
            if (VENDOR_LINES.xslName().equals(modelParameter.getXslName())) {
                actual = modelParameter.getOptions();
            }
        }

        assertThat("No brand lines left", actual, hasItem(expectedOption));
        assertThat("Too many lines left.", actual, hasSize(1));
    }

    @Test
    public void filterOutWrongBrandLines_NoBrandLinesInFullList() {
        List<ModelParameterValueOption> fullList = asList(option1, option2, option3);
        List<Integer> brandList = singletonList(expectedOption.getId());

        ModelParameter parameter = new ModelParameter();
        parameter.setParamId(VENDOR_LINES.paramId());
        parameter.setXslName(VENDOR_LINES.xslName());
        parameter.setOptions(fullList);

        List<ModelParameter> filteredParameters = ModelEditorUtils.filterOutWrongOptionsForParam(VENDOR_LINES.xslName(), singletonList(parameter), brandList);

        List<ModelParameterValueOption> actual = emptyList();
        for (ModelParameter modelParameter : filteredParameters) {
            if (VENDOR_LINES.xslName().equals(modelParameter.getXslName())) {
                actual = modelParameter.getOptions();
            }
        }

        assertThat("Brand line has not been filtered out", actual, not(hasItem(expectedOption)));
        assertThat("Too many lines left.", actual, is(empty()));
    }

    @Test
    public void draftDescriptionShouldBeEditableIfDescriptionIsEmpty() {
        ModelParameter descriptionParam = buildParameter(DESCRIPTION);
        ModelParameter draftDescriptionParam = buildParameter(DRAFT_DESCRIPTION);
        draftDescriptionParam.setValue(singletonList("draft"));
        ModelParameter descriptionMeta = buildParameter(DESCRIPTION);
        descriptionMeta.setEditable(false);
        ModelParameter draftDescriptionMeta = buildParameter(DRAFT_DESCRIPTION);
        draftDescriptionMeta.setEditable(true);

        List<ModelParameter> parameters = ModelEditorUtils.mergeParametersWithMeta(Arrays.asList(descriptionParam, draftDescriptionParam), Arrays.asList(descriptionMeta, draftDescriptionMeta));
        assertTrue("draft_description is not editable", parameters.stream().filter(p -> DRAFT_DESCRIPTION.xslName().equals(p.getXslName())).findFirst().map(ModelParameter::isEditable).orElse(false));
    }

    @Test
    public void draftDescriptionShouldBeAbsentIfDescriptionIsNotEmpty() {
        ModelParameter descriptionParam = buildParameter(DESCRIPTION);
        descriptionParam.setValue(singletonList("description"));
        ModelParameter draftDescriptionParam = buildParameter(DRAFT_DESCRIPTION);
        draftDescriptionParam.setValue(singletonList("draft"));
        ModelParameter descriptionMeta = buildParameter(DESCRIPTION);
        descriptionMeta.setEditable(false);
        ModelParameter draftDescriptionMeta = buildParameter(DRAFT_DESCRIPTION);
        draftDescriptionMeta.setEditable(true);

        List<ModelParameter> parameters = ModelEditorUtils.mergeParametersWithMeta(Arrays.asList(descriptionParam, draftDescriptionParam), Arrays.asList(descriptionMeta, draftDescriptionMeta));
        assertFalse("draft_description is present", parameters.stream().anyMatch(p -> DRAFT_DESCRIPTION.xslName().equals(p.getXslName())));
    }

    @Test
    public void modelSkuNameShouldNotExceed100Chars() {
        ModelParameter parameter = new ModelParameter();
        parameter.setParamId(VENDOR_CODE.paramId());
        parameter.setXslName(VENDOR_CODE.xslName());
        parameter.setValue(
                singletonList(
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value. " +
                                "Some kind of a very long parameter value."));

        ModelSku modelSku = new ModelSku();
        modelSku.setParameters(singletonList(parameter));
        String skuName = ModelEditorUtils.generateSkuName(modelSku);
        assertTrue(skuName.length() <= 1000);
    }

    private static ModelParameter buildParameter(MboParam mboParam) {
        ModelParameter parameter = new ModelParameter();
        parameter.setParamId(mboParam.paramId());
        parameter.setXslName(mboParam.xslName());

        return parameter;
    }
}