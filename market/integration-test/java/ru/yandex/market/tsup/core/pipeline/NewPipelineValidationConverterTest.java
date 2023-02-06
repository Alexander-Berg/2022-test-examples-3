package ru.yandex.market.tsup.core.pipeline;

import javax.validation.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.core.pipeline.data.StringIntPayload;
import ru.yandex.market.tsup.domain.entity.TestPipelineName;

class NewPipelineValidationConverterTest extends AbstractContextualTest {

    @Autowired
    private NewPipelineValidationConverter validationConverter;

    @Test
    void validateInvalid() {

        softly.assertThatThrownBy(() -> validationConverter.validate("INVALID_PIPE_NAME", validNode()))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Unknown pipeline name");

        softly.assertThatThrownBy(() -> validationConverter.validate("TEST_SIMPLE_PIPELINE", invalidNode()))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("a:");
    }

    @Test
    void validate() {
        var result = validationConverter.validate("TEST_SIMPLE_PIPELINE", validNode());
        softly.assertThat(result.getPipelineName()).isEqualTo(TestPipelineName.TEST_SIMPLE_PIPELINE);
        softly.assertThat(result.getInitialPayload())
            .isEqualTo(new StringIntPayload().setA("string").setB(123));
    }

    private JsonNode invalidNode() {
        return JsonNodeFactory.instance.objectNode();
    }

    private JsonNode validNode() {
        return JsonNodeFactory.instance.objectNode()
            .put("a", "string")
            .put("b", 123);
    }
}
