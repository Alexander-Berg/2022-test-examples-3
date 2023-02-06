import { simpleMapping, shopModel, vendorParameter } from 'src/test/data';
import { getShopValues } from './ValueInfo.utils';

test('value info utils', () => {
  const values = getShopValues([simpleMapping], shopModel, vendorParameter.id);
  expect(values.length).toBe(1);
  expect(values[0].paramName).toBe(vendorParameter.name);
  expect(values[0].value).toBe(shopModel.shopValues.vendor);
});

test('value info utils - without shopValues', () => {
  const values = getShopValues([simpleMapping], { ...shopModel, shopValues: {} }, vendorParameter.id);
  expect(values.length).toBe(0);
});
