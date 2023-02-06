import { fireEvent } from '@testing-library/react';
import React from 'react';

import { shopModel, categoryData } from 'src/test/data';
import { setupWithReatom, useDescribeCategoryTree } from 'src/test/withReatom';
import { ShopCategoryBlock } from './ShopCategoryBlock';

const TestApp = () => {
  useDescribeCategoryTree({ [categoryData.hid]: categoryData });
  return (
    <ShopCategoryBlock
      shopCategoryName={shopModel.shopCategoryName}
      shopCategory={{ totalModels: 1, marketCategories: [shopModel.marketCategoryId] }}
    />
  );
};

test('correct render ShopCategoryBlock', () => {
  const { app } = setupWithReatom(<TestApp />);

  const title = app.getByText(shopModel.shopCategoryName);
  // разворачиваем категорию магазина
  fireEvent.click(title);
  app.getByText(categoryData.name);
});
