package ru.yandex.direct.web;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.utils.JsonUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;
import ru.yandex.direct.validation.wrapper.ModelItemValidationBuilder;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.validation.kernel.ValidationResultConversionService;
import ru.yandex.direct.web.validation.model.WebValidationResult;

import static ru.yandex.direct.validation.constraint.CollectionConstraints.listSize;
import static ru.yandex.direct.validation.constraint.CommonConstraints.notNull;
import static ru.yandex.direct.validation.constraint.StringConstraints.maxStringLength;


@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class WebDefectServiceTest {
    @Autowired
    private ValidationResultConversionService validationResultConversionService;

    @Test
    public void someTest() {
        RetargetingCondition retargetingCondition = new RetargetingCondition();
        retargetingCondition
                .withName("asdfg");

        Rule retargetingConditionRule = new Rule();
        Goal retargetingConditionGoal = new Goal();

        retargetingConditionRule.setGoals(Collections.singletonList(retargetingConditionGoal));
        retargetingCondition.setRules(Collections.singletonList(retargetingConditionRule));

        ModelItemValidationBuilder<RetargetingCondition> v1 = ModelItemValidationBuilder
                .of(retargetingCondition);


        v1.item(RetargetingCondition.NAME)
                .check(maxStringLength(3));

        v1.list(RetargetingCondition.RULES)
                .check(listSize(0, 0))
                .checkEachBy(retargetingConditionRule1 -> {
                    ModelItemValidationBuilder<Rule> v2 =
                            ModelItemValidationBuilder.of(retargetingConditionRule1);
                    v2.item(Rule.TYPE)
                            .check(notNull());
                    v2.list(Rule.GOALS)
                            .checkEachBy(retargetingConditionGoal1 -> {
                                ModelItemValidationBuilder<Goal> v3 =
                                        ModelItemValidationBuilder.of(retargetingConditionGoal1);
                                v3.item(Goal.ID)
                                        .check(notNull());
                                return v3.getResult();
                            });
                    return v2.getResult();
                });

        ValidationResult<RetargetingCondition, Defect> result = v1.getResult();

        //т.к. в текущей архитектуре в дефекте нет информации о пути, добавляем в BaseDefectDefinition DefectInfo

        WebValidationResult vr = validationResultConversionService.buildWebValidationResult(result);

        JsonUtils.toJson(vr);
    }


}
