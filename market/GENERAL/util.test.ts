import { sortLogEntityStat, statCellKey, uid } from 'src/pages/Formalization/Stat/util';
import { LogsEntityStat } from 'src/java/definitions';

describe('FormalizationStat utility', () => {
  it('statCellKey', () => {
    const payload = {
      categoryId: '12345',
      shopId: '67890',
      parameterId: 'abcde',
    };
    const result = statCellKey(payload);
    expect(result).toEqual('12345:67890:abcde:');
  });

  it('sortLogEntityStat', () => {
    const arr = [
      {
        id: 0,
        name: 'cat',
        formalised: 90,
        totalOffers: 100,
        trashOffers: 5,
      } as LogsEntityStat,
      {
        id: 2,
        name: 'cat',
        formalised: 90,
        totalOffers: 110,
        trashOffers: 5,
      } as LogsEntityStat,
    ];
    const result = sortLogEntityStat(arr);
    expect(result).toEqual(arr.reverse());
  });

  describe('uid test', () => {
    it('category shop', () => {
      const categoryId = '12345';
      const result = uid.category.shops(categoryId);
      expect(result).toEqual('category-shop-12345:::');
    });

    it('category params', () => {
      const categoryId = 12345;
      const result = uid.category.params(categoryId);
      expect(result).toEqual('category-param-12345:::');
    });

    it('shop', () => {
      const categoryId = '12345';
      const shopId = 67890;
      const result = uid.shop(categoryId, shopId);
      expect(result).toEqual('shop-12345:67890::');
    });

    it('paramForShop', () => {
      const categoryId = '12345';
      const shopId = 67890;
      const result = uid.paramForShop(categoryId, shopId);
      expect(result).toEqual('shop-param-12345:67890::');
    });

    it('paramValue paramForShop', () => {
      const categoryId = '12345';
      const shopId = 67890;
      const parameterId = 'abcde';
      const result = uid.paramValue.paramForShop(categoryId, shopId, parameterId);
      expect(result).toEqual('shop-param-value-12345:67890:abcde:');
    });

    it('paramValue paramForCategory', () => {
      const categoryId = '12345';
      const parameterId = 'abcde';
      const result = uid.paramValue.paramForCategory(categoryId, parameterId);
      expect(result).toEqual('category-param-value-12345::abcde:');
    });
  });
});
