package ru.yandex.market.mbo.export.modelstorage.pipe;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.titlemaker.ForTitleParameter;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SetManufacturerPipePartTest {

    private final List<ForTitleParameter> categoriesWithManufacturerParam = new ArrayList<>();
    private static final Long MANUFACTURER_ID = 2L;

    private static final long PROTO_MODEL_VENDOR_ID = 1L;
    private final ModelStorage.Model protoModel =
        CommonModelBuilder.newBuilder(1, 1, PROTO_MODEL_VENDOR_ID)
            .currentType(CommonModel.Source.GURU)
            .getRawModel();

    @Before
    public void setUp() {
        ForTitleParameter forTitleParameter = new ForTitleParameter();
        forTitleParameter.setXslName(XslNames.MANUFACTURER);

        Option option = new OptionImpl();
        option.setId(MANUFACTURER_ID);

        forTitleParameter.setOverrideOptions(Collections.singletonList(option));

        categoriesWithManufacturerParam.add(forTitleParameter);
    }

    @Test
    public void testSettingManufacturerWithOverrideOption() throws IOException {
        categoriesWithManufacturerParam.clear();

        ForTitleParameter forTitleParameter = new ForTitleParameter();
        forTitleParameter.setXslName(XslNames.MANUFACTURER);

        Option option = new OptionImpl();
        option.setId(MANUFACTURER_ID);

        Option optionChild = new OptionImpl();
        optionChild.setId(MANUFACTURER_ID + 1);
        optionChild.setParent(option);

        forTitleParameter.setOverrideOptions(Collections.singletonList(optionChild));
        categoriesWithManufacturerParam.add(forTitleParameter);

        ValueLink valueLink = getValueLink(PROTO_MODEL_VENDOR_ID);
        List<ValueLink> valueLinks = Collections.singletonList(valueLink);

        SetManufacturerPipePart setManufacturerPipePart =
            new SetManufacturerPipePart(valueLinks, categoriesWithManufacturerParam);
        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.emptyList()
        );

        setManufacturerPipePart.acceptModelsGroup(modelPipeContext);

        assertFalse(modelPipeContext.getModel().getParameterValuesList().isEmpty());
        assertTrue(modelPipeContext.getModel().getParameterValuesList()
            .stream()
            .anyMatch(e -> e.getXslName().equals(XslNames.MANUFACTURER))
        );
        assertEquals(MANUFACTURER_ID.intValue() + 1, modelPipeContext.getModel().getParameterValuesList()
            .stream()
            .filter(e -> e.getXslName().equals(XslNames.MANUFACTURER))
            .findFirst().get().getOptionId()
        );
    }

    @Test
    public void testSettingManufacturerForGuruModel() throws IOException {
        ValueLink valueLink = getValueLink(PROTO_MODEL_VENDOR_ID);
        List<ValueLink> valueLinks = Collections.singletonList(valueLink);

        SetManufacturerPipePart setManufacturerPipePart =
            new SetManufacturerPipePart(valueLinks, categoriesWithManufacturerParam);
        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.emptyList()
        );

        setManufacturerPipePart.acceptModelsGroup(modelPipeContext);

        assertFalse(modelPipeContext.getModel().getParameterValuesList().isEmpty());
        assertTrue(modelPipeContext.getModel().getParameterValuesList()
            .stream()
            .anyMatch(e -> e.getXslName().equals(XslNames.MANUFACTURER))
        );
        assertEquals(MANUFACTURER_ID.intValue(), modelPipeContext.getModel().getParameterValuesList()
            .stream()
            .filter(e -> e.getXslName().equals(XslNames.MANUFACTURER))
            .findFirst().get().getOptionId()
        );
    }

    @Test
    public void testForCategoryWithoutManufacturerParam() throws IOException {
        ValueLink valueLink = getValueLink(PROTO_MODEL_VENDOR_ID);
        List<ValueLink> valueLinks = Collections.singletonList(valueLink);

        SetManufacturerPipePart setManufacturerPipePart =
            new SetManufacturerPipePart(valueLinks, Collections.emptyList());
        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.emptyList()
        );

        setManufacturerPipePart.acceptModelsGroup(modelPipeContext);

        assertTrue(modelPipeContext.getModel().getParameterValuesList()
            .stream()
            .noneMatch(e -> e.getXslName().equals(XslNames.MANUFACTURER))
        );
    }

    @Test
    public void testNotExistsValueLinkForModel() throws IOException {
        final long anotherVendorId = 3L;

        ValueLink valueLink = getValueLink(anotherVendorId);
        List<ValueLink> valueLinks = Collections.singletonList(valueLink);

        SetManufacturerPipePart setManufacturerPipePart =
            new SetManufacturerPipePart(valueLinks, categoriesWithManufacturerParam);
        ModelPipeContext modelPipeContext = new ModelPipeContext(
            protoModel, Collections.emptyList(), Collections.emptyList()
        );

        setManufacturerPipePart.acceptModelsGroup(modelPipeContext);

        assertTrue(modelPipeContext.getModel().getParameterValuesList()
            .stream()
            .noneMatch(e -> e.getXslName().equals(XslNames.MANUFACTURER))
        );
    }


    private ValueLink getValueLink(long vendorId) {
        ValueLink valueLink = new ValueLink();
        valueLink.setSourceOptionId(vendorId);
        valueLink.setSourceParamId(KnownIds.VENDOR_PARAM_ID);
        valueLink.setTargetOptionId(MANUFACTURER_ID);
        return valueLink;
    }

}
