package ru.yandex.market.gutgin.tms.assertions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.google.common.base.Objects;
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

import ru.yandex.market.ir.autogeneration.common.util.LocalizedStringUtils;
import ru.yandex.market.mbo.http.ModelStorage;

import static org.assertj.core.internal.CommonValidations.checkIsNotNull;

/**
 * @author s-ermakov
 */
public class ModelAssertions extends AbstractObjectAssert<ModelAssertions, ModelStorage.Model> {

    public ModelAssertions(ModelStorage.Model commonModel) {
        super(commonModel, ModelAssertions.class);
    }

    public ModelAssertions hasId(long id) {
        super.isNotNull();
        if (actual.getId() != id) {
            failWithMessage("Expected model (%s) to have equal id. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), id, actual.getId());
        }
        return myself;
    }

    public ModelAssertions hasTitle(String expectedTitle) {
        super.isNotNull();
        String actualTitle = LocalizedStringUtils.getDefaultString(actual.getTitlesList());
        if (!Objects.equal(expectedTitle, actualTitle)) {
            failWithMessage("Expected model (%s) to has title:\n<%s>\nactual:\n<%s>",
                toString(actual), expectedTitle, actualTitle);
        }
        return myself;
    }

    public ModelAssertions hasCategoryId(long categoryId) {
        super.isNotNull();
        if (actual.getCategoryId() != categoryId) {
            failWithMessage("Expected model (%s) to have equal category id. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), categoryId, actual.getCategoryId());
        }
        return myself;
    }

    public ModelAssertions hasSupplierId(long supplierId) {
        super.isNotNull();
        if (actual.getSupplierId() != supplierId) {
            failWithMessage("Expected model (%s) to have equal supplier id. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), supplierId, actual.getSupplierId());
        }
        return myself;
    }

    public ModelAssertions hasPublished(boolean published) {
        super.isNotNull();
        if (actual.getPublished() != published) {
            failWithMessage("Expected model (%s) to have equal published. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), published, actual.getPublished());
        }
        return myself;
    }

    public ModelAssertions hasModifiedTs(long modifiedTs) {
        super.isNotNull();
        if (actual.getModifiedTs() != modifiedTs) {
            failWithMessage("Expected model (%s) to have equal modifiedTs. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), modifiedTs, actual.getModifiedTs());
        }
        return myself;
    }


    public ModelAssertions hasSourceType(String sourceType) {
        super.isNotNull();
        if (!actual.getSourceType().equals(sourceType)) {
            failWithMessage("Expected model (%s) to have equal source type. Expected:\n<%s>\nActual:\n<%s>",
                    toString(actual), sourceType, actual.getSourceType());
        }
        return myself;
    }

    public ModelAssertions hasCurrentType(String currentType) {
        super.isNotNull();
        if (!actual.getCurrentType().equals(currentType)) {
            failWithMessage("Expected model (%s) to have equal current type. Expected:\n<%s>\nActual:\n<%s>",
                    toString(actual), currentType, actual.getCurrentType());
        }
        return myself;
    }

    public ModelAssertions hasSourceType(ModelStorage.ModelType sourceType) {
        return hasSourceType(sourceType.name());
    }

    public ModelAssertions hasCurrentType(ModelStorage.ModelType currentType) {
        return hasCurrentType(currentType.name());
    }

    public ModelAssertions hasVendorId(long vendorId) {
        super.isNotNull();
        if (actual.getVendorId() != vendorId) {
            failWithMessage("Expected model (%s) to have equal vendor id. Expected:\n<%d>\nActual:\n<%d>",
                toString(actual), vendorId, actual.getVendorId());
        }
        return myself;
    }

    public ModelAssertions isDeleted() {
        super.isNotNull();
        if (!actual.getDeleted()) {
            failWithMessage("Expected model (%s) to be deleted. Actual is NOT.",
                toString(actual));
        }
        return myself;
    }

    public ModelAssertions isPublished() {
        super.isNotNull();
        if (!actual.getPublished()) {
            failWithMessage("Expected model (%s) to be published. Actual is NOT.",
                    toString(actual));
        }
        return myself;
    }

    public ModelAssertions containParameterValues(String... expectedXslNames) {
        super.isNotNull();
        List<String> actualXslNames = actual.getParameterValuesList().stream()
            .map(ModelStorage.ParameterValue::getXslName)
            .collect(Collectors.toList());
        Assertions.assertThat(actualXslNames).contains(expectedXslNames);
        return myself;
    }


    public ModelAssertions containParameterWithValue(Long paramId, String value) {
        super.isNotNull();
        List<ModelStorage.ParameterValue> paramWithId = actual.getParameterValuesList().stream()
                .filter(p -> p.getParamId() == paramId)
                .collect(Collectors.toList());
        Assertions.assertThat(paramWithId).hasSize(1);
        ModelStorage.ParameterValue parameterValue = paramWithId.get(0);
        Assertions.assertThat(parameterValue)
                .extracting(v -> v.getStrValue(0))
                .extracting(ModelStorage.LocalizedString::getValue)
                .isEqualTo(value);
        return myself;
    }


    public ModelAssertions containParameterWithValue(Long paramId, boolean value) {
        super.isNotNull();
        List<ModelStorage.ParameterValue> paramWithId = actual.getParameterValuesList().stream()
                .filter(p -> p.getParamId() == paramId)
                .collect(Collectors.toList());
        Assertions.assertThat(paramWithId).hasSize(1);
        ModelStorage.ParameterValue parameterValue = paramWithId.get(0);
        Assertions.assertThat(parameterValue)
                .extracting(ModelStorage.ParameterValue::getBoolValue)
                .isEqualTo(value);
        return myself;
    }

    public ModelAssertions containParameterValues(Long... paramIds) {
        super.isNotNull();
        List<Long> actualParamIds = actual.getParameterValuesList().stream()
            .map(ModelStorage.ParameterValue::getParamId)
            .collect(Collectors.toList());
        Assertions.assertThat(actualParamIds).contains(paramIds);
        return myself;
    }

    public ModelAssertions containOnlyThisParameterValues(Long... paramIds) {
        super.isNotNull();
        List<Long> actualParamIds = actual.getParameterValuesList().stream()
            .map(ModelStorage.ParameterValue::getParamId)
            .collect(Collectors.toList());
        Assertions.assertThat(actualParamIds).containsOnly(paramIds);
        return myself;
    }

    public ModelAssertions hasParamWithValue(Long paramId, String value) {
        super.isNotNull();
        List<ModelStorage.ParameterValue> param = actual.getParameterValuesList().stream()
                .filter(parameterValue -> parameterValue.getParamId() == paramId)
                .collect(Collectors.toList());
        Assertions.assertThat(param).hasSize(1);
        Assertions.assertThat(param).extracting(parameterValue -> parameterValue.getStrValue(0))
                .containsOnly(LocalizedStringUtils.defaultString(value));
        return myself;
    }

    public ModelAssertions doesNotContainParameterValues(String... expectedXslNames) {
        super.isNotNull();
        List<String> actualXslNames = actual.getParameterValuesList().stream()
            .map(ModelStorage.ParameterValue::getXslName)
            .collect(Collectors.toList());
        Assertions.assertThat(actualXslNames).doesNotContain(expectedXslNames);
        return myself;
    }

    public ModelAssertions doesNotContainParameterValues(Long... paramIds) {
        super.isNotNull();
        List<Long> actualParamIds = actual.getParameterValuesList().stream()
            .map(ModelStorage.ParameterValue::getParamId)
            .collect(Collectors.toList());
        Assertions.assertThat(actualParamIds).doesNotContain(paramIds);
        return myself;
    }

    private ModelAssertions containParameterValuesHypothesis(String... expectedXslNames) {
        super.isNotNull();
        List<String> actualXslNames = actual.getParameterValueHypothesisList().stream()
            .map(ModelStorage.ParameterValueHypothesis::getXslName)
            .collect(Collectors.toList());
        Assertions.assertThat(actualXslNames).contains(expectedXslNames);
        return myself;
    }

    public ModelAssertions containParameterValuesHypothesis(Long... paramIds) {
        super.isNotNull();
        List<Long> actualParamIds = actual.getParameterValueHypothesisList().stream()
            .map(ModelStorage.ParameterValueHypothesis::getParamId)
            .collect(Collectors.toList());
        Assertions.assertThat(actualParamIds).contains(paramIds);
        return myself;
    }

    public ModelAssertions doesNotContainParameterValuesHypothesis(String... expectedXslNames) {
        super.isNotNull();
        List<String> actualXslNames = actual.getParameterValueHypothesisList().stream()
            .map(ModelStorage.ParameterValueHypothesis::getXslName)
            .collect(Collectors.toList());
        Assertions.assertThat(actualXslNames).doesNotContain(expectedXslNames);
        return myself;
    }

    public ModelAssertions doesNotContainParameterValuesHypothesis(Long... paramIds) {
        super.isNotNull();
        List<Long> actualParamIds = actual.getParameterValueHypothesisList().stream()
            .map(ModelStorage.ParameterValueHypothesis::getParamId)
            .collect(Collectors.toList());
        Assertions.assertThat(actualParamIds).doesNotContain(paramIds);
        return myself;
    }

    public ModelAssertions doesNotContainPictures() {
        super.isNotNull();
        if (!actual.getPicturesList().isEmpty()) {
            String pictures = actual.getPicturesList().stream()
                .map(ModelStorage.Picture::toString)
                .collect(Collectors.joining("\n"));
            failWithMessage("Expected model (%s) not to contain pictures. Actual is NOT.\nPictures:\n%s",
                toString(actual), pictures);
        }
        return myself;
    }

    public ModelAssertions containsUrlPicturesExactlyInOrder(String... url) {
        super.isNotNull();
        Assertions.assertThat(actual.getPicturesList())
            .extracting(ModelStorage.Picture::getUrl)
            .containsExactlyInAnyOrder(url);
        return myself;
    }

    public ModelAssertions containsUrlSourcePicturesExactlyInOrder(String... url) {
        super.isNotNull();
        Assertions.assertThat(actual.getPicturesList())
                .extracting(ModelStorage.Picture::getUrlSource)
                .containsExactlyInAnyOrder(url);
        return myself;
    }

    public ModelAssertions hasSkuRelation(long id, long categoryId) {
        super.isNotNull();
        List<ModelStorage.Relation> relations = actual.getRelationsList().stream()
            .filter(r -> r.getType() == ModelStorage.RelationType.SKU_MODEL)
            .collect(Collectors.toList());
        boolean match = relations.stream()
            .anyMatch(r -> r.getId() == id && r.getCategoryId() == categoryId);
        if (!match) {
            failWithMessage("Expected model (%s) to contain relation to sku with id" +
                " <%d> and category id <%d>.\nActual relations:\n%s", toString(actual), id, categoryId, relations);
        }
        return myself;
    }

    public ModelAssertions hasSkuRelation(@Nonnull ModelStorage.ModelOrBuilder sku) {
        return hasSkuRelation(sku.getId(), sku.getCategoryId());
    }

    public ModelAssertions hasSkuRelations(ModelStorage.ModelOrBuilder... skus) {
        for (ModelStorage.ModelOrBuilder sku : skus) {
            hasSkuRelation(sku.getId(), sku.getCategoryId());
        }
        return myself;
    }

    public ModelAssertions hasExactlySkuRelations(ModelStorage.ModelOrBuilder... skus) {
        checkIsNotNull(skus);
        List<ModelStorage.Relation> relations = actual.getRelationsList().stream()
                .filter(r -> r.getType() == ModelStorage.RelationType.SKU_MODEL)
                .collect(Collectors.toList());
        List<ModelStorage.Relation> unexpected = new ArrayList<>(relations);
        List<ModelStorage.ModelOrBuilder> notFound = Arrays.stream(skus)
                .collect(Collectors.toCollection(ArrayList::new));

        for (ModelStorage.ModelOrBuilder sku : skus) {
            boolean found = unexpected
                    .removeIf(r -> r.getId() == sku.getId() && r.getCategoryId() == sku.getCategoryId());
            if (found) {
                notFound.remove(sku);
            }
        }

        if (!unexpected.isEmpty() || !notFound.isEmpty()) {
            failWithMessage("Expected model (%s) to contain exactly relations to skus\n%s" +
                            "\nUnexpected relations:\n%s" +
                            "\nNot found relations for skus:\n%s" +
                            "\nActual relations:\n%s",
                    toString(actual),
                    Arrays.stream(skus).map(ModelAssertions::toString).collect(Collectors.toList()),
                    unexpected,
                    notFound.stream().map(ModelAssertions::toString).collect(Collectors.toList()),
                    relations);
        }
        return myself;
    }

    public ModelAssertions hasSkuParentRelation(long id, long categoryId) {
        super.isNotNull();
        List<ModelStorage.Relation> relations = actual.getRelationsList().stream()
            .filter(r -> r.getType() == ModelStorage.RelationType.SKU_PARENT_MODEL)
            .collect(Collectors.toList());
        boolean match = relations.stream()
            .anyMatch(r -> r.getId() == id && r.getCategoryId() == categoryId);
        if (!match) {
            failWithMessage("Expected model (%s) to contain relation to sku parent with id" +
                " <%d> and category id <%d>.\nActual relations:\n%s", toString(actual), id, categoryId, relations);
        }
        return myself;
    }

    public ModelAssertions hasSkuParentRelation(@Nonnull ModelStorage.Model model) {
        return hasSkuParentRelation(model.getId(), model.getCategoryId());
    }

    private static String toString(ModelStorage.ModelOrBuilder model) {
        return "id:" + model.getId() + ", title:" + LocalizedStringUtils.getDefaultString(model.getTitlesList());
    }
}
