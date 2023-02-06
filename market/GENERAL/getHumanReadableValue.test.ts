import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { getHumanReadableValue, getHumanReadableValues } from 'src/shared/common-logs/helpers/getHumanReadableValue';

describe('getHumanParameterValue', () => {
  it('works with ValueType.STRING', () => {
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.STRING }, {})).toEqual('');
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.STRING, stringValue: [{}] }, {})).toEqual('');
    expect(
      getHumanReadableValue(
        { parameterId: 123, type: ValueType.STRING, stringValue: [{ value: 'test', isoCode: 'ru' }] },
        {}
      )
    ).toEqual('test');
  });
  it('works with ValueType.BOOLEAN', () => {
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.BOOLEAN }, {}, true)).toEqual('');
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.BOOLEAN }, {})).toEqual('Нет информации');
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.BOOLEAN, booleanValue: true }, {})).toEqual('Да');
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.BOOLEAN, booleanValue: false }, {})).toEqual(
      'Нет'
    );
  });
  it('works with ValueType.NUMERIC', () => {
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.NUMERIC }, {})).toEqual('');
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.NUMERIC, numericValue: '1' }, {})).toEqual('1');
  });
  it('works with ValueType.NUMERIC_ENUM', () => {
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.NUMERIC_ENUM }, {}, true)).toEqual('');
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.NUMERIC_ENUM }, {})).toEqual(
      'Неизвестное значение'
    );
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.NUMERIC_ENUM, optionId: 1 })).toEqual(
      'Неизвестное значение'
    );
    expect(
      getHumanReadableValue(
        { parameterId: 123, type: ValueType.NUMERIC_ENUM, optionId: 1 },
        { 1: { id: 1, name: 'test' } }
      )
    ).toEqual('test');
  });
  it('works with ValueType.ENUM', () => {
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.ENUM }, {})).toEqual('Неизвестное значение');
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.ENUM, optionId: 1 })).toEqual(
      'Неизвестное значение'
    );
    expect(
      getHumanReadableValue({ parameterId: 123, type: ValueType.ENUM, optionId: 1 }, { 1: { id: 1, name: 'test' } })
    ).toEqual('test');
  });
  it('works with other types', () => {
    expect(getHumanReadableValue({ parameterId: 123, type: ValueType.HYPOTHESIS })).toEqual('');
  });
});

describe('getHumanParameterValues', () => {
  it('works', () => {
    expect(
      getHumanReadableValues([
        { parameterId: 123, type: ValueType.STRING, stringValue: [{ value: 'test', isoCode: 'ru' }] },
      ])
    ).toEqual(['test']);
  });
});
