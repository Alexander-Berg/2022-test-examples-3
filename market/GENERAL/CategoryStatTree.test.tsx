import React, { FC } from 'react';
import userEvent from '@testing-library/user-event';

import { categoryInfo, categoryStat, shopModel } from 'src/test/data';
import {
  setupWithReatom,
  useDescribeCategoryTree,
  useDescribeShopModels,
  useDescribeCategoryStat,
} from 'src/test/withReatom';
import { CategoryStatTree } from './CategoryStatTree';
import { ROOT_CATEGORY_HID } from 'src/constants';
import { UiCategoryStat } from 'src/utils/types';

const statCategories: UiCategoryStat[] = [
  { ...categoryInfo, ...categoryStat, parentHid: 2, name: 'leafCategory', ...categoryStat },
  { ...categoryInfo, ...categoryStat, parentHid: ROOT_CATEGORY_HID, hid: 2, name: 'parentCategory', isLeaf: false },
  {
    ...categoryInfo,
    ...categoryStat,
    hid: ROOT_CATEGORY_HID,
    parentHid: 0,
    name: 'Все товары',
    isLeaf: false,
    total: 30000,
  },
];

const TestApp: FC<{ onSelect: () => void }> = ({ onSelect }) => {
  useDescribeShopModels([{ ...shopModel, marketCategoryId: categoryInfo.hid }]);
  useDescribeCategoryTree({
    [categoryInfo.hid]: statCategories[0],
    2: statCategories[1],
    [ROOT_CATEGORY_HID]: statCategories[2],
  });
  useDescribeCategoryStat(statCategories);
  return <CategoryStatTree columns={[]} onSelect={onSelect} />;
};

describe('CategoryTree', () => {
  test('collapse item', () => {
    const onSelect = jest.fn();
    const { app } = setupWithReatom(<TestApp onSelect={onSelect} />);

    userEvent.click(app.getByText('parentCategory'));
    app.getByText('leafCategory');

    userEvent.click(app.getByText('parentCategory'));
    expect(app.queryByText('leafCategory')).toBeFalsy();
  });

  // раскоментить когда приедет статистика
  // test('warning about large models', async () => {
  //   const onSelect = jest.fn();
  //   const { app } = setupWithReatom(<TestApp onSelect={onSelect} />);

  //   const selectRoot = app.getAllByText(/Выбрать/i)[0];
  //   userEvent.click(selectRoot);

  //   await waitFor(() => {
  //     expect(app.getByText(/В категории много товаров/)).toBeTruthy();
  //   });
  // });
});
