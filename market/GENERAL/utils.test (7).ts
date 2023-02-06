import { getShopParamKey, getRuleValue, getMarketValue } from './utils';
import { shopModel, simpleMappingWithRule, simpleRules, vendorParameter } from 'src/test/data';

test('test RuleEditor utils getShopParamKey', () => {
  const key = getShopParamKey([{ name: 'vendor' }], { vendor: 'Satake' });
  expect(key).toEqual('vendor:satake');
});

test('test RuleEditor utils getShopValues', () => {
  const withoutRule = getRuleValue({ ...simpleMappingWithRule, rules: undefined }, shopModel);
  expect(withoutRule).toBeUndefined();

  const ruleValue = getRuleValue(simpleMappingWithRule, shopModel);
  expect(ruleValue).toBeTruthy();
});

test('test RuleEditor utils getRuleValue', () => {
  const marketValue = getMarketValue(shopModel, vendorParameter.id, Object.values(simpleRules)[0]);
  expect(marketValue).toHaveLength(1);
});
