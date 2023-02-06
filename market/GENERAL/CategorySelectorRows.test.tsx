import React from 'react';
import userEvent from '@testing-library/user-event';
import { waitFor } from '@testing-library/react';

import {
  categoriesStatMapAtom,
  categoryTreeAtom,
  setCategoryStatisticAction,
  setCategoryTreeAction,
} from 'src/store/categories';
import { setupWithReatom } from 'src/test/withReatom';
import { CategoryColumn } from '../CategoryTreeItem/types';
import { CategorySelectorRows } from './CategorySelectorRows';
import { categoryInfo, categoryStat, shopModel } from 'src/test/data';
import { ROOT_CATEGORY_HID } from 'src/constants';
import { DEFAULT_SORT } from './ColumnHeader';

import { resolveLoadModelsRequest } from 'src/test/api/resolves';

export const categoryStatColumns: CategoryColumn[] = [
  {
    title: 'Товаров в категории',
    key: 'total',
    formatter: (value: any) => value.hid,
  },
];

const categoryTree = {
  [ROOT_CATEGORY_HID]: { ...categoryInfo, hid: ROOT_CATEGORY_HID, parentHid: -1, name: 'Все товары', isLeaf: false },
  [categoryInfo.hid]: { ...categoryInfo, parentHid: ROOT_CATEGORY_HID },
};

const atoms = { categoryTreeAtom, categoriesStatMapAtom };
const actions = [
  setCategoryTreeAction(categoryTree),
  setCategoryStatisticAction([{ ...categoryStat, hid: ROOT_CATEGORY_HID }, categoryStat]),
];

describe('<CategorySelectorRows />', () => {
  test('model with no exist category', async () => {
    const onSelectCategory = jest.fn();
    const { app, api } = setupWithReatom(
      <CategorySelectorRows columns={categoryStatColumns} onSelect={onSelectCategory} sortState={DEFAULT_SORT} />,
      atoms,
      actions
    );

    userEvent.type(app.getByRole('textbox'), '90666');

    // немного ждем дебаунс
    await waitFor(() => expect(api.allActiveRequests).not.toEqual({}));

    resolveLoadModelsRequest(api, [{ ...shopModel, marketCategoryId: 0 }]);

    // компонент не должен крешится просто должен выводить пустой список категорий
    await waitFor(() => expect(app.queryByText(categoryInfo.name)).toBeFalsy());
  });

  test('search category by hid', async () => {
    const onSelectCategory = jest.fn();

    const { app } = setupWithReatom(
      <CategorySelectorRows columns={categoryStatColumns} onSelect={onSelectCategory} sortState={DEFAULT_SORT} />,
      atoms,
      actions
    );

    // ищем категорию по hid
    userEvent.type(app.getByRole('textbox'), categoryInfo.hid.toString());

    await waitFor(() => expect(app.queryByText(categoryInfo.name)).toBeTruthy());
  });

  test('search category by model sku', async () => {
    const onSelectCategory = jest.fn();

    const { app, api } = setupWithReatom(
      <CategorySelectorRows columns={categoryStatColumns} onSelect={onSelectCategory} sortState={DEFAULT_SORT} />,
      atoms,
      actions
    );

    // вставляем sku товара
    userEvent.type(app.getByRole('textbox'), '90666');

    // немного ждем дебаунс
    await waitFor(() => expect(api.allActiveRequests).not.toEqual({}));

    // что бы понять какая категория нужна, поиск идет в ручку за товаром что бы вытянуть из него ид категории
    // тут как раз и резолвим товар с нужной категорией
    resolveLoadModelsRequest(api, [{ ...shopModel, marketCategoryId: categoryInfo.hid }]);

    // должна отображаться категория этого товара
    await waitFor(() => expect(app.queryByText(categoryInfo.name)).toBeTruthy());
  });
});
