import { ModificationSource, ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { Option, Parameter, ParameterValue } from '@yandex-market/mbo-parameter-editor';
import * as R from 'ramda';

import { Domain } from 'src/Domain';
import * as mocks from 'src/mocks';
import { ModelRule, RulePredicate } from 'src/ModelRule';
import { ModelRuleExecutor } from 'src/ModelRuleExecutor/ModelRuleExecutor';
import { ModelRuleResultItem, ResultType } from 'src/ModelRuleResultItem';
import { valueIsEmpty } from 'src/utils';
import { XslNames } from 'src/XslNames';

interface TestRulePredicate extends Partial<Pick<RulePredicate, Exclude<keyof RulePredicate, 'paramId'>>> {
  xslName: string;
}

interface TestModelRule extends Partial<Pick<ModelRule, Exclude<keyof ModelRule, 'ifs' | 'thens'>>> {
  ifs: TestRulePredicate[];
  thens: TestRulePredicate[];
}

export const intersection = (domain1: Domain, domain2: Domain, result: Domain) => {
  expect(domain1.intersection(domain2).isEqual(result)).toBe(true);
  expect(domain2.intersection(domain1).isEqual(result)).toBe(true);
};

export const validation = (domain: Domain, value: ParameterValue[], expectedMessage: string) => {
  expect(domain.validateValues(value)).toEqual(expectedMessage);
};

export const commonAssertion = (parameter: Parameter, values: ParameterValue[], type: ValueType) => {
  for (const value of values) {
    expect(value.type).toBe(type);
    expect(value.parameterId).toBe(parameter.id);
  }

  return values;
};

export const createItemAssertions = (item: ModelRuleResultItem) => {
  const assertions = {
    valid: () => {
      expect(item).not.toBeUndefined();
      expect(item.isValid()).toBe(true);

      return assertions;
    },
    invalid: () => {
      expect(item).not.toBeUndefined();
      expect(item.isValid()).toBe(false);

      return assertions;
    },
    modified: () => {
      expect(item).not.toBeUndefined();
      expect(item.isValueModified()).toBe(true);

      return assertions;
    },
    notModified: () => {
      expect(item).not.toBeUndefined();
      expect(item.isValueModified()).toBe(false);
    },
    shouldUndefined: () => {
      expect(item.getValues()).toBeUndefined();

      return assertions;
    },
    isEmpty: () => {
      const values = item.getValues();

      expect(values).not.toBeUndefined();
      expect(R.all(valueIsEmpty, values || [])).toBe(true);

      return assertions;
    },
    optionIds: () => {
      const values = item.getValues();

      return expect(values!.map(value => value.optionId));
    },
    strings: () => {
      const values = item.getValues();

      return expect(values!.map(value => value.stringValue![0].value));
    },
    numerics: () => {
      const values = item.getValues();

      expect(values).not.toBeUndefined();

      return expect(values!.map(value => value.numericValue));
    },
    booleans: () => {
      const values = item.getValues();

      expect(values).not.toBeUndefined();

      return expect(values!.map(value => value.booleanValue));
    },
    maxFailedPriority(maxFailedPriority: number) {
      expect(item).not.toBeUndefined();
      expect(item.getMaxFailedPriority()).toBe(maxFailedPriority);

      return assertions;
    },
    conflict: () => {
      expect(item).not.toBeUndefined();
      expect(item.getResultType()).toBe(ResultType.CONFLICT);

      return assertions;
    },
    message: (expected: string) => {
      expect(item.getMessage()).toBe(expected);
    },
  };

  return assertions;
};

export const createModelRuleTester = () => {
  const options: Option[] = [];
  const parameters: Parameter[] = [];
  const parameterValues: ParameterValue[] = [];
  const rules: ModelRule[] = [];

  const getParamByXslName = (xslName: string) => {
    return R.find(param => param.xslName === xslName, parameters)!;
  };

  const createPredicate = (predicates: TestRulePredicate[]) => {
    return predicates.map(({ xslName, ...rest }) =>
      mocks.getRulePredicateMock({
        paramId: getParamByXslName(xslName).id,
        ...rest,
      })
    );
  };

  const builder = {
    param: (parameter: Partial<Parameter>, opts?: Array<Partial<Option>>) => {
      let optionIds;

      if (opts && opts.length > 0) {
        optionIds = [];
        for (const opt of opts) {
          const option = mocks.getOptionMock(opt);

          optionIds.push(option.id);
          options.push(option);
        }
      }

      parameters.push(mocks.getParameterMock({ ...parameter, optionIds }));

      return builder;
    },
    option: (option: Partial<Option>) => {
      options.push(mocks.getOptionMock(option));

      return builder;
    },
    value: (xslName: string, value: Partial<ParameterValue> = {}) => {
      const parameter = getParamByXslName(xslName);

      parameterValues.push(
        mocks.getParameterValueMock({
          parameterId: parameter.id,
          type: parameter.valueType,
          ...value,
        })
      );

      return builder;
    },
    rule: ({ ifs, thens, ...rest }: TestModelRule) => {
      rules.push(
        mocks.getRuleMock({
          ...rest,
          ifs: createPredicate(ifs),
          thens: createPredicate(thens),
        })
      );

      return builder;
    },
    doInference: () => {
      const categoryData = mocks.getCategoryDataMock({ id: 1, parameters, options });
      const model = mocks.getModelMock({ id: 1, categoryId: categoryData.id, parameterValues });
      const executor = new ModelRuleExecutor();
      const context = executor.applyRules(categoryData, rules, model, ModificationSource.RULE);
      const result = context.getResult();

      return {
        context: () => context,
        count: (num: number) => {
          const items = R.values(result.getItemsByParamId());

          expect(items.length).toBe(num);
        },
        iterationCount: (num: number) => {
          expect(context.getIterationCount()).toBe(num);
        },
        resultItem: (name: string) => {
          const parameter = getParamByXslName(name);

          return result.getResultItem(parameter.id);
        },
        itemAssertions: (name: string) => {
          const parameter = getParamByXslName(name);

          return createItemAssertions(result.getResultItem(parameter.id));
        },
      };
    },
  };

  return builder;
};

export const getInferenceTestData = () => {
  const tester = createModelRuleTester()
    .param({ xslName: 'num1', valueType: ValueType.NUMERIC })
    .param({ xslName: 'num2', valueType: ValueType.NUMERIC })
    .param({ xslName: 'num3', valueType: ValueType.NUMERIC })
    .param({ xslName: 'num4', valueType: ValueType.NUMERIC })
    .param({ xslName: 'str1', valueType: ValueType.STRING })
    .param({ xslName: 'str2', valueType: ValueType.STRING, isMultivalue: true })
    .param({ xslName: 'str3', valueType: ValueType.STRING })
    .param({ xslName: 'str4', valueType: ValueType.STRING })
    .param({ xslName: XslNames.OPERATOR_SIGN, valueType: ValueType.BOOLEAN }, [
      { id: 1, name: 'TRUE' },
      { id: 2, name: 'FALSE' },
    ])
    .param({ xslName: 'bool1', valueType: ValueType.BOOLEAN }, [
      { id: 1, name: 'TRUE' },
      { id: 2, name: 'FALSE' },
    ])
    .param({ xslName: 'bool2', valueType: ValueType.BOOLEAN }, [
      { id: 3, name: 'True' },
      { id: 4, name: 'False' },
    ])
    .param({ xslName: 'bool3', valueType: ValueType.BOOLEAN }, [
      { id: 1, name: 'TRUE' },
      { id: 2, name: 'FALSE' },
    ])
    .param({ xslName: 'bool4', valueType: ValueType.BOOLEAN }, [
      { id: 5, name: 'true' },
      { id: 6, name: 'false' },
    ])
    .param({ xslName: 'enum1', valueType: ValueType.ENUM }, [
      { id: 7, name: 'Option1' },
      { id: 8, name: 'Option2' },
      { id: 9, name: 'Option3' },
    ])
    .param({ xslName: 'enum2', valueType: ValueType.ENUM }, [
      { id: 7, name: 'Option1' },
      { id: 8, name: 'Option2' },
    ])
    .param({ xslName: 'enum3', valueType: ValueType.ENUM }, [{ id: 7, name: 'Option1' }])
    .param({ xslName: 'enum4', valueType: ValueType.ENUM });

  return tester;
};
