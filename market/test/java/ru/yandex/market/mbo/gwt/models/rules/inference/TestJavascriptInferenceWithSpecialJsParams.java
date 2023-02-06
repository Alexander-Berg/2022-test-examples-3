package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.SpecialJsParamsProvider;

/**
 * Тесты, которые проверяют корректность javascript правил с использованием специальных параметров из
 * {@link SpecialJsParamsProvider}.
 *
 * @author s-ermakov
 */
public class TestJavascriptInferenceWithSpecialJsParams extends TestInferenceBase {

    @Test
    public void testIsModificationForModel() {
        tester
            .startModel()
                .id(1).category(1).currentType(CommonModel.Source.GURU)
                .param("bool1").setEmpty()
                .param("bool2").setEmpty()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("bool2").javascript("return val('@is_modification');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("bool2").valid().modified().bool(false).endParam()
            .endResults();
    }

    @Test
    public void testIsModificationForModification() {
        tester
            .startModel()
                .id(1).category(1).currentType(CommonModel.Source.GURU)
                .param("bool1").setEmpty()
                .param("bool2").setEmpty()
                .parentModelId(2)
                .startParentModel()
                    .id(2).category(1)
                    .param("bool1").setEmpty()
                    .param("bool2").setEmpty()
                .endModel()
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("bool2").javascript("return val('@is_modification');")
                .endRule()
            .endRuleSet()
            .doInference()
            .results()
                .count(1)
                .iterationCount(2)
                .param("bool2").valid().modified().bool(true).endParam()
            .endResults();
    }
}
