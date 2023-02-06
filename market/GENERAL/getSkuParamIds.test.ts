import { SKUParameterMode } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { getSkuParamIds } from './getSkuParamIds';
import { BARCODE_PARAM_ID, VENDOR_CODE_PARAM_ID } from './constants';
import { CategoryData, Parameter } from 'src/entities';

describe('getSkuParamIds', () => {
  it('works with empty', () => {
    expect(getSkuParamIds({} as any)).toEqual([]);
  });
  it('works with data', () => {
    expect(
      getSkuParamIds(
        cdw({
          parameters: [
            pw({
              isRequired: true,
              id: 1,
            }),
            pw({
              showOnSkuTab: true,
              id: 2,
            }),
            pw({
              skuMode: SKUParameterMode.SKU_INFORMATIONAL,
              id: 3,
            }),
            pw({
              skuMode: SKUParameterMode.SKU_DEFINING,
              id: 3,
            }),
            pw({
              showOnSkuTab: true,
              id: 5,
            }),
            pw({
              showOnSkuTab: true,
              id: 6,
            }),
            pw({
              isRequired: true,
              skuMode: SKUParameterMode.SKU_INFORMATIONAL,
              id: 7,
            }),
            pw({
              showOnSkuTab: true,
              skuMode: SKUParameterMode.SKU_INFORMATIONAL,
              id: 8,
            }),
          ],
        })
      )
    ).toEqual([3, 7, 8]);
  });
  it('sort', () => {
    expect(
      getSkuParamIds(
        cdw({
          parameters: [
            pw({
              skuMode: SKUParameterMode.SKU_DEFINING,
              id: 3,
            }),
            pw({
              skuMode: SKUParameterMode.SKU_DEFINING,
              id: VENDOR_CODE_PARAM_ID,
            }),
            pw({
              skuMode: SKUParameterMode.SKU_DEFINING,
              id: BARCODE_PARAM_ID,
            }),
          ],
        })
      )
    ).toEqual([BARCODE_PARAM_ID, VENDOR_CODE_PARAM_ID, 3]);
  });
});

function pw(p: Partial<Parameter>) {
  return p as Parameter;
}

function cdw(c: Partial<CategoryData>) {
  return c as CategoryData;
}
