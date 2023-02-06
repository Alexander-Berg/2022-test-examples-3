package ru.yandex.market.mbo.gwt.models.rules.inference;

import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.rules.JavascriptUtils;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleException;

/**
 * Тесты проверяют, что правила корректно отображают ошибки {@link JavascriptUtils} в случае падений.
 *
 * @author s-ermakov
 */
public class TestJavascriptErrorInference extends TestInferenceBase {

    @Test
    public void testBoolNullInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("bool2").javascript("return null;")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: логический тип JavaScript или регистронезависимая строка, " +
                "содержащая true или false. Пришло: null значение.");
    }

    @Test
    public void testBoolStringInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("bool2").javascript("return 'abracadabra';")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: логический тип JavaScript или регистронезависимая строка, " +
                "содержащая true или false. Пришло: строка 'abracadabra', отличающаяся от true и false.");
    }

    @Test
    public void testBoolArrayInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("bool2").javascript("return ['abracadabra'];")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: логический тип JavaScript или регистронезависимая строка, " +
                "содержащая true или false. Пришло: значение '[abracadabra]', эквивалентное java-классу 'String[]'.");
    }

    @Test
    public void testNumericNullInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("num1").javascript("return null;")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: число или строка, которую можно преобразовать в число. " +
                "Пришло: null значение.");
    }

    @Test
    public void testNumericStringInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("num1").javascript("return 'abracadabra';")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: число или строка, которую можно преобразовать в число. " +
                "Пришло: строка 'abracadabra', которую невозможно преобразовать в число.");
    }

    @Test
    public void testNumericArrayInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("num1").javascript("return ['abracadabra'];")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: число или строка, которую можно преобразовать в число. " +
                "Пришло: значение '[abracadabra]', эквивалентное java-классу 'String[]'.");
    }

    @Test
    public void testStringNullInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("str1").javascript("return null;")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: строка или массив не null строк. Пришло: null значение.");
    }

    @Test
    public void testStringArrayWithNullValuesInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("str1").javascript("return ['abracadabra', null];")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: строка или массив не null строк. " +
                "Пришло: массив '[abracadabra, null]', содержащий null значение.");
    }

    @Test
    public void testEnumNullInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("enum1").javascript("return null;")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: непустая строка, содержащая название опции. " +
                "Пришло: null значение.");
    }

    @Test
    public void testEnumStringInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("enum1").javascript("return '';")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: непустая строка, содержащая название опции. " +
                "Пришло: пустая или состоящая из пробелов строка.");
    }

    //TODO - test didn't start in github, but fails after moving to arcadia, fix it later
    @Test
    public void testEnumWhitespaceStringInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("enum1").javascript("return '  ';")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: непустая строка, содержащая название опции. " +
                "Пришло: пустая или состоящая из пробелов строка.");
    }

    //TODO - test didn't start in github, but fails after moving to arcadia, fix it later
    @Test
    @Ignore
    public void testEnumArrayInference() {
        tester
            .startModel()
                .id(1).category(1)
            .endModel()
            .startRuleSet()
                .id(1)
                .startRule()
                    .name("Rule 1").group("Test")
                    ._if()
                        .param("bool1").isEmpty()
                    .then()
                        .param("enum1").javascript("return ['abracadabra'];")
                .endRule()
            .endRuleSet()
            .doInferenceWithFail()
            .withException(ModelRuleException.class)
            .withMessage("Ожидалось: непустая строка, содержащая название опции. " +
                "Пришло: значение '[abracadabra]', эквивалентное java-классу 'String[]'.");
    }
}
