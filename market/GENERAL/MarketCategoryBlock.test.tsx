import React from 'react';
import { fireEvent } from '@testing-library/react';
import { categoryTreeAtom, setCategoryTreeAction } from 'src/store/categories';
import { resolveLoadModelsRequest } from 'src/test/api/resolves';

import { shopModel, categoryData, categoryInfo } from 'src/test/data';
import { setupWithReatom } from 'src/test/withReatom';
import { setAllShopCategoriesAction, shopCategoriesStatAtom } from '../../store/shopCategoriesStat.atom';
import { MarketCategoryBlock } from './MarketCategoryBlock';

const catStat = [
  {
    shopCategoryName: shopModel.shopCategoryName,
    marketCategoryId: categoryInfo.hid,
    shopModelTotal: 1,
  },
];

test('correct render MarketCategoryBlock', async () => {
  const { app, api } = setupWithReatom(
    <MarketCategoryBlock category={categoryInfo} />,
    { categoryTreeAtom, shopCategoriesStatAtom },
    [setAllShopCategoriesAction(catStat), setCategoryTreeAction({ [categoryData.hid]: categoryInfo })]
  );
  fireEvent.click(app.getByText(categoryData.name));

  resolveLoadModelsRequest(api, [shopModel]);
  await app.findByText(shopModel.name);
});
