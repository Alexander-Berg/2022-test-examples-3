import React from 'react';

import { categoryInfo } from 'src/test/data';
import { setupWithReatom } from 'src/test/withReatom';
import { CategoryTreeItem } from './CategoryTreeItem';
import { CategoryColumn } from './types';
import { fireEvent } from '@testing-library/react';

export const categoryStatColumns: CategoryColumn[] = [
  {
    title: 'Товаров в категории',
    key: 'total',
    formatter: (value: any) => value.hid,
  },
];

describe('CategoryTreeItem', () => {
  test('render children', () => {
    const onSelect = jest.fn();
    const children = { ...categoryInfo, hid: 1, name: 'children category' };
    const { app } = setupWithReatom(
      <CategoryTreeItem
        category={{ ...categoryInfo, isLeaf: false }}
        categoryTree={{ [categoryInfo.hid]: [children] }}
        columns={[]}
        onSelect={onSelect}
      />
    );

    fireEvent.click(app.getByText(categoryInfo.name));
    app.getByText(children.name);
  });

  test('render without columns', () => {
    const onSelect = jest.fn();
    const { app } = setupWithReatom(
      <CategoryTreeItem
        category={categoryInfo}
        categoryTree={{ [categoryInfo.hid]: [] }}
        columns={[]}
        onSelect={onSelect}
      />
    );

    app.getByText(categoryInfo.name);
  });

  test('render with columns', () => {
    const onSelect = jest.fn();
    const { app } = setupWithReatom(
      <CategoryTreeItem
        category={categoryInfo}
        categoryTree={{ [categoryInfo.hid]: [] }}
        columns={categoryStatColumns}
        onSelect={onSelect}
      />
    );

    app.getByText(categoryInfo.name);

    fireEvent.click(app.getByText(/Выбрать/i));

    expect(onSelect.mock.calls.length).toEqual(1);
  });
});
