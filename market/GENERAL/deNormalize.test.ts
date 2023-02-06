import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { ParameterValue } from './types';
import { getProtoParameterValues } from './deNormalize';

const values: Record<number, ParameterValue[]> = {
  1: [
    {
      parameterId: 1,
      type: ValueType.BOOLEAN,
      booleanValue: true,
      optionId: 1,
      xslName: 'param1',
    },
  ],
  2: [
    {
      parameterId: 2,
      type: ValueType.BOOLEAN,
      booleanValue: undefined,
      optionId: undefined,
      xslName: 'param2',
    },
  ],
  3: [
    {
      parameterId: 3,
      type: ValueType.ENUM,
      optionId: 1,
      xslName: 'param3',
    },
    {
      parameterId: 3,
      type: ValueType.ENUM,
      optionId: 2,
      xslName: 'param3',
    },
    {
      parameterId: 3,
      type: ValueType.ENUM,
      optionId: undefined,
      xslName: 'param3',
    },
  ],
  4: [
    {
      parameterId: 4,
      type: ValueType.NUMERIC,
      numericValue: '1.99',
      xslName: 'param4',
    },
    {
      parameterId: 4,
      type: ValueType.NUMERIC,
      numericValue: undefined,
      xslName: 'param4',
    },
  ],
  5: [
    {
      parameterId: 5,
      type: ValueType.NUMERIC_ENUM,
      optionId: 1,
      xslName: 'param5',
    },
    {
      parameterId: 5,
      type: ValueType.NUMERIC_ENUM,
      optionId: 2,
      xslName: 'param5',
    },
    {
      parameterId: 5,
      type: ValueType.NUMERIC_ENUM,
      optionId: undefined,
      xslName: 'param5',
    },
  ],
  6: [
    {
      parameterId: 6,
      type: ValueType.STRING,
      stringValue: [
        {
          isoCode: 'ru',
          value: 'foo',
        },
      ],
      xslName: 'param6',
    },
    {
      parameterId: 6,
      type: ValueType.STRING,
      stringValue: [
        {
          isoCode: 'ru',
          value: 'bar',
        },
      ],
      xslName: 'param6',
    },
    {
      parameterId: 6,
      type: ValueType.STRING,
      stringValue: [
        {
          isoCode: 'ru',
          value: '  baz  ',
        },
      ],
      xslName: 'param6',
    },
    {
      parameterId: 6,
      type: ValueType.STRING,
      stringValue: undefined,
      xslName: 'param6',
    },
    {
      parameterId: 6,
      type: ValueType.STRING,
      stringValue: [
        {
          isoCode: 'ru',
          value: '    ',
        },
      ],
      xslName: 'param6',
    },
  ],
};

describe('src/utils/convertParameterValuesToProto', () => {
  test('должен вернуть массив денормализованных значений параметров', () => {
    expect(getProtoParameterValues(values)).toEqual([
      {
        param_id: 1,
        value_type: ValueType.BOOLEAN,
        type_id: 0,
        bool_value: true,
        xsl_name: 'param1',
        option_id: 1,
      },
      {
        param_id: 3,
        value_type: ValueType.ENUM,
        type_id: 1,
        option_id: 1,
        xsl_name: 'param3',
      },
      {
        param_id: 3,
        value_type: ValueType.ENUM,
        type_id: 1,
        option_id: 2,
        xsl_name: 'param3',
      },
      {
        param_id: 4,
        value_type: ValueType.NUMERIC,
        type_id: 2,
        numeric_value: '1.99',
        xsl_name: 'param4',
      },
      {
        param_id: 5,
        value_type: ValueType.NUMERIC_ENUM,
        type_id: 3,
        option_id: 1,
        xsl_name: 'param5',
      },
      {
        param_id: 5,
        value_type: ValueType.NUMERIC_ENUM,
        type_id: 3,
        option_id: 2,
        xsl_name: 'param5',
      },
      {
        param_id: 6,
        value_type: ValueType.STRING,
        type_id: 4,
        str_value: [
          {
            isoCode: 'ru',
            value: 'foo',
          },
        ],
        xsl_name: 'param6',
      },
      {
        param_id: 6,
        value_type: ValueType.STRING,
        type_id: 4,
        str_value: [
          {
            isoCode: 'ru',
            value: 'bar',
          },
        ],
        xsl_name: 'param6',
      },
      {
        param_id: 6,
        value_type: ValueType.STRING,
        type_id: 4,
        str_value: [
          {
            isoCode: 'ru',
            value: 'baz',
          },
        ],
        xsl_name: 'param6',
      },
    ]);
  });

  test('должен вернуть пустой массив', () => {
    expect(
      getProtoParameterValues({
        2: [
          {
            parameterId: 2,
            type: ValueType.BOOLEAN,
            booleanValue: undefined,
          },
        ],
        3: [
          {
            parameterId: 3,
            type: ValueType.ENUM,
            optionId: undefined,
          },
        ],
        4: [
          {
            parameterId: 4,
            type: ValueType.NUMERIC,
            numericValue: undefined,
          },
        ],
        5: [
          {
            parameterId: 5,
            type: ValueType.NUMERIC_ENUM,
            optionId: undefined,
          },
        ],
        6: [
          {
            parameterId: 6,
            type: ValueType.STRING,
            stringValue: undefined,
          },
          {
            parameterId: 6,
            type: ValueType.STRING,
            stringValue: [
              {
                isoCode: 'ru',
                value: '    ',
              },
            ],
          },
        ],
      })
    ).toEqual([]);
  });
});
