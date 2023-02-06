import React, { FC, useState } from 'react';
import userEvent from '@testing-library/user-event';

import { categoryInfo, categoryStat, shopModel } from 'src/test/data';
import {
  setupWithReatom,
  useDescribeCategoryTree,
  useDescribeShopModels,
  useDescribeCategoryStat,
} from 'src/test/withReatom';
import { CategorySelector, CategorySelectorProps } from './CategorySelector';
import { TestingRouter } from 'src/test/setupApp';
import { ROOT_CATEGORY_HID } from 'src/constants';
import { CategoryColumn } from '../CategoryTreeItem';

export const categoryStatColumns: CategoryColumn[] = [
  {
    title: 'Товаров в категории',
    key: 'total',
    formatter: (value: any) => value.hid,
  },
];

const TestApp: FC<Partial<CategorySelectorProps>> = ({ columns, onChange }) => {
  const [value, setValue] = useState<number>();
  const onChangeHandler = (category: number) => {
    onChange!(category);
    setValue(category);
  };
  useDescribeShopModels([{ ...shopModel, marketCategoryId: categoryInfo.hid }]);
  useDescribeCategoryTree({
    [ROOT_CATEGORY_HID]: { ...categoryInfo, hid: ROOT_CATEGORY_HID, parentHid: -1, name: 'Все товары', isLeaf: false },
    [categoryInfo.hid]: { ...categoryInfo, parentHid: ROOT_CATEGORY_HID },
  });
  useDescribeCategoryStat([{ ...categoryStat, hid: ROOT_CATEGORY_HID }, categoryStat]);
  return <CategorySelector columns={columns!} value={value} onChange={onChangeHandler} />;
};

describe('CategorySelector', () => {
  test('render', () => {
    const { app } = setupWithReatom(<TestApp columns={[]} />);
    app.getByText(/Выберите категорию/i);
  });

  test('select category', () => {
    const onChange = jest.fn();
    const { app } = setupWithReatom(
      <TestingRouter route="/parameters">
        <TestApp columns={categoryStatColumns} onChange={onChange} />
      </TestingRouter>
    );
    // изначально категория не выбрана
    app.getByText(/Выберите категорию/i);
    // рутовая категория
    app.getByText(/Все товары/i);
    // отображается ли дочерня
    app.getByText(categoryInfo.name);
    // выбираем категорию
    const selectedButtons = app.getAllByText(/выбрать/i);
    userEvent.click(selectedButtons[0]);

    expect(app.queryByText(/Выберите категорию/i)).toBeFalsy();
  });

  test('search category', async () => {
    const onChange = jest.fn((categoryId: number) => {
      expect(categoryId).toBe(categoryInfo.hid);
    });

    const { app } = setupWithReatom(
      <TestingRouter route="/parameters">
        <TestApp columns={categoryStatColumns} onChange={onChange} />
      </TestingRouter>
    );

    // ищем категорию
    userEvent.type(app.getByRole('textbox'), 'мусаты');
    // находим, выбираем
    await app.findByText(/Мусаты, точилки, точильные камни/i);
    userEvent.click(app.getAllByText('Выбрать')[1]);
  });
});
