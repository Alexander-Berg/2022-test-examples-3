package ru.yandex.market.mbo.export.modelstorage.pipe;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.core.title.BlueGroupingTitleGenerator;
import ru.yandex.market.mbo.export.modelstorage.BlueGroupingPipePart;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.models.titlemaker.ModelTitle;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 13.09.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class BlueGroupingPipeTest {

    private ModelPipeContextTestGenerator generator;

    @Before
    public void setUp() {
        generator = new ModelPipeContextTestGenerator(true);
    }

    @Test
    public void testNoParams() throws IOException {
        BlueGroupingTitleGenerator titleGenerator = new BlueGroupingTitleGenerator("blueGroupingTemplate",
            "guruTemplate", Collections.emptyList(), Collections.emptyList());
        BlueGroupingPipePart pipePart = new BlueGroupingPipePart(Collections.emptyList(), titleGenerator);

        ModelStorage.Model model = generator.createGuru(true);

        ModelPipeContext context =
            new ModelPipeContext(model, Collections.emptyList(),
                Arrays.asList(createSkuFor(model, true),
                    createSkuFor(model, true)));
        pipePart.acceptModelsGroup(context);
        assertEquals(0, context.getModel().getBlueUngroupingInfosCount());
    }

    @Test
    public void testSimple() throws IOException {

        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku1 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 100))
            .build();

        ModelStorage.Model sku2 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 100))
            .build();

        ModelStorage.Model sku3 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 105))
            .build();

        ModelStorage.Model modification = generator.createModificationFor(model, true);

        ModelStorage.Model modificationSku = createSkuFor(modification, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 110))
            .build();

        ModelPipeContext context = new ModelPipeContext(model,
            Collections.singletonList(modification),
            Arrays.asList(sku1, sku2, sku3, modificationSku));
        pipePart.acceptModelsGroup(context);

        context.getModel().getBlueUngroupingInfosList().forEach(ui -> {
            assertEquals(1, ui.getParameterValuesCount());
        });

        List<String> titles = context.getModel().getBlueUngroupingInfosList().stream()
            .map(u -> u.getTitle())
            .collect(Collectors.toList());

        assertEquals(2, titles.size());

        assertThat(titles, containsInAnyOrder("100", "105"));
        List<ModelStorage.UngroupingInfo> infos = new ArrayList<>(context.getModel().getBlueUngroupingInfosList());
        assertEquals(100, infos.get(0).getParameterValues(0).getOptionId());
        assertEquals(105, infos.get(1).getParameterValues(0).getOptionId());

        assertEquals(1, context.getModifications().iterator().next().getBlueUngroupingInfosCount());
    }

    @Test
    public void testSkipDependencyRule() throws IOException {

        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku1 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 100).toBuilder()
                .setValueSource(ModelStorage.ModificationSource.DEPENDENCY_RULE)
            )
            .addParameterValues(generator.enumParam("1", 1L, 101))
            .build();

        ModelStorage.Model sku2 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 105).toBuilder()
                .setValueSource(ModelStorage.ModificationSource.DEPENDENCY_RULE)
            )
            .addParameterValues(generator.enumParam("1", 1L, 101))
            .build();

        ModelPipeContext context = new ModelPipeContext(model,
            Collections.emptyList(),
            Arrays.asList(sku1, sku2));
        pipePart.acceptModelsGroup(context);

        assertEquals(1, context.getModel().getBlueUngroupingInfosCount());

        assertEquals(101, context.getModel().getBlueUngroupingInfos(0)
            .getParameterValues(0).getOptionId());
    }

    @Test
    public void testSkipMultivalue() throws IOException {

        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku1 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 101))
            .addParameterValues(generator.enumParam("1", 1L, 105))
            .build();

        ModelPipeContext context = new ModelPipeContext(model,
            Collections.emptyList(),
            Arrays.asList(sku1));
        pipePart.acceptModelsGroup(context);

        assertEquals(0, context.getModel().getBlueUngroupingInfosCount());
    }

    @Test
    public void testSkipUnpublished() throws IOException {

        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku1 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 100))
            .build();

        ModelStorage.Model sku2 = createSkuFor(model, false)
            .toBuilder()
            .addParameterValues(generator.enumParam("2", 2L, 102))
            .build();

        ModelPipeContext context = new ModelPipeContext(model,
            Collections.emptyList(),
            Arrays.asList(sku1, sku2));

        pipePart.acceptModelsGroup(context);

        assertEquals(1, context.getModel().getBlueUngroupingInfosCount());
    }

    @Test
    public void testParamsOrdering() throws IOException {
        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true),
            param(2L, "2", true),
            param(3L, "3", false)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku1 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 100))
            .addParameterValues(generator.enumParam("2", 2L, 102))
            .build();
        //reverse order
        ModelStorage.Model sku2 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("2", 2L, 102))
            .addParameterValues(generator.enumParam("1", 1L, 100))
            .build();

        ModelPipeContext context = new ModelPipeContext(model,
            Collections.emptyList(), Arrays.asList(sku1, sku2));

        pipePart.acceptModelsGroup(context);

        assertEquals(1, context.getModel().getBlueUngroupingInfosCount());
        assertEquals(2, context.getModel().getBlueUngroupingInfos(0).getParameterValuesCount());
        assertEquals("100,102", context.getModel().getBlueUngroupingInfos(0).getTitle());
    }

    @Test
    public void testNumeric() throws IOException {
        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true),
            param(2L, "2", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku1 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 100))
            .addParameterValues(generator.numericParam("2", 2L, new BigDecimal(1000)))
            .build();
        ModelStorage.Model sku2 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 100))
            .addParameterValues(generator.numericParam("2", 2L, new BigDecimal(1000)))
            .build();

        ModelPipeContext context = new ModelPipeContext(model,
            Collections.emptyList(), Arrays.asList(sku1, sku2));

        pipePart.acceptModelsGroup(context);

        assertEquals(1, context.getModel().getBlueUngroupingInfosCount());
        assertEquals(2, context.getModel().getBlueUngroupingInfos(0)
            .getParameterValuesCount());

        ModelStorage.UngroupingInfo ungroupingInfo = context.getModel().getBlueUngroupingInfos(0);
        assertEquals("100,1000", ungroupingInfo.getTitle());
        assertEquals(100, ungroupingInfo.getParameterValues(0).getOptionId());
        assertFalse(ungroupingInfo.getParameterValues(0).hasNumericValue());
        assertEquals("1000", ungroupingInfo.getParameterValues(1).getNumericValue());
        assertFalse(ungroupingInfo.getParameterValues(1).hasOptionId());
    }

    @Test
    public void testErrorTitles() throws IOException {

        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1L", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, true);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 100))
            .build();

        ModelPipeContext context = new ModelPipeContext(model, Collections.emptyList(), Collections.singletonList(sku));
        pipePart.acceptModelsGroup(context);

        assertEquals(0, context.getModel().getBlueUngroupingInfosCount());
    }

    @Test
    public void testTwoCommonUngroupingParameters() throws IOException {
        //sku  |param1|param2
        //sku1 |1     |11
        //sku2 |2     |12
        // should be ungrouped: 1,11; 2,12
        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true),
            param(2L, "2", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku1 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 1))
            .addParameterValues(generator.enumParam("2", 2L, 11))
            .build();

        ModelStorage.Model sku2 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 2))
            .addParameterValues(generator.enumParam("2", 2L, 12))
            .build();


        ModelPipeContext context = new ModelPipeContext(model,
            Collections.emptyList(),
            Arrays.asList(sku1, sku2));
        pipePart.acceptModelsGroup(context);

        context.getModel().getBlueUngroupingInfosList().forEach(ui -> {
            assertEquals(2, ui.getParameterValuesCount());
        });

        List<String> titles = context.getModel().getBlueUngroupingInfosList().stream()
            .map(ModelStorage.UngroupingInfo::getTitle)
            .collect(Collectors.toList());

        assertEquals(2, titles.size());

        assertThat(titles, containsInAnyOrder("1,11", "2,12"));
        List<ModelStorage.UngroupingInfo> infos = context.getModel().getBlueUngroupingInfosList().stream()
            .sorted(Comparator.comparing(ModelStorage.UngroupingInfo::getTitle))
            .collect(Collectors.toList());
        assertEquals(1, infos.get(0).getParameterValues(0).getOptionId());
        assertEquals(11, infos.get(0).getParameterValues(1).getOptionId());
        assertEquals(2, infos.get(1).getParameterValues(0).getOptionId());
        assertEquals(12, infos.get(1).getParameterValues(1).getOptionId());
    }

    @Test
    public void testOneCommonUngroupingParameter() throws IOException {
        //sku  |param1|param2
        //sku1 |1     |11
        //sku2 |2     |
        // should be ungrouped: 1; 2
        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true),
            param(2L, "2", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku1 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 1))
            .addParameterValues(generator.enumParam("2", 2L, 11))
            .build();

        ModelStorage.Model sku2 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 2))
            .build();


        ModelPipeContext context = new ModelPipeContext(model,
            Collections.emptyList(),
            Arrays.asList(sku1, sku2));
        pipePart.acceptModelsGroup(context);

        context.getModel().getBlueUngroupingInfosList().forEach(ui -> {
            assertEquals(1, ui.getParameterValuesCount());
        });

        List<String> titles = context.getModel().getBlueUngroupingInfosList().stream()
            .map(ModelStorage.UngroupingInfo::getTitle)
            .collect(Collectors.toList());

        assertEquals(2, titles.size());

        assertThat(titles, containsInAnyOrder("1", "2"));
        List<ModelStorage.UngroupingInfo> infos = context.getModel().getBlueUngroupingInfosList().stream()
            .sorted(Comparator.comparing(ModelStorage.UngroupingInfo::getTitle))
            .collect(Collectors.toList());
        assertEquals(1, infos.get(0).getParameterValues(0).getOptionId());
        assertEquals(2, infos.get(1).getParameterValues(0).getOptionId());
    }

    @Test
    public void testZeroCommonUngroupingParameter() throws IOException {
        //sku  |param1|param2
        //sku1 |1     |
        //sku2 |      |12
        // should not be ungrouped
        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true),
            param(2L, "2", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model model = generator.createGuru(true);

        ModelStorage.Model sku1 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 1))
            .build();

        ModelStorage.Model sku2 = createSkuFor(model, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("2", 2L, 12))
            .build();


        ModelPipeContext context = new ModelPipeContext(model,
            Collections.emptyList(),
            Arrays.asList(sku1, sku2));
        pipePart.acceptModelsGroup(context);

        List<String> titles = context.getModel().getBlueUngroupingInfosList().stream()
            .map(ModelStorage.UngroupingInfo::getTitle)
            .collect(Collectors.toList());

        assertEquals(0, titles.size());
    }

    @Test
    public void testSkipPskusEvenWhichHaveUngroupingParam() throws IOException {
        //psku  |param1|param2
        //psku1 |1     |11
        //psku2 |2     |
        // should be ignored
        List<ForTitleParameter> forTitleParameters = params(
            param(1L, "1", true),
            param(2L, "2", true)
        );

        BlueGroupingPipePart pipePart = initPart(forTitleParameters, false);

        ModelStorage.Model pModel = generator.createModel(CommonModel.Source.PARTNER, true);

        ModelStorage.Model psku1 = generator.createSkuFor(CommonModel.Source.PARTNER_SKU, pModel, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 1))
            .addParameterValues(generator.enumParam("2", 2L, 11))
            .build();

        ModelStorage.Model psku2 = generator.createSkuFor(CommonModel.Source.PARTNER_SKU, pModel, true)
            .toBuilder()
            .addParameterValues(generator.enumParam("1", 1L, 2))
            .build();


        ModelPipeContext context = new ModelPipeContext(pModel,
            Collections.emptyList(),
            Arrays.asList(psku1, psku2));
        pipePart.acceptModelsGroup(context);

        List<String> titles = context.getModel().getBlueUngroupingInfosList().stream()
            .map(ModelStorage.UngroupingInfo::getTitle)
            .collect(Collectors.toList());

        assertEquals(0, titles.size());
    }

    private BlueGroupingPipePart initPart(List<ForTitleParameter> forTitleParameters, boolean returnErrors) {
        BlueGroupingTitleGenerator titleGenerator = mockGenerator(forTitleParameters, returnErrors);
        return new BlueGroupingPipePart(forTitleParameters, titleGenerator);
    }

    private ModelStorage.Model createSkuFor(ModelStorage.Model model, boolean publishedOnBlueMarket) {
        return generator.createSkuFor(model, true).toBuilder()
            .setPublishedOnBlueMarket(publishedOnBlueMarket)
            .build();
    }

    private List<ForTitleParameter> params(ForTitleParameter... params) {
        List<ForTitleParameter> result = Arrays.asList(params);
        int i = 0;
        for (ForTitleParameter param : result) {
            param.setModelFilterIndex(i++);
        }
        return result;
    }

    private ForTitleParameter param(Long id, String xslName, boolean ungrouping) {
        ForTitleParameter param = new ForTitleParameter();
        param.setId(id);
        param.setXslName(xslName);
        param.setBlueGrouping(ungrouping);
        return param;
    }

    private BlueGroupingTitleGenerator mockGenerator(List<ForTitleParameter> params, boolean returnErrors) {
        BlueGroupingTitleGenerator result = mock(BlueGroupingTitleGenerator.class);
        when(result.createTitle(any(ModelStorage.Model.Builder.class),
            any(ModelStorage.Model.Builder.class), Mockito.anyCollection())).then(invocation -> {
                    ModelStorage.Model.Builder sku = invocation.getArgument(0);
                    if (returnErrors) {
                        return ModelTitle.withError(sku.getId(), sku.getCurrentType(), "error");
                    }
                    Map<String, String> paramValues = new LinkedHashMap<>();
            Collection<String> forbiddenSkuParams = invocation.getArgument(2);
            params.stream()
                .filter(p -> !forbiddenSkuParams.contains(p.getXslName()))
                .forEach(p -> paramValues.put(p.getXslName(), null));
                    for (ModelStorage.ParameterValue parameterValue : sku.getParameterValuesList()) {
                        if (paramValues.keySet().contains(parameterValue.getXslName())) {
                            paramValues.put(parameterValue.getXslName(), extractValue(parameterValue));
                        }
                    }
                    String title = paramValues.entrySet().stream()
                        .filter(e -> e.getValue() != null)
                        .map(e -> e.getValue())
                        .collect(Collectors.joining(","));
                    return new ModelTitle(sku.getId(), sku.getCurrentType(), title);

        });

        return result;
    }

    private String extractValue(ModelStorage.ParameterValue parameterValue) {
        switch (parameterValue.getValueType()) {
            case BOOLEAN:
                return String.valueOf(parameterValue.getBoolValue());
            case NUMERIC:
                return parameterValue.getNumericValue();
            case ENUM:
            case NUMERIC_ENUM:
                return String.valueOf(parameterValue.getOptionId());
            default:
                return parameterValue.getStrValue(0).getValue();

        }
    }

}
