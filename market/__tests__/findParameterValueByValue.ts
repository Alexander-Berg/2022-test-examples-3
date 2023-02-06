import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { findParameterValueByValue } from 'src/utils/findParameterValueByValue';

const parameters = [
  {
    param_id: 1,
    value_type: ValueType.BOOLEAN,
    bool_value: true,
  },
  {
    param_id: 2,
    value_type: ValueType.ENUM,
    option_id: 1,
  },
  {
    param_id: 3,
    value_type: ValueType.NUMERIC_ENUM,
    option_id: 2,
  },
  {
    param_id: 4,
    value_type: ValueType.NUMERIC,
    numeric_value: '199.9',
  },
  {
    param_id: 5,
    value_type: ValueType.STRING,
    str_value: [
      {
        isoCode: 'ru',
        value: 'c',
      },
      {
        isoCode: 'ru',
        value: 'b',
      },
      {
        isoCode: 'ru',
        value: 'a',
      },
    ],
  },
];

describe('src/utils/findParameterValueByValue', () => {
  test('должен вернуть найденное значение параметра', () => {
    for (const parameter of parameters) {
      expect(findParameterValueByValue(parameter, parameters)).toEqual(parameter);
    }

    expect(
      findParameterValueByValue(
        {
          param_id: 4,
          value_type: ValueType.NUMERIC,
          numeric_value: '0199.90',
        },
        parameters
      )
    ).toEqual({
      param_id: 4,
      value_type: ValueType.NUMERIC,
      numeric_value: '199.9',
    });
  });

  test('должен вернуть undefined', () => {
    const paramValue = { param_id: 10, value_type: ValueType.BOOLEAN, bool_value: true };

    expect(findParameterValueByValue(paramValue)).toBeUndefined();
    expect(findParameterValueByValue(paramValue, parameters)).toBeUndefined();

    expect(
      findParameterValueByValue({ param_id: 1, value_type: 'boolean', bool_value: true } as any, parameters)
    ).toBeUndefined();
  });
});
