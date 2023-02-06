package ru.yandex.market.mbo.db.modelstorage.utils;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("checkstyle:MagicNumber")
public class PreprocessingUtilsTest {
    public static final CommonModel R_1 = CommonModelBuilder.newBuilder(11L, 1L).getModel();
    public static final CommonModel R_2 = CommonModelBuilder.newBuilder(12L, 1L).getModel();
    public static final CommonModel R_3 = CommonModelBuilder.newBuilder(13L, 1L).getModel();
    public static final CommonModel R_4 = CommonModelBuilder.newBuilder(14L, 2L).getModel();
    public static final CommonModel R_5 = CommonModelBuilder.newBuilder(15L, 2L).getModel();
    public static final CommonModel R_6 = CommonModelBuilder.newBuilder(16L, 3L).getModel();

    public static final Function<ModelRelation, Boolean> RELATION_BOOLEAN_FUNCTION = relation -> {
        long modelId = relation.getId();
        long categoryId = relation.getCategoryId();
        return categoryId > 0 && categoryId < 4 && modelId > 9 && modelId < 17;
    };

    @Test
    public void clearBrokenRelationsAllValid() {
        CommonModel model = CommonModelBuilder.newBuilder()
            .withSkuRelations(R_1, R_2, R_3, R_4, R_5, R_6)
            .getModel();

        PreprocessingUtils.clearBrokenRelations(model, RELATION_BOOLEAN_FUNCTION);
        List<CommonModel> result = model.getRelations().stream()
            .map(relation -> CommonModelBuilder.newBuilder(relation.getId(), relation.getCategoryId()).getModel())
            .collect(Collectors.toList());
        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(R_1, R_2, R_3, R_4, R_5, R_6));
    }

    @Test
    public void clearBrokenRelationsAllValidCopySameTypeSameCategory() {
        CommonModel rd = CommonModelBuilder.newBuilder(13L, 1L).getModel();

        CommonModel model = CommonModelBuilder.newBuilder()
            .withSkuRelations(R_1, R_2, R_3, R_4, R_5, R_6)
            .withSkuRelations(rd)
            .getModel();

        PreprocessingUtils.clearBrokenRelations(model, RELATION_BOOLEAN_FUNCTION);
        List<CommonModel> result = model.getRelations().stream()
            .map(relation -> CommonModelBuilder.newBuilder(relation.getId(), relation.getCategoryId()).getModel())
            .collect(Collectors.toList());
        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(R_1, R_2, R_3, R_4, R_5, R_6));
    }

    @Test
    public void clearBrokenRelationsAllValidCopyDiffTypeSameValidCategory() {
        CommonModel rd = CommonModelBuilder.newBuilder(13L, 1L).getModel();

        CommonModel model = CommonModelBuilder.newBuilder()
            .withSkuRelations(R_1, R_2, R_3, R_4, R_5, R_6)
            .withSkuParentRelation(rd)
            .getModel();

        PreprocessingUtils.clearBrokenRelations(model, RELATION_BOOLEAN_FUNCTION);
        List<CommonModel> result = model.getRelations().stream()
            .map(relation -> CommonModelBuilder.newBuilder(relation.getId(), relation.getCategoryId()).getModel())
            .collect(Collectors.toList());
        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(R_1, R_2, R_3, R_4, R_5, R_6, rd));
    }

    @Test
    public void clearBrokenRelationsOneValidCopySameTypeDiffInvalidCategory() {
        CommonModel rd = CommonModelBuilder.newBuilder(13L, 5L).getModel();

        CommonModel model = CommonModelBuilder.newBuilder()
            .withSkuRelations(R_1, R_2, R_3, R_4, R_5, R_6, rd)
            .getModel();

        PreprocessingUtils.clearBrokenRelations(model, RELATION_BOOLEAN_FUNCTION);
        List<CommonModel> result = model.getRelations().stream()
            .map(relation -> CommonModelBuilder.newBuilder(relation.getId(), relation.getCategoryId()).getModel())
            .collect(Collectors.toList());
        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(R_1, R_2, R_3, R_4, R_5, R_6));
    }

    @Test
    public void clearBrokenRelationsOneValidCopyDiffTypeDiffInvalidCategory() {
        CommonModel rd = CommonModelBuilder.newBuilder(13L, 5L).getModel();

        CommonModel model = CommonModelBuilder.newBuilder()
            .withSkuRelations(R_1, R_2, R_3, R_4, R_5, R_6)
            .withSkuParentRelation(rd)
            .getModel();

        PreprocessingUtils.clearBrokenRelations(model, RELATION_BOOLEAN_FUNCTION);
        List<CommonModel> result = model.getRelations().stream()
            .map(relation -> CommonModelBuilder.newBuilder(relation.getId(), relation.getCategoryId()).getModel())
            .collect(Collectors.toList());
        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(R_1, R_2, R_3, R_4, R_5, R_6));
    }

    @Test
    public void clearBrokenRelationsOneValidCopyDiffTypeDiffValidCategory() {
        CommonModel rd = CommonModelBuilder.newBuilder(13L, 2L).getModel();

        CommonModel model = CommonModelBuilder.newBuilder()
            .withSkuRelations(R_1, R_2, R_3, R_4, R_5, R_6)
            .withSkuParentRelation(rd)
            .getModel();

        PreprocessingUtils.clearBrokenRelations(model, RELATION_BOOLEAN_FUNCTION);
        List<CommonModel> result = model.getRelations().stream()
            .map(relation -> CommonModelBuilder.newBuilder(relation.getId(), relation.getCategoryId()).getModel())
            .collect(Collectors.toList());
        MatcherAssert.assertThat(result, Matchers.containsInAnyOrder(R_1, R_2, R_3, R_4, R_5, R_6));
    }
}
