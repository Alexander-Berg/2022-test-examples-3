import { ValueType } from 'src/java/definitions';
import { categoryData, simpleMapping, stringParameter, parameter } from 'src/test/data';
import { getParameterType, isSameParameterValue } from './parameter-values';

describe('parameter-values', () => {
  test('getParameterType mapping witthout parameters', () => {
    // в маппинге можеет не быть параметров, к примеру в мапингах на картинки
    const type = getParameterType({ ...simpleMapping, marketParams: [] }, categoryData);
    expect(type).toBeUndefined();
  });

  test('getParameterType mapping with string parameter', () => {
    const type = getParameterType(
      { ...simpleMapping, marketParams: [{ parameterId: stringParameter.hid }] },
      categoryData
    );
    expect(type).toBe('STRING');
  });

  getCompareMarketValuesTestCases().forEach(el => {
    test(`isSameMarketValue parameter ${el.valueType} -> ${el.expect}`, () => {
      expect(isSameParameterValue(el.originParam, el.editableParam, { ...parameter, valueType: el.valueType })).toBe(
        el.expect
      );
    });
  });
});

function getCompareMarketValuesTestCases() {
  return [
    {
      editableParam: { booleanValue: true },
      originParam: { booleanValue: false },
      valueType: ValueType.BOOLEAN,
      expect: false,
    },
    {
      editableParam: { booleanValue: true },
      originParam: { booleanValue: true },
      valueType: ValueType.BOOLEAN,
      expect: true,
    },
    {
      editableParam: { stringValue: '123' },
      originParam: { stringValue: '321' },
      valueType: ValueType.STRING,
      expect: false,
    },
    {
      editableParam: { stringValue: '123' },
      originParam: { stringValue: '123' },
      valueType: ValueType.STRING,
      expect: true,
    },
    {
      editableParam: { numericValue: 123 },
      originParam: { numericValue: 321 },
      valueType: ValueType.NUMERIC,
      expect: false,
    },
    {
      editableParam: { numericValue: 123 },
      originParam: { numericValue: 123 },
      valueType: ValueType.NUMERIC,
      expect: true,
    },
    {
      editableParam: { hypothesis: '123', optionId: 1 },
      originParam: { hypothesis: '123', optionId: 4 },
      valueType: ValueType.ENUM,
      expect: false,
    },
    {
      editableParam: { hypothesis: '123', optionId: 1 },
      originParam: { hypothesis: '123', optionId: 1 },
      valueType: ValueType.ENUM,
      expect: true,
    },
  ];
}
