import { extractPossibleValues, collectPossibleValues, getShopValues } from './possibleValues';
import { shopModel, simpleMapping, rule } from 'src/test/data';

const shopValues = {
  Материал: 'Керамика, сталь',
};

const shopParam = {
  name: 'Материал',
  split: undefined,
};

const mapping = {
  ...simpleMapping,
  shopParams: [shopParam],
};

const models = [{ ...shopModel, shopValues }];

describe('Сбор всех возможных магазинных значений', () => {
  test('extractPossibleValues получаем значения из shopValues ', () => {
    const possibleValue = extractPossibleValues(shopValues, [shopParam]);
    expect(possibleValue).toEqual([{ Материал: 'керамика, сталь' }]);
  });

  test('extractPossibleValues получаем значения из shopValues с разделителем', () => {
    const possibleValue = extractPossibleValues(shopValues, [{ ...shopParam, split: ',' }]);
    expect(possibleValue).toEqual([{ Материал: 'керамика' }, { Материал: 'сталь' }]);
  });

  test('collectPossibleValues получаем значения магазина из товаров и мапинга ', () => {
    const possibleValue = collectPossibleValues(models, mapping);
    expect(possibleValue).toEqual([{ Материал: 'керамика, сталь' }]);
  });

  test('collectPossibleValues получаем значения магазина из товаров и мапинга с existingValues (в основном достаются из правил)', () => {
    const possibleValue = collectPossibleValues(models, mapping, [rule.shopValues]);
    expect(possibleValue).toEqual([rule.shopValues, { Материал: 'керамика, сталь' }]);
  });

  test('getShopValues получаем только значения магазина без указания названия параметра', () => {
    const possibleValue = getShopValues(models, mapping);
    expect(possibleValue).toEqual(['керамика, сталь']);
  });
});
