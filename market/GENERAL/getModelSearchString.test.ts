import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { ParameterId, ParameterValue, Model } from '@yandex-market/mbo-parameter-editor';

import { getModelSearchString } from './getModelSearchString';

describe('getModelSearchString', () => {
  it('works', () => {
    expect(
      getModelSearchString(
        {
          id: 123,
          parameterValues: {
            1: [{ parameterId: 1, type: ValueType.STRING, stringValue: [{ value: 'test1', isoCode: 'ru' }] }],
            2: [{ parameterId: 2, type: ValueType.STRING, stringValue: [{ value: 'test2', isoCode: 'ru' }] }],
          } as Record<ParameterId, ParameterValue[]>,
        } as Model,
        [1, 2],
        {}
      )
    ).toEqual('123test1test2');
  });
});
