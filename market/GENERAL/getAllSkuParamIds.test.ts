import { SKUParameterMode } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { CategoryData, Parameter } from '@yandex-market/mbo-parameter-editor';
import { partialWrapper } from '@yandex-market/mbo-test-utils';

import { getAllSkuParamIds } from './getAllSkuParamIds';

describe('getAllSkuParamIds', () => {
  it('works empty', () => {
    expect(getAllSkuParamIds()).toEqual([]);
    expect(getAllSkuParamIds(partialWrapper({}))).toEqual([]);
  });
  it('works', () => {
    expect(
      getAllSkuParamIds(
        partialWrapper<CategoryData>({
          parameters: {
            123: partialWrapper<Parameter>({
              id: 123,
              skuMode: SKUParameterMode.SKU_INFORMATIONAL,
            }),
            1232: partialWrapper<Parameter>({
              id: 1232,
              skuMode: SKUParameterMode.SKU_INFORMATIONAL,
            }),
            234: partialWrapper<Parameter>({
              id: 234,
              skuMode: SKUParameterMode.SKU_DEFINING,
              name: 'test2',
            }),
            2342: partialWrapper<Parameter>({
              id: 2342,
              skuMode: SKUParameterMode.SKU_DEFINING,
              name: 'test1',
            }),
          },
        })
      )
    ).toEqual([123, 1232, 2342, 234]);
  });
});
