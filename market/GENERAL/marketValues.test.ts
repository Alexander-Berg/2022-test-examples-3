import { ValueSource } from 'src/java/definitions';
import { formalizationValues, vendorParameter } from 'src/test/data';
import { isSameMarketParameterValues } from './marketValues';

describe('isSameMarketParameterValues', () => {
  getNoSameValueCases().forEach(testCase => {
    test(`no same values: ${testCase.name}`, () => {
      const isSame = isSameMarketParameterValues(testCase.originValues, testCase.changedValues, testCase.parameter);
      expect(isSame).toBeFalsy();
    });
  });

  test('compare same market values', () => {
    const isSame = isSameMarketParameterValues(formalizationValues, formalizationValues, vendorParameter);
    expect(isSame).toBeTruthy();
  });
});

function getNoSameValueCases() {
  const parameterValue = {
    hypothesis: 'king',
    optionId: 161,
  };

  const ruleId = 123;

  return [
    {
      name: 'разная длина',
      originValues: [
        {
          parameterId: vendorParameter.id,
          ruleId,
          value: parameterValue,
          valueSource: ValueSource.RULE,
        },
      ],
      changedValues: [],
      parameter: vendorParameter,
    },
    {
      name: 'разные id-шники правил',
      originValues: [
        {
          parameterId: vendorParameter.id,
          ruleId,
          value: parameterValue,
          valueSource: ValueSource.RULE,
        },
      ],
      changedValues: [
        {
          parameterId: vendorParameter.id,
          ruleId: 321,
          value: parameterValue,
          valueSource: ValueSource.RULE,
        },
      ],
      parameter: vendorParameter,
    },
    {
      name: 'разные valueSource',
      originValues: [
        {
          parameterId: vendorParameter.id,
          ruleId,
          value: parameterValue,
          valueSource: ValueSource.RULE,
        },
      ],
      changedValues: [
        {
          parameterId: vendorParameter.id,
          ruleId,
          value: parameterValue,
          valueSource: ValueSource.MANUAL,
        },
      ],
      parameter: vendorParameter,
    },
    {
      name: 'разные значения параметра',
      originValues: [
        {
          ruleId: 0,
          parameterId: vendorParameter.id,
          value: parameterValue,
          valueSource: ValueSource.MANUAL,
        },
      ],
      changedValues: [
        {
          ruleId: 0,
          parameterId: vendorParameter.id,
          value: {
            hypothesis: 'next',
            optionId: 162,
          },
          valueSource: ValueSource.MANUAL,
        },
      ],
      parameter: vendorParameter,
    },
  ];
}
