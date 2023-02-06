package ru.yandex.market.mbo.db.modelstorage.guru;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModelUtils;
import ru.yandex.market.mbo.gwt.models.modelstorage.EnumValueAlias;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@SuppressWarnings("checkstyle:MagicNumber")
public class MoveSkusHelperTest {

    private static final long TASTE_PARAM_ID = 764010L;
    private static final long SMELL_PARAM_ID = 300929L;

    private static final long TASTE_CHICKEN = 80001L;
    private static final long TASTE_GOOSE = 80002L;
    private static final long TASTE_TOMATOE = 60001L;
    private static final long TASTE_KETCHUP = 60002L;

    private static final long SMELL_SOCKS = 20001L;
    private static final long SMELL_FEET = 20002L;

    private static int sequencer = 0;

    private CommonModel source;
    private CommonModel target;
    private List<CommonModel> skus;
    private CategoryParam taste;
    private CategoryParam smell;

    @Before
    public void setup() {
        skus = new ArrayList<>(2);
        source = CommonModelBuilder.newBuilder(1L, 9000L)
            .source(CommonModel.Source.GURU)
            .endModel();
        target = CommonModelBuilder.newBuilder(2L, 9000L)
            .source(CommonModel.Source.GURU)
            .endModel();
        skus.add(CommonModelBuilder.newBuilder(10001L, 9000L)
            .source(CommonModel.Source.SKU)
            .endModel()
        );
        skus.add(CommonModelBuilder.newBuilder(10002L, 9000L)
            .source(CommonModel.Source.SKU)
            .endModel()
        );
        taste = CategoryParamBuilder.newBuilder()
            .setId(TASTE_PARAM_ID)
            .setCategoryHid(9000L)
            .setType(Param.Type.ENUM)
            .setXslName("Taste")
            .addOption(OptionBuilder.newBuilder().setId(TASTE_CHICKEN))
            .addOption(OptionBuilder.newBuilder().setId(TASTE_GOOSE))
            .addOption(OptionBuilder.newBuilder().setId(TASTE_TOMATOE))
            .addOption(OptionBuilder.newBuilder().setId(TASTE_KETCHUP))
            .build();
        smell = CategoryParamBuilder.newBuilder()
            .setId(SMELL_PARAM_ID)
            .setCategoryHid(9000L)
            .setType(Param.Type.ENUM)
            .setXslName("Smell")
            .addOption(OptionBuilder.newBuilder().setId(SMELL_SOCKS))
            .addOption(OptionBuilder.newBuilder().setId(SMELL_FEET))
            .build();
    }

    @Test
    public void testMoveEmpty() {
        MoveSkusHelper.moveAliases(skus, source, target);
        MoveSkusHelper.movePickers(skus, source, target, false);
        assertThat(source.getParameterValueLinks()).isEmpty();
        assertThat(target.getParameterValueLinks()).isEmpty();
        assertThat(source.getEnumValueAliases()).isEmpty();
        assertThat(target.getEnumValueAliases()).isEmpty();
    }

    @Test
    public void testMoveAliases() {
        addAliases(source, taste, TASTE_CHICKEN, TASTE_GOOSE);
        addAliases(source, taste, TASTE_KETCHUP, TASTE_TOMATOE);
        MoveSkusHelper.moveAliases(skus, source, target);
        assertThat(source.getEnumValueAliases()).containsExactlyInAnyOrder(
            alias(taste, TASTE_CHICKEN, TASTE_GOOSE),
            alias(taste, TASTE_KETCHUP, TASTE_TOMATOE)
        );
        assertThat(target.getEnumValueAliases()).isEmpty();

        addParameterValue(skus.get(0), taste, TASTE_CHICKEN);
        addParameterValue(skus.get(1), taste, TASTE_KETCHUP);
        MoveSkusHelper.moveAliases(skus, source, target);
        assertThat(source.getEnumValueAliases()).containsExactlyInAnyOrder(
            alias(taste, TASTE_CHICKEN, TASTE_GOOSE),
            alias(taste, TASTE_KETCHUP, TASTE_TOMATOE)
        );
        assertThat(target.getEnumValueAliases()).containsExactlyInAnyOrder(
            alias(taste, TASTE_CHICKEN, TASTE_GOOSE),
            alias(taste, TASTE_KETCHUP, TASTE_TOMATOE)
        );
    }

    @Test
    public void testMoveAliasesOptionDoesntMatchAlias() {
        addParameterValue(skus.get(0), taste, TASTE_CHICKEN);
        addAliases(source, taste, TASTE_KETCHUP, TASTE_TOMATOE);
        MoveSkusHelper.moveAliases(skus, source, target);
        assertThat(source.getEnumValueAliases()).containsExactlyInAnyOrder(
            alias(taste, TASTE_KETCHUP, TASTE_TOMATOE)
        );
        assertThat(target.getEnumValueAliases()).isEmpty();
    }

    @Test
    public void testMoveAliasesSameOptionAdded() {
        addParameterValue(skus.get(0), taste, TASTE_CHICKEN);
        addAliases(source, taste, TASTE_CHICKEN, TASTE_GOOSE);
        addAliases(source, taste, TASTE_CHICKEN, TASTE_TOMATOE);
        addAliases(target, taste, TASTE_CHICKEN, TASTE_GOOSE);
        MoveSkusHelper.moveAliases(skus, source, target);
        assertThat(source.getEnumValueAliases()).containsExactlyInAnyOrder(
            alias(taste, TASTE_CHICKEN, TASTE_GOOSE),
            alias(taste, TASTE_CHICKEN, TASTE_TOMATOE)
        );
        assertThat(target.getEnumValueAliases()).containsExactlyInAnyOrder(
            alias(taste, TASTE_CHICKEN, TASTE_GOOSE),
            alias(taste, TASTE_CHICKEN, TASTE_TOMATOE)
        );
    }

    @Test
    public void testMoveAliasesMultipleParams() {
        addParameterValue(skus.get(0), taste, TASTE_CHICKEN);
        addParameterValue(skus.get(1), smell, SMELL_SOCKS);
        addAliases(source, taste, TASTE_CHICKEN, TASTE_GOOSE);
        addAliases(source, taste, TASTE_CHICKEN, TASTE_TOMATOE);
        addAliases(source, smell, SMELL_SOCKS, SMELL_FEET);
        addAliases(target, taste, TASTE_CHICKEN, TASTE_GOOSE);
        MoveSkusHelper.moveAliases(skus, source, target);
        assertThat(source.getEnumValueAliases()).containsExactlyInAnyOrder(
            alias(taste, TASTE_CHICKEN, TASTE_GOOSE),
            alias(taste, TASTE_CHICKEN, TASTE_TOMATOE),
            alias(smell, SMELL_SOCKS, SMELL_FEET)
        );
        assertThat(target.getEnumValueAliases()).containsExactlyInAnyOrder(
            alias(taste, TASTE_CHICKEN, TASTE_GOOSE),
            alias(taste, TASTE_CHICKEN, TASTE_TOMATOE),
            alias(smell, SMELL_SOCKS, SMELL_FEET)
        );
    }

    @Test
    public void testMovePickersSimple() {
        ParameterValue link1 = addPicker(source, taste, TASTE_CHICKEN);
        ParameterValue link2 = addPicker(source, taste, TASTE_KETCHUP);
        MoveSkusHelper.movePickers(skus, source, target, false);
        assertThat(source.getParameterValueLinks()).containsExactlyInAnyOrder(link1, link2);
        assertThat(target.getEnumValueAliases()).isEmpty();

        addParameterValue(skus.get(0), taste, TASTE_CHICKEN);
        addParameterValue(skus.get(1), taste, TASTE_KETCHUP);
        MoveSkusHelper.movePickers(skus, source, target, false);
        assertThat(source.getParameterValueLinks()).containsExactlyInAnyOrder(link1, link2);
        assertThat(target.getParameterValueLinks()).containsExactlyInAnyOrder(link1, link2);
    }

    @Test
    public void testMovePickersNoOverwrite() {
        ParameterValue link1 = addPicker(source, taste, TASTE_CHICKEN);
        ParameterValue link2 = addPicker(source, taste, TASTE_KETCHUP);
        ParameterValue link3 = addPicker(target, taste, TASTE_CHICKEN);
        assertNotEquals(link1, link3); //пикеры одной и той же опции гарантированно разные на двух моделях
        addParameterValue(skus.get(0), taste, TASTE_CHICKEN);
        addParameterValue(skus.get(1), taste, TASTE_KETCHUP);

        MoveSkusHelper.movePickers(skus, source, target, false);
        assertThat(source.getParameterValueLinks()).containsExactlyInAnyOrder(link1, link2);
        assertThat(target.getParameterValueLinks()).containsExactlyInAnyOrder(link2, link3);
    }

    @Test
    public void testMovePickersDoOverwrite() {
        ParameterValue link1 = addPicker(source, taste, TASTE_CHICKEN);
        ParameterValue link2 = addPicker(source, taste, TASTE_KETCHUP);
        ParameterValue link3 = addPicker(target, taste, TASTE_CHICKEN);
        assertNotEquals(link1, link3); //пикеры одной и той же опции гарантированно разные на двух моделях
        addParameterValue(skus.get(0), taste, TASTE_CHICKEN);
        addParameterValue(skus.get(1), taste, TASTE_KETCHUP);

        MoveSkusHelper.movePickers(skus, source, target, true);
        assertThat(source.getParameterValueLinks()).containsExactlyInAnyOrder(link1, link2);
        assertThat(target.getParameterValueLinks()).containsExactlyInAnyOrder(link1, link2);
    }

    @Test
    public void testMovePickersOptionDoesntMatchPicker() {
        ParameterValue link1 = addPicker(source, taste, TASTE_CHICKEN);
        ParameterValue link2 = addPicker(source, taste, TASTE_KETCHUP);
        addParameterValue(skus.get(0), taste, TASTE_TOMATOE);
        MoveSkusHelper.movePickers(skus, source, target, true);
        assertThat(source.getParameterValueLinks()).containsExactlyInAnyOrder(link1, link2);
        assertThat(target.getParameterValueLinks()).isEmpty();
    }

    @Test
    public void testRelationAddOperations() {
        MoveSkusHelper.addSkuParentRelation(skus.get(0), source.getId());
        MoveSkusHelper.addSkuParentRelation(skus.get(1), source.getId());
        assertEquals(source.getId(), CommonModelUtils.getSkuParentId(skus.get(0)));
        assertEquals(source.getId(), CommonModelUtils.getSkuParentId(skus.get(1)));

        MoveSkusHelper.addSkuRelations(source, skus.stream().map(CommonModel::getId).collect(Collectors.toList()));
        assertThat(CommonModelUtils.getModelSkuIds(source)).containsExactlyInAnyOrder(
            skus.get(0).getId(),
            skus.get(1).getId()
        );
    }

    @Test
    public void testRelationRemoveOperations() {
        MoveSkusHelper.addSkuParentRelation(skus.get(0), source.getId());
        MoveSkusHelper.addSkuParentRelation(skus.get(1), source.getId());
        MoveSkusHelper.addSkuRelations(source, skus.stream().map(CommonModel::getId).collect(Collectors.toList()));
        //---//

        MoveSkusHelper.removeSkuParentRelation(skus.get(0));
        MoveSkusHelper.removeSkuParentRelation(skus.get(1));
        assertEquals(CommonModel.NO_ID, CommonModelUtils.getSkuParentId(skus.get(0)));
        assertEquals(CommonModel.NO_ID, CommonModelUtils.getSkuParentId(skus.get(1)));
        MoveSkusHelper.removeSkuRelations(source, skus.stream().map(CommonModel::getId).collect(Collectors.toList()));
        assertThat(CommonModelUtils.getModelSkuIds(source)).isEmpty();
    }

    private void addAliases(CommonModel model, CategoryParam param, long optionId, long... aliasIds) {
        for (long aliasId : aliasIds) {
            model.addEnumValueAlias(alias(param, optionId, aliasId));
        }
    }

    private ParameterValue addPicker(CommonModel model, CategoryParam param, long optionId) {
        ParameterValue link = new ParameterValue(param, optionId);
        link.setPickerImage(new PickerImage());
        link.getPickerImage().setImageName(String.valueOf(sequencer++));
        model.addParameterValueLink(link);
        return link;
    }

    private void addParameterValue(CommonModel sku, CategoryParam param, long optionId) {
        sku.addParameterValue(new ParameterValue(param, optionId));
    }

    private EnumValueAlias alias(CategoryParam param, long optionId, long aliasId) {
        return new EnumValueAlias(param.getId(), param.getXslName(), optionId, aliasId);
    }
}
