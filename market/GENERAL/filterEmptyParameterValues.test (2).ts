import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { filterEmptyProtoParameterValues } from 'src/shared/common-logs/helpers/filterEmptyParameterValues';

describe('filterEmptyProtoParameterValues', () => {
  it('works with ValueType.STRING', () => {
    expect(
      filterEmptyProtoParameterValues([
        { value_type: ValueType.STRING, str_value: [{ value: ' test ' }] },
        { value_type: ValueType.STRING, str_value: [{ value: '   ' }] },
        { value_type: ValueType.STRING, str_value: [{}] },
        { value_type: ValueType.STRING },
      ])
    ).toEqual([{ value_type: ValueType.STRING, str_value: [{ value: 'test' }] }]);
  });
  it('works with ValueType.NUMERIC', () => {
    expect(
      filterEmptyProtoParameterValues([
        { value_type: ValueType.NUMERIC, numeric_value: '1' },
        { value_type: ValueType.NUMERIC, numeric_value: '' },
        { value_type: ValueType.NUMERIC },
      ])
    ).toEqual([{ value_type: ValueType.NUMERIC, numeric_value: '1' }]);
  });
  it('works with ValueType.BOOLEAN', () => {
    expect(
      filterEmptyProtoParameterValues([
        { value_type: ValueType.BOOLEAN, bool_value: true },
        { value_type: ValueType.BOOLEAN, bool_value: false },
        { value_type: ValueType.BOOLEAN },
      ])
    ).toEqual([
      { value_type: ValueType.BOOLEAN, bool_value: true },
      { value_type: ValueType.BOOLEAN, bool_value: false },
    ]);
  });
  it('works with ValueType.ENUM', () => {
    expect(
      filterEmptyProtoParameterValues([{ value_type: ValueType.ENUM, option_id: 1 }, { value_type: ValueType.ENUM }])
    ).toEqual([{ value_type: ValueType.ENUM, option_id: 1 }]);
  });
  it('works with other', () => {
    expect(filterEmptyProtoParameterValues([{ value_type: ValueType.HYPOTHESIS }])).toEqual([
      { value_type: ValueType.HYPOTHESIS },
    ]);
  });
});
