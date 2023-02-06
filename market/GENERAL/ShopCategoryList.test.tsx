import React, { FC } from 'react';
import userEvent from '@testing-library/user-event';

import { categoryInfo, shopModel } from 'src/test/data';
import { ShopCategoryList, ShopCategoryListProps } from './ShopCategoryList';
import { setupWithReatom, useDescribeCategoryTree, useDescribeMarketToShopCategories } from 'src/test/withReatom';

const categoryModels = [{ ...shopModel, marketCategoryId: categoryInfo.hid }];
const categoryTree = { [categoryInfo.hid]: categoryInfo };

const TestApp: FC<ShopCategoryListProps> = props => {
  useDescribeCategoryTree(categoryTree);
  useDescribeMarketToShopCategories([
    {
      shopCategoryName: shopModel.shopCategoryName,
      marketCategoryId: shopModel.marketCategoryId,
      shopModelTotal: 1,
    },
  ]);
  return <ShopCategoryList {...props} />;
};

describe('ShopCategoryList', () => {
  test('render', () => {
    const { app } = setupWithReatom(<TestApp models={categoryModels} categoryTree={categoryTree} filter={{}} />);

    userEvent.click(app.getByText(shopModel.shopCategoryName));
    app.getByText(categoryInfo.name);
  });
});
