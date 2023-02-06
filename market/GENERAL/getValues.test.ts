import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { getValues } from './getValues';
import { RU_ISO_CODE } from './constants';

describe('getValues', () => {
  it('works with empty data', () => {
    expect(getValues()).toEqual([]);
    expect(getValues([])).toEqual([]);
  });
  it('works with ValueType.BOOLEAN', () => {
    expect(
      getValues([
        { parameterId: 123, type: ValueType.BOOLEAN, booleanValue: true },
        { parameterId: 123, type: ValueType.BOOLEAN, booleanValue: false },
      ])
    ).toEqual([true, false]);
  });
  it('works with ValueType.ENUM', () => {
    expect(
      getValues([
        { parameterId: 123, type: ValueType.ENUM, optionId: 1 },
        { parameterId: 123, type: ValueType.ENUM, optionId: 2 },
      ])
    ).toEqual([1, 2]);
  });
  it('works with ValueType.NUMERIC_ENUM', () => {
    expect(
      getValues([
        { parameterId: 123, type: ValueType.NUMERIC_ENUM, optionId: 1 },
        { parameterId: 123, type: ValueType.NUMERIC_ENUM, optionId: 2 },
      ])
    ).toEqual([1, 2]);
  });
  it('works with ValueType.NUMERIC', () => {
    expect(
      getValues([
        { parameterId: 123, type: ValueType.NUMERIC, numericValue: '1' },
        { parameterId: 123, type: ValueType.NUMERIC, numericValue: '2' },
      ])
    ).toEqual(['1', '2']);
  });
  it('works with ValueType.STRING', () => {
    expect(
      getValues([
        { parameterId: 123, type: ValueType.STRING, stringValue: [{ value: '1', isoCode: RU_ISO_CODE }] },
        { parameterId: 123, type: ValueType.STRING, stringValue: [{ value: '2', isoCode: RU_ISO_CODE }] },
      ])
    ).toEqual(['1', '2']);
  });
});
