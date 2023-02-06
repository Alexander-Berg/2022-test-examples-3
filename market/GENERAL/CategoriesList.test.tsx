import React from 'react';

import { categoryInfo, shopModel } from 'src/test/data';
import { CategoriesList, EMPTY_MODELS, LOAD_DATA_TEXT } from './CategoriesList';
import { setupWithReatom } from 'src/test/withReatom';
import { setAllShopCategoriesAction, shopCategoriesStatAtom } from '../../store/shopCategoriesStat.atom';
import { categoryTreeAtom, setCategoryTreeAction } from 'src/store/categories';
import { TestingRouter } from 'src/test/setupApp';

const categoryModels = [{ ...shopModel, marketCategoryId: categoryInfo.hid }];
const categoryTree = { [categoryInfo.hid]: categoryInfo };

const categories = [
  {
    shopCategoryName: shopModel.shopCategoryName,
    marketCategoryId: shopModel.marketCategoryId,
    shopModelTotal: 1,
  },
];

const atoms = { shopCategoriesStatAtom, categoryTreeAtom };
const actions = [setCategoryTreeAction(categoryTree)];
describe('CategoriesList', () => {
  test('without models', () => {
    const { app } = setupWithReatom(
      <TestingRouter route="/categories?groupByMarketCategory=1">
        <CategoriesList />
      </TestingRouter>,
      atoms,
      actions
    );

    app.getByText(EMPTY_MODELS);
  });

  test('without categories tree', () => {
    const { app } = setupWithReatom(
      <TestingRouter route="/categories?groupByMarketCategory=1">
        <CategoriesList />
      </TestingRouter>,
      atoms,
      [setCategoryTreeAction(null)]
    );

    app.getByText(LOAD_DATA_TEXT);
  });

  test('show only market categories', () => {
    const { app } = setupWithReatom(
      <TestingRouter route="/categories?groupByMarketCategory=1">
        <CategoriesList />
      </TestingRouter>,
      atoms,
      [setAllShopCategoriesAction(categories), ...actions]
    );

    app.getByText(categoryInfo.name);
  });

  test('show shop categpries', async () => {
    const { app } = setupWithReatom(
      <TestingRouter route="/categories?groupByMarketCategory=0">
        <CategoriesList />
      </TestingRouter>,
      atoms,
      [setAllShopCategoriesAction(categories), ...actions]
    );

    app.getByText(categoryModels[0].shopCategoryName);
  });
});
