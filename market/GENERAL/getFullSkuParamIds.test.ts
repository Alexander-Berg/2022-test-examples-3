import { SKUParameterMode } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { CategoryData } from '@yandex-market/mbo-parameter-editor/es/entities/categoryData/types';
import { Parameter } from '@yandex-market/mbo-parameter-editor/es/entities/parameter/types';

import { partialWrapper } from 'src/test/utils/partialWrapper';
import { getFullSkuParamIds } from './getFullSkuParamIds';

describe('getFullSkuParamIds', () => {
  it('works empty', () => {
    expect(getFullSkuParamIds()).toEqual([]);
    expect(getFullSkuParamIds(partialWrapper({}))).toEqual([]);
  });
  it('works', () => {
    expect(
      getFullSkuParamIds(
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
