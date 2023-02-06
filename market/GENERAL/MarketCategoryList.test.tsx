import React from 'react';

import { categoryInfo, shopModel } from 'src/test/data';
import { MarketCategoryList, getCategories } from './MarketCategoryList';
import { setupWithReatom } from 'src/test/withReatom';
import { categoryTreeAtom, setCategoryTreeAction } from 'src/store/categories';
import { setAllShopCategoriesAction, shopCategoriesStatAtom } from '../../store/shopCategoriesStat.atom';

const categoryTree = { [categoryInfo.hid]: categoryInfo };

const statistic = [
  {
    shopCategoryName: shopModel.shopCategoryName,
    marketCategoryId: shopModel.marketCategoryId,
    shopModelTotal: 1,
  },
];

describe('MarketCategoryList', () => {
  test('render', () => {
    const { app } = setupWithReatom(
      <MarketCategoryList categoryIds={[categoryInfo.hid]} />,
      {
        categoryTreeAtom,
        shopCategoriesStatAtom,
      },
      [setAllShopCategoriesAction(statistic), setCategoryTreeAction(categoryTree)]
    );

    app.getByText(categoryInfo.name);
  });

  test('getCategories fn', () => {
    const categories = getCategories(categoryTree, [categoryInfo.hid, 5]);
    expect(categories.length).toEqual(2);
    expect(categories[1].name).toEqual('Неизвестная категория #5');
  });
});
