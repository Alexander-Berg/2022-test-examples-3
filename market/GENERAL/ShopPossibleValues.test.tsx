import React, { FC } from 'react';

import { ShopPossibleValues, ShopPossibleValuesProps } from './ShopPossibleValues';
import { shopModel, categoryData, simpleMapping } from 'src/test/data';
import {
  setupWithReatom,
  useDescribeAllModels,
  useDescribeCategoryData,
  useDescribeFilterData,
} from 'src/test/withReatom';

const TestApp: FC<ShopPossibleValuesProps> = props => {
  useDescribeAllModels([shopModel]);
  const currentCategoryData = useDescribeCategoryData(categoryData);
  useDescribeFilterData({ marketCategoryId: shopModel.marketCategoryId });

  return currentCategoryData ? <ShopPossibleValues {...props} /> : <></>;
};

describe('ShopPossibleValues', () => {
  test('render', async () => {
    const { app } = setupWithReatom(<TestApp mapping={simpleMapping} />);

    // отображение возможных значений
    app.getByText(new RegExp(shopModel.shopValues.vendor, 'i'));
  });
});
