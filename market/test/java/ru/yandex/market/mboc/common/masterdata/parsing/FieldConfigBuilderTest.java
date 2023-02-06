package ru.yandex.market.mboc.common.masterdata.parsing;

import java.util.Optional;

import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.parsing.validator.ComplexValidator;
import ru.yandex.market.mboc.common.masterdata.parsing.validator.FieldValidator;
import ru.yandex.market.mboc.common.masterdata.parsing.validator.NoOpValidator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 13.09.2018
 */
public class FieldConfigBuilderTest {

    private static final FieldProcessor<String> DUMMY_FIELD_PROCESSOR = (item, rawValue) -> {
    };

    @Test
    public void whenNoValidatorsShouldUseNoOp() {
        FieldConfig<String> config = new FieldConfig.Builder<>(DUMMY_FIELD_PROCESSOR)
            .build();

        assertThat(config.getValidator()).isInstanceOf(NoOpValidator.class);
    }

    @Test
    public void whenSingleValidatorShouldUseItself() {
        FieldValidator validator = createDummyValidator();

        FieldConfig<String> config = new FieldConfig.Builder<>(DUMMY_FIELD_PROCESSOR)
            .addValidator(validator)
            .build();

        assertThat(config.getValidator()).isSameAs(validator);
    }

    @Test
    public void whenMultipleValidatorsShouldUserComplex() {
        FieldValidator validator = createDummyValidator();

        FieldConfig<String> config = new FieldConfig.Builder<>(DUMMY_FIELD_PROCESSOR)
            .required()
            .addValidator(validator)
            .build();

        assertThat(config.getValidator()).isInstanceOf(ComplexValidator.class);
    }

    private FieldValidator createDummyValidator() {
        return (header, value) -> Optional.empty();
    }
}
