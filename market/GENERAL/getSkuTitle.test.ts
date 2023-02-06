import { SKUParameterMode, ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';
import { CategoryData, Parameter, NormalisedModel } from '@yandex-market/mbo-parameter-editor';

import { getSkuTitle } from './getSkuTitle';

describe('getSkuTitle', () => {
  it('works with insufficient data', () => {
    expect(getSkuTitle(wm({ title: 'modelTitle' }), undefined)).toEqual('modelTitle');
    expect(getSkuTitle(wm({ title: 'modelTitle' }), wc({ options: {} }))).toEqual('modelTitle');
    expect(getSkuTitle(wm({ title: 'modelTitle' }), wc({ parameters: {} }))).toEqual('modelTitle');
    expect(getSkuTitle(wm({ title: 'modelTitle' }), wc({ options: {}, parameters: {} }))).toEqual('modelTitle');
  });
  it('works with data', () => {
    expect(
      getSkuTitle(
        wm({
          title: 'modelTitle',
          parameterValues: {
            123: [{ parameterId: 123, type: ValueType.ENUM, optionId: 123 }],
            234: [
              { parameterId: 123, type: ValueType.STRING, stringValue: [{ value: 'test2', isoCode: 'ru' }] },
              { parameterId: 123, type: ValueType.STRING, stringValue: [{ value: 'test3', isoCode: 'ru' }] },
              { parameterId: 123, type: ValueType.STRING, stringValue: [{ value: 'test4' }] },
            ],
          },
        }),
        wc({
          options: { 123: { id: 123, name: 'test1' } },
          parameters: {
            123: wp({ id: 123, skuMode: SKUParameterMode.SKU_DEFINING, valueType: ValueType.ENUM }),
            231: wp({ id: 231 }),
            234: wp({ id: 234, skuMode: SKUParameterMode.SKU_DEFINING, valueType: ValueType.STRING }),
            345: wp({ id: 345, skuMode: SKUParameterMode.SKU_DEFINING, valueType: ValueType.STRING }),
          },
        })
      )
    ).toEqual('modelTitle, test1, test2, test3');
  });
});

function wm(partial: Partial<NormalisedModel>) {
  return partial as NormalisedModel;
}

function wc(partial: Partial<CategoryData>) {
  return partial as CategoryData;
}

function wp(partial: Partial<Parameter>) {
  return partial as Parameter;
}
