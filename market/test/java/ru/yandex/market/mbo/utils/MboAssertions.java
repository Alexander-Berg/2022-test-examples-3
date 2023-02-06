package ru.yandex.market.mbo.utils;

import org.assertj.core.api.SoftAssertions;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChangesAssertion;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValueHypothesis;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.modelstorage.assertions.ModelAssertions;
import ru.yandex.market.mbo.gwt.models.modelstorage.assertions.ParameterValueAssertion;
import ru.yandex.market.mbo.gwt.models.modelstorage.assertions.ParameterValueHypothesisAssertion;
import ru.yandex.market.mbo.gwt.models.modelstorage.assertions.ParameterValuesAssertion;
import ru.yandex.market.mbo.gwt.models.modelstorage.assertions.ParameterValuesInModelAssertion;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.utils.assertions.GroupOperationStatusAssertions;

import java.util.function.Consumer;

/**
 * @author s-ermakov
 */
public class MboAssertions {
    private MboAssertions() {
    }

    public static ModelAssertions assertThat(CommonModel model) {
        return new ModelAssertions(model);
    }

    public static ParameterValueAssertion assertThat(ParameterValue actual) {
        return new ParameterValueAssertion(actual);
    }

    public static ParameterValuesAssertion assertThat(ParameterValues actual) {
        return new ParameterValuesAssertion(actual);
    }

    public static ParameterValuesInModelAssertion assertThat(CommonModel model, String xslName) {
        return new ParameterValuesInModelAssertion(model, xslName);
    }

    public static ParameterValuesInModelAssertion assertThat(CommonModel model, long paramId) {
        return new ParameterValuesInModelAssertion(model, paramId);
    }

    public static ParameterValuesInModelAssertion assertThat(CommonModel model, CategoryParam param) {
        return new ParameterValuesInModelAssertion(model, param.getId());
    }

    public static ParameterValueHypothesisAssertion assertThat(ParameterValueHypothesis actual) {
        return new ParameterValueHypothesisAssertion(actual);
    }

    public static ModelChangesAssertion assertThat(ModelChanges changes) {
        return new ModelChangesAssertion(changes);
    }

    public static GroupOperationStatusAssertions assertThat(GroupOperationStatus status) {
        return new GroupOperationStatusAssertions(status);
    }

    public static void assertSoftly(Consumer<MboSoftAssertions> softly) {
        MboSoftAssertions assertions = new MboSoftAssertions();
        softly.accept(assertions);
        assertions.assertAll();
    }

    public static class MboSoftAssertions extends SoftAssertions {

        public ModelAssertions assertThat(CommonModel model) {
            return proxy(ModelAssertions.class, CommonModel.class, model);
        }
    }
}
