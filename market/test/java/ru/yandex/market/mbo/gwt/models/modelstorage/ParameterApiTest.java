package ru.yandex.market.mbo.gwt.models.modelstorage;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author V.Zaytsev (breezzo@yandex-team.ru)
 * @since 21.03.2017
 */
public class ParameterApiTest {

    private static final int PARAM_ID3 = 3;

    @Test(expected = IllegalArgumentException.class)
    public void putMustWorkThrowExceptionIfParamValuesHasNoParamId() {
        CommonModel model = new CommonModel();
        ParameterValues parameterValues = new ParameterValues();
        model.putParameterValues(parameterValues);
    }

    @Test
    public void putAndGetSingleShouldWorkCorrectly() {
        CommonModel model = new CommonModel();
        ParameterValue original = ParameterValueTestHelper.numeric();
        model.putParameterValues(ParameterValues.of(original));

        ParameterValue byId = model.getSingleParameterValue(original.getParamId());
        ParameterValue byXslName = model.getSingleParameterValue(original.getXslName());

        Assert.assertEquals(original, byId);
        Assert.assertEquals(original, byXslName);
    }

    @Test
    public void getParameterValuesForSingleShouldReturnListWithOneElement() {
        CommonModel model = new CommonModel();
        ParameterValue original = ParameterValueTestHelper.numeric();
        model.putParameterValues(ParameterValues.of(original));

        ParameterValues byId = model.getParameterValues(original.getParamId());
        ParameterValues byXslName = model.getParameterValues(original.getXslName());

        Assert.assertEquals(1, byId.getValues().size());
        Assert.assertEquals(1, byXslName.getValues().size());
    }

    @Test
    public void getSingleAfterAddSingleWhenNoValuesShouldWorkCorrectly() {
        CommonModel model = new CommonModel();
        ParameterValue original = ParameterValueTestHelper.numeric();
        model.addParameterValue(original);

        ParameterValue byId = model.getSingleParameterValue(original.getParamId());
        ParameterValue byXslName = model.getSingleParameterValue(original.getXslName());

        Assert.assertEquals(original, byId);
        Assert.assertEquals(original, byXslName);
    }

    @Test
    public void getValuesAfterAddSingleWhenNoValuesShouldReturnListWithOneElement() {
        CommonModel model = new CommonModel();
        ParameterValue original = ParameterValueTestHelper.numeric();
        model.addParameterValue(original);

        ParameterValues byId = model.getParameterValues(original.getParamId());
        ParameterValues byXslName = model.getParameterValues(original.getXslName());

        Assert.assertEquals(1, byId.getValues().size());
        Assert.assertEquals(1, byXslName.getValues().size());
    }

    @Test(expected = IllegalStateException.class)
    public void getSingleByIdOnManyValuesShouldThrowException() {
        CommonModel model = new CommonModel();
        ParameterValue original = ParameterValueTestHelper.numeric();
        model.addParameterValue(original);
        model.addParameterValue(original);

        model.getSingleParameterValue(original.getParamId());
    }

    @Test(expected = IllegalStateException.class)
    public void getSingleByXslNameOnManyValuesShouldThrowException() {
        CommonModel model = new CommonModel();
        ParameterValue original = ParameterValueTestHelper.numeric();
        model.addParameterValue(original);
        model.addParameterValue(original);

        model.getSingleParameterValue(original.getXslName());
    }

    @Test
    public void getSingleAndManyOnNoValuesShouldReturnNull() {
        CommonModel model = new CommonModel();
        Assert.assertNull(model.getSingleParameterValue(1L));
        Assert.assertNull(model.getSingleParameterValue("123"));
        Assert.assertNull(model.getParameterValues(1L));
        Assert.assertNull(model.getParameterValues("123"));
    }

    @Test
    public void addSingleAfterPutSingleShouldAddValue() {
        CommonModel model = new CommonModel();
        ParameterValue original = ParameterValueTestHelper.numeric();
        model.putParameterValues(ParameterValues.of(original));
        model.addParameterValue(original);
        Assert.assertEquals(2, model.getParameterValues(original.getParamId()).getValues().size());
    }

    @Test
    public void getParametersValuesShouldReturnAllValues() {
        CommonModel model = new CommonModel();
        ParameterValue first = ParameterValueTestHelper.numeric(1, "first");
        ParameterValue second = ParameterValueTestHelper.numeric(2, "second");
        List<ParameterValue> thirdValues = Arrays.asList(
            ParameterValueTestHelper.numeric(PARAM_ID3, "third"), ParameterValueTestHelper.numeric(PARAM_ID3, "third")
        );

        model.putParameterValues(ParameterValues.of(first));
        model.putParameterValues(ParameterValues.of(second));
        model.addParameterValues(thirdValues);

        Collection<ParameterValues> allValues = model.getParameterValues();

        final int expectedSize = 3;
        Assert.assertEquals(expectedSize, allValues.size());

        Map<Long, List<ParameterValue>> grouped = allValues.stream()
            .map(ParameterValues::getValues)
            .flatMap(Collection::stream)
            .collect(Collectors.groupingBy(ParameterValue::getParamId));

        Assert.assertEquals(Collections.singletonList(first), grouped.get(1L));
        Assert.assertEquals(Collections.singletonList(second), grouped.get(2L));
        Assert.assertEquals(thirdValues, grouped.get((long) PARAM_ID3));
    }

    @Test
    public void getFlatParametersValuesShouldReturnAllValues() {
        CommonModel model = new CommonModel();
        ParameterValue first = ParameterValueTestHelper.numeric(1, "first");
        ParameterValue second = ParameterValueTestHelper.numeric(2, "second");
        List<ParameterValue> thirdValues = Arrays.asList(
            ParameterValueTestHelper.numeric(PARAM_ID3, "third"), ParameterValueTestHelper.numeric(PARAM_ID3, "third")
        );

        model.putParameterValues(ParameterValues.of(first));
        model.putParameterValues(ParameterValues.of(second));
        model.addParameterValues(thirdValues);

        Collection<ParameterValue> allValues = model.getFlatParameterValues();

        final int expectedSize = 4;
        Assert.assertEquals(expectedSize, allValues.size());

        Map<Long, List<ParameterValue>> grouped = allValues.stream()
            .collect(Collectors.groupingBy(ParameterValue::getParamId));

        Assert.assertEquals(Collections.singletonList(first), grouped.get(1L));
        Assert.assertEquals(Collections.singletonList(second), grouped.get(2L));
        Assert.assertEquals(thirdValues, grouped.get((long) PARAM_ID3));
    }

    @Test
    public void getAliasesOnManyValuesShouldReturnAllStrings() {
        CommonModel model = new CommonModel();
        String[] originalAliases = new String[] {"a", "b", "c"};
        model.addParameterValue(
            ParameterValueTestHelper.string(1, XslNames.ALIASES, WordUtil.defaultWords(originalAliases[0]))
        );
        model.addParameterValue(
            ParameterValueTestHelper.string(
                1, XslNames.ALIASES, WordUtil.defaultWords(originalAliases[1], originalAliases[2])
            )
        );

        List<String> aliases = model.getAliases().values().stream()
            .flatMap(Collection::stream)
            .map(Word::getWord)
            .collect(Collectors.toList());

        Assert.assertEquals(originalAliases.length, aliases.size());
        Assert.assertTrue(Arrays.stream(originalAliases).allMatch(aliases::contains));

    }

    @Test(expected = IllegalStateException.class)
    public void getVendorIdOnManyValuesShouldThrowException() {
        CommonModel model = new CommonModel();
        ParameterValue vendorValue = new ParameterValue(
            1, XslNames.VENDOR, Param.Type.ENUM, null, null, 1L, null, null
        );
        model.addParameterValue(vendorValue);
        model.addParameterValue(vendorValue);

        model.getVendorId();
    }

    @Test(expected = IllegalStateException.class)
    public void getTitleOnManyValuesShouldThrowException() {
        CommonModel model = new CommonModel();
        model.addParameterValue(
            ParameterValueTestHelper.string(1L, XslNames.NAME, WordUtil.defaultWords("model1"))
        );
        model.addParameterValue(
            ParameterValueTestHelper.string(1L, XslNames.NAME, WordUtil.defaultWords("model2"))
        );

        model.getTitle();
    }

    @Test(expected = IllegalStateException.class)
    public void getDescriptionOnManyValuesShouldThrowException() {
        CommonModel model = new CommonModel();
        model.addParameterValue(
            ParameterValueTestHelper.string(1L, XslNames.DESCRIPTION, WordUtil.defaultWords("model1"))
        );
        model.addParameterValue(
            ParameterValueTestHelper.string(1L, XslNames.DESCRIPTION, WordUtil.defaultWords("model2"))
        );

        model.getDescription();
    }
}
