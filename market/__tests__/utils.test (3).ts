import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { getParameterValueMock } from '../mocks';
import { containsValue, valueEquals } from '../utils';

describe('utils', () => {
  describe('valueEquals', () => {
    test('same links', () => {
      const val = getParameterValueMock();

      expect(valueEquals(val, val)).toBe(true);
    });

    test('different parameterId', () => {
      const val1 = getParameterValueMock({ parameterId: 1 });
      const val2 = getParameterValueMock({ parameterId: 2 });

      expect(valueEquals(val1, val2)).toBe(false);
    });

    test('different value type', () => {
      const val1 = getParameterValueMock({ parameterId: 1, type: ValueType.BOOLEAN });
      const val2 = getParameterValueMock({ parameterId: 1, type: ValueType.ENUM });

      expect(valueEquals(val1, val2)).toBe(false);
    });

    test('boolean type', () => {
      const val1 = getParameterValueMock({ parameterId: 1, type: ValueType.BOOLEAN });
      const val2 = getParameterValueMock({ parameterId: 1, type: ValueType.BOOLEAN, booleanValue: null as any });
      const val3 = getParameterValueMock({ parameterId: 1, type: ValueType.BOOLEAN, booleanValue: true });
      const val4 = getParameterValueMock({ parameterId: 1, type: ValueType.BOOLEAN, booleanValue: true });
      const val5 = getParameterValueMock({ parameterId: 1, type: ValueType.BOOLEAN, booleanValue: false });
      const val6 = getParameterValueMock({ parameterId: 1, type: ValueType.BOOLEAN, booleanValue: false });

      expect(valueEquals(val1, val2)).toBe(true);
      expect(valueEquals(val3, val4)).toBe(true);
      expect(valueEquals(val5, val6)).toBe(true);
      expect(valueEquals(val1, val3)).toBe(false);
      expect(valueEquals(val1, val5)).toBe(false);
    });

    test('enum and numeric enum type', () => {
      const val1 = getParameterValueMock({ parameterId: 1, type: ValueType.ENUM });
      const val2 = getParameterValueMock({ parameterId: 1, type: ValueType.ENUM, optionId: null as any });
      const val3 = getParameterValueMock({ parameterId: 1, type: ValueType.ENUM, optionId: 1 });
      const val4 = getParameterValueMock({ parameterId: 1, type: ValueType.ENUM, optionId: 1 });
      const val5 = getParameterValueMock({ parameterId: 1, type: ValueType.NUMERIC_ENUM, optionId: 1 });
      const val6 = getParameterValueMock({ parameterId: 1, type: ValueType.NUMERIC_ENUM, optionId: 1 });

      expect(valueEquals(val1, val2)).toBe(true);
      expect(valueEquals(val3, val4)).toBe(true);
      expect(valueEquals(val5, val6)).toBe(true);
      expect(valueEquals(val1, val3)).toBe(false);
      expect(valueEquals(val3, val5)).toBe(false);
      expect(valueEquals(val1, val5)).toBe(false);
    });

    test('numeric type', () => {
      const val1 = getParameterValueMock({ parameterId: 1, type: ValueType.NUMERIC });
      const val2 = getParameterValueMock({ parameterId: 1, type: ValueType.NUMERIC, numericValue: null as any });
      const val3 = getParameterValueMock({ parameterId: 1, type: ValueType.NUMERIC, numericValue: '1' });
      const val4 = getParameterValueMock({ parameterId: 1, type: ValueType.NUMERIC, numericValue: '1.0' });
      const val5 = getParameterValueMock({ parameterId: 1, type: ValueType.NUMERIC, numericValue: '2.0' });
      const val6 = getParameterValueMock({ parameterId: 1, type: ValueType.NUMERIC, numericValue: '2.0' });

      expect(valueEquals(val1, val2)).toBe(true);
      expect(valueEquals(val3, val4)).toBe(true);
      expect(valueEquals(val5, val6)).toBe(true);
      expect(valueEquals(val1, val3)).toBe(false);
      expect(valueEquals(val3, val5)).toBe(false);
      expect(valueEquals(val1, val5)).toBe(false);
    });

    test('string type', () => {
      const val1 = getParameterValueMock({ parameterId: 1, type: ValueType.STRING });
      const val2 = getParameterValueMock({ parameterId: 1, type: ValueType.STRING, stringValue: null as any });
      const val3 = getParameterValueMock({
        parameterId: 1,
        type: ValueType.STRING,
        stringValue: [{ value: undefined }],
      });
      const val4 = getParameterValueMock({
        parameterId: 1,
        type: ValueType.STRING,
        stringValue: [{ value: undefined }],
      });
      const val5 = getParameterValueMock({
        parameterId: 1,
        type: ValueType.STRING,
        stringValue: [{ value: 'test', isoCode: 'ru' }],
      });
      const val6 = getParameterValueMock({
        parameterId: 1,
        type: ValueType.STRING,
        stringValue: [{ value: 'test', isoCode: 'ru' }],
      });

      expect(valueEquals(val1, val2)).toBe(true);
      expect(valueEquals(val3, val4)).toBe(true);
      expect(valueEquals(val5, val6)).toBe(true);
      expect(valueEquals(val1, val3)).toBe(false);
      expect(valueEquals(val3, val5)).toBe(false);
      expect(valueEquals(val1, val5)).toBe(false);
    });
  });

  describe('containsValue', () => {
    test('should contains value', () => {
      const search = getParameterValueMock();
      const values = [getParameterValueMock(), getParameterValueMock(), search];

      expect(containsValue(search, values)).toBe(true);
    });

    test('not should contains value', () => {
      const search = getParameterValueMock();
      const values = [getParameterValueMock(), getParameterValueMock()];

      expect(containsValue(search, values)).toBe(false);
    });
  });
});
