package ru.yandex.market.core.supplier.banner.service.parser;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.supplier.banner.model.filter.condition.FilterCondition;
import ru.yandex.market.core.supplier.banner.model.filter.condition.OnBoardingStateCondition;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;

public class SupplierBannerConditionParserTest {

    @Test
    public void testNewbie() {
        Map<String, String> source = Map.of(
                "onBoardingState", "NEWBIE");

        List<FilterCondition> result = SupplierBannerConditionParser.parseConditionsFromMap(source);
        assertThat(result.size(), equalTo(1));

        FilterCondition cond = result.get(0);
        assertThat(cond, instanceOf(OnBoardingStateCondition.class));
        assertThat(cond.getParams().get("param_type_id"), equalTo(ParamType.IS_NEWBIE.getId()));
        assertThat(cond.getParams().get("param_num_value"), equalTo(1));
    }

    @Test
    public void testAlreadyConnected() {
        Map<String, String> source = Map.of(
                "onBoardingState", "FINISHED");

        List<FilterCondition> result = SupplierBannerConditionParser.parseConditionsFromMap(source);
        assertThat(result.size(), equalTo(1));

        FilterCondition cond = result.get(0);
        assertThat(cond, instanceOf(OnBoardingStateCondition.class));
        assertThat(cond.getParams().get("param_num_value"), equalTo(0));
    }

    @Test
    public void testNotSpecified() {
        Map<String, String> source = Map.of(
                "onBoardingState", "NOT_SPECIFIED");

        List<FilterCondition> result = SupplierBannerConditionParser.parseConditionsFromMap(source);
        assertThat(result.size(), equalTo(0));
    }

    @Test
    public void testWrongEnumFieldFails() {
        Map<String, String> source = Map.of(
                "onBoardingState", "jiowpfjo");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SupplierBannerConditionParser.parseConditionsFromMap(source);
        });
    }

    @Test
    public void testWrongFieldFails() {
        Map<String, String> source = Map.of(
                "onBoardingState", "NEWBIE",
                "SOME_WRONG_FIELD", "VALUE_OF_WRONG_FIELD"
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            SupplierBannerConditionParser.parseConditionsFromMap(source);
        });
    }

}
