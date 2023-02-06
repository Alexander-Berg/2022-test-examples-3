package ru.yandex.market.mbo.reactui.dto.modelrule.mappers;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.rules.ModelRule;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate;
import ru.yandex.market.mbo.reactui.examples.modelRule.dto.ModelRuleDto;
import ru.yandex.market.mbo.reactui.examples.modelRule.mappers.ModelRuleDtoMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelRuleDtoMapperTest {
    private static final boolean ACTIVE = true;
    private static final Long ID = 1L;
    private static final int PRIORITY = 2;
    private static final String NAME = "name";
    private static final String GROUP = "group";
    private static final Long RULE_SET_ID = 3L;

    @Test
    public void mappingTest() {

        ModelRule modelRule = generateModelRule();

        ModelRuleDto result = ModelRuleDtoMapper.INSTANCE.toModelRuleDto(modelRule);
        assertThat(result.getActive()).isEqualTo(ACTIVE);
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getPriority()).isEqualTo(PRIORITY);
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getGroup()).isEqualTo(GROUP);
        assertThat(result.isApplyToClusters()).isEqualTo(false);
        assertThat(result.isApplyToGuru()).isEqualTo(false);
        assertThat(result.isApplyToSKU()).isEqualTo(false);
        assertThat(result.getRuleSetId()).isEqualTo(RULE_SET_ID);
    }

    private ModelRule generateModelRule() {
        return new ModelRule()
            .setActive(ACTIVE)
            .setId(ID)
            .setPriority(PRIORITY)
            .setName(NAME)
            .setGroup(GROUP)
            .setApplyToGuru(false)
            .setApplyToClusters(false)
            .setApplyToSKU(false)
            .setAllowedApplyToPSKU(false)
            .setRuleSetId(RULE_SET_ID)
            .setIfs(generateModelRulePredicate())
            .setThens(generateModelRulePredicate());

    }

    private ModelRulePredicate generateModelRulePredicate() {
        return new ModelRulePredicate()
            .setOperation(ModelRulePredicate.PredicateOperation.MANDATORY)
            .setType(ModelRulePredicate.PredicateType.IF);

    }
}
