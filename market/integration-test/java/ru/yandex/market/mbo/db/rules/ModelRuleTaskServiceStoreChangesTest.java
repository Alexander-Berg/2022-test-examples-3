package ru.yandex.market.mbo.db.rules;

import com.google.common.collect.Iterators;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import org.apache.log4j.Logger;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.mbo.configs.db.parameter.ParameterLoaderServiceConfig;
import ru.yandex.market.mbo.configs.yt.YtModelStoreConfig;
import ru.yandex.market.mbo.db.params.ParameterDAO;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;
import ru.yandex.market.mbo.gwt.models.rules.ModelProcessStatus;
import ru.yandex.market.mbo.gwt.models.rules.ModelRule;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleResult;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleResultItem;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTask;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleTaskStatus;
import ru.yandex.market.mbo.randomizers.ParameterValueRandomizer;
import ru.yandex.market.mbo.randomizers.ParameterValuesRandomizer;
import ru.yandex.market.mbo.user.AutoUser;

import javax.annotation.Nonnull;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateOperation.MATCHES;
import static ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate.PredicateType.ANY;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 24.05.2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class ModelRuleTaskServiceStoreChangesTest {
    private static final Logger log = Logger.getLogger(ModelRuleTaskServiceStoreChangesTest.class);
    private static final int XXX_CATEGORY_ID = 91810;

    private static final long RANDOM_SEED = 20121221L;
    private static final long MODIFICATION_RULE_ID = 20110801;
    private static final long PARAMETER_ID = 333333;
    private static final String PARAMETER_XSL_NAME = "xsl_name__" + PARAMETER_ID;
    private static final int PARAMETER_VALUES_COUNT = 14;
    private static final int PARAMETERS_META_SIZE = PARAMETER_VALUES_COUNT * 20;
    private static final long MODEL_ID = 100L;

    @Autowired
    private ParameterDAO parameterDAO;

    @Autowired
    private ModelRuleTaskService modelRuleTaskService;

    @Autowired
    private AutoUser autoUser;

    private ParameterValuesRandomizer valuesRandomizer;

    private Parameter parameter;

    @After
    public void after() {
        parameterDAO.removeParameter(XXX_CATEGORY_ID, parameter.getId());
    }

    @Before
    public void before() {
        ParameterValueRandomizer valueRandomizer = new ParameterValueRandomizer(
            EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .exclude(PickerImage.class)
                .seed(RANDOM_SEED)
                .build(),
            PARAMETERS_META_SIZE,
            false,
            true
        );

        valuesRandomizer = new ParameterValuesRandomizer(
            valueRandomizer,
            EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .exclude(PickerImage.class)
                .seed(RANDOM_SEED)
                .build()
        );
    }

    @Test
    public void createTask() {
        ModelRuleTask task = modelRuleTaskService.createTask(createRuleSet(), autoUser.getId(), pass -> {
        });

        log.info("created task #" + task.getId());

        modelRuleTaskService.setTaskStatus(task, autoUser.getId(), ModelRuleTaskStatus.EXEC_CANCELLED, null);

        log.info("task marked as cancelled");

        List<ParameterValues> values = randomValues();
        Map<Long, ParameterValues> oldValues = shakeValues(values, true);
        Map<Long, ParameterValues> newValues = shakeValues(values, false);
        // shakeValue may produce empty values for same parameter in both collections, old and new
        // empty value in both collections is invalid situation, so remote it
        newValues.keySet().stream()
            .filter(paramId -> oldValues.get(paramId).isEmpty() && newValues.get(paramId).isEmpty())
            .collect(Collectors.toSet())
            .forEach(paramId -> {
                oldValues.remove(paramId);
                newValues.remove(paramId);
            });

        checkValuesAreDifferent(oldValues, newValues);

        CommonModel model = new CommonModel();
        model.setId(MODEL_ID);
        model.setCategoryId(XXX_CATEGORY_ID);

        ModelRuleTaskStatus modelProcessResult = modelRuleTaskService.registerModelProcessResult(
            task.getId(), model, ModelProcessStatus.APPLIED,
            "integration test descr", newValues.values(), oldValues);

        log.info("task rule status: " + modelProcessResult);

        ModelRuleResult loadedChange = modelRuleTaskService.getModelChangedParameters(task.getId(), MODEL_ID);

        assertThat(loadedChange.getItemsByParamId().keySet())
            .isEqualTo(newValues.keySet());

        SoftAssertions.assertSoftly(softAssertions -> {
            boolean atLeastOneChecked = false;
            for (ParameterValues value : values) {
                long paramId = value.getParamId();
                ParameterValues oldValue = oldValues.get(paramId);
                ParameterValues newValue = newValues.get(paramId);

                if (oldValue == null && newValue == null) {
                    continue;
                }
                ModelRuleResultItem resultItem = loadedChange.getResultItem(paramId);
                assertThat(resultItem).isNotNull();

                if (oldValue == null) {
                    softAssertions.<ParameterValues>assertThat(resultItem.getOldValues())
                        .usingComparator(ParameterValueRandomizer.PARAMETER_VALUES_COMPARATOR)
                        .isNull();
                } else {
                    softAssertions.<ParameterValues>assertThat(resultItem.getOldValues())
                        .usingComparator(ParameterValueRandomizer.PARAMETER_VALUES_COMPARATOR)
                        .isEqualTo(oldValue);
                    atLeastOneChecked = true;
                }

                if (newValue == null) {
                    softAssertions.<ParameterValues>assertThat(resultItem.getValue())
                        .usingComparator(ParameterValueRandomizer.PARAMETER_VALUES_COMPARATOR)
                        .isNull();
                } else {
                    softAssertions.<ParameterValues>assertThat(resultItem.getValue())
                        .usingComparator(ParameterValueRandomizer.PARAMETER_VALUES_COMPARATOR)
                        .isEqualTo(newValue);
                    atLeastOneChecked = true;
                }
            }

            softAssertions.assertThat(atLeastOneChecked).isTrue();
        });
    }

    private void checkValuesAreDifferent(Map<Long, ParameterValues> oldValues, Map<Long, ParameterValues> newValues) {
        assertThat(oldValues).isNotEmpty();
        assertThat(newValues).isNotEmpty();

        Set<String> oldXslNames = oldValues.values()
            .stream().map(ParameterValues::getXslName).collect(Collectors.toSet());
        Set<String> newXslNames = newValues.values()
            .stream().map(ParameterValues::getXslName).collect(Collectors.toSet());

        assertThat(oldXslNames.equals(newXslNames))
            .isTrue();

        assertThat(newValues.values()).filteredOn(ParameterValues::isEmpty)
            .withFailMessage("should have at least one empty parameter value")
            .isNotEmpty();
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Map<Long, ParameterValues> shakeValues(List<ParameterValues> values, boolean modeA) {

        // clone
        values = values.stream().map(ParameterValues::of).collect(Collectors.toList());

        Date modificationDate = Date.from(ZonedDateTime.parse(
            modeA ? "2012-12-21T13:13:30+01:00[UTC]" : "2000-01-01T12:14:30+01:00[UTC]"
        ).toInstant());
        long uid = modeA ? autoUser.getId() : autoUser.getId() + 1;
        long ruleId = modeA ? MODIFICATION_RULE_ID : MODIFICATION_RULE_ID + 1;

        // make different sets depending on mode flag
        // by removing patterns
        // it produces different combinations of values
        // ---###---###---###---###---###---
        // --##--##--##--##--##--##--##--##-
        int stepSize = modeA ? 2 : 3;

        Iterator<ParameterValue> iterator = Iterators.concat(values.stream().map(Iterable::iterator).iterator());
        int i = 0;
        while (iterator.hasNext()) {
            ParameterValue pv = iterator.next();
            if (++i / stepSize % 2 == 0) {
                iterator.remove();
            } else {
                pv.setLastModificationDate(modificationDate);
                pv.setLastModificationUid(uid);
                pv.setRuleModificationId(ruleId);
            }
        }

        return values.stream()
            .collect(Collectors.toMap(ParameterValues::getParamId, Function.identity(), (a, b) -> b));
    }

    private List<ParameterValues> randomValues() {
        List<ParameterValues> values = new ArrayList<>();

        for (int i = 0; i < PARAMETER_VALUES_COUNT; i++) {
            ParameterValues randomValue = valuesRandomizer.getRandomValue();
            randomValue.forEach(pv -> {
                // rules don't operate pickers and don't store changes of them
                pv.setPickerModificationSource(null);
                pv.setPickerImage(null);
            });
            values.add(randomValue);
        }

        return values;
    }

    @Nonnull
    @SuppressWarnings("checkstyle:magicnumber")
    private ModelRuleSet createRuleSet() {
        ModelRuleSet ruleSet = new ModelRuleSet();
        ruleSet.setCategoryId(XXX_CATEGORY_ID);

        ModelRule rule = new ModelRule();
        rule.setName("model-rule-integration-test-rule");
        rule.setPriority(1);

        parameter = new Parameter();
        parameter.setId(PARAMETER_ID);
        parameter.setXslName(PARAMETER_XSL_NAME);
        parameter.setType(Param.Type.STRING);
        parameter.setUseForGuru(true);
        parameter.setCategoryHid(XXX_CATEGORY_ID);

        parameterDAO.insertParameter(parameter);
        parameterDAO.updateParameterClobs(parameter);
        parameterDAO.touchParam(parameter);

        ModelRulePredicate predicate = new ModelRulePredicate(parameter.getId(), ANY, MATCHES);
        predicate.setValueIds(Collections.singleton(3L));

        rule.setIfs(Collections.singletonList(predicate));
        rule.setThens(Collections.singletonList(predicate));

        ruleSet.setRules(Collections.singletonList(rule));
        return ruleSet;
    }

    @Configuration
    @ComponentScan("ru.yandex.market.mbo.db.rules")

    @Import({
        TestConfiguration.class,
        YtModelStoreConfig.class,
        ParameterLoaderServiceConfig.class
    })
    @ImportResource({
        "classpath:mbo-core/mbo-db-config.xml"
    })

    public static class SpringConfig {

    }
}
