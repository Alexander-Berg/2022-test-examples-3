import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { getModelMock, getParameterMock, getParameterValueMock } from '../__mocks__';
import { inheritParameters } from '../inheritParameters';

describe('inheritParameters', () => {
  test('parameter not exists in category', () => {
    const parameters = {
      1: getParameterMock({
        id: 1,
        xslName: 'bool',
        valueType: ValueType.BOOLEAN,
      }),
      2: getParameterMock({
        id: 2,
        xslName: 'numeric',
        valueType: ValueType.NUMERIC,
      }),
    };
    const boolValue = getParameterValueMock({
      parameterId: 1,
      type: ValueType.BOOLEAN,
      booleanValue: true,
    });
    const numericValue = getParameterValueMock({
      parameterId: 2,
      type: ValueType.NUMERIC,
      numericValue: '10',
    });
    const strValue = getParameterValueMock({
      parameterId: 3,
      type: ValueType.STRING,
      stringValue: [{ isoCode: 'ru', value: 'Foo' }],
    });

    const child = getModelMock({
      parameterValues: [boolValue],
    });

    const model = getModelMock({
      parameterValues: [numericValue, strValue],
    });

    const { parameterValues } = inheritParameters({ parameters, child, model });
    expect(parameterValues).toEqual({
      1: [boolValue],
      2: [numericValue],
    });
  });
});
