import { shopModel, vendorParameter, vendorRules, categoryData } from 'src/test/data';
import { collectShopModelChanges } from './collectShopModelChanges';

const originMarketValues = { [vendorParameter.id]: [vendorRules] };
const originModel = { ...shopModel, marketValues: originMarketValues };

const changedMarketValues = { [vendorParameter.id]: [] };
const changedModel = { ...shopModel, marketValues: changedMarketValues };

describe('collectShopModelChanges', () => {
  test('check changes', () => {
    const changes = collectShopModelChanges(originModel, changedModel, categoryData);
    expect(changes).toHaveLength(1);

    const changeItem = changes[0];
    expect(changeItem.shopModelId).toBe(shopModel.id);
    expect(changeItem.parameterId).toBe(vendorParameter.id);
    expect(changeItem.newValues).toBe(changedMarketValues[vendorParameter.id]);
    expect(changeItem.oldValues).toBe(originMarketValues[vendorParameter.id]);
    expect(changeItem.canHotReset).toBe(true);
  });
});
