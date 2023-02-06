package ru.yandex.market.mbo.reactui.dto.modelrule.mappers;

import org.junit.Test;
import org.mapstruct.factory.Mappers;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate;
import ru.yandex.market.mbo.gwt.models.rules.ValueHolder;
import ru.yandex.market.mbo.gwt.models.rules.ValueSource;
import ru.yandex.market.mbo.http.ModelRules;
import ru.yandex.market.mbo.reactui.examples.modelRule.dto.ModelRulePredicateDto;
import ru.yandex.market.mbo.reactui.examples.modelRule.mappers.ModelRulePredicateDtoMapper;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ModelRulePredicateDtoMapperTest {

    private final ModelRulePredicateDtoMapper modelRulePredicateDtoMapper =
        Mappers.getMapper(ModelRulePredicateDtoMapper.class);

    private static final BigDecimal MAX_VALUE = new BigDecimal("321");
    private static final BigDecimal MIN_VALUE = new BigDecimal("123");
    private static final ModelRules.ModelRulePredicate.PredicateType TYPE =
        ModelRules.ModelRulePredicate.PredicateType.IF;
    private static final ModelRules.ModelRulePredicate.PredicateOperation OPERATION =
        ModelRules.ModelRulePredicate.PredicateOperation.CONTAINS;
    private static final Long RULE_ID = 1L;
    private static final String STRING_VALUE = "string";
    private static final Long VALUE_HOLDER_ID = 2L;
    private static final Set<Long> VALUE_IDS = Set.of(
        3L, 4L, 5L
    );


    @Test
    public void mappingTest() {
        ModelRulePredicate modelRulePredicate = generatePredicate();

        ModelRulePredicateDto result = modelRulePredicateDtoMapper.toModelRulePredicateDto(modelRulePredicate);

        assertThat(result.getParamId()).isEqualTo(VALUE_HOLDER_ID);
        assertThat(result.getRuleId()).isEqualTo(RULE_ID);
        assertThat(result.getMaxValue()).isEqualTo(MAX_VALUE);
        assertThat(result.getMinValue()).isEqualTo(MIN_VALUE);
        assertThat(result.getStringValue()).isEqualTo(STRING_VALUE);
        assertThat(result.getAllValueId()).isEqualTo(VALUE_IDS);
        assertThat(result.getPredicateType()).isEqualTo(TYPE);
        assertThat(result.getPredicateOperation()).isEqualTo(OPERATION);
    }

    private ModelRulePredicate generatePredicate() {
        ValueHolder valueHolder = new ValueHolder(ValueSource.MODEL_PARAMETER, VALUE_HOLDER_ID);
        return new ModelRulePredicate()
            .setMaxValue(MAX_VALUE)
            .setMinValue(MIN_VALUE)
            .setType(ModelRulePredicate.PredicateType.IF)
            .setRuleId(RULE_ID)
            .setOperation(ModelRulePredicate.PredicateOperation.CONTAINS)
            .setStringValue(STRING_VALUE)
            .setValueHolder(valueHolder)
            .setValueIds(VALUE_IDS);
    }

}
