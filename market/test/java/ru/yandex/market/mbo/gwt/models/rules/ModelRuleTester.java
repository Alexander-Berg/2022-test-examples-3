package ru.yandex.market.mbo.gwt.models.rules;

import org.assertj.core.api.Assertions;
import org.junit.Assert;
import ru.yandex.market.mbo.db.rules.JavaModelRuleValidator;
import ru.yandex.market.mbo.db.rules.NashornJsExecutor;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.FullModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.ThinCategoryParam;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleResultItem.ResultType;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.OptionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.EMPTY_LIST;

/**
 * @author gilmulla
 */
@SuppressWarnings("checkstyle:methodName")
public class ModelRuleTester {

    public class TestModelRuleBuilder
        extends AbstractModelRuleBuilder<TestModelRuleBuilder> {

        public TestModelRuleBuilder() {
            setBuilder(this);
        }

        public ModelRuleTester endRuleSet() {
            ModelRuleSet ruleSet = buildRuleSet();
            ModelRuleTester tester = ModelRuleTester.this;
            if (ruleSet != null) {
                tester.ruleSets.put(ruleSet.getId(), ruleSet);
                tester.currentRuleSet = ruleSet;
                ruleSet = null;
            }
            return tester;
        }
    }

    public class ResultsAssertion {

        public class ParameterAssertion {

            public class StringDomainBuilder {
                private StringDomain stringDomain;

                public StringDomainBuilder any() {
                    stringDomain = StringDomain.any(param);
                    return this;
                }

                public StringDomainBuilder empty() {
                    stringDomain = StringDomain.empty(param);
                    return this;
                }

                public StringDomainBuilder notContainsEmpty() {
                    stringDomain = StringDomain.notContainsEmpty(param);
                    return this;
                }

                public StringDomainBuilder match(String... value) {
                    stringDomain = StringDomain.match(param, value);
                    return this;
                }

                public StringDomainBuilder match(Collection<String> value) {
                    stringDomain = StringDomain.match(param, value);
                    return this;
                }

                public StringDomainBuilder substring(String... value) {
                    stringDomain = StringDomain.substring(param, value);
                    return this;
                }

                public ParameterAssertion endDomain() {
                    Assert.assertTrue(stringDomain.isEqual(item.getStringDomain()));
                    return ParameterAssertion.this;
                }
            }

            public class EnumDomainBuilder {
                private EnumDomain enumDomain;

                public EnumDomainBuilder options(Long... options) {
                    enumDomain = EnumDomain.of(param, options);
                    return this;
                }

                public EnumDomainBuilder any() {
                    enumDomain = EnumDomain.any(param);
                    return this;
                }

                public EnumDomainBuilder empty() {
                    enumDomain = EnumDomain.empty(param);
                    return this;
                }

                public EnumDomainBuilder notContainsEmpty() {
                    enumDomain = EnumDomain.notContainsEmpty(param);
                    return this;
                }

                public ParameterAssertion endDomain() {
                    Assert.assertTrue(enumDomain.isEqual(item.getMutation().getDomain()));
                    return ParameterAssertion.this;
                }
            }

            public class NumericDomainBuilder {
                private NumericDomain numericDomain;

                public NumericDomainBuilder range(long start, long end) {
                    numericDomain = NumericDomain.range(param, start, end);
                    return this;
                }

                public NumericDomainBuilder range(BigDecimal start, BigDecimal end) {
                    numericDomain = NumericDomain.range(param, start, end);
                    return this;
                }

                public NumericDomainBuilder singleEmpty() {
                    numericDomain = NumericDomain.singleEmptyValue(param);
                    return this;
                }

                public NumericDomainBuilder single(long single) {
                    return single(new BigDecimal(single));
                }

                public NumericDomainBuilder single(double single) {
                    return single(new BigDecimal(single));
                }

                public NumericDomainBuilder single(BigDecimal single) {
                    numericDomain = NumericDomain.single(param, single);
                    return this;
                }

                public NumericDomainBuilder any() {
                    numericDomain = NumericDomain.any(param);
                    return this;
                }

                public NumericDomainBuilder empty() {
                    numericDomain = NumericDomain.empty(param);
                    return this;
                }

                public NumericDomainBuilder notContainsEmpty() {
                    numericDomain = NumericDomain.notContainsEmpty(param);
                    return this;
                }

                public ParameterAssertion endDomain() {
                    Assert.assertTrue(numericDomain.isEqual(item.getMutation().getDomain()));
                    return ParameterAssertion.this;
                }
            }

            private ThinCategoryParam param;
            private ModelRuleResultItem item;
            private ParameterValues value;

            public ParameterAssertion(long paramId) {
                this.param = paramsById.get(paramId);
                item = result.getItemsByParamId().get(paramId);
                value = item.getValue();
            }

            public ParameterAssertion missing() {
                Assert.assertNull(item);
                return this;
            }

            public ParameterAssertion shouldNull() {
                Assert.assertNull(value);
                return this;
            }

            public ParameterAssertion isEmpty() {
                Assert.assertNotNull(value);
                Assert.assertTrue(value.isEmpty());
                return this;
            }

            public ParameterAssertion numeric(long val) {
                return numeric(new BigDecimal(val));
            }

            public ParameterAssertion numeric(BigDecimal val) {
                Assert.assertNotNull("Expected value '" + value + "' to be not null. Actual is null", value);
                Assert.assertTrue("Expected value '" + value + "' to be single. Actual is not", value.isSingle());
                BigDecimal actual = value.getSingle().getNumericValue();
                Assert.assertEquals("value should be equal " + val + ", but " + actual,
                    0, actual.compareTo(val));
                return this;
            }

            public ParameterAssertion optionId(Long optionId) {
                Assert.assertNotNull("Expected value '" + value + "' to be not null. Actual is null", value);
                Assert.assertTrue("Expected value '" + value + "' to be single. Actual is not", value.isSingle());
                Assert.assertEquals(optionId, value.getSingle().getOptionId());
                return this;
            }

            public ParameterAssertion optionIds(Long... optionId) {
                Assert.assertNotNull("value should not be null", value);
                Assert.assertEquals(Arrays.asList(optionId), value.getOptionIds());
                return this;
            }

            public ParameterAssertion bool(boolean bool) {
                Assert.assertNotNull("Expected value '" + value + "' to be not null. Actual is null", value);
                Assert.assertTrue("Expected value '" + value + "' to be single. Actual is not", value.isSingle());
                Assert.assertEquals(bool, value.getSingle().getBooleanValue());
                Assert.assertEquals(OptionUtils.findBooleanOption(param, bool).getValueId(),
                    value.getSingle().getOptionId().longValue());
                return this;
            }

            public ParameterAssertion string(String... str) {
                Assert.assertNotNull(this.value);
                Assertions.assertThat(this.value.getStringValues())
                    .extracting(Word::getWord)
                    .isEqualTo(Arrays.asList(str));
                return this;
            }

            public ParameterAssertion valid() {
                Assert.assertNotNull(item);
                Assert.assertTrue(item.isValid());
                return this;
            }

            public ParameterAssertion invalid() {
                Assert.assertNotNull(item);
                Assert.assertTrue(!item.isValid());
                return this;
            }

            public ParameterAssertion modified() {
                Assert.assertNotNull(item);
                Assert.assertTrue("value should be modified", item.isValueModified());
                return this;
            }

            public ParameterAssertion notModified() {
                Assert.assertNotNull(item);
                Assert.assertTrue(!item.isValueModified());
                return this;
            }

            public ParameterAssertion conflict() {
                Assert.assertNotNull(item);
                Assert.assertSame(ResultType.CONFLICT, item.getResultType());
                return this;
            }

            public ParameterAssertion maxFailedPriority(Integer maxFailedPriority) {
                Assert.assertNotNull(item);
                Assert.assertEquals(maxFailedPriority, item.getMaxFailedPriority());
                return this;
            }

            public StringDomainBuilder stringDomain() {
                return new StringDomainBuilder();
            }

            public EnumDomainBuilder enumDomain() {
                return new EnumDomainBuilder();
            }

            public NumericDomainBuilder numericDomain() {
                return new NumericDomainBuilder();
            }

            public ResultsAssertion endParam() {
                return ResultsAssertion.this;
            }
        }

        public ResultsAssertion valid() {
            Assert.assertTrue(result.isValid());
            return this;
        }

        public ResultsAssertion empty() {
            Assert.assertTrue(result.getItemsByParamId().isEmpty());
            return this;
        }

        public ResultsAssertion count(int resultsCount) {
            Assert.assertEquals(resultsCount, result.getItemsByParamId().size());
            return this;
        }

        public ResultsAssertion iterationCount(int iterationCount) {
            Assert.assertEquals(iterationCount, context.getIterationCount());
            return this;
        }

        public ParameterAssertion param(String paramXslName) {
            ThinCategoryParam param = paramsByName.get(paramXslName);
            Assert.assertNotNull(param);
            return param(param.getId());
        }

        public ResultsAssertion noParam(String paramXslName) {
            ThinCategoryParam param = paramsByName.get(paramXslName);
            Assert.assertNotNull(param);
            return this;
        }

        public ParameterAssertion param(long paramId) {
            return new ParameterAssertion(paramId);
        }

        public ModelRuleTester endResults() {
            return ModelRuleTester.this;
        }
    }

    private ModificationSource modificationSource;

    protected Map<Long, CommonModel> models = new HashMap<>();
    protected Map<Long, ModelRuleSet> ruleSets = new HashMap<>();

    private ModelRuleResult result;
    private ExecutionContext context;
    protected CommonModel resultModel;
    private List<? extends ThinCategoryParam> params;
    private Map<Long, ThinCategoryParam> paramsById = new HashMap<>();
    private Map<String, ThinCategoryParam> paramsByName = new HashMap<>();

    protected CommonModel currentModel;
    protected ModelRuleSet currentRuleSet;

    private static final ModelRuleExecutor MODEL_RULE_EXECUTOR = new ModelRuleExecutor(
        JavaModelRuleValidator.INSTANCE, new NashornJsExecutor());

    private final ModelRuleExecutor executor;

    private ModelRuleTester() {
        executor = MODEL_RULE_EXECUTOR;
    }

    public ModelRuleTester(ModelRuleExecutor executor) {
        this.executor = executor;
    }

    public ThinCategoryParam param(long paramId) {
        return this.paramsById.get(paramId);
    }

    public ThinCategoryParam param(String paramXslName) {
        return this.paramsByName.get(paramXslName);
    }

    public static ModelRuleTester testCase() {
        return new ModelRuleTester();
    }

    public static ModelRuleTester testCaseWith(ModelRuleExecutor executor) {
        return new ModelRuleTester(executor);
    }

    public ParametersBuilder<ModelRuleTester> startParameters() {
        return new ParametersBuilder<>(this::endParameters);
    }

    public CommonModelBuilder<ModelRuleTester> startModel() {
        if (params == null) {
            throw new IllegalStateException("Missing parameters definition");
        }
        return new CommonModelBuilder<>(this::endModel).parameters(paramsById, paramsByName);
    }

    public TestModelRuleBuilder startRuleSet() {
        if (params == null) {
            throw new IllegalStateException("Missing parameters definition");
        }
        return new TestModelRuleBuilder()
            .parameters(paramsByName).parametersById(paramsById);
    }

    public ModelRuleTester selectModel(long id) {
        CommonModel m = this.models.get(id);
        if (m == null) {
            throw new IllegalArgumentException(
                "Failed to find model with id: " + id);
        }
        this.currentModel = m;
        return this;
    }

    public ModelRuleTester selectRuleSet(long id) {
        ModelRuleSet rs = this.ruleSets.get(id);
        if (rs == null) {
            throw new IllegalArgumentException(
                "Failed to find rule set with id: " + id);
        }
        this.currentRuleSet = rs;
        return this;
    }

    public ModelRuleTester doInference() {
        context = executor.applyRulesInternal(paramsById.values(),
            currentRuleSet.getRules(), new FullModel(currentModel, models), modificationSource, EMPTY_LIST);
        result = context.getResult();
        resultModel = new CommonModel(currentModel);
        for (ModelRuleResultItem item : result.getItemsByParamId().values()) {
            if (item.getValue() != null) {
                resultModel.putParameterValues(ParameterValues.of(item.getValue()));
            }
        }
        return this;
    }

    public ModelRuleFailTester doInferenceWithFail() {
        try {
            executor.applyRulesInternal(paramsById.values(),
                currentRuleSet.getRules(), new FullModel(currentModel, models), modificationSource, EMPTY_LIST);
            throw new AssertionError("Expecting rules applying to fail");
        } catch (Throwable e) {
            return new ModelRuleFailTester(e);
        }
    }

    public ResultsAssertion results() {
        return new ResultsAssertion();
    }

    public ModelRuleTester endParameters(List<CategoryParam> params) {
        this.params = params;
        for (ThinCategoryParam param : this.params) {
            paramsById.put(param.getId(), param);
            paramsByName.put(param.getXslName(), param);
        }
        return this;
    }

    public ModelRuleTester endModel(CommonModel model) {
        if (model != null) {
            currentModel = model;
            models.put(model.getId(), currentModel);
        }
        return this;
    }

}
