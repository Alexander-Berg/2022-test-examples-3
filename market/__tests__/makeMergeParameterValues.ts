import { ModificationSource } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { makeMergeParameterValues } from 'src/utils/makeMergeParameterValues';

const values = [
  {
    param_id: 1,
    value_type: ValueType.BOOLEAN,
    type_id: 0,
    bool_value: true,
    xsl_name: 'param1',
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
      {
        isoCode: 'ru',
        value: 'bar',
      },
      {
        isoCode: 'ru',
        value: 'baz',
      },
    ],
    xsl_name: 'param6',
  },
];

describe('src/utils/makeMergeParameterValues', () => {
  test('должен вернуть исходный массив', () => {
    const merge = makeMergeParameterValues();

    expect(merge([...values], [...values])).toEqual(values);
  });

  test('должен добавить всем новым значениям value_source', () => {
    const merge = makeMergeParameterValues(v => ({
      ...v,
      value_source: ModificationSource.OPERATOR_FILLED,
    }));

    expect(
      merge(
        [...values],
        [
          ...values,
          {
            param_id: 100,
            value_type: ValueType.BOOLEAN,
            type_id: 0,
            bool_value: false,
            xsl_name: 'param100',
          },
        ]
      )
    ).toEqual([
      ...values,
      {
        param_id: 100,
        value_type: ValueType.BOOLEAN,
        type_id: 0,
        bool_value: false,
        xsl_name: 'param100',
        value_source: ModificationSource.OPERATOR_FILLED,
      },
    ]);
  });
});
