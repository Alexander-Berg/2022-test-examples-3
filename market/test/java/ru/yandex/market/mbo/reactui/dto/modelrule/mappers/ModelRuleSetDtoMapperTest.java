package ru.yandex.market.mbo.reactui.dto.modelrule.mappers;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.rules.ModelRule;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;
import ru.yandex.market.mbo.reactui.examples.modelRule.dto.ModelRuleSetDto;
import ru.yandex.market.mbo.reactui.examples.modelRule.mappers.ModelRuleSetDtoMapper;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ModelRuleSetDtoMapperTest {

    private static final Long CATEGORY_ID = 1L;
    private static final Long ID = 2L;
    private static final Boolean EDITABLE = true;


    @Test
    public void mappingTest() {

        ModelRuleSet modelRuleSet = new ModelRuleSet()
            .setCategoryId(CATEGORY_ID)
            .setId(ID)
            .setEditable(EDITABLE)
            .setRules(List.of(new ModelRule(), new ModelRule()));

        ModelRuleSetDto result = ModelRuleSetDtoMapper.INSTANCE.toModelRuleSetDto(modelRuleSet);

        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(result.getEditable()).isEqualTo(EDITABLE);
    }
}
