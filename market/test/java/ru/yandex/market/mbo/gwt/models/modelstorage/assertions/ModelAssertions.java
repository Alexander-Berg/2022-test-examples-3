package ru.yandex.market.mbo.gwt.models.modelstorage.assertions;

import com.google.common.base.Objects;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.internal.Iterables;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation.RelationType;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class ModelAssertions extends AbstractObjectAssert<ModelAssertions, CommonModel> {

    private Iterables iterables = Iterables.instance();

    public ModelAssertions(CommonModel commonModel) {
        super(commonModel, ModelAssertions.class);
    }

    public ModelAssertions hasId(long id) {
        super.isNotNull();
        if (actual.getId() != id) {
            failWithMessage("Expected model (%s) to id: <%d>, actual: <%d>",
                toString(actual), id, actual.getId());
        }
        return super.myself;
    }

    public ModelAssertions hasTitle(String expectedTitle) {
        super.isNotNull();
        if (!Objects.equal(expectedTitle, actual.getTitle())) {
            failWithMessage("Expected model (%s) to has title:\n\"<%s>\"\nactual:\n\"<%s>\"",
                toString(actual), expectedTitle, actual.getTitle());
        }
        return super.myself;
    }

    /**
     * Проверяет, что айдишник модели диапазона int.
     */
    public ModelAssertions hasIdInIntRange() {
        super.isNotNull();
        if (!(0L < actual.getId() && actual.getId() < ModelStoreInterface.GENERATED_ID_MIN_VALUE)) {
            failWithMessage("Expected model (%s) id to be in int range (between %d and %d), actual: %d",
                toString(actual), 0L, Integer.MAX_VALUE, actual.getId());
        }
        return super.myself;
    }

    /**
     * Проверяет, что айдишник модели диапазона long.
     */
    public ModelAssertions hasIdInLongRange() {
        super.isNotNull();
        if (actual.getId() < ModelStoreInterface.GENERATED_ID_MIN_VALUE) {
            failWithMessage("Expected model (%s) id to be in long range (more then %d), actual: %d",
                toString(actual), ModelStoreInterface.GENERATED_ID_MIN_VALUE, actual.getId());
        }
        return super.myself;
    }

    public ModelAssertions hasCategoryid(long categoryId) {
        super.isNotNull();
        if (actual.getCategoryId() != categoryId) {
            failWithMessage("Expected model (%s) to have category id: <%d>, actual: <%d>",
                toString(actual), categoryId, actual.getCategoryId());
        }
        return super.myself;
    }

    public ModelAssertions hasVendorId(long vendorId) {
        super.isNotNull();
        if (actual.getVendorId() != vendorId) {
            failWithMessage("Expected model (%s) to have vendor id: <%d>, actual: <%d>",
                toString(actual), vendorId, actual.getVendorId());
        }
        return super.myself;
    }

    public ModelAssertions isDeleted() {
        super.isNotNull();
        if (!actual.isDeleted()) {
            failWithMessage("Expected model (%s) to be deleted. Actual is NOT.",
                toString(actual));
        }
        return super.myself;
    }

    public ModelAssertions isNotDeleted() {
        super.isNotNull();
        if (actual.isDeleted()) {
            failWithMessage("Expected model (%s) to be not deleted. Actual is deleted.",
                toString(actual));
        }
        return super.myself;
    }

    public ModelAssertions hasDoubtful(boolean expected) {
        super.isNotNull();
        if (actual.isDoubtful() != expected) {
            failWithMessage("Expected model (%s) to has be doubtful = %s. Actual is NOT.",
                toString(actual), expected);
        }
        return super.myself;
    }

    public ModelAssertions containsParameterValues(String... expectedXslNames) {
        super.isNotNull();
        List<String> actualXslNames = actual.getParameterValues().stream()
            .map(ParameterValues::getXslName)
            .collect(Collectors.toList());
        Assertions.assertThat(actualXslNames).contains(expectedXslNames);
        return super.myself;
    }

    private ModelAssertions containsParameterValues(Long... paramIds) {
        super.isNotNull();
        List<Long> actualParamIds = actual.getParameterValues().stream()
            .map(ParameterValues::getParamId)
            .collect(Collectors.toList());
        Assertions.assertThat(actualParamIds).contains(paramIds);
        return super.myself;
    }

    public ModelAssertions doesNotContainParameterValues(String... expectedXslNames) {
        super.isNotNull();
        List<String> actualXslNames = actual.getParameterValues().stream()
            .map(ParameterValues::getXslName)
            .collect(Collectors.toList());
        Assertions.assertThat(actualXslNames).doesNotContain(expectedXslNames);
        return super.myself;
    }

    public ModelAssertions doesNotContainParameterValues(Long... paramIds) {
        super.isNotNull();
        List<Long> actualParamIds = actual.getParameterValues().stream()
            .map(ParameterValues::getParamId)
            .collect(Collectors.toList());
        Assertions.assertThat(actualParamIds).doesNotContain(paramIds);
        return super.myself;
    }

    private ModelAssertions containsParameterValuesHypothesis(String... expectedXslNames) {
        super.isNotNull();
        List<String> actualXslNames = actual.getParameterValueHypotheses().stream()
            .map(ParameterValueHypothesis::getXslName)
            .collect(Collectors.toList());
        Assertions.assertThat(actualXslNames).contains(expectedXslNames);
        return super.myself;
    }

    private ModelAssertions containsParameterValuesHypothesis(Long... paramIds) {
        super.isNotNull();
        List<Long> actualParamIds = actual.getParameterValueHypotheses().stream()
            .map(ParameterValueHypothesis::getParamId)
            .collect(Collectors.toList());
        Assertions.assertThat(actualParamIds).contains(paramIds);
        return super.myself;
    }

    public ModelAssertions doesNotContainParameterValuesHypothesis(String... expectedXslNames) {
        super.isNotNull();
        List<String> actualXslNames = actual.getParameterValueHypotheses().stream()
            .map(ParameterValueHypothesis::getXslName)
            .collect(Collectors.toList());
        Assertions.assertThat(actualXslNames).doesNotContain(expectedXslNames);
        return super.myself;
    }

    public ModelAssertions doesNotContainParameterValuesHypothesis(Long... paramIds) {
        super.isNotNull();
        List<Long> actualParamIds = actual.getParameterValueHypotheses().stream()
            .map(ParameterValueHypothesis::getParamId)
            .collect(Collectors.toList());
        Assertions.assertThat(actualParamIds).doesNotContain(paramIds);
        return super.myself;
    }

    public ModelAssertions doesContainPictures() {
        super.isNotNull();
        if (!actual.getPictures().isEmpty()) {
            String pictures = actual.getPictures().stream()
                .map(Picture::toString)
                .collect(Collectors.joining("\n"));
            failWithMessage("Expected model (%s) not to contain pictures. Actual is NOT.\nPictures:\n%s",
                toString(actual), pictures);
        }
        return super.myself;
    }

    public ModelAssertions containsUrlPicturesExactlyInOrder(String... url) {
        super.isNotNull();
        Assertions.assertThat(actual.getPictures())
            .extracting(Picture::getUrl)
            .containsExactlyInAnyOrder(url);
        return super.myself;
    }

    public ParameterValuesInModelAssertion getParameterValues(String xlsName) {
        containsParameterValues(xlsName);
        return new ParameterValuesInModelAssertion(actual, xlsName);
    }

    public ParameterValuesInModelAssertion getParameterValues(long paramId) {
        containsParameterValues(paramId);
        return new ParameterValuesInModelAssertion(actual, paramId);
    }

    public ParameterValueHypothesisAssertion getParameterValuesHypothesis(String xlsName) {
        containsParameterValuesHypothesis(xlsName);
        return new ParameterValueHypothesisAssertion(actual.getParameterValueHypothesis(xlsName).get());
    }

    public ParameterValueHypothesisAssertion getParameterValuesHypothesis(long paramId) {
        containsParameterValuesHypothesis(paramId);
        return new ParameterValueHypothesisAssertion(actual.getParameterValueHypothesis(paramId).get());
    }

    public ModelAssertions containsSkuRelation(long id, long categoryId) {
        super.isNotNull();
        List<ModelRelation> relations = actual.getRelations();
        if (relations.stream().noneMatch(r -> matchRelation(r, id, categoryId, RelationType.SKU_MODEL))) {
            failWithMessage("Expected model (%s) to contain relation to sku with id" +
                " <%d> and category id <%d>.\nActual relations:\n%s", toString(actual), id, categoryId, relations);
        }
        return super.myself;
    }

    public ModelAssertions containsOnlySkuRelation(long id, long categoryId) {
        super.isNotNull();
        List<ModelRelation> relations = actual.getRelations();
        List<ModelRelation> matchRelations = relations.stream()
            .filter(r -> matchRelation(r, id, categoryId, RelationType.SKU_MODEL))
            .collect(Collectors.toList());

        if (matchRelations.size() != 1) {
            failWithMessage("Expected model (%s) to contain only one relation to sku with id" +
                " <%d> and category id <%d>.\nActual relations:\n%s", toString(actual), id, categoryId, relations);
        }
        return super.myself;
    }

    public ModelAssertions containsSkuRelation(@Nonnull CommonModel sku) {
        return containsSkuRelation(sku.getId(), sku.getCategoryId());
    }

    public ModelAssertions containsOnlySkuRelation(@Nonnull CommonModel sku) {
        return containsOnlySkuRelation(sku.getId(), sku.getCategoryId());
    }

    public ModelAssertions containsSkuParentRelation(long id, long categoryId) {
        super.isNotNull();
        List<ModelRelation> relations = actual.getRelations();
        List<ModelRelation> matchRelations = relations.stream()
            .filter(r -> matchRelation(r, id, categoryId, RelationType.SKU_PARENT_MODEL))
            .collect(Collectors.toList());
        if (matchRelations.size() != 1) {
            failWithMessage("Expected model (%s) to contain single relation to sku parent with id" +
                " <%d> and category id <%d>.\nActual relations:\n%s", toString(actual), id, categoryId, relations);
        }
        return super.myself;
    }

    public ModelAssertions containsSkuParentRelation(@Nonnull CommonModel model) {
        return containsSkuParentRelation(model.getId(), model.getCategoryId());
    }

    public ModelAssertions doesNotContainRelations() {
        super.isNotNull();

        List<ModelRelation> relations = actual.getRelations();
        if (!relations.isEmpty()) {
            failWithMessage("Expected model (%s) do not contain relations. Actual relations:\n%s",
                toString(actual), relations);
        }

        return super.myself;
    }

    public ModelAssertions containsExactlyInAnyOrderRelations(ModelRelation... modelRelations) {
        super.isNotNull();
        iterables.assertContainsExactlyInAnyOrder(info, actual.getRelations(), modelRelations);
        return super.myself;
    }

    private static boolean matchRelation(ModelRelation relation, long id, long categoryId, RelationType type) {
        return relation.getType() == type
            && relation.getId() == id
            && relation.getCategoryId() == categoryId;
    }

    private static String toString(CommonModel model) {
        return "id:" + model.getId() + ",title:" + model.getTitle();
    }
}
